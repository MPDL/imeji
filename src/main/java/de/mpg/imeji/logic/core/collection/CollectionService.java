package de.mpg.imeji.logic.core.collection;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.exceptions.UnprocessableError;
import de.mpg.imeji.logic.config.Imeji;
import de.mpg.imeji.logic.core.facade.MoveFacade;
import de.mpg.imeji.logic.core.facade.WorkflowFacade;
import de.mpg.imeji.logic.core.item.ItemService;
import de.mpg.imeji.logic.events.MessageService;
import de.mpg.imeji.logic.events.messages.CollectionMessage;
import de.mpg.imeji.logic.events.messages.Message.MessageType;
import de.mpg.imeji.logic.generic.SearchServiceAbstract;
import de.mpg.imeji.logic.hierarchy.HierarchyService;
import de.mpg.imeji.logic.model.CollectionImeji;
import de.mpg.imeji.logic.model.Item;
import de.mpg.imeji.logic.model.License;
import de.mpg.imeji.logic.model.Properties.Status;
import de.mpg.imeji.logic.model.User;
import de.mpg.imeji.logic.model.factory.ImejiFactory;
import de.mpg.imeji.logic.search.Search;
import de.mpg.imeji.logic.search.Search.SearchObjectTypes;
import de.mpg.imeji.logic.search.elasticsearch.ElasticIndexer;
import de.mpg.imeji.logic.search.facet.model.Facet;
import de.mpg.imeji.logic.search.factory.SearchFactory;
import de.mpg.imeji.logic.search.factory.SearchFactory.SEARCH_IMPLEMENTATIONS;
import de.mpg.imeji.logic.search.jenasearch.ImejiSPARQL;
import de.mpg.imeji.logic.search.jenasearch.JenaCustomQueries;
import de.mpg.imeji.logic.search.model.SearchQuery;
import de.mpg.imeji.logic.search.model.SearchResult;
import de.mpg.imeji.logic.search.model.SortCriterion;
import de.mpg.imeji.logic.util.StringHelper;
import de.mpg.imeji.logic.workflow.WorkflowValidator;

