package de.mpg.imeji.logic.db.indexretry.queue;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.config.Imeji;
import de.mpg.imeji.logic.config.ImejiConfiguration;
import de.mpg.imeji.logic.db.indexretry.model.RetryBaseRequest;
import de.mpg.imeji.logic.db.indexretry.model.RetryDeleteFromIndexRequest;
import de.mpg.imeji.logic.db.indexretry.model.RetryIndexRequest;
import de.mpg.imeji.logic.db.reader.Reader;
import de.mpg.imeji.logic.db.reader.ReaderFactory;
import de.mpg.imeji.logic.db.writer.WriterFacade;
import de.mpg.imeji.logic.init.ImejiInitializer;
import de.mpg.imeji.logic.search.elasticsearch.ElasticDocumentSearch;


/**
 * If writing to search index fails within the regular program, so called index requests are stored
 * in this queue. The thread starts when the first request is submitted and exits when all stored
 * requests have successfully been written to search index.
 * 
 * @author breddin
 *
 */
public class RetryQueueRunnable implements Runnable {


  private static final Logger LOGGER = LogManager.getLogger(RetryQueueRunnable.class);

  private final RetryQueue retryQueueReference;
  private final Map<URI, RetryBaseRequest> retryQueue;

  /**
   * Constructor for RetryQueueRunnable
   */
  public RetryQueueRunnable(RetryQueue retryQueueReference) {

    this.retryQueueReference = retryQueueReference;
    this.retryQueue = retryQueueReference.retryQueue;
  }

  @Override
  public void run() {

    // (re-) load sleep time from configuration
    long threadSleepTime = getSleepTime();
    // instantiate executor service in order to handle each connection to Elastic Search in own thread 
    ExecutorService executor = Executors.newSingleThreadExecutor();

    while (true) {

      if (elasticSearchUpAndRunning(executor)) {

        synchronized (this.retryQueueReference) {
          if (this.retryQueueReference.moreRequestsExist()) {
            indexQueue(executor);
          } else {
            LOGGER.info("retryQueue isEmpty, no further requests (stored in files) exist, thread terminates");
            executor.shutdown();
            // detach thread from RetryQueue
            this.retryQueueReference.detachRetryQueueThread();
            break;
          }
        }
      } else {
        try {
          LOGGER.info("retryQueue is going to sleep");
          Thread.sleep(threadSleepTime);
          LOGGER.info("retryQueue is waking up");
        } catch (InterruptedException e) {
          LOGGER.info("Retry Queue Thread interrupted, terminates");
          executor.shutdown();
          return;
        }
      }
    }
  }

  /**
   * Reads sleep time from configuration and returns it.
   * 
   * @return
   */
  private long getSleepTime() {
    String sleepTime = Imeji.CONFIG.getRetryQueueSleepTime();
    if (sleepTime.isEmpty()) {
      sleepTime = ImejiConfiguration.DEFAULT_RETRY_QUEUE_SLEEP_MIL_SEC;
    }
    return Long.parseLong(sleepTime);
  }

  /**
   * Create a thread that checks whether Elastic Search is up an running and cluster health is
   * Yellow.
   * 
   * @return
   */
  private boolean elasticSearchUpAndRunning(ExecutorService executor) {

    try {

      Callable<Boolean> findElasticSearchUpAndRunning = () -> {
        // check if we can connect to Elastic Search
        if (ElasticDocumentSearch.pingElasticSearch()) {
          // check cluster health
          ElasticDocumentSearch.getClusterHealthYellow();
          LOGGER.info("elasticSearch yellow health ");
          return Boolean.TRUE;
        }
        LOGGER.info("elasticSearch does not answer ping ");
        return Boolean.FALSE;
      };

      executor.submit(findElasticSearchUpAndRunning).get();
      LOGGER.info("elasticSearchUpAndRunning");
      return true;
    }
    // in case of exceptions, return false   
    catch (InterruptedException | ExecutionException e) {
      LOGGER.info("elasticSearch not UpAndRunning " + e.getMessage());
      return false;
    }
  }



