package de.mpg.imeji.logic.batch;

import java.util.concurrent.Callable;

import org.apache.log4j.Logger;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.security.user.pwdreset.PasswordResetController;

public class CleanPasswordResetsJob implements Callable<Integer> {
  private static final Logger LOGGER = Logger.getLogger(CleanInactiveUsersJob.class);

  @Override
  public Integer call() throws ImejiException {
    LOGGER.info(" Cleaning expiered password reset tokens...");
    new PasswordResetController().deleteExpiredTokens();
    LOGGER.info("...done!");
    return 1;
  }
}
