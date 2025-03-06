package de.mpg.imeji.logic.db.repositories;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.db.writer.EntityManagerHelper;
import de.mpg.imeji.logic.model.Item;
import de.mpg.imeji.logic.model.User;
import de.mpg.imeji.logic.util.ObjectHelper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import org.apache.jena.assembler.Mode;
import org.apache.jena.query.Dataset;
import org.bouncycastle.math.raw.Mod;

import java.net.URI;
import java.util.List;
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


    public void delete(String id) throws ImejiException {
        inSession(em -> {
            ModelType obj = em.find(classType, id);
            em.remove(obj);
            return null;
        });
    }

    public void delete(ModelType object) throws ImejiException {
        inSession(em -> {
            em.remove(object);
            return null;
        });
    }

    public List<ModelType> retrieveAll() throws ImejiException {
        return inSession(em -> {
            String className = classType.getSimpleName();
            return em.createQuery("select u from " + className + " u", classType)
                    .getResultList();
        });
    }

    public List<ModelType> retrieveByID(List<String> ids) throws ImejiException {
        return inSession(em -> {
            String className = classType.getSimpleName();
            return em.createQuery("select u from " + className + " u WHERE u.dbId IN :ids", classType)
                    .setParameter("ids", ids)
                    .getResultList();
        });
    }

    public List<ModelType> readByCreator(String creatorId) throws ImejiException {
        return inSession(em -> {
            String className = classType.getSimpleName();
            return em.createQuery("select u from " + className + " u WHERE u.createdBy = :creatorId", classType)
                    .setParameter("creatorId", creatorId)
                    .getResultList();
        });
    }

    public List<String> retrieveAllIds() throws ImejiException {
        return inSession(em -> {
            String className = classType.getSimpleName();
            return em.createQuery("select u.dbId from " + className + " u", String.class)
                    .getResultList();
        });
    }



    public long countAll() throws ImejiException {
        return inSession(em -> {
            String className = classType.getSimpleName();
            Object res = em.createQuery("select count(*) from " + className + " u")
                    .getSingleResult();
            return ((Number) res).longValue();
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


    public static DbRepository getRepositoryForModel(ObjectHelper.ObjectType type)
    {

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
            case CONTENT: {
                return new ContentDbRepository();
            }
            case USER: {
                return new UserDbRepository();
            }
            case USERGROUP: {
                return new UserGroupDbRepository();
            }
            case STATEMENT: {
                return new StatementDbRepository();
            }
            case SUBSCRIPTION: {
                return new SubscriptionDbRepository();
            }
        }
        return null;
    }


}
