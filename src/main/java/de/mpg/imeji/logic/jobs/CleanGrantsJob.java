package de.mpg.imeji.logic.jobs;

import java.util.concurrent.Callable;

import org.apache.log4j.Logger;

import de.mpg.imeji.logic.ImejiSPARQL;
import de.mpg.imeji.logic.search.jenasearch.JenaCustomQueries;
import de.mpg.imeji.logic.vo.MetadataProfile;

/**
 * Clean empty {@link MetadataProfile}, which are not referenced by any collection
 * 
 * @author saquet
 *
 */
public class CleanGrantsJob implements Callable<Integer> {
  private static final Logger LOGGER = Logger.getLogger(CleanGrantsJob.class);

  @Override
  public Integer call() throws Exception {
    LOGGER.info("Cleaning grants...");
    ImejiSPARQL.execUpdate(JenaCustomQueries.removeGrantWithoutObject());
    ImejiSPARQL.execUpdate(JenaCustomQueries.removeGrantWithoutUser());
    ImejiSPARQL.execUpdate(JenaCustomQueries.removeGrantEmtpy());
    LOGGER.info("...done!");
    return 1;
  }
}
