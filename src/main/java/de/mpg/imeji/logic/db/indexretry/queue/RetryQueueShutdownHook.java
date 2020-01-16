package de.mpg.imeji.logic.db.indexretry.queue;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Shutdown hook, to be executed in case Imeji exits while retry queue thread is still running.
 * 
 * @author breddin
 *
 */
public class RetryQueueShutdownHook extends Thread {


  private static final Logger LOGGER = LogManager.getLogger(RetryQueueShutdownHook.class);

  public void run() {

    LOGGER.info("Shutting down retry queue");
    RetryQueue retryQueue = RetryQueue.getInstance();
    retryQueue.shutDown();

  }
}
