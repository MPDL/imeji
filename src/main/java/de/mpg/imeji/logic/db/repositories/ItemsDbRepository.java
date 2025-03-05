package de.mpg.imeji.logic.db.repositories;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.config.Imeji;
import de.mpg.imeji.logic.model.Item;
import de.mpg.imeji.logic.model.User;

import java.net.URI;
import java.util.List;

public class ItemsDbRepository extends DbRepository<Item> {

    public ItemsDbRepository() {
        super(Item.class);
    }

    public List<Item> retrieveAllItemsForCollectionWithoutLicense(String collectionId) throws ImejiException {
        return inSession(em -> {

            return em.createNativeQuery("select * from item where collection = :collId  AND (licenses IS NULL OR jsonb_array_length( licenses::jsonb ) = 0)", Item.class)
                    .setParameter("collId", collectionId)
                    .getResultList();
        });
    }
}
