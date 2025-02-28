package de.mpg.imeji.logic.db.repositories;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.config.Imeji;
import de.mpg.imeji.logic.model.User;

import java.util.List;

public class UserDbRepository extends DbRepository<User> {

    public UserDbRepository() {
        super(User.class);
    }

    public List<User> retrieveAllAdmins() throws ImejiException {
        return inSession(em -> {
            String adminRule = "ADMIN," + Imeji.PROPERTIES.getBaseURI();
            return em.createNativeQuery("select * from Users where jsonb_exists(grants, :adminGrant);", User.class)
                    .setParameter("adminGrant", adminRule)
                    .getResultList();
        });
    }


}
