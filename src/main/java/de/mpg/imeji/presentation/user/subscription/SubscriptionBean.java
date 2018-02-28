package de.mpg.imeji.presentation.user.subscription;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import org.apache.log4j.Logger;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.config.Imeji;
import de.mpg.imeji.logic.core.collection.CollectionService;
import de.mpg.imeji.logic.model.CollectionImeji;
import de.mpg.imeji.logic.model.Subscription;
import de.mpg.imeji.logic.model.Subscription.Type;
import de.mpg.imeji.logic.model.User;
import de.mpg.imeji.logic.model.factory.ImejiFactory;
import de.mpg.imeji.logic.notification.subscription.SubscriptionService;
import de.mpg.imeji.logic.security.user.UserService;
import de.mpg.imeji.logic.util.ObjectHelper;
import de.mpg.imeji.logic.util.StringHelper;
import de.mpg.imeji.logic.util.UrlHelper;
import de.mpg.imeji.presentation.beans.SuperBean;
import de.mpg.imeji.presentation.session.BeanHelper;

/**
 * Bean for the Subscriptions page
 * 
 * @author saquet
 *
 */
@ManagedBean(name = "SubscriptionBean")
@ViewScoped
public class SubscriptionBean extends SuperBean {
  private static final long serialVersionUID = 8885253104306297753L;
  private static final Logger LOGGER = Logger.getLogger(SubscriptionBean.class);
  private List<SubscriptionGroup> groups = new ArrayList<>();
  private final CollectionService collectionService = new CollectionService();
  private User user;
  private boolean showAllCollections = false;

  @PostConstruct
  public void construct() {
    try {
      initUser();
    } catch (ImejiException e) {
      BeanHelper.error(
          "You are not allowed to view the subscriptions for this user, or the user doesn't exists");
      LOGGER.error("Error retrieving user", e);
    }
  }

  /**
   * Init according to url parameters
   */
  public void init() {
    try {
      initGroups(retrieveCollections());
    } catch (ImejiException e) {
      LOGGER.error("Error initializing SubscriptionBean", e);
      BeanHelper.error("Error initializing page: " + e.getMessage());
    }
  }

  /**
   * Init for one collection
   * 
   * @param collection
   */
  public void init(CollectionImeji collection) {
    initGroup(collection);
  }

  /**
   * Retrieve the collections and initialize the list of SubscriptionGroup with them <br/>
   * 
   * 
   * @throws ImejiException
   */
  private void initGroups(List<CollectionImeji> collections) throws ImejiException {
    groups = collections.stream()
        .map(c -> new SubscriptionGroup(c, user == null ? getSessionUser() : user))
        .peek(g -> g.init()).filter(g -> filterSubscriptionGroup(g)).collect(Collectors.toList());
  }



  /**
   * Initialize the Bean for only one collection
   * 
   * @param collection
   */
  private void initGroup(CollectionImeji collection) {
    SubscriptionGroup g = new SubscriptionGroup(collection, getSessionUser());
    g.init();
    groups.add(g);
  }

  /**
   * Check if a SubscriptionGroup should be displayed on the page
   * 
   * @param group
   * @return
   */
  private boolean filterSubscriptionGroup(SubscriptionGroup group) {
    if (user != null) {
      return showAllCollections || group.isSubscribed(user);
    } else {
      return !group.getSubscribedUsers().isEmpty();
    }
  }

  /**
   * Get the user for the email in the url parameter, or the user of the session
   * 
   * @throws ImejiException
   */
  private void initUser() throws ImejiException {
    final String email = UrlHelper.getParameterValue("email");
    if (!StringHelper.isNullOrEmptyTrim(email)) {
      user = new UserService().retrieve(email, getSessionUser());
    }
  }

  /**
   * Retrieve all the collections which can be read by the current user
   * 
   * @return
   * @throws ImejiException
   */
  private List<CollectionImeji> retrieveCollections() throws ImejiException {
    final String colId = UrlHelper.getParameterValue("c");
    if (user != null) {
      if (showAllCollections) {
        return collectionService.searchAndRetrieve(null, null, user, -1, 0);
      } else {
        return collectionService.retrieve(retrieveUserSubscriptions().stream()
            .map(s -> ObjectHelper.getURI(CollectionImeji.class, s.getObjectId()).toString())
            .collect(Collectors.toList()), Imeji.adminUser);
      }
    } else if (!StringHelper.isNullOrEmptyTrim(colId)) {
      return Arrays
          .asList(collectionService.retrieve(ObjectHelper.getURI(CollectionImeji.class, colId),
              user != null ? user : getSessionUser()));
    } else {
      return collectionService.retrieve(
          retrieveAllSubscriptions().stream().filter(distinctByKey(Subscription::getObjectId))
              .map(s -> ObjectHelper.getURI(CollectionImeji.class, s.getObjectId()).toString())
              .collect(Collectors.toList()),
          Imeji.adminUser);
    }
  }

