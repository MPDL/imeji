package de.mpg.imeji.logic.jobs;

import org.apache.log4j.Logger;

import de.mpg.imeji.logic.Imeji;
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
    Imeji.getEXECUTOR().submit(new CleanTempFilesJob());
    try {
      Imeji.getEXECUTOR().submit(new StorageUsageAnalyseJob());
    } catch (Exception e) {
      LOGGER.error("Error: " + e.getMessage());
    }
    Imeji.getEXECUTOR().submit(new CleanInactiveUsersJob());
    Imeji.getEXECUTOR().submit(new ReadMaxPlanckIPMappingJob());
    Imeji.getEXECUTOR().submit(new CleanGrantsJob());
    Imeji.getEXECUTOR().submit(new CleanStatementsJob());
    Imeji.getEXECUTOR().submit(new CleanUserGroupsJob());
    Imeji.getEXECUTOR().submit(new CleanContentVOsJob());
  }

}
