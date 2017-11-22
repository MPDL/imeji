package de.mpg.imeji.logic.security.user.listener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.config.Imeji;
import de.mpg.imeji.logic.events.messages.Message.MessageType;
import de.mpg.imeji.logic.events.listener.Listener;
import de.mpg.imeji.logic.events.messages.ShareMessage;
import de.mpg.imeji.logic.model.CollectionImeji;
import de.mpg.imeji.logic.model.User;
import de.mpg.imeji.logic.model.UserGroup;
import de.mpg.imeji.logic.security.user.UserService;
import de.mpg.imeji.logic.security.usergroup.UserGroupService;
import de.mpg.imeji.logic.util.ObjectHelper;
import de.mpg.imeji.logic.util.StringHelper;

/**
 * Subscriber when a collection is unshared
 * 
 * @author saquet
 *
 */
public class UnshareCollectionListener extends Listener {
  private final UserService userService = new UserService();
  private static final Logger LOGGER = Logger.getLogger(UnshareCollectionListener.class);

  public UnshareCollectionListener() {
    super(MessageType.UNSHARE);
  }

  @Override
  public Integer call() throws Exception {
    String collectionId = getMessage().getObjectId();
    List<User> users = readUsersFromMessage();
    users = removeSubsciption(users, collectionId);
    userService.updateBatch(users, Imeji.adminUser);
    return 1;
  }

  /**
   * Remove the subscription to all users which subscribed to the collection. Return the users which
   * needs to be updated
   * 
   * @param users
   * @param collectionId
   * @return
   */
  private List<User> removeSubsciption(List<User> users, String collectionId) {
    String collectionUri = ObjectHelper.getURI(CollectionImeji.class, collectionId).toString();
    // filter the users which have subscribed
    List<User> usersWithSubscription = users.stream()
        .filter(u -> u.getSubscriptionCollections().stream().anyMatch(c -> c.equals(collectionUri)))
        .collect(Collectors.toList());
    // Remove the subscription of all users
    return usersWithSubscription.stream()
        .peek(u -> u.setSubscriptionCollections(u.getSubscriptionCollections().stream()
            .filter(c -> !c.equals(collectionUri)).collect(Collectors.toList())))
        .collect(Collectors.toList());
  }

  /**
   * Return all users in the message, i.e. the users which have been unshared
   * 
   * @return
   * @throws ImejiException
   */
  private List<User> readUsersFromMessage() throws ImejiException {
    if (getMessage() instanceof ShareMessage) {
      ShareMessage m = (ShareMessage) getMessage();
      if (!StringHelper.isNullOrEmptyTrim(m.getEmail())) {
        return Arrays.asList(new UserService().retrieve(m.getEmail(), Imeji.adminUser));
      } else if (!StringHelper.isNullOrEmptyTrim(m.getGroupId())) {
        return retrieveUsersOfGroup(m.getGroupId());
      }
    }
    return new ArrayList<>();
  }

  /**
   * Return the users of the group
   * 
   * @param groupId
   * @return
   * @throws ImejiException
   */
  private List<User> retrieveUsersOfGroup(String groupId) throws ImejiException {
    UserGroup g = new UserGroupService()
        .retrieve(ObjectHelper.getURI(UserGroup.class, groupId).toString(), Imeji.adminUser);
    return (List<User>) userService.retrieveBatch(
        g.getUsers().stream().map(uri -> uri.toString()).collect(Collectors.toList()), -1);
  }

  /**
   * Unsubscribe user from the collection
   * 
   * @param user
   * @param collectionId
   */
  private void unsubscribe(User user, String collectionId) {
    String collectionUri = ObjectHelper.getURI(CollectionImeji.class, collectionId).toString();
    user.setSubscriptionCollections(user.getSubscriptionCollections().stream()
        .filter(c -> !c.equals(collectionUri)).collect(Collectors.toList()));
    try {
      userService.update(user, Imeji.adminUser);
    } catch (ImejiException e) {
      LOGGER.error("Error unsubscribing user", e);
    }
  }

  /**
   * True if the user have subscribed for this collection
   * 
   * @param user
   * @param collectionId
   * @return
   */
  private boolean hasSubscribed(User user, String collectionId) {
    String collectionUri = ObjectHelper.getURI(CollectionImeji.class, collectionId).toString();
    return user.getSubscriptionCollections().stream().anyMatch(c -> c.equals(collectionUri));
  }

}
