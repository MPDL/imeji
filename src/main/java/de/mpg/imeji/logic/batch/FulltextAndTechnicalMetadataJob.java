package de.mpg.imeji.logic.batch;

import java.util.concurrent.Callable;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import de.mpg.imeji.logic.core.content.ContentService;

/**
 * Job to extract fulltext and technical metadata for all files
 *
 * @author saquet
 *
 */
public class FulltextAndTechnicalMetadataJob implements Callable<Integer> {
  private static final Logger LOGGER = LogManager.getLogger(FulltextAndTechnicalMetadataJob.class);

  @Override
  public Integer call() throws Exception {
    LOGGER.info("Extracting fulltext and technical metadata for all files...");
    new ContentService().extractFulltextAndTechnicalMetadataForAllFiles();
    LOGGER.info("... Extracting fulltext and technical metadata for all files done!");
    return 1;
  }

}
