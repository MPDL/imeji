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
    Imeji.EXECUTOR.submit(new CleanTempFilesJob());
    try {
      Imeji.EXECUTOR.submit(new StorageUsageAnalyseJob());
    } catch (Exception e) {
      LOGGER.error("Error: " + e.getMessage());
    }
    Imeji.EXECUTOR.submit(new CleanInactiveUsersJob());
    Imeji.EXECUTOR.submit(new ReadMaxPlanckIPMappingJob());
    Imeji.EXECUTOR.submit(new CleanEmptyMetadataProfileJob());
    Imeji.EXECUTOR.submit(new CleanMetadataJob(null));
    Imeji.EXECUTOR.submit(new CleanGrantsJob());
    Imeji.EXECUTOR.submit(new CleanStatementsJob());
    Imeji.EXECUTOR.submit(new CleanUserGroupsJob());
    Imeji.EXECUTOR.submit(new CleanContentVOsJob());
  }

}
