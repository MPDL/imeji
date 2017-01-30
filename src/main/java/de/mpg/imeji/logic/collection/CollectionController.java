package de.mpg.imeji.logic.collection;

import java.util.List;
import java.util.stream.Collectors;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.config.Imeji;
import de.mpg.imeji.logic.db.reader.ReaderFacade;
import de.mpg.imeji.logic.db.writer.WriterFacade;
import de.mpg.imeji.logic.service.ImejiControllerAbstract;
import de.mpg.imeji.logic.vo.CollectionImeji;
import de.mpg.imeji.logic.vo.User;
import de.mpg.imeji.logic.vo.factory.ImejiFactory;

/**
 * Controller for {@link CollectionImeji}
 * 
 * @author saquet
 *
 */
class CollectionController extends ImejiControllerAbstract<CollectionImeji> {
  private static final ReaderFacade READER = new ReaderFacade(Imeji.collectionModel);
  private static final WriterFacade WRITER = new WriterFacade(Imeji.collectionModel);

  @Override
  public List<CollectionImeji> createBatch(List<CollectionImeji> l, User user)
      throws ImejiException {
    for (final CollectionImeji c : l) {
      prepareCreate(c, user);
    }
    WRITER.create(toObjectList(l), user);
    for (final CollectionImeji c : l) {
      updateCreatorGrants(user, c.getId().toString());
    }
    return l;
  }

  @Override
  public List<CollectionImeji> retrieveBatch(List<String> ids, User user) throws ImejiException {
    final List<CollectionImeji> l = ids.stream()
        .map(id -> ImejiFactory.newCollection().setId(id).build()).collect(Collectors.toList());
    READER.read(toObjectList(l), user);
    return l;
  }

  @Override
  public List<CollectionImeji> retrieveBatchLazy(List<String> ids, User user)
      throws ImejiException {
    final List<CollectionImeji> l = ids.stream()
        .map(id -> ImejiFactory.newCollection().setId(id).build()).collect(Collectors.toList());
    READER.readLazy(toObjectList(l), user);
    return l;
  }

  @Override
  public List<CollectionImeji> updateBatch(List<CollectionImeji> l, User user)
      throws ImejiException {
    for (final CollectionImeji c : l) {
      prepareUpdate(c, user);
    }
    WRITER.update(toObjectList(l), user, true);
    return l;
  }

  @Override
  public void deleteBatch(List<CollectionImeji> l, User user) throws ImejiException {
    WRITER.delete(toObjectList(l), user);
  }

}
