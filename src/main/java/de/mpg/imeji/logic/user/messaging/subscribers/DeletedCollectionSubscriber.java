package de.mpg.imeji.logic.user.messaging.subscribers;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;

import de.mpg.imeji.logic.config.Imeji;
import de.mpg.imeji.logic.messaging.Message.MessageType;
import de.mpg.imeji.logic.messaging.subscription.Subscriber;
import de.mpg.imeji.logic.user.UserService;
import de.mpg.imeji.logic.util.ObjectHelper;
import de.mpg.imeji.logic.vo.Grant;
import de.mpg.imeji.logic.vo.User;

/**
 * {@link Subscriber} to unsubscribe users of deleted collection
 * 
 * @author saquet
 *
 */
public class DeletedCollectionSubscriber extends Subscriber {
  private UserService userService = new UserService();

  private static final Logger LOGGER = Logger.getLogger(DeletedCollectionSubscriber.class);


  public DeletedCollectionSubscriber() {
    super(MessageType.DELETE_COLLECTION);
  }

  @Override
  public Integer call() throws Exception {
    try {
      List<User> allUsers = userService.retrieveAll();
      Map<String, User> modifiedUsers = new HashMap<>();
      modifiedUsers = unsubscribe(allUsers, message.getObjectId(), modifiedUsers);
      modifiedUsers = unshare(allUsers, message.getObjectId(), modifiedUsers);
      userService.updateBatch(new ArrayList<>(modifiedUsers.values()), Imeji.adminUser);
    } catch (Exception e) {
      LOGGER.error("Error unsubscribing users", e);
    }
    return 1;
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
