package de.mpg.imeji.util;

import static org.awaitility.Awaitility.await;

import java.util.Collection;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Utility/Helper class for concurrency utilities.
 * 
 * @author helk
 *
 */
public final class ConcurrencyUtil {

  private ConcurrencyUtil() {
    //Should not be instantiated.
  }

  /**
   * Wait for all threads of all {@code ThreadPoolExecutors} to complete.
   * 
   * @param threadPoolExecutors Collection of {@code ThreadPoolExecutors} to wait for.
   */
  public static void waitForThreadsToComplete(Collection<ThreadPoolExecutor> threadPoolExecutors) {
    for (ThreadPoolExecutor threadPoolExecutor : threadPoolExecutors) {
      waitForThreadsToComplete(threadPoolExecutor);
    }
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
