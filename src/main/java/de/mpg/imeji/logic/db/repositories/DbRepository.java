package de.mpg.imeji.logic.db.repositories;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.db.writer.EntityManagerHelper;
import de.mpg.imeji.logic.util.ObjectHelper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import org.apache.jena.assembler.Mode;
import org.apache.jena.query.Dataset;
import org.bouncycastle.math.raw.Mod;

import java.net.URI;
import java.util.function.Consumer;
import java.util.function.Function;

public abstract class DbRepository<ModelType> {

    private final Class<ModelType> classType;

    public DbRepository(Class<ModelType> classType) {
        this.classType = classType;
    }

    public ModelType create(ModelType object) throws ImejiException {
        inSession((em) -> {
            em.persist(object);
            return null;
        });
        return object;
    }

    public ModelType update(ModelType object) throws ImejiException {
       return inSession(em -> {
            return em.merge(object);
        });
    }

    public ModelType read(String id) throws ImejiException {
        return inSession(em -> {
            return em.find(classType, id);
        });
    }


    public int delete(String id) throws ImejiException {
        return inSession(em -> {
            return em.createQuery("delete from " + classType.getSimpleName() + " where dbid = :id")
                    .setParameter("id", id)
                    .executeUpdate();
        });
    }

    public void delete(ModelType object) throws ImejiException {
        inSession(em -> {
            em.remove(object);
            return null;
        });
    }




    public static <T> T inSession(Function<EntityManager, T> work) throws ImejiException {
        EntityManager entityManager = EntityManagerHelper.factory.createEntityManager();
        EntityTransaction transaction = entityManager.getTransaction();
        try {
            transaction.begin();
            T retVal = work.apply(entityManager);
            //work.accept(entityManager);
            transaction.commit();
            return retVal;
        } catch (Exception e) {
            if (transaction.isActive())
                transaction.rollback();
            throw new ImejiException("Error with database",e);
        } finally {
            entityManager.close();
        }
    }


    public static DbRepository getRepositoryForModel(String modelURI)
    {
        ObjectHelper.ObjectType type = ObjectHelper.getObjectType(URI.create(modelURI));
        //
        switch (type) {
            case ITEM: {
                return new ItemsDbRepository();

            }
            case FACET: {
                return new FacetDbRepository();

            }
            case COLLECTION: {
                return new CollectionsDbRepository();
            }
            case USER: {
                return new UserDbRepository();
            }
            case USERGROUP: {
                return new UserGroupDbRepository();
            }
        }
        return null;
    }


}
