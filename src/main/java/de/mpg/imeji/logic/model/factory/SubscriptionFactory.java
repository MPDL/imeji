package de.mpg.imeji.logic.model.factory;

import java.net.URI;

import de.mpg.imeji.logic.model.CollectionImeji;
import de.mpg.imeji.logic.model.Subscription;
import de.mpg.imeji.logic.model.User;

/**
 * Factory to build {@link Subscription}
 * 
 * @author saquet
 *
 */
public class SubscriptionFactory {

  private final Subscription subscription = new Subscription();

  public Subscription build() {
    return subscription;
  }

  public SubscriptionFactory setId(URI uri) {
    subscription.setId(uri);
    return this;
  }

  public SubscriptionFactory setObjectId(CollectionImeji collection) {
    subscription.setObjectId(collection.getId().toString());
    return this;
  }

  public SubscriptionFactory setType(Subscription.Type type) {
    subscription.setType(type);
    return this;
  }

  public SubscriptionFactory setUserId(User user) {
    subscription.setUserId(user.getId().toString());
    return this;
  }

}
