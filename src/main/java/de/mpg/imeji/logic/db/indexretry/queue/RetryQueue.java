package de.mpg.imeji.logic.db.indexretry.queue;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URI;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.mpg.imeji.logic.config.Imeji;
import de.mpg.imeji.logic.db.indexretry.model.RetryBaseRequest;
import de.mpg.imeji.logic.db.indexretry.model.RetryDeleteFromIndexRequest;
import de.mpg.imeji.logic.db.indexretry.model.RetryIndexRequest;
import de.mpg.imeji.logic.util.StringHelper;



/**
 * RetryQueue administers a single thread that stores so called "index requests". If writing to
 * search index fails within the regular program (i.e. due to search index being down or other
 * problems), index requests are created and stored in a queue (hash map) that is administered by
 * this class. An index request holds information about the object (i.e. its uri) that could not be
 * copied to or deleted from the search index. The retry queue thread checks periodically if search
 * index is up and working and if so, it tries to re-index (copy from database or delete) the
 * requests stored in the queue.
 * 
 * @author breddin
 *
 */
public class RetryQueue {


  private static final String RETRY_QUEUE_FILE_NAME = "ReIndex_";
  private static final String RETRY_QUEUE_FILE_SUFFIX = ".bin";
  private static final String REGEX_FILE_NAME = RETRY_QUEUE_FILE_NAME + "[0-9]+[\\w\\W]+";	
	
  /**
   * HashMap storing retry requests
   */
  protected final Map<URI, RetryBaseRequest> retryQueue;
  // read from configuration file
  private int maxSizeOfRetryQueue = 2^10;
  
  private static RetryQueue singletonRetryQueue;
  private static final Logger LOGGER = LogManager.getLogger(RetryQueue.class);

  /**
   * Consumer thread that tries to index existing requests to elastic search
   */
  private final RetryQueueRunnable retryQueueRunnable;
  private Thread retryQueueThread;

  /**
   * Constructor for RetryQueue. Singleton pattern.
   */
  private RetryQueue() {

	this.retryQueue = Collections.synchronizedMap(new HashMap<URI, RetryBaseRequest>());
    this.retryQueueRunnable = new RetryQueueRunnable(this);
  }


  /**
   * Returns a reference to a RetryQueue object
   * 
   * @return
   */
  public static synchronized RetryQueue getInstance() {

    if (singletonRetryQueue == null) {
      singletonRetryQueue = new RetryQueue();
    }
    return singletonRetryQueue;
  }
  
  
  // -------------- section consumer thread, trying to index existing requests ------------------

  /**
   * In case the thread is currently not executing the runnable, start the thread.
   */
  private synchronized void activateThreadIfNotActive() {


    if (this.retryQueueThread == null) {
      this.retryQueueThread = new Thread(this.retryQueueRunnable);
      this.retryQueueThread.setUncaughtExceptionHandler(new RetryUncaughtExceptionHandler());
    }

    if (this.retryQueueThread.getState() == Thread.State.NEW) {
      this.retryQueueThread.start();
    } else if (this.retryQueueThread.getState() == Thread.State.TERMINATED) {
      this.retryQueueThread = null;
      activateThreadIfNotActive();
    }
  }

  
  // ------------- interface for producers (adding requests) --------------------------------

  /**
   * Add a list of retry request to the queue
   * 
   * @param requests
   */
  public synchronized void addRetryIndexRequests(List<RetryBaseRequest> requests) {

    for (RetryBaseRequest request : requests) {
      putRetryIndexRequest(request);
    }
    activateThreadIfNotActive();
    
  }
  
  /**
   * Add a single retry request.
   * 
   * @param request
   */
  public synchronized void addRetryRequest(RetryBaseRequest request) {
    putRetryIndexRequest(request);
    activateThreadIfNotActive();
  }

