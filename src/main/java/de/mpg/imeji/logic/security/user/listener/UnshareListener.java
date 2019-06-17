package de.mpg.imeji.logic.security.user.listener;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.config.Imeji;
import de.mpg.imeji.logic.events.listener.Listener;
import de.mpg.imeji.logic.events.messages.Message.MessageType;
import de.mpg.imeji.logic.model.CollectionImeji;
import de.mpg.imeji.logic.model.Grant;
import de.mpg.imeji.logic.model.User;
import de.mpg.imeji.logic.model.UserGroup;
import de.mpg.imeji.logic.security.user.UserService;
import de.mpg.imeji.logic.security.usergroup.UserGroupService;
import de.mpg.imeji.logic.util.ObjectHelper;

/**
 * {@link Listener} related to all events leading to unshare operations (for example: a collection
 * is deleted, all grants related to this colelction should be removed)
 * 
 * @author saquet
 *
 */
public class UnshareListener extends Listener {
  private final UserService userService = new UserService();
  private static final Logger LOGGER = LogManager.getLogger(UnshareListener.class);

  public UnshareListener() {
    super(MessageType.DELETE_COLLECTION);
  }

  @Override
  public Integer call() {
    try {
      // (1) Read all existing users from database
      List<User> allUsers = userService.retrieveAll();
      // (2) Find all users that have a grant to the deleted collection
      Map<String, User> usersToNotify = new HashMap<>();
      String uriOfRemovedCollection = getMessage().getObjectId();
      usersToNotify = unshare(allUsers, uriOfRemovedCollection, usersToNotify);
      // (3) delete the grants in the user objects in database
      for (User deleteGrantFromThisUser : usersToNotify.values()) {
        userService.removeGrantsFromUser(Imeji.adminUser, deleteGrantFromThisUser, new Grant(null, uriOfRemovedCollection));
      }
      unshareUserGroup(getMessage().getObjectId());
    } catch (Exception e) {
      LOGGER.error("Error unsubscribing users", e);
    }
    return 1;
  }

  /**
   * Unshare the collection to the group
   * 
   * @param collectionId
   * @throws ImejiException
   */
  private void unshareUserGroup(String collectionId) throws ImejiException {
    UserGroupService userGroupService = new UserGroupService();
    List<UserGroup> groups = (List<UserGroup>) userGroupService.retrieveAll();
    for (UserGroup group : groups) {
      if (hasGrantFor((List<String>) group.getGrants(), collectionId)) {
        userGroupService.removeGrantFromGroup(Imeji.adminUser, group, new Grant(null, collectionId));
      }
    }
  }

  /**
   * True if one the grant is for the collection
   * 
   * @param grants
   * @param collectionId
   * @return
   */
  private boolean hasGrantFor(List<String> grants, String collectionId) {
    String collectionUri = ObjectHelper.getURI(CollectionImeji.class, collectionId).toString();
    return grants.stream().map(s -> new Grant(s)).anyMatch(g -> g.getGrantFor().equals(collectionUri));
  }

  /**
   * Unshare the deleted collection with all concerned users
   * 
   * @param allUsers
   * @param collectionId
   * @return
   */
  private Map<String, User> unshare(List<User> allUsers, String collectionId, Map<String, User> modifiedUsers) {
    for (User user : allUsers) {
      List<String> newGrants = user.getGrants().stream().map(s -> new Grant(s))
          .filter(g -> !ObjectHelper.getId(URI.create(g.getGrantFor())).equals(collectionId)).map(g -> g.toGrantString())
          .collect(Collectors.toList());
      if (newGrants.size() < user.getGrants().size()) {
        user.setGrants(newGrants);
        modifiedUsers.put(user.getEmail(), user);
      }
    }
    return modifiedUsers;
  }
}
