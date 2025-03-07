package de.mpg.imeji.logic.db.writer;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.config.util.PropertyReader;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.function.Consumer;

import static jakarta.persistence.Persistence.createEntityManagerFactory;

public class EntityManagerHelper {

  public static EntityManagerFactory factory;

  private static final Logger LOGGER = LogManager.getLogger(EntityManagerHelper.class);

  static {
    try {
      factory = createEntityManagerFactory("imeji-persistence-unit", PropertyReader.loadProperties("imeji-db.properties"));
    } catch (IOException e) {
      LOGGER.error("Error reading imeji-db.properties", e);
      throw new RuntimeException(e);
    }
  }

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
      throw new ImejiException("Error with database", e);
    } finally {
      entityManager.close();
    }
  }
}
