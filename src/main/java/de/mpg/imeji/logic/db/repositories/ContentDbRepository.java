package de.mpg.imeji.logic.db.repositories;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.config.Imeji;
import de.mpg.imeji.logic.model.ContentVO;
import de.mpg.imeji.logic.model.Item;
import de.mpg.imeji.logic.model.User;

import java.util.List;

public class ContentDbRepository extends DbRepository<ContentVO> {

    public ContentDbRepository() {
        super(ContentVO.class);
    }


    public List<ContentVO> retrieveAllContentWithFile(String filePath) throws ImejiException {
        return inSession(em -> {
            return em.createQuery("SELECT c FROM ContentVO c WHERE c.thumbnail ILIKE :filePath OR c.preview ILIKE :filePath OR c.full ILIKE :filePath", ContentVO.class)
                    .setParameter("filePath", "%" + filePath + "%")
                    .getResultList();
        });
    }

    public List<ContentVO> retrieveUnusedContent() throws ImejiException {
        return inSession(em -> {
            return em.createNativeQuery("SELECT * FROM content WHERE itemid NOT IN (SELECT dbId FROM item)", ContentVO.class)
                    .getResultList();
        });
    }

}
