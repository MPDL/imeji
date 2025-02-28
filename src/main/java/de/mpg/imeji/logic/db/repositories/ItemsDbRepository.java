package de.mpg.imeji.logic.db.repositories;

import de.mpg.imeji.logic.model.Item;
import de.mpg.imeji.logic.model.User;

public class ItemsDbRepository extends DbRepository<Item> {

    public ItemsDbRepository() {
        super(Item.class);
    }
}
