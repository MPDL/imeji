package de.mpg.imeji.util;

import static org.awaitility.Awaitility.await;

import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.concurrent.ThreadPoolExecutor;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Utility/Helper class for concurrency utilities.
 * 
 * @author helk
 *
 */
public final class ConcurrencyUtil {

  private static final Logger LOGGER = LogManager.getLogger(ConcurrencyUtil.class);

  private ConcurrencyUtil() {
    //Should not be instantiated.
  }

  /**
   * Wait for all threads of all {@code ThreadPoolExecutors} to complete.
   * 
   * @param threadPoolExecutors Collection of {@code ThreadPoolExecutors} to wait for.
   */
  public static void waitForThreadsToComplete(Collection<ThreadPoolExecutor> threadPoolExecutors) {
    LOGGER.info("Wait for Threads to compelete...");

    try {
      LOGGER.info("Iterating over " + threadPoolExecutors.size() + " ThreadPoolExecutors.");
      for (ThreadPoolExecutor threadPoolExecutor : threadPoolExecutors) {
        waitForThreadsToComplete(threadPoolExecutor);
      }
    } catch (ConcurrentModificationException e) {
      LOGGER.info("Catched a ConcurrentModificationException! -> Restart iterating over the ThreadPoolExecutors:");
      waitForThreadsToComplete(threadPoolExecutors);
    }

    LOGGER.info("Threads compeleted.");
  }

  /**
   * Wait for all threads of {@code ThreadPoolExecutor} to complete.
   * 
   * @param threadPoolExecutor the {@code ThreadPoolExecutor} to wait for.
   */
  public static void waitForThreadsToComplete(ThreadPoolExecutor threadPoolExecutor) {
    await().until(() -> threadPoolExecutor.getTaskCount() == threadPoolExecutor.getCompletedTaskCount());
  }

}
