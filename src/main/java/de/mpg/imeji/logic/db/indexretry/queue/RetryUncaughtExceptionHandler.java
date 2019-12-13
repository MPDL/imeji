package de.mpg.imeji.logic.db.indexretry.queue;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Handles unchecked exceptions (and errors?) thrown by/within RetryQueueRunnable.
 * 
 * @author breddin
 *
 */
public class RetryUncaughtExceptionHandler implements Thread.UncaughtExceptionHandler {

  private static final Logger LOGGER = LogManager.getLogger(RetryUncaughtExceptionHandler.class);

  @Override
  public void uncaughtException(Thread thread, Throwable throwable) {

    LOGGER.error("Uncaught exception " + throwable.getClass().toString() + " in retry queue thread " + thread.getId() + ". Thread died.");
    LOGGER.error("Uncaught exception message " + throwable.getMessage());
    RetryQueue currentRetryQueue = RetryQueue.getInstance();
    if (currentRetryQueue != null) {
      try {
        currentRetryQueue.saveQueueToFile();
      } catch (IOException e) {
        LOGGER.error("Retry queue thread terminated due to error. Could not save existing requests to file. Reason: " + e.getMessage());
      }
    }
  }

}
