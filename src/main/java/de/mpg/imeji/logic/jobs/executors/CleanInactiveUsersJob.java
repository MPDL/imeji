package de.mpg.imeji.logic.jobs.executors;

import java.util.concurrent.Callable;

import org.apache.log4j.Logger;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.registration.RegistrationBusinessController;
import de.mpg.imeji.logic.vo.Item;

/**
 * Job which read all Items, read for each {@link Item} the size of the original File, and write the
 * items in Jena back with the file size;
 *
 * @author saquet
 *
 */
public class CleanInactiveUsersJob implements Callable<Integer> {
  private static final Logger LOGGER = Logger.getLogger(CleanInactiveUsersJob.class);

  @Override
  public Integer call() throws ImejiException {
    LOGGER.info(" Cleaning expiered registration Users...");
    new RegistrationBusinessController().deleteExpiredRegistration();
    LOGGER.info("...done!");
    return 1;
  }
}
