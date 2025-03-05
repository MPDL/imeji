package de.mpg.imeji.logic.db.repositories;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.model.User;
import de.mpg.imeji.logic.model.UserGroup;

import java.util.List;

public class UserGroupDbRepository extends DbRepository<UserGroup> {

    public UserGroupDbRepository() {
        super(UserGroup.class);
    }

    public UserGroup readByName(String name) throws ImejiException {
        return inSession(em -> {
            return em.createQuery("SELECT u FROM UserGroup u WHERE u.name = :name", UserGroup.class)
                    .setParameter("name", name)
                    .getSingleResult();
        });
    }

    public List<UserGroup> searchByName(String name) throws ImejiException {
        return inSession(em -> {
            return em.createQuery("SELECT u FROM UserGroup u WHERE u.name ilike :name", UserGroup.class)
                    .setParameter("name", "%" + name + "%")
                    .getResultList();
        });
    }
}
