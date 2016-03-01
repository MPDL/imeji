package de.mpg.imeji.logic.jobs;

import java.util.concurrent.Callable;

import org.apache.log4j.Logger;

import de.mpg.imeji.logic.controller.ProfileController;
import de.mpg.imeji.logic.vo.Metadata;
import de.mpg.imeji.logic.vo.MetadataProfile;
import de.mpg.imeji.logic.vo.Statement;

/**
 * Clean the {@link Metadata} which have a {@link Statement} which doesn't exist anymore
 * 
 * @author saquet
 *
 */
public class CleanMetadataJob implements Callable<Integer> {
  private static final Logger LOGGER = Logger.getLogger(CleanMetadataJob.class);

  private MetadataProfile p;

  public CleanMetadataJob(MetadataProfile p) {
    this.p = p;
  }

  @Override
  public Integer call() throws Exception {
    LOGGER.info("Cleaning Metadata...");
    ProfileController pc = new ProfileController();
    pc.removeMetadataWithoutStatement(p);
    LOGGER.info("...done!");
    return 1;
  }

}
