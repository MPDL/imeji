package de.mpg.imeji.logic.db.repositories;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.model.CollectionImeji;
import de.mpg.imeji.logic.model.Item;

import java.util.List;

public class CollectionsDbRepository extends DbRepository<CollectionImeji> {

    public CollectionsDbRepository() {
        super(CollectionImeji.class);
    }


    public List<CollectionImeji> retrieveAllSubCollections() throws ImejiException {
        return inSession(em -> {
            return em.createQuery("select i from CollectionImeji i WHERE i.collection IS NOT NULL", CollectionImeji.class)
                    .getResultList();
        });
    }
}