/**
 * CRUD controller for {@link CollectionImeji}, plus search methods related to
 * {@link CollectionImeji}
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public class CollectionService extends SearchServiceAbstract<CollectionImeji> {
  private static final Logger LOGGER = LogManager.getLogger(CollectionService.class);
  private final Search search = SearchFactory.create(SearchObjectTypes.COLLECTION, SEARCH_IMPLEMENTATIONS.ELASTIC);
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
    if (c.getCollection() != null) {
      CollectionImeji p = retrieveLazy(c.getCollection(), user);
      if (p.getStatus() == Status.RELEASED) {
        prepareRelease(c, user);
      } else if (p.getStatus() == Status.WITHDRAWN) {
        prepareWithdraw(c, p.getDiscardComment());
      }
    }
    c = controller.create(c, user);
    messageService.add(new CollectionMessage(MessageType.CREATE_COLLECTION, c));
    HierarchyService.reloadHierarchy();
    return c;
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
   * Retrieve the subcollection of the collection for this path. If the path doesn't exists, create
   * the subcollection
   * 
   * @param parent
   * @param path
   * @param user
   * @return
   * @throws ImejiException
   */
  public CollectionImeji getSubCollectionForPath(CollectionImeji parent, String path, User user) throws ImejiException {
    if (StringHelper.isNullOrEmptyTrim(path)) {
      return parent;
    }
    List<String> names = getPathAsList(path);
    Optional<CollectionImeji> child = retrieveSubcollectionWithName(parent, names.get(0), user);
    if (child.isPresent()) {
      parent = child.get();
    } else {
      parent = createSubcollection(parent, names.get(0), user);
    }
    return names.size() > 1
        ? getSubCollectionForPath(parent, names.subList(1, names.size()).stream().collect(Collectors.joining("/")), user)
        : parent;
  }

  /**
   * True return a path "/name/of/collection" as a list of String
   * 
   * @param path
   * @return
   */
  private List<String> getPathAsList(String path) {
    if (path.startsWith("/")) {
      path = path.substring(1);
    }
    return Arrays.asList(path.split("/"));
  }

  /**
   * Create a sub collection of the parent collection with the following name
   * 
   * @param parent
   * @param name
   * @param user
   * @return
   * @throws ImejiException
   */
  private CollectionImeji createSubcollection(CollectionImeji parent, String name, User user) throws ImejiException {
    return create(ImejiFactory.newCollection().setPerson(user.getPerson()).setTitle(name).setCollection(parent.getId().toString()).build(),
        user);
  }

  /**
   * True if the collection has already a subcollection with this name
   * 
   * @param parent
   * @param name
   * @param user
   * @return
   */
  private Optional<CollectionImeji> retrieveSubcollectionWithName(CollectionImeji parent, String name, User user) {
    try {
      return retrieve(new HierarchyService().findAllSubcollections(parent.getId().toString()), user).stream()
          .filter(c -> c.getTitle().equals(name)).findAny();
    } catch (ImejiException e) {
      return Optional.empty();
    }
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
  public void updateLogo(CollectionImeji ic, File f, User u) throws ImejiException, IOException, URISyntaxException {
    ic = (CollectionImeji) setLogo(ic, f);
    update(ic, u);
  }

  /**
   * Delete a collection and its subcollection
   * 
   * @param collection
   * @param user
   * @throws ImejiException
   */
  public void delete(CollectionImeji collection, User user) throws ImejiException {
    List<CollectionImeji> collectionToDelete =
        controller.retrieveBatchLazy(new HierarchyService().findAllSubcollections(collection.getId().toString()), user);
    collectionToDelete.add(collection);
    for (CollectionImeji c : collectionToDelete) {
      deleteSingleCollection(c, user);
    }
    for (CollectionImeji c : collectionToDelete) {
      messageService.add(new CollectionMessage(MessageType.DELETE_COLLECTION, c));
    }
  }

  /**
   * Delete a {@link CollectionImeji} and all its {@link Item}
   *
   * @param collection
   * @param user
   * @throws ImejiException
   */
  private void deleteSingleCollection(CollectionImeji collection, User user) throws ImejiException {
    final ItemService itemService = new ItemService();
    final List<String> itemUris =
        itemService.search(collection.getId(), null, null, user, Search.GET_ALL_RESULTS, Search.SEARCH_FROM_START_INDEX).getResults();
    if (hasImageLocked(itemUris, user)) {
      throw new UnprocessableError("Collection can not be deleted: It contains locked items");
    }
    new WorkflowValidator().isDeleteAllowed(collection);
    final List<Item> items =
        (List<Item>) itemService.retrieveBatchLazy(itemUris, Search.GET_ALL_RESULTS, Search.SEARCH_FROM_START_INDEX, user);
    itemService.delete(items, user);
    controller.delete(collection, user);
  }

  /**
   * Release a {@link CollectionImeji} and all its {@link Item}
   *
   * @param collection
   * @param user
   * @param defaultLicense
   * @throws ImejiException
   */
  public void release(CollectionImeji collection, User user, License defaultLicense) throws ImejiException {
    new WorkflowFacade().release(collection, user, defaultLicense);
  }

  /**
   * Release a collection and set the instance default license to items without licenses
   *
   * @param collection
   * @param user
   * @throws ImejiException
   */
  public void releaseWithDefaultLicense(CollectionImeji collection, User user) throws ImejiException {
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
  public void moveCollection(CollectionImeji collection, CollectionImeji parent, User user, License license) throws ImejiException {
    new MoveFacade().moveCollection(collection, parent, user, license);
  }


  @Override
  public SearchResult search(SearchQuery searchQuery, SortCriterion sortCri, User user, int size, int offset) {
    return search.search(searchQuery, sortCri, user, null, offset, size);
  }


  /**
   * Search and add {@link Facet} to the {@link SearchResult}
   * 
   * @param containerUri
   * @param searchQuery
   * @param sortCri
   * @param user
   * @param size
   * @param offset
   * @return
   */
  public SearchResult searchWithFacetsAndMultiLevelSorting(SearchQuery searchQuery, List<SortCriterion> sortCriteria, User user, int size,
      int offset, boolean includeSubcollections) {
    SearchResult facetSearchResult =
        search.searchWithFacetsAndMultiLevelSorting(searchQuery, sortCriteria, user, null, offset, size, includeSubcollections);
    return facetSearchResult;
  }

  @Override
  public List<CollectionImeji> retrieve(List<String> ids, User user) throws ImejiException {
    return controller.retrieveBatchLazy(ids, user);
  }

  @Override
  public List<String> searchAll() {
    return ImejiSPARQL.exec(JenaCustomQueries.selectCollectionAll(), Imeji.collectionModel);
  }

  /**
   * Reindex all collections
   *
   * @param index
   * @throws ImejiException
   */
  public void reindex(String index) throws Exception {
    LOGGER.info("Indexing collections...");
    final ElasticIndexer indexer = new ElasticIndexer(index);
    final List<CollectionImeji> collections = retrieveAll();
    indexer.indexBatch(collections);
    LOGGER.info("collections reindexed!");
  }

}