  /**
   * "Retry": Write all requests in the queue to search index. In case the queue is empty, look if
   * there are files with stored retry requests, read and index these.
   */
  private void indexQueue(ExecutorService executor) {

    // take queue and index	
    // make sure that ES does work first ..
    Collection<RetryBaseRequest> retryRequests = retryQueue.values();
    List<URI> deleteObjectsFromQueue = new ArrayList<URI>(this.retryQueue.size());

    WriterFacade writer = new WriterFacade();
    // wir müssten die objekte nach art sortieren, und dann batch index machen 
    // try to copy request  objects to index or delete request objects from index
    for (RetryBaseRequest retryRequest : retryRequests) {

      long currentDocumentVersionInElasticSearch = getDocumentVersion(retryRequest.getUri(), executor);
      // a) request to re-index an object
      if (retryRequest instanceof RetryIndexRequest) {
        // document does not exist in ES (any more/yet) or version could not be obtained
        if (currentDocumentVersionInElasticSearch == ElasticDocumentSearch.VERSION_NOT_FOUND) {
          copyObjectToSeachIndex((RetryIndexRequest) retryRequest, writer, deleteObjectsFromQueue);
        }
        // since fail time no new copy of the document has been indexed
        else if (currentDocumentVersionInElasticSearch <= retryRequest.getFailTime().getTimeInMillis()) {
          copyObjectToSeachIndex((RetryIndexRequest) retryRequest, writer, deleteObjectsFromQueue);
        } else {
          deleteObjectsFromQueue.add(retryRequest.getUri());
        }
      }
      // b) request to delete an object/document from index
      else {
        if (currentDocumentVersionInElasticSearch != ElasticDocumentSearch.VERSION_NOT_FOUND) {
          deleteDocumentFromIndex((RetryDeleteFromIndexRequest) retryRequest, writer, deleteObjectsFromQueue);
        } else {
          deleteObjectsFromQueue.add(retryRequest.getUri());
        }
      }
    }

    // remove all successfully indexed/deleted objects from queue
    for (URI deleteObjectFromQueue : deleteObjectsFromQueue) {
      this.retryQueue.remove(deleteObjectFromQueue);
    }
  }

  /**
   * Create a thread that checks for a given uid whether a document with the uid exists in Elastic
   * Search. Returns the version of the document if it exists.
   * 
   * @return
   */
  private long getDocumentVersion(URI documentId, ExecutorService executor) {

    try {
      Callable<Long> getDocumentVersionTask = () -> {
        long documentVersion = ElasticDocumentSearch.searchVersionOfDocument(documentId);
        return Long.valueOf(documentVersion);
      };

      Long documentVersion = executor.submit(getDocumentVersionTask).get();
      return documentVersion.longValue();
    }
    // in case of exceptions, return false   
    catch (InterruptedException | ExecutionException e) {
      return ElasticDocumentSearch.VERSION_NOT_FOUND;
    }
  }



  /**
   * Given a RetryIndexRequest, read the object from database and write it to search index.
   * 
   * @param retryRequest
   */
  private void copyObjectToSeachIndex(RetryIndexRequest retryRequest, WriterFacade writer, List<URI> deleteObjectsFromQueue) {

    LOGGER.info("copyObjectToSeachIndex");

    Object objectToRead;
    try {
      Class clazz = retryRequest.getObjectClass();
      Constructor defaultConstructor = clazz.getConstructor();
      objectToRead = defaultConstructor.newInstance();
      String modelName = ImejiInitializer.getJenaModelName(retryRequest.getObjectClass());
      Reader databaseReader = ReaderFactory.create(modelName);
      Object objectInDatabase = databaseReader.read(retryRequest.getUri().toString(), Imeji.adminUser, objectToRead);
      // copy to search index
      try {
        writer.indexObject(objectInDatabase);
        deleteObjectsFromQueue.add(retryRequest.getUri());
      } catch (ExecutionException | InterruptedException e) {
        // if there was an unchecked exception during indexing, this is caught in the ExecutionException  	  
        // retryRequest stays in queue if not successful
        LOGGER.error("try to re-index, didn't work " + e.getMessage());
      }
    } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException
        | SecurityException exception) {
      // all exceptions are thrown by command: retryRequest.getObjectClass().getDeclaredConstructor().newInstance();
      LOGGER.error(exception);
    } catch (ImejiException e) {
      // von databaseReader.read
      // z.B. falls object inzwischen gelöscht wurde
      // z.B. falls Jena down ist
      LOGGER.error(e);
    }
  }

  /**
   * Delete an object from search index
   * 
   * @param request
   * @param writer
   */
  private void deleteDocumentFromIndex(RetryDeleteFromIndexRequest request, WriterFacade writer, List<URI> deleteObjectsFromQueue) {

    LOGGER.info("deleteDocumentFromIndex");

    try {
      writer.deleteObjectFromIndex(request.getObjectToDelete());
      deleteObjectsFromQueue.add(request.getUri());
    } catch (ExecutionException | InterruptedException exception) {
      LOGGER.error("Could not delete document from index, reason: " + exception.getMessage());
    }
  }

}
