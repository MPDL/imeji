package de.mpg.imeji.logic.db.repositories;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.config.Imeji;
import de.mpg.imeji.logic.model.User;

import java.util.List;

public class UserDbRepository extends DbRepository<User> {

    public UserDbRepository() {
        super(User.class);
    }


    public List<User> readByEmail(String email) throws ImejiException {
        return inSession(em -> {
            return em.createQuery("SELECT u FROM User u WHERE u.email = :email", User.class)
                    .setParameter("email", email)
                    .getResultList();
        });
    }

    public List<User> retrieveAllAdmins() throws ImejiException {
        return inSession(em -> {
            String adminRule = "ADMIN," + Imeji.PROPERTIES.getBaseURI();
            return em.createNativeQuery("select * from Users where jsonb_exists(grants, :adminGrant);", User.class)
                    .setParameter("adminGrant", adminRule)
                    .getResultList();
        });
    }

    public int countObjectsModifiedOrCreated(String id) throws ImejiException {
        return inSession(em -> {
            String adminRule = "ADMIN," + Imeji.PROPERTIES.getBaseURI();
            Object result = em.createNativeQuery("select count(*) from collection c, item i, statement s , facet f, user u where " +
                            "c.modifiedBy = :userId OR c.createdBy :userId OR" +
                            "i.modifiedBy = :userId OR i.createdBy :userId OR" +
                            "s.modifiedBy = :userId OR s.createdBy :userId OR" +
                            "f.modifiedBy = :userId OR f.createdBy :userId OR" +
                            "u.modifiedBy = :userId OR u.createdBy :userId")
                    .setParameter("userId", id)
                    .getSingleResult();
            return ((Number) result).intValue();
        });
    }

    public long getFileSize(String userId) throws ImejiException {
        return inSession(em -> {
            String adminRule = "ADMIN," + Imeji.PROPERTIES.getBaseURI();
            Object result = em.createNativeQuery("select sum(filesize) from item where createdBy=:creatorId AND status='http://imeji.org/terms/status#WITHDRAWN';")
                    .setParameter("creatorId", userId)
                    .getSingleResult();
            return ((Number) result).longValue();
        });
    }


}
