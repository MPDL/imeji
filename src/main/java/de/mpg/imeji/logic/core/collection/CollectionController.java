package de.mpg.imeji.logic.core.collection;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.config.Imeji;
import de.mpg.imeji.logic.db.reader.ReaderFacade;
import de.mpg.imeji.logic.db.writer.WriterFacade;
import de.mpg.imeji.logic.generic.ImejiControllerAbstract;
import de.mpg.imeji.logic.model.CollectionImeji;
import de.mpg.imeji.logic.model.Grant;
import de.mpg.imeji.logic.model.Grant.GrantType;
import de.mpg.imeji.logic.model.User;
import de.mpg.imeji.logic.model.factory.ImejiFactory;
import de.mpg.imeji.logic.security.user.UserService;

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
  public List<CollectionImeji> createBatch(List<CollectionImeji> l, User user) throws ImejiException {
    for (final CollectionImeji c : l) {
      prepareCreate(c, user);
    }
    List<CollectionImeji> createdCollections = this.fromObjectList(WRITER.create(toObjectList(l), user));
    for (final CollectionImeji c : l.stream().filter(c -> !c.isSubCollection()).collect(Collectors.toList())) {
      updateCreatorGrants(user, c.getId().toString());
    }
    return createdCollections;
  }

  @Override
  public List<CollectionImeji> retrieveBatch(List<String> ids, User user) throws ImejiException {
    final List<CollectionImeji> l = ids.stream().map(id -> ImejiFactory.newCollection().setUri(id).build()).collect(Collectors.toList());
    READER.read(toObjectList(l), user);
    return l;
  }

  @Override
  public List<CollectionImeji> retrieveBatchLazy(List<String> ids, User user) throws ImejiException {
    final List<CollectionImeji> l = ids.stream().map(id -> ImejiFactory.newCollection().setUri(id).build()).collect(Collectors.toList());
    READER.readLazy(toObjectList(l), user);
    return l;
  }

  @Override
  public List<CollectionImeji> updateBatch(List<CollectionImeji> l, User user) throws ImejiException {
    for (final CollectionImeji c : l) {
      prepareUpdate(c, user);
    }
    List<CollectionImeji> updatedCollections = this.fromObjectList(WRITER.update(toObjectList(l), user, true));
    return updatedCollections;
  }

  @Override
  public void deleteBatch(List<CollectionImeji> l, User user) throws ImejiException {
    WRITER.delete(toObjectList(l), user);
  }


  @Override
  public List<CollectionImeji> fromObjectList(List<?> objectList) {
    List<CollectionImeji> collectionList = new ArrayList<CollectionImeji>(0);
    if (!objectList.isEmpty()) {
      if (objectList.get(0) instanceof CollectionImeji) {
        collectionList = (List<CollectionImeji>) objectList;
      }
    }
    return collectionList;
  }



  /**
   * Update the grants of the user who created the objects
   *
   * @param user
   * @param uri
   * @throws ImejiException
   */
  private void updateCreatorGrants(User user, String uri) throws ImejiException {
    user = new UserService().addEditGrantToUser(Imeji.adminUser, user, new Grant(GrantType.ADMIN, uri));
  }



}
