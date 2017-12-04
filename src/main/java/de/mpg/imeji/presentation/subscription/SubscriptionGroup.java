package de.mpg.imeji.presentation.subscription;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.model.CollectionImeji;
import de.mpg.imeji.logic.model.Subscription;
import de.mpg.imeji.logic.model.User;
import de.mpg.imeji.logic.model.factory.ImejiFactory;
import de.mpg.imeji.logic.notification.subscription.SubscriptionService;
import de.mpg.imeji.logic.security.authorization.util.SecurityUtil;
import de.mpg.imeji.logic.security.user.UserService;

/**
 * A group of subscription
 * 
 * @author saquet
 *
 */
public class SubscriptionGroup implements Serializable {
  private static final long serialVersionUID = -2569072377550287718L;
  private List<Subscription> subscriptions = new ArrayList<>();
  private Map<String, User> subscribedUserMap = new HashMap<>();
  private final CollectionImeji collection;
  private final User user;
  private final boolean active;

  public SubscriptionGroup(CollectionImeji collection, User user) {
    this.collection = collection;
    this.user = user;
    active = SecurityUtil.authorization().read(user, collection);
  }

  public SubscriptionGroup(CollectionImeji collection, User user, User sessionUser) {
    this.collection = collection;
    this.user = user;
    active = SecurityUtil.authorization().read(user, collection);
  }

  /**
   * Retrieve the subscriptions of this group
   */
  public void init() {
    subscriptions = retrieveSubscriptions();
    subscribedUserMap = retrieveUsers(subscriptions);
  }

  /**
   * Retrieve all users which have subscribed to this collection
   * 
   * @param l
   * @return
   */
  private Map<String, User> retrieveUsers(List<Subscription> l) {
    List<String> userIds = l.stream().map(s -> s.getUserId()).collect(Collectors.toList());
    return new UserService().retrieveBatchLazy(userIds, -1).stream()
        .collect(Collectors.toMap(u -> u.getId().toString(), Function.identity(), (u1, u2) -> u1));
  }

  /**
   * Return the completename of a the user of a subscription
   * 
   * @param s
   * @return
   */
  public String getUserCompleteName(Subscription s) {
    return subscribedUserMap.get(s.getUserId()).getPerson().getCompleteName();
  }

  /**
   * Return the Subscription of a user
   * 
   * @param user
   * @return
   */
  public Subscription getSubscriptionForUser(User user) {
    return subscriptions.stream().filter(s -> s.getUserId().equals(user.getId().toString()))
        .findAny().orElse(null);
  }

  public String getName() {
    return collection.getName();
  }

  /**
   * The id of the object of this group
   * 
   * @return
   */
  public String getObjectId() {
    return ImejiFactory.newSubscription().setObjectId(collection).build().getObjectId();
  }

  /**
   * Retrieve the subscription of this group
   * 
   * @return
   */
  public List<Subscription> retrieveSubscriptions() {
    try {
      return new SubscriptionService().retrieveByObjectId(getObjectId(), user);
    } catch (ImejiException e) {
      e.printStackTrace();
      return new ArrayList<>();
    }
  }

  public boolean isSubscribed(User user) {
    return subscribedUserMap.containsKey(user.getId().toString());
  }

  /**
   * @return the subscriptions
   */
  public List<Subscription> getSubscriptions() {
    return subscriptions;
  }

  /**
   * Retrieve all the users who subscribed to this collection sorted by name
   * 
   * @return
   */
  public List<User> getSubscribedUsers() {
    return subscribedUserMap.values().stream().sorted(
        (u1, u2) -> u1.getPerson().getCompleteName().compareTo(u2.getPerson().getCompleteName()))
        .collect(Collectors.toList());
  }


  public CollectionImeji getCollection() {
    return collection;
  }

  public boolean isActive() {
    return active;
  }

}
