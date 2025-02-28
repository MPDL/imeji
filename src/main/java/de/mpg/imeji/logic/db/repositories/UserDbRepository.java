package de.mpg.imeji.logic.db.repositories;

import de.mpg.imeji.logic.model.User;

import java.util.List;

public class UserDbRepository extends DbRepository<User> {

    public UserDbRepository() {
        super(User.class);
    }

    public List<User> retrieveAllAdmins() {
        return inSession(em -> {
            return em.createQuery("select u from User u WHERE u.", User.class)
        });
    }
}
