package de.mpg.imeji.logic.core.collection;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import org.apache.log4j.Logger;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.exceptions.UnprocessableError;
import de.mpg.imeji.logic.config.Imeji;
import de.mpg.imeji.logic.core.facade.MoveFacade;
import de.mpg.imeji.logic.core.facade.WorkflowFacade;
import de.mpg.imeji.logic.core.item.ItemService;
import de.mpg.imeji.logic.events.Message;
import de.mpg.imeji.logic.events.Message.MessageType;
import de.mpg.imeji.logic.events.MessageService;
import de.mpg.imeji.logic.generic.SearchServiceAbstract;
import de.mpg.imeji.logic.model.CollectionImeji;
import de.mpg.imeji.logic.model.Item;
import de.mpg.imeji.logic.model.License;
import de.mpg.imeji.logic.model.Properties.Status;
import de.mpg.imeji.logic.model.User;
import de.mpg.imeji.logic.search.Search;
import de.mpg.imeji.logic.search.Search.SearchObjectTypes;
import de.mpg.imeji.logic.search.elasticsearch.ElasticIndexer;
import de.mpg.imeji.logic.search.elasticsearch.ElasticService;
import de.mpg.imeji.logic.search.elasticsearch.ElasticService.ElasticTypes;
import de.mpg.imeji.logic.search.factory.SearchFactory;
import de.mpg.imeji.logic.search.factory.SearchFactory.SEARCH_IMPLEMENTATIONS;
import de.mpg.imeji.logic.search.jenasearch.ImejiSPARQL;
import de.mpg.imeji.logic.search.jenasearch.JenaCustomQueries;
import de.mpg.imeji.logic.search.model.SearchQuery;
import de.mpg.imeji.logic.search.model.SearchResult;
import de.mpg.imeji.logic.search.model.SortCriterion;
import de.mpg.imeji.logic.security.authorization.util.SecurityUtil;
import de.mpg.imeji.logic.util.ObjectHelper;

