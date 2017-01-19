package de.mpg.imeji.logic.batch;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

import org.apache.log4j.Logger;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.exceptions.NotFoundException;
import de.mpg.imeji.logic.config.Imeji;
import de.mpg.imeji.logic.user.UserService;
import de.mpg.imeji.logic.usergroup.UserGroupService;
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
    } catch (final ImejiException e) {
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
    final UserGroupService controller = new UserGroupService();
    for (final URI userId : findZombieMember()) {
      final User user = new User();
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
    final Set<URI> zombies = new HashSet<>();
    for (final UserGroup group : getAllUserGroups()) {
      for (final URI member : group.getUsers()) {
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
    } catch (final NotFoundException e) {
      return false;
    } catch (final ImejiException e) {
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
    final UserService controller = new UserService();
    return controller.retrieve(userId, Imeji.adminUser);
  }

  /**
   * Get All User Group
   *
   * @return
   */
  private List<UserGroup> getAllUserGroups() {
    final UserGroupService controller = new UserGroupService();
    return (List<UserGroup>) controller.searchByName("", Imeji.adminUser);
  }
}
