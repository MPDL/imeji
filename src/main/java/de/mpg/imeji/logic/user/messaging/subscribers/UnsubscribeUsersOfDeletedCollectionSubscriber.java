package de.mpg.imeji.logic.user.messaging.subscribers;

import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.config.Imeji;
import de.mpg.imeji.logic.messaging.Message;
import de.mpg.imeji.logic.messaging.Message.MessageType;
import de.mpg.imeji.logic.messaging.subscription.Subscriber;
import de.mpg.imeji.logic.user.UserService;
import de.mpg.imeji.logic.util.ObjectHelper;
import de.mpg.imeji.logic.vo.User;

/**
 * {@link Subscriber} to unsubscribe users of deleted collection
 * 
 * @author saquet
 *
 */
public class UnsubscribeUsersOfDeletedCollectionSubscriber extends Subscriber {
  private UserService userService = new UserService();

  private static final Logger LOGGER =
      Logger.getLogger(UnsubscribeUsersOfDeletedCollectionSubscriber.class);


  public UnsubscribeUsersOfDeletedCollectionSubscriber() {
    super(MessageType.DELETE_COLLECTION);
  }

  @Override
  public Integer call() throws Exception {
    try {
      unsubscribeUsers(getSubscribedUsers(message.getObjectId()), message.getObjectId());
    } catch (ImejiException e) {
      LOGGER.error("Error unsubscribing users", e);
    }
    return 1;
  }

  @Override
  public void send(Message message) {
    this.message = message;
  }

  /**
   * Get all users which subscribed to this collection
   * 
   * @param collectionId
   * @return
   * @throws ImejiException
   */
  private List<User> getSubscribedUsers(String collectionId) throws ImejiException {
    return userService.retrieveAll().stream()
        .filter(u -> u.getSubscriptionCollections().stream()
            .filter(c -> ObjectHelper.getId(URI.create(c)).equals(collectionId)).findAny()
            .isPresent())
        .collect(Collectors.toList());
  }

  /**
   * Unsubscribe all user from the collection
   * 
   * @param users
   * @param collectionId
   */
  private void unsubscribeUsers(List<User> users, String collectionId) {
    for (User user : users) {
      List<String> newSubscriptions = user.getSubscriptionCollections().stream()
          .filter(c -> !ObjectHelper.getId(URI.create(c)).equals(collectionId))
          .collect(Collectors.toList());
      user.setSubscriptionCollections(newSubscriptions);
      try {
        userService.update(user, Imeji.adminUser);
      } catch (ImejiException e) {
        LOGGER.error("Error updating user with new subscitions", e);
      }
    }
  }
}
