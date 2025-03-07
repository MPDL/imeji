package de.mpg.imeji.logic.db.repositories;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.model.CollectionImeji;
import de.mpg.imeji.logic.model.Subscription;
import de.mpg.imeji.logic.model.User;

import java.util.List;

public class SubscriptionDbRepository extends DbRepository<Subscription> {

  public SubscriptionDbRepository() {
    super(Subscription.class);
  }

  public List<Subscription> readByObjectId(String objectId) throws ImejiException {
    return inSession(em -> {
      return em.createQuery("SELECT u FROM Subscription u WHERE u.objectId = :objectId", Subscription.class)
          .setParameter("objectId", objectId).getResultList();
    });
  }

  public List<Subscription> readByUserId(String objectId) throws ImejiException {
    return inSession(em -> {
      return em.createQuery("SELECT u FROM Subscription u WHERE u.userId = :objectId", Subscription.class)
          .setParameter("objectId", objectId).getResultList();
    });
  }

  public List<Subscription> readByType(Subscription.Type type) throws ImejiException {
    return inSession(em -> {
      return em.createQuery("SELECT u FROM Subscription u WHERE u.type = :type", Subscription.class).setParameter("type", type.name())
          .getResultList();
    });
  }
}
