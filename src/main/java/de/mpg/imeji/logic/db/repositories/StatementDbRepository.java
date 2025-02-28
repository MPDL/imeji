package de.mpg.imeji.logic.db.repositories;

import de.mpg.imeji.logic.model.Statement;
import de.mpg.imeji.logic.model.User;

public class StatementDbRepository extends DbRepository<Statement> {

    public StatementDbRepository() {
        super(Statement.class);
    }
}
