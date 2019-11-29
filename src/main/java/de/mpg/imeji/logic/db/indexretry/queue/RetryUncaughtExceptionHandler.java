package de.mpg.imeji.logic.db.indexretry.queue;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class RetryUncaughtExceptionHandler implements Thread.UncaughtExceptionHandler {

  private static final Logger LOGGER = LogManager.getLogger(RetryUncaughtExceptionHandler.class);

  @Override
  public void uncaughtException(Thread thread, Throwable throwable) {

    LOGGER.error("Uncaught exception " + throwable.getClass().toString() + " in retry queue thread " + thread.getId() + ". Thread died.");

  }

}
