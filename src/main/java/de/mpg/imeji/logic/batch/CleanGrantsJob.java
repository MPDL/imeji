package de.mpg.imeji.logic.batch;

import java.util.concurrent.Callable;

import org.apache.logging.log4j.Logger; 
import org.apache.logging.log4j.LogManager;

/**
 * Clean empty {@link MetadataProfile}, which are not referenced by any collection
 *
 * @author saquet
 *
 */
public class CleanGrantsJob implements Callable<Integer> {
  private static final Logger LOGGER = LogManager.getLogger(CleanGrantsJob.class);

  @Override
  public Integer call() throws Exception {
    LOGGER.info("Cleaning grants...");
    // ImejiSPARQL.execUpdate(JenaCustomQueries.removeGrantWithoutObject());
    // ImejiSPARQL.execUpdate(JenaCustomQueries.removeGrantWithoutUser());
    // ImejiSPARQL.execUpdate(JenaCustomQueries.removeGrantEmtpy());
    LOGGER.info("...done!");
    return 1;
  }
}
