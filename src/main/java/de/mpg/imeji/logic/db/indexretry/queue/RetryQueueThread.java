package de.mpg.imeji.logic.db.indexretry.queue;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.config.Imeji;
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

public class RetryQueueThread implements Runnable {


  private File retryQueueFile;
  private HashMap<URI, RetryBaseRequest> retryQueue;
  // read from configuration file
  private int maxSize = 2 ^ 10;

  private static long SLEEP = 10000; // 900000; // quarter of a hour
  private static final Logger LOGGER = LogManager.getLogger(RetryQueueThread.class);



  public RetryQueueThread() {
    this.retryQueue = new HashMap<URI, RetryBaseRequest>();
  }


  @Override
  public void run() {

    while (true) {

      if (ElasticDocumentSearch.pingElasticSearch()) {
        indexQueue();
        if (this.retryQueue.isEmpty()) {
          LOGGER.error("retryQueue isEmpty, exit");
          break;
        }
      }

      try {
        LOGGER.error("RetryQueueThread going to sleep");
        Thread.sleep(SLEEP);
        LOGGER.error("RetryQueueThread waking up");
      } catch (InterruptedException e) {
        LOGGER.error(e.getMessage());
      }
    }

  }

  /**
   * Add a list of retry request to the queue
   * 
   * @param requests
   */
  protected void addRetryIndexRequests(List<RetryBaseRequest> requests) {

    for (RetryBaseRequest request : requests) {
      addRetryIndexRequest(request);
    }
  }


  /**
   * Add a retry request to the queue
   * 
   * @param request
   */
  protected void addRetryIndexRequest(RetryBaseRequest request) {

    URI requestUri = request.getUri();
    // (a) requests for uris that are not in the queue yet
    if (!this.retryQueue.containsKey(requestUri)) {
      putIntoQueue(request);
    }
    // (b) requests for uris that are already in the queue
    else {
      RetryBaseRequest storedRequest = this.retryQueue.get(requestUri);
      if (request instanceof RetryIndexRequest && storedRequest instanceof RetryDeleteFromIndexRequest) {
        LOGGER.error("Error in RetryQueue: trying to add write request for an object after a previous delete request.");
      } else if (request instanceof RetryDeleteFromIndexRequest) {
        // replace
        putIntoQueue(request);
      }
    }
  }

  /**
   * Put (add new or overwrite previous) request into queue
   * 
   * @param request
   */
  private void putIntoQueue(RetryBaseRequest request) {
    this.retryQueue.put(request.getUri(), request);
    // in case queue is large, save to file and empty queue
    if (this.retryQueue.size() >= this.maxSize) {
      saveQueueToFile();
    }
  }



  /**
   * Save all current requests in the queue to file
   */
  private void saveQueueToFile() {
    // weil durch andere threads weitere requests reinkommen können, müssen wir hier blockieren bis schreiben fertig

  }

  /**
   * 
   */
  private void readFileIntoQueue() {


  }



  /**
   * "Retry": Write all requests in the queue to search index.
   */
  private void indexQueue() {

    // take queue and index	
    // make sure that ES does work first ..
    Collection<RetryBaseRequest> retryRequests = retryQueue.values();

    WriterFacade writer = new WriterFacade();
    // wir müssten die objekte nach art sortieren, und dann batch index machen 		
    for (RetryBaseRequest retryRequest : retryRequests) {

      long currentDocumentVersionInElasticSearch = ElasticDocumentSearch.searchVersionOfDocument(retryRequest.getUri());
      // a) request to re-index an object
      if (retryRequest instanceof RetryIndexRequest) {
        // document does not exist in ES (any more/yet) or version could not be obtained
        if (currentDocumentVersionInElasticSearch == ElasticDocumentSearch.VERSION_NOT_FOUND) {
          copyObjectToSeachIndex((RetryIndexRequest) retryRequest, writer);
        }
        //  since fail time no new copy of the document has been indexed
        else if (currentDocumentVersionInElasticSearch < retryRequest.getFailTime().getTimeInMillis()) {
          copyObjectToSeachIndex((RetryIndexRequest) retryRequest, writer);
        }
      }
      // b) request to delete an object/document from index
      else {
        if (currentDocumentVersionInElasticSearch != ElasticDocumentSearch.VERSION_NOT_FOUND) {
          deleteDocumentFromIndex((RetryDeleteFromIndexRequest) retryRequest, writer);
        }
      }
    }
  }


  /**
   * Given a RetryIndexRequest, read the object from database and write it to search index
   * 
   * @param retryRequest
   */
  private void copyObjectToSeachIndex(RetryIndexRequest retryRequest, WriterFacade writer) {

    LOGGER.error("copyObjectToSeachIndex");

    Object objectToRead;
    try {
      Class clazz = retryRequest.getObjectClass();
      Constructor defaultConstructor = clazz.getConstructor();
      objectToRead = defaultConstructor.newInstance();
      String modelName = ImejiInitializer.getModelName(retryRequest.getObjectClass());
      Reader databaseReader = ReaderFactory.create(modelName);
      Object objectInDatabase = databaseReader.read(retryRequest.getUri().toString(), Imeji.adminUser, objectToRead);
      // copy to search index
      try {
        writer.indexObject(objectInDatabase);
        // delete from queue if successful
        retryQueue.remove(retryRequest.getUri());
      } catch (Exception e) {
        // retryRequest stays in queue if not successful
        // what could have happened, why was it not successful?
        // if ES is down stop processing .. and retry later on
        LOGGER.error("try to re index, didn't work " + e.getMessage());
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
  private void deleteDocumentFromIndex(RetryDeleteFromIndexRequest request, WriterFacade writer) {

    LOGGER.error("deleteDocumentFromIndex");

    try {
      writer.deleteObjectFromIndex(request.getObjectToDelete());
      retryQueue.remove(request.getUri());
    } catch (Exception e) {

      // a) document does not exist? 
      // b) Elastic Search is down
      // retryRequest stays in queue if not successful
      // what could have happened, why was it not successful?
      // if ES is down stop processing .. and retry later on
    }
  }

}
