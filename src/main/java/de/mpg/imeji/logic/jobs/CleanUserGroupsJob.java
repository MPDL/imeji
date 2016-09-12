package de.mpg.imeji.logic.jobs;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

import org.apache.log4j.Logger;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.exceptions.NotFoundException;
import de.mpg.imeji.logic.Imeji;
import de.mpg.imeji.logic.user.controller.GroupBusinessController;
import de.mpg.imeji.logic.user.controller.UserBusinessController;
import de.mpg.imeji.logic.vo.MetadataProfile;
import de.mpg.imeji.logic.vo.User;
import de.mpg.imeji.logic.vo.UserGroup;

/**
 * Clean empty {@link MetadataProfile}, which are not referenced by any collection
 *
 * @author saquet
 *
 */
public class CleanUserGroupsJob implements Callable<Integer> {
  private static final Logger LOGGER = Logger.getLogger(CleanUserGroupsJob.class);

  @Override
  public Integer call() {
    LOGGER.info("Cleaning User Groups...");
    try {
      cleanZombieMember();
    } catch (ImejiException e) {
      LOGGER.error("Error cleaning user groups: " + e.getMessage());
    }
    LOGGER.info("...done!");
    return 1;
  }

  /**
   * Clean all usergrouf of their zombie members, ie, members that doesn't exist anymore in the db
   *
   * @throws ImejiException
   */
  private void cleanZombieMember() throws ImejiException {
    GroupBusinessController controller = new GroupBusinessController();
    for (URI userId : findZombieMember()) {
      User user = new User();
      user.setId(userId);
      controller.removeUserFromAllGroups(user, Imeji.adminUser);
    }
  }

  /**
   * Look for all group member which are no user anymore
   *
   * @return
   */
  private List<URI> findZombieMember() {
    Set<URI> zombies = new HashSet<>();
    for (UserGroup group : getAllUserGroups()) {
      for (URI member : group.getUsers()) {
        if (!userExists(member) && !zombies.contains(member)) {
          zombies.add(member);
        }
      }
    }
    return new ArrayList<>(zombies);
  }


  private boolean userExists(URI userId) {
    try {
      retrieveUser(userId);
      return true;
    } catch (NotFoundException e) {
      return false;
    } catch (ImejiException e) {
      LOGGER.error("Erro reading user: ", e);
      return true;
    }
  }

  /**
   * Retrieve a user
   *
   * @param userId
   * @return
   * @throws ImejiException
   */
  private User retrieveUser(URI userId) throws ImejiException {
    UserBusinessController controller = new UserBusinessController();
    return controller.retrieve(userId, Imeji.adminUser);
  }

  /**
   * Get All User Group
   *
   * @return
   */
  private List<UserGroup> getAllUserGroups() {
    GroupBusinessController controller = new GroupBusinessController();
    return (List<UserGroup>) controller.searchByName("", Imeji.adminUser);
  }
}
