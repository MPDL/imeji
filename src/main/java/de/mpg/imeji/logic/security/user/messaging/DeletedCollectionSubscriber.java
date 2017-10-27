package de.mpg.imeji.logic.security.user.messaging;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.config.Imeji;
import de.mpg.imeji.logic.events.Message.MessageType;
import de.mpg.imeji.logic.events.subscription.Subscriber;
import de.mpg.imeji.logic.model.CollectionImeji;
import de.mpg.imeji.logic.model.Grant;
import de.mpg.imeji.logic.model.User;
import de.mpg.imeji.logic.model.UserGroup;
import de.mpg.imeji.logic.security.user.UserService;
import de.mpg.imeji.logic.security.usergroup.UserGroupService;
import de.mpg.imeji.logic.util.ObjectHelper;

/**
 * {@link Subscriber} to unsubscribe users of deleted collection
 * 
 * @author saquet
 *
 */
public class DeletedCollectionSubscriber extends Subscriber {
  private final UserService userService = new UserService();
  private static final Logger LOGGER = Logger.getLogger(DeletedCollectionSubscriber.class);


  public DeletedCollectionSubscriber() {
    super(MessageType.DELETE_COLLECTION);
  }

  @Override
  public Integer call() throws Exception {
    try {
      List<User> allUsers = userService.retrieveAll();
      Map<String, User> modifiedUsers = new HashMap<>();
      modifiedUsers = unsubscribe(allUsers, getMessage().getObjectId(), modifiedUsers);
      modifiedUsers = unshare(allUsers, getMessage().getObjectId(), modifiedUsers);
      userService.updateBatch(new ArrayList<>(modifiedUsers.values()), Imeji.adminUser);
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
    UserGroupService groupService = new UserGroupService();
    List<UserGroup> groups = (List<UserGroup>) groupService.retrieveAll();
    for (UserGroup group : groups) {
      if (hasGrantFor((List<String>) group.getGrants(), collectionId)) {
        List<String> newGrants = group.getGrants().stream().map(s -> new Grant(s))
            .filter(g -> !ObjectHelper.getId(URI.create(g.getGrantFor())).equals(collectionId))
            .map(g -> g.toGrantString()).collect(Collectors.toList());
        group.setGrants(newGrants);
        groupService.update(group, Imeji.adminUser);
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
    return grants.stream().map(s -> new Grant(s))
        .anyMatch(g -> g.getGrantFor().equals(collectionUri));
  }

  /**
   * Unshare the deleted collection with all concerned users
   * 
   * @param allUsers
   * @param collectionId
   * @return
   */
  private Map<String, User> unshare(List<User> allUsers, String collectionId,
      Map<String, User> modifiedUsers) {
    allUsers = merge(modifiedUsers, allUsers);
    for (User user : allUsers) {
      List<String> newGrants = user.getGrants().stream().map(s -> new Grant(s))
          .filter(g -> !ObjectHelper.getId(URI.create(g.getGrantFor())).equals(collectionId))
          .map(g -> g.toGrantString()).collect(Collectors.toList());
      if (newGrants.size() < user.getGrants().size()) {
        user.setGrants(newGrants);
        modifiedUsers.put(user.getEmail(), user);
      }
    }
    return modifiedUsers;
  }

  /**
   * Unsubscribe all user to the deleted collection
   * 
   * @param allUsers
   * @param collectionId
   */
  private Map<String, User> unsubscribe(List<User> allUsers, String collectionId,
      Map<String, User> modifiedUsers) {
    allUsers = merge(modifiedUsers, allUsers);
    for (User user : allUsers) {
      List<String> newSubscriptions = user.getSubscriptionCollections().stream()
          .filter(c -> !ObjectHelper.getId(URI.create(c)).equals(collectionId))
          .collect(Collectors.toList());
      if (newSubscriptions.size() < user.getSubscriptionCollections().size()) {
        user.setSubscriptionCollections(newSubscriptions);
        modifiedUsers.put(user.getEmail(), user);
      }
    }
    return modifiedUsers;
  }


  /**
   * Merge the modified users into allUsers to update the list of all users with the modification
   * 
   * @param modifiedUsers
   * @param allUsers
   * @return
   */
  private List<User> merge(Map<String, User> modifiedUsers, List<User> allUsers) {
    Map<String, User> allUsersMap = toUserMap(allUsers);
    allUsersMap.putAll(modifiedUsers);
    return new ArrayList<>(allUsersMap.values());
  }


  /**
   * Map a user list by email
   * 
   * @param users
   * @return
   */
  private Map<String, User> toUserMap(List<User> users) {
    return users.stream().collect(Collectors.toMap(User::getEmail, Function.identity()));
  }

}
