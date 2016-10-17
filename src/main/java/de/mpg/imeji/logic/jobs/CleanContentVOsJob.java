package de.mpg.imeji.logic.jobs;

import java.util.List;
import java.util.concurrent.Callable;

import org.apache.log4j.Logger;

import de.mpg.imeji.logic.controller.resource.ContentController;
import de.mpg.imeji.logic.search.jenasearch.ImejiSPARQL;
import de.mpg.imeji.logic.search.jenasearch.JenaCustomQueries;

/**
 * Job to clean contentVO
 * 
 * @author saquet
 *
 */
public class CleanContentVOsJob implements Callable<Integer> {
  private static final Logger LOGGER = Logger.getLogger(CleanContentVOsJob.class);

  @Override
  public Integer call() throws Exception {
    LOGGER.info("Cleaning contents...");
    List<String> contentIds = ImejiSPARQL.exec(JenaCustomQueries.selectUnusedContent(), null);
    LOGGER.info(contentIds.size() + " content found to be removed");
    ContentController controller = new ContentController();
    for (String id : contentIds) {
      controller.delete(id);
    }
    LOGGER.info("Contents cleaned!");
    return 1;
  }

}