/**
 * CRUD controller for {@link CollectionImeji}, plus search mehtods related to
 * {@link CollectionImeji}
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public class CollectionService extends SearchServiceAbstract<CollectionImeji> {
  private static final Logger LOGGER = Logger.getLogger(CollectionService.class);
  private final Search search =
      SearchFactory.create(SearchObjectTypes.COLLECTION, SEARCH_IMPLEMENTATIONS.ELASTIC);
  private final CollectionController controller = new CollectionController();
  private final MessageService messageService = new MessageService();

  /**
   * Default constructor
   */
  public CollectionService() {
    super(SearchObjectTypes.COLLECTION);
  }

  /**
   * Creates a new collection. - Add a unique id - Write user properties
   *
   * @param c
   * @param p
   * @param user
   * @param method
   * @return
   * @throws ImejiException
   */
  public CollectionImeji create(CollectionImeji c, User user) throws ImejiException {
    isLoggedInUser(user);
    return controller.create(c, user);
  }

  /**
   * Retrieve a complete {@link CollectionImeji} (inclusive its {@link Item}: slow for huge
   * {@link CollectionImeji})
   *
   * @param uri
   * @param user
   * @return
   * @throws ImejiException
   */
  public CollectionImeji retrieve(URI uri, User user) throws ImejiException {
    return controller.retrieve(uri.toString(), user);
  }

  /**
   * Retrieve a complete {@link CollectionImeji} (inclusive its {@link Item}: slow for huge
   * {@link CollectionImeji})
   *
   * @param uri
   * @param user
   * @return
   * @throws ImejiException
   */
  public CollectionImeji retrieve(String uri, User user) throws ImejiException {
    return controller.retrieve(uri, user);
  }

  /**
   * Retrieve the {@link CollectionImeji} without its {@link Item}
   *
   * @param uri
   * @param user
   * @return
   * @throws ImejiException
   */
  public CollectionImeji retrieveLazy(URI uri, User user) throws ImejiException {
    return controller.retrieveLazy(uri.toString(), user);
  }


  /**
   * Update a {@link CollectionImeji} (inclusive its {@link Item}: slow for huge
   * {@link CollectionImeji})
   *
   * @param ic
   * @param user
   * @throws ImejiException
   */
  public CollectionImeji update(CollectionImeji ic, User user) throws ImejiException {
    return controller.update(ic, user);
  }


  /**
   * Update a {@link CollectionImeji} (with its Logo)
   *
   * @param ic
   * @param hasgrant
   * @throws ImejiException
   */
  public void updateLogo(CollectionImeji ic, File f, User u)
      throws ImejiException, IOException, URISyntaxException {
    ic = (CollectionImeji) setLogo(ic, f);
    update(ic, u);
  }


  /**
   * Delete a {@link CollectionImeji} and all its {@link Item}
   *
   * @param collection
   * @param user
   * @throws ImejiException
   */
  public void delete(CollectionImeji collection, User user) throws ImejiException {
    final ItemService itemService = new ItemService();
    final List<String> itemUris =
        itemService.search(collection.getId(), null, null, user, -1, 0).getResults();
    if (hasImageLocked(itemUris, user)) {
      throw new UnprocessableError("Collection can not be deleted: It contains locked items");
    } else {
      if (collection.getStatus() != Status.PENDING
          && !SecurityUtil.authorization().isSysAdmin(user)) {
        throw new UnprocessableError("collection_is_not_pending");
      }
      // Delete items
      final List<Item> items = (List<Item>) itemService.retrieveBatch(itemUris, -1, 0, user);
      for (final Item it : items) {
        if (it.getStatus().equals(Status.RELEASED)) {
          throw new UnprocessableError("collection_has_released_items");
        }
      }
      itemService.delete(items, user);
      controller.delete(collection, user);
      messageService.add(
          new Message(MessageType.DELETE_COLLECTION, ObjectHelper.getId(collection.getId()), null));
    }
  }



  /**
   * Release a {@link CollectionImeji} and all its {@link Item}
   *
   * @param collection
   * @param user
   * @param defaultLicense TODO
   * @throws ImejiException
   */
  public void release(CollectionImeji collection, User user, License defaultLicense)
      throws ImejiException {
    new WorkflowFacade().release(collection, user, defaultLicense);
    /*
     * final ItemService itemController = new ItemService(); isLoggedInUser(user);
     * 
     * if (collection == null) { throw new NotFoundException("collection object does not exists"); }
     * 
     * prepareRelease(collection, user); final List<String> itemUris =
     * itemController.search(collection.getId(), null, null, user, -1, 0).getResults();
     * 
     * if (hasImageLocked(itemUris, user)) { throw new
     * UnprocessableError("Collection has locked items: can not be released"); } else if
     * (itemUris.isEmpty()) { throw new
     * UnprocessableError("An empty collection can not be released!"); } else { final List<Item>
     * items = (List<Item>) itemController.retrieveBatch(itemUris, -1, 0, user);
     * itemController.release(items, user, defaultLicense); update(collection, user); }
     */
  }

  /**
   * Release a collection and set the instance default license to items without licenses
   *
   * @param collection
   * @param user
   * @throws ImejiException
   */
  public void releaseWithDefaultLicense(CollectionImeji collection, User user)
      throws ImejiException {
    release(collection, user, getDefaultLicense());
  }

  /**
   * Withdraw a {@link CollectionImeji} and all its {@link Item}
   *
   * @param coll
   * @throws ImejiException
   */
  public void withdraw(CollectionImeji coll, User user) throws ImejiException {
    new WorkflowFacade().withdraw(coll, coll.getDiscardComment(), user);
    /*
     * final ItemService itemController = new ItemService(); isLoggedInUser(user);
     * 
     * if (coll == null) { throw new NotFoundException("Collection does not exists"); }
     * prepareWithdraw(coll, null); final List<String> itemUris =
     * itemController.search(coll.getId(), null, null, user, -1, 0).getResults(); if
     * (hasImageLocked(itemUris, user)) { throw new
     * UnprocessableError("Collection has locked images: can not be withdrawn"); } else { final
     * List<Item> items = (List<Item>) itemController.retrieveBatch(itemUris, -1, 0, user);
     * itemController.withdraw(items, coll.getDiscardComment(), user); update(coll, user); }
     */
  }

  /**
   * Move the collection to another collection. If the new parent collection is released, release
   * the moved collection and to its items the license
   * 
   * @param collection
   * @param parent
   * @param user
   * @param license
   * @throws ImejiException
   */
  public void moveCollection(CollectionImeji collection, CollectionImeji parent, User user,
      License license) throws ImejiException {
    new MoveFacade().moveCollection(collection, parent, user, license);
  }

  @Override
  public SearchResult search(SearchQuery searchQuery, SortCriterion sortCri, User user, int size,
      int offset) {
    return search.search(searchQuery, sortCri, user, null, offset, size);
  }

  @Override
  public List<CollectionImeji> retrieve(List<String> ids, User user) throws ImejiException {
    return controller.retrieveBatchLazy(ids, user);
  }

  @Override
  public List<CollectionImeji> retrieveAll() throws ImejiException {
    final List<String> uris =
        ImejiSPARQL.exec(JenaCustomQueries.selectCollectionAll(), Imeji.collectionModel);
    return retrieve(uris, Imeji.adminUser);
  }

  /**
   * Reindex all collections
   *
   * @param index
   * @throws ImejiException
   */
  public void reindex(String index) throws ImejiException {
    LOGGER.info("Indexing collections...");
    final ElasticIndexer indexer =
        new ElasticIndexer(index, ElasticTypes.folders, ElasticService.ANALYSER);
    final List<CollectionImeji> collections = retrieveAll();
    indexer.indexBatch(collections);
    LOGGER.info("collections reindexed!");
  }
}
