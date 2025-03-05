package de.mpg.imeji.logic.db.repositories;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.model.CollectionImeji;
import de.mpg.imeji.logic.model.Item;

import java.util.List;

public class CollectionsDbRepository extends DbRepository<CollectionImeji> {

    public CollectionsDbRepository() {
        super(CollectionImeji.class);
    }


    public List<String> retrieveAllSubCollectionIds() throws ImejiException {
        return inSession(em -> {
            return em.createQuery("select i.dbId from CollectionImeji i WHERE i.collection IS NOT NULL", String.class)
                    .getResultList();
        });
    }
}
