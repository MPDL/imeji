package de.mpg.imeji.logic.db.repositories;

import de.mpg.imeji.logic.model.CollectionImeji;
import de.mpg.imeji.logic.model.Item;

public class CollectionsDbRepository extends DbRepository<CollectionImeji> {

    public CollectionsDbRepository() {
        super(CollectionImeji.class);
    }
}
