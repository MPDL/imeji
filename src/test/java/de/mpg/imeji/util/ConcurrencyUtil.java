package de.mpg.imeji.util;

import static org.awaitility.Awaitility.await;

import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.concurrent.ThreadPoolExecutor;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.mpg.imeji.logic.config.Imeji;

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
   * Wait for all threads of the {@code ThreadPoolExecutors} to complete.
   * 
   * @param threadPoolExecutors the {@code ThreadPoolExecutors} to wait for.
   */
  public static void waitForThreadsToComplete(ThreadPoolExecutor... threadPoolExecutors) {
    LOGGER.info("Waiting for Threads of ThreadPoolExecutors to compelete...");
    for (ThreadPoolExecutor threadPoolExecutor : threadPoolExecutors) {
      await().until(() -> threadPoolExecutor.getTaskCount() == threadPoolExecutor.getCompletedTaskCount());
    }
    LOGGER.info("Threads compeleted.");
  }

  /**
   * Wait for the imeji threads of the accessible threadpools to complete.
   */
  public static void waitForImejiThreadsToComplete() {
    //TODO: Wait for further ThreadPools not added yet => Make ThreadPools (like WriterFacade.executor) accessible (public static), if functional possible!
    ConcurrencyUtil.waitForThreadsToComplete((ThreadPoolExecutor) Imeji.getEXECUTOR(), Imeji.getCONTENT_EXTRACTION_EXECUTOR(),
        Imeji.getINTERNAL_STORAGE_EXECUTOR());
  }

}