  private static <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
    Set<Object> seen = ConcurrentHashMap.newKeySet();
    return t -> seen.add(keyExtractor.apply(t));
  }

  /**
   * Return all the subscription the user subscribed to
   * 
   * @return
   * @throws ImejiException
   */
  private List<Subscription> retrieveUserSubscriptions() throws ImejiException {
    return new SubscriptionService().retrieveByUserId(user.getId().toString(), user);
  }

  private List<Subscription> retrieveAllSubscriptions() throws ImejiException {
    return new SubscriptionService().retrieveAll(getSessionUser());
  }

  /**
   * Subscribe to the collection
   * 
   * @throws IOException
   */
  public void subscribe(User user, CollectionImeji collection) {
    try {
      new SubscriptionService().subscribe(ImejiFactory.newSubscription().setObjectId(collection)
          .setType(Type.DEFAULT).setUserId(user).build(), getSessionUser());
      reload();
    } catch (Exception e) {
      LOGGER.error("Error subscribing to collection", e);
      BeanHelper.error("Error subscribing to the collection");
    }
  }

  /**
   * Unsubscribe
   * 
   * @param user
   * @param type
   * @param collection
   */
  public void unSubscribe(User user, CollectionImeji collection) {
    try {
      Subscription s = groups.stream()
          .filter(g -> g.getCollection().getIdString().equals(collection.getIdString()))
          .map(g -> g.getSubscriptionForUser(user)).findAny().get();
      new SubscriptionService().unSubscribe(s, user);
      reload();
    } catch (Exception e) {
      LOGGER.error("Error subscribing to collection", e);
      BeanHelper.error("Error un-subscribing from collection");
    }
  }

  /**
   * Subscribe the user of the current session to the collection
   * 
   * @param collection
   */
  public void unSubscribe(CollectionImeji collection) {
    unSubscribe(getSessionUser(), collection);
  }

  /**
   * Unsubscribe the user of the current session
   * 
   * @param collection
   */
  public void subscribe(CollectionImeji collection) {
    subscribe(getSessionUser(), collection);
  }

  /**
   * Subscribe the user of the current session to the current collection (i.e., there is only one
   * SubscriptionGroup)
   * 
   * @param collection
   */
  public void unSubscribe() {
    unSubscribe(getSessionUser(), groups.get(0).getCollection());
  }

  /**
   * Unsubscribe the user of the current session from the current collection (i.e. there is only one
   * SubscriptionGoup)
   * 
   * @param collection
   */
  public void subscribe() {
    subscribe(getSessionUser(), groups.get(0).getCollection());
  }


  /**
   * Unsubscribe (i.e. remove subscription)
   * 
   * @param subscription
   */
  public void unSubscribe(Subscription subscription) {
    try {
      new SubscriptionService().unSubscribe(subscription, getSessionUser());
      initGroups(retrieveCollections());
    } catch (ImejiException e) {
      LOGGER.error("Error subscribing to collection", e);
      BeanHelper.error("Error un-subscribing from collection");
    }
  }

  /**
   * True if the user subscribed to the current collection
   * 
   * @param user
   * @param collection
   * @return
   */
  public boolean isSubscribed(User user, CollectionImeji collection) {
    Optional<SubscriptionGroup> group = groups.stream()
        .filter(g -> g.getCollection().getIdString().equals(collection.getIdString())).findAny();
    return group.isPresent() && group.get().isSubscribed(user);
  }

  /**
   * @return the groups
   */
  public List<SubscriptionGroup> getGroups() {
    return groups;
  }


  /**
   * @param groups the groups to set
   */
  public void setGroups(List<SubscriptionGroup> groups) {
    this.groups = groups;
  }

  public User getUser() {
    return user;
  }

  /**
   * @return the showAllCollections
   */
  public boolean isShowAllCollections() {
    return showAllCollections;
  }

  public String getUserUrl() {
    return getNavigation().getUserUrl() + "?email=\"" + UTF8(user.getEmail()) + "\"";
  }

  public void toggleShowAll() throws ImejiException {
    showAllCollections = showAllCollections ? false : true;
    initGroups(retrieveCollections());
  }
}
