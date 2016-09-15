package de.mpg.imeji.logic.jobs;

import org.apache.log4j.Logger;

import de.mpg.imeji.logic.Imeji;
import de.mpg.imeji.logic.controller.CleanMetadataJob;
import de.mpg.imeji.logic.jobs.executors.CleanInactiveUsersJob;
import de.mpg.imeji.logic.jobs.executors.NightlyExecutor;

/**
 * This job calls all Jobs which should be run every night, and is called by the
 * {@link NightlyExecutor} every night
 *
 * @author bastiens
 *
 */
public class NightlyJob implements Runnable {
  private static final Logger LOGGER = Logger.getLogger(NightlyJob.class);

  @Override
  public void run() {
    LOGGER.info("Running Nightly Jobs");
    Imeji.getExecutor().submit(new CleanTempFilesJob());
    try {
      Imeji.getExecutor().submit(new StorageUsageAnalyseJob());
    } catch (Exception e) {
      LOGGER.error("Error: " + e.getMessage());
    }
    Imeji.getExecutor().submit(new CleanInactiveUsersJob());
    Imeji.getExecutor().submit(new ReadMaxPlanckIPMappingJob());
    Imeji.getExecutor().submit(new CleanEmptyMetadataProfileJob());
    Imeji.getExecutor().submit(new CleanMetadataJob(null));
    Imeji.getExecutor().submit(new CleanGrantsJob());
    Imeji.getExecutor().submit(new CleanStatementsJob());
    Imeji.getExecutor().submit(new CleanUserGroupsJob());
  }

}
