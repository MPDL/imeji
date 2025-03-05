package de.mpg.imeji.logic.db.repositories;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.model.Statement;
import de.mpg.imeji.logic.model.User;

import java.util.List;

public class StatementDbRepository extends DbRepository<Statement> {

    public StatementDbRepository() {
        super(Statement.class);
    }

    public List<Statement> readByIndex(String index) throws ImejiException {
        return inSession(em -> {
            return em.createQuery("SELECT u FROM Statement u WHERE u.index = :index", Statement.class)
                    .setParameter("index", index)
                    .getResultList();
        });
    }
}
