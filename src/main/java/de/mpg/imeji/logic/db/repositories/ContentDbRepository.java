package de.mpg.imeji.logic.db.repositories;

import de.mpg.imeji.logic.model.ContentVO;
import de.mpg.imeji.logic.model.Item;

public class ContentDbRepository extends DbRepository<ContentVO> {

    public ContentDbRepository() {
        super(ContentVO.class);
    }
}
