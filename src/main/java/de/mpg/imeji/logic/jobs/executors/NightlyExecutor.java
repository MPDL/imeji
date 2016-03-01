package de.mpg.imeji.logic.jobs.executors;

import java.util.Calendar;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import de.mpg.imeji.logic.jobs.NightlyJob;

/**
 * {@link Executor} which schedule Jobs at a specified time every day
 * 
 * @author bastiens
 * 
 */
public class NightlyExecutor {

  private static final Logger LOGGER = Logger.getLogger(NightlyExecutor.class);
  /**
   * The Hour the executor will be executate at
   */
  private static final int JOB_HOUR = 1;
  /**
   * The minute the executor will be executate at
   */
  private static final int JOB_MINUTE = 0;
  /**
   */
  private static Calendar NEXT_JOB_DATE = scheduleNextDate();
  private static ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1);


  /**
   * Start the nightly jobs
   */
  public void start() {
    // Execute first job (by imeji start)
    executor.execute(new NightlyJob());
    // Schedule the next executions
    executor.scheduleAtFixedRate(new NightlyJob(), getDelay(), 1, TimeUnit.DAYS);
    LOGGER.info(
        "Nightly Executor started. First job planned at " + NEXT_JOB_DATE.getTime().toString());

  }

  /**
   * Stop the nightly jobs
   */
  public void stop() {
    LOGGER.info("Shutting down nightly Job");
    executor.purge();
    executor.shutdown();
    LOGGER.info("Nightly Job stopped");
  }

  /**
   * Return the next Date when the Job should be run
   * 
   * @return
   */
  private static Calendar scheduleNextDate() {
    Calendar date = Calendar.getInstance();
    // Set Date to today with defined hour
    date.set(date.get(Calendar.YEAR), date.get(Calendar.MONTH), date.get(Calendar.DATE), JOB_HOUR,
        JOB_MINUTE);
    // Add one day
    date.add(Calendar.DATE, 1);
    return date;

  }

  /**
   * Calculate the Delay until the next Job execution in milliseconds
   * 
   * @return
   */
  private static long getDelay() {
    return NEXT_JOB_DATE.getTimeInMillis() - System.currentTimeMillis();
  }

  public static void main(String[] args) {
    NightlyExecutor nightlyExecutor = new NightlyExecutor();
    nightlyExecutor.start();

  }

}
