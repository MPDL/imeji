package de.mpg.imeji.logic.batch;

import java.util.concurrent.Callable;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.model.Item;
import de.mpg.imeji.logic.security.registration.RegistrationService;

/**
 * Job which read all Items, read for each {@link Item} the size of the original File, and write the
 * items in Jena back with the file size;
 *
 * @author saquet
 *
 */
public class CleanInactiveUsersJob implements Callable<Integer> {
  private static final Logger LOGGER = LogManager.getLogger(CleanInactiveUsersJob.class);

  @Override
  public Integer call() throws ImejiException {
    LOGGER.info(" Cleaning expiered registration Users...");
    new RegistrationService().deleteExpiredRegistration();
    LOGGER.info("...done!");
    return 1;
  }
}
