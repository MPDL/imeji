package de.mpg.imeji.presentation.subscription;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import org.apache.log4j.Logger;

import de.mpg.imeji.exceptions.ImejiException;
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

  public void init() {
    try {
      initUser();
      initGroups();
    } catch (ImejiException e) {
      LOGGER.error("Error initializing SubscriptionBean", e);
      BeanHelper.error("Error initializing page: " + e.getMessage());
    }
  }

  /**
   * Retrieve the collections and initialize the list of SubscriptionGroup with them <br/>
   * 
   * 
   * @throws ImejiException
   */
  private void initGroups() throws ImejiException {
    groups = retrieveCollections().stream().map(c -> new SubscriptionGroup(c, getSessionUser()))
        .peek(g -> g.init()).filter(g -> filterSubscriptionGroup(g)).collect(Collectors.toList());
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
    if (StringHelper.isNullOrEmptyTrim(colId)) {
      return collectionService.searchAndRetrieve(null, null, user != null ? user : getSessionUser(),
          -1, 0);
    } else {
      return Arrays
          .asList(collectionService.retrieve(ObjectHelper.getURI(CollectionImeji.class, colId),
              user != null ? user : getSessionUser()));
    }
  }

  /**
   * Subscribe to the collection
   */
  public void subscribe(User user, CollectionImeji collection) {
    try {
      new SubscriptionService().subscribe(ImejiFactory.newSubscription().setObjectId(collection)
          .setType(Type.UPLOAD).setUserId(user).build(), getSessionUser());
      initGroups();
    } catch (ImejiException e) {
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
      new SubscriptionService().unSubscribe(ImejiFactory.newSubscription().setObjectId(collection)
          .setType(Type.UPLOAD).setUserId(user).build(), user);
      initGroups();
    } catch (ImejiException e) {
      LOGGER.error("Error subscribing to collection", e);
      BeanHelper.error("Error un-subscribing from collection");
    }
  }

  /**
   * Unsubscribe (i.e. remove subscription)
   * 
   * @param subscription
   */
  public void unSubscribe(Subscription subscription) {
    try {
      new SubscriptionService().unSubscribe(subscription, getSessionUser());
      initGroups();
    } catch (ImejiException e) {
      LOGGER.error("Error subscribing to collection", e);
      BeanHelper.error("Error un-subscribing from collection");
    }
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


  public void toggleShowAll() throws ImejiException {
    showAllCollections = showAllCollections ? false : true;
    initGroups();
  }
}
