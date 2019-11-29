package de.mpg.imeji.logic.db.indexretry.queue;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import de.mpg.imeji.logic.db.indexretry.model.RetryBaseRequest;



/**
 * RetryQueue administers a single thread that stores "index requests". When writing to search index
 * fails within the regular program (i.e. due to index being down), so called index requests are
 * stored in the queue. Thread periodically checks whether search index is up and working and if so,
 * it indexes the requests.
 * 
 * @author breddin
 *
 */
public class RetryQueue {


  private static ExecutorService executorService;
  private static RetryQueue singletonRetryQueue;

  private RetryQueueThread retryQueueThread;


  /**
   * Constructor for RetryQueue. Singleton pattern.
   */
  private RetryQueue() {

    executorService = Executors.newSingleThreadExecutor(new RetryThreadFactory());
    this.retryQueueThread = new RetryQueueThread();
    executorService.execute(this.retryQueueThread);

  }


  /**
   * Returns a reference to a RetryQueue object
   * 
   * @return
   */
  public static RetryQueue getInstance() {

    if (singletonRetryQueue == null) {
      singletonRetryQueue = new RetryQueue();
    }
    return singletonRetryQueue;
  }

  /**
   * Add a single retry request
   * 
   * @param request
   */
  public void addRetryRequest(RetryBaseRequest request) {
    this.retryQueueThread.addRetryIndexRequest(request);
  }

  /**
   * Add a list of retry requests
   * 
   * @param requests
   */
  public void addRetryIndexRequests(List<RetryBaseRequest> requests) {
    this.retryQueueThread.addRetryIndexRequests(requests);
  }


}
