package de.mpg.imeji.logic.jobs;

import java.util.concurrent.Callable;

import org.apache.log4j.Logger;

import de.mpg.imeji.logic.Imeji;
import de.mpg.imeji.logic.controller.UserController;
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
  public Integer call() {
    LOGGER.info(" Cleaning of Inactive Users...");
    UserController userController = new UserController(Imeji.adminUser);
    int numCleaned = userController.cleanInactiveUsers();
    LOGGER.info("...done ! " + numCleaned + " inactive users have been deleted!");
    return 1;
  }
}
