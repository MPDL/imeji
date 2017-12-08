package de.mpg.imeji.logic.notification.subscription;

import java.io.Serializable;
import java.util.List;
import java.util.stream.Collectors;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.config.Imeji;
import de.mpg.imeji.logic.model.Subscription;
import de.mpg.imeji.logic.model.User;
import de.mpg.imeji.logic.search.jenasearch.ImejiSPARQL;
import de.mpg.imeji.logic.search.jenasearch.JenaCustomQueries;

/**
 * Service to manage {@link Subscription}
 * 
 * @author saquet
 *
 */
public class SubscriptionService implements Serializable {
  private static final long serialVersionUID = 8078709382388541728L;
  private final SubscriptionController controller = new SubscriptionController();


  /**
   * Add the subscription
   * 
   * @param subscription
   * @param user
   * @throws ImejiException
   */
  public void subscribe(Subscription subscription, User user) throws ImejiException {
    controller.create(subscription, user);
  }

  /**
   * Remove the subscription
   * 
   * @param subscription
   * @param user
   * @throws ImejiException
   */
  public void unSubscribe(Subscription subscription, User user) throws ImejiException {
    controller.delete(subscription, user);
  }

  /**
   * Retrieve all {@link Subscription} for this objectId
   * 
   * @param objectId
   * @param user
   * @return
   * @throws ImejiException
   */
  public List<Subscription> retrieveByObjectId(String objectId, User user) throws ImejiException {
    return controller.retrieveBatch(
        ImejiSPARQL.exec(JenaCustomQueries.selectSubscriptionByObjectId(objectId), Imeji.userModel),
        user);
  }

  /**
   * Retrieve all subscriptions of the user with this id
   * 
   * @param userId
   * @param user
   * @return
   * @throws ImejiException
   */
  public List<Subscription> retrieveByUserId(String userId, User user) throws ImejiException {
    return controller.retrieveBatch(
        ImejiSPARQL.exec(JenaCustomQueries.selectSubscriptionByUserId(userId), Imeji.userModel),
        user);
  }

  public void sendEmails(Subscription.Type type) {

  }

  /**
   * Return all subscription for one type
   * 
   * @param type
   * @param user
   * @return
   * @throws ImejiException
   */
  public List<Subscription> retrieveByType(Subscription.Type type, User user)
      throws ImejiException {
    return retrieveAll(user).stream().filter(s -> s.getType() == type).collect(Collectors.toList());
  }

  /**
   * Retrieve all subscriptions
   * 
   * @param user
   * @return
   * @throws ImejiException
   */
  public List<Subscription> retrieveAll(User user) throws ImejiException {
    return controller.retrieveBatch(
        ImejiSPARQL.exec(JenaCustomQueries.selectSubscriptionAll(), Imeji.userModel), user);
  }
}
