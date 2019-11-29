package de.mpg.imeji.logic.db.indexretry.queue;

import java.util.concurrent.ThreadFactory;

/**
 * Sets an UncaughtExceptionHandler to a thread.
 * 
 * @author breddin
 *
 */

public class RetryThreadFactory implements ThreadFactory {

  @Override
  public Thread newThread(Runnable runnable) {

    Thread thread = new Thread(runnable);
    thread.setUncaughtExceptionHandler(new RetryUncaughtExceptionHandler());
    return thread;
  }

}