  /**
   * Add a retry request to the queue.
   * 
   * @param request
   */
  private synchronized void putRetryIndexRequest(RetryBaseRequest request) {

    URI requestUri = request.getUri();
    synchronized (this.retryQueue) {
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
  }

  /**
   * Put (add new or overwrite previous) request into queue.
   * 
   * @param request
   */
  private synchronized void putIntoQueue(RetryBaseRequest request) {

    synchronized (this.retryQueue) {
      if (this.retryQueue.size() + 1 > this.maxSizeOfRetryQueue) {
        try {
          // in case queue is large, save to file and empty queue
          saveQueueToFile();
          this.retryQueue.put(request.getUri(), request);
        } catch (Throwable t) {
          LOGGER.error(
              "Could not write retry requests to file. Queue is full and further requests will not be saved. Reason: " + t.getMessage());
        }
      } else {
        this.retryQueue.put(request.getUri(), request);
      }
    }
  }

   
  /**
   * Check if requests exist in queue or in a backup file
   * @return
   */
  public synchronized boolean moreRequestsExist() {
	  
	  synchronized(this.retryQueue) {
		  
		  if(this.retryQueue.isEmpty()) {
			  File latestRetryQueueFile = this.findLatestRetryQueueFile();
			  if(latestRetryQueueFile == null) {
				  return false;
			  }
			  else {
				  readFileIntoQueue(latestRetryQueueFile);
			  }
		  }
		  return true;
	  }
  }
  
  

  // ---------------- section backup file --------------------------------------------------

  /**
   * Get the name of the retry queue file.
   * 
   * @return
   */
  private String getRetryQueueFileName() {

    Calendar now = Calendar.getInstance();
    String nowInMilliSeconds = Long.toUnsignedString(now.getTimeInMillis());
    String fileName = RETRY_QUEUE_FILE_NAME + nowInMilliSeconds + RETRY_QUEUE_FILE_SUFFIX;
    return fileName;
  }


  /**
   * Searches for retry request files in the file system.
   * 
   * @return The most recently saved retry request file or null if no file is found.
   */
  private File findLatestRetryQueueFile() {

    File myDir = new File(StringHelper.normalizePath(Imeji.tdbPath));
    File[] filesInDir = myDir.listFiles();
    Long latestTime = Long.MIN_VALUE;
    File latestFile = null;
    if (filesInDir != null) {
      for (File file : filesInDir) {
        if (Pattern.matches(REGEX_FILE_NAME, file.getName())) {

          String fullFileName = file.getName();
          String timePartInName = fullFileName.substring(RETRY_QUEUE_FILE_NAME.length(), fullFileName.length() - 4);
          Long millisecondsInName = Long.parseLong(timePartInName);
          if (millisecondsInName > latestTime) {
            latestFile = file;
            latestTime = millisecondsInName;
          }
        }
      }
    }
    return latestFile;
  }

  /**
   * Save all current requests in the queue to file.
   * 
   * @throws IOException
   */
  public synchronized void saveQueueToFile() throws IOException {


    synchronized (this.retryQueue) {
      if (!this.retryQueue.isEmpty()) {
        try (ObjectOutputStream objectOutputStream =
            new ObjectOutputStream(new FileOutputStream(new File(StringHelper.normalizePath(Imeji.tdbPath) + getRetryQueueFileName())))) {

          objectOutputStream.writeObject(this.retryQueue);
          objectOutputStream.flush();
          this.retryQueue.clear();

        } catch (IOException e) {
          // re-throw exception in order to notify caller that queue was not emptied
          throw e;
        }
      }
    }
  }


  /**
   * Reads retry requests that are stored in a file from that file and puts them into retryQueue.
   */
  private void readFileIntoQueue(File retryQueueFile) {

    if (retryQueueFile != null) {

      synchronized (this.retryQueue) {
        // read file content into retryQueue 
        try (ObjectInputStream objectInputStream = new ObjectInputStream(new FileInputStream(retryQueueFile))) {
          this.retryQueue.putAll((Map<URI, RetryBaseRequest>) objectInputStream.readObject());
        } catch (ClassNotFoundException | IOException e) {
          LOGGER.error("Could not read retry requests from file. " + retryQueueFile.getName() + " Reason: " + e.getMessage());
          return;
        }

        // delete file
        try {
          if (!this.retryQueue.isEmpty()) {
            String pathString = retryQueueFile.getCanonicalPath();
            File filePath = new File(pathString);
            boolean success = filePath.delete();
            if (!success) {
              LOGGER.error("Could not delete retry request file. " + retryQueueFile.getName());
            }
          }
        } catch (IOException e) {
          LOGGER.error("Could not delete retry request file. " + retryQueueFile.getName() + " because " + e.getMessage());
        }
      }
    }
  }


}
