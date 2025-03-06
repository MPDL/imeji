package de.mpg.imeji.logic.batch;

import java.util.List;
import java.util.concurrent.Callable;

import de.mpg.imeji.logic.db.repositories.ContentDbRepository;
import de.mpg.imeji.logic.model.ContentVO;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import de.mpg.imeji.logic.core.content.ContentService;
import de.mpg.imeji.logic.search.jenasearch.ImejiSPARQL;
import de.mpg.imeji.logic.search.jenasearch.JenaCustomQueries;

/**
 * Job to clean contentVO
 *
 * @author saquet
 *
 */
public class CleanContentVOsJob implements Callable<Integer> {
  private static final Logger LOGGER = LogManager.getLogger(CleanContentVOsJob.class);

  @Override
  public Integer call() throws Exception {
    LOGGER.info("Cleaning contents...");
    final List<ContentVO> contents = new ContentDbRepository().retrieveUnusedContent();
    //final List<String> contentIds = ImejiSPARQL.exec(JenaCustomQueries.selectUnusedContent(), null);
    LOGGER.info(contents.size() + " content found to be removed");
    final ContentService controller = new ContentService();
    for (final ContentVO contentVO : contents) {
      try {
        // controller.delete(id);
      } catch (final Exception e) {
        LOGGER.error("Error removing content " + contentVO.getId(), e);
      }
    }
    LOGGER.info("Contents cleaned!");
    return 1;
  }

}
