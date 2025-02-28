package de.mpg.imeji.logic.db.repositories;

import de.mpg.imeji.logic.model.UserGroup;

public class UserGroupDbRepository extends DbRepository<UserGroup> {

    public UserGroupDbRepository() {
        super(UserGroup.class);
    }
}
