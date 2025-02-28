package de.mpg.imeji.logic.db.writer;

import de.mpg.imeji.exceptions.ImejiException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

import java.util.function.Consumer;

import static jakarta.persistence.Persistence.createEntityManagerFactory;

public class EntityManagerHelper {

  public static EntityManagerFactory factory = createEntityManagerFactory("imeji-persistence-unit");

  public static void inSession(Consumer<EntityManager> work) throws ImejiException {
    var entityManager = factory.createEntityManager();
    var transaction = entityManager.getTransaction();
    try {
      transaction.begin();
      work.accept(entityManager);
      transaction.commit();
    } catch (Exception e) {
      if (transaction.isActive())
        transaction.rollback();
      throw new ImejiException("Error with database",e);
    } finally {
      entityManager.close();
    }
  }
}
