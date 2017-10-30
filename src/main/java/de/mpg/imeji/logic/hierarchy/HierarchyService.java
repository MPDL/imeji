package de.mpg.imeji.logic.hierarchy;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableMap;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.exceptions.NotAllowedError;
import de.mpg.imeji.exceptions.WorkflowException;
import de.mpg.imeji.logic.batch.messageAggregations.NotifyUsersOnFileUploadAggregation;
import de.mpg.imeji.logic.core.collection.CollectionService;
import de.mpg.imeji.logic.core.content.ContentService;
import de.mpg.imeji.logic.core.item.ItemService;
import de.mpg.imeji.logic.events.Message;
import de.mpg.imeji.logic.events.Message.MessageType;
import de.mpg.imeji.logic.events.MessageService;
import de.mpg.imeji.logic.hierarchy.Hierarchy.Node;
import de.mpg.imeji.logic.model.CollectionImeji;
import de.mpg.imeji.logic.model.ContentVO;
import de.mpg.imeji.logic.model.Item;
import de.mpg.imeji.logic.model.License;
import de.mpg.imeji.logic.model.Properties;
import de.mpg.imeji.logic.model.Properties.Status;
import de.mpg.imeji.logic.model.User;
import de.mpg.imeji.logic.search.elasticsearch.ElasticIndexer;
import de.mpg.imeji.logic.search.elasticsearch.ElasticService;
import de.mpg.imeji.logic.search.elasticsearch.ElasticService.ElasticTypes;
import de.mpg.imeji.logic.search.jenasearch.ImejiSPARQL;
import de.mpg.imeji.logic.search.jenasearch.JenaCustomQueries;
import de.mpg.imeji.logic.security.authorization.Authorization;
import de.mpg.imeji.logic.workflow.WorkflowValidator;

/**
 * Service to manage the hierarchy between imeji objects
 * 
 * @author saquet
 *
 */
public class HierarchyService implements Serializable {
  private static final long serialVersionUID = -3479895793901732353L;
  private final ElasticIndexer collectionIndexer =
      new ElasticIndexer(ElasticService.DATA_ALIAS, ElasticTypes.folders, ElasticService.ANALYSER);
  private final ElasticIndexer itemsIndexer =
      new ElasticIndexer(ElasticService.DATA_ALIAS, ElasticTypes.items, ElasticService.ANALYSER);
  private final ItemService itemService = new ItemService();
  private final CollectionService collectionService = new CollectionService();
  private final MessageService messageService = new MessageService();
  private final WorkflowValidator workflowValidator = new WorkflowValidator();
  private static Hierarchy hierarchy = new Hierarchy();

  public HierarchyService() {
    if (hierarchy == null) {
      hierarchy = new Hierarchy();
      hierarchy.init();
    }
  }


  /**
   * Reload the hierarchy
   */
  public static void reloadHierarchy() {
    hierarchy.init();
  }

  /**
   * Move all items to the collection if:
   * <li>it has pending state
   * <li>the user is authorized to move the item to the collection
   * <li>it doens't already exists in the target collection (i.e. no items with the same checksum)
   * <br/>
   * <br/>
   * <strong> Important:</strong> If the target collection is released, all items are going to be
   * released
   * 
   * @param items
   * @param toCollectionId
   * @throws ImejiException
   */
  public List<Item> moveItems(List<Item> items, CollectionImeji collection, User user,
      License license) throws ImejiException {
    String colUri = collection.getId().toString();
    // Keep only private content
    items = items.stream().filter(item -> item.getStatus().equals(Status.PENDING))
        .collect(Collectors.toList());
    // Check if user can move the items
    checkIfUserCanMoveObjectsToCollection(new ArrayList<>(items), collection, user);
    // Filters files already existing in the collection
    items = filterAlreadyExists(items, collection);
    // Release items if necessary
    if (collection.getStatus().equals(Status.RELEASED)) {
      itemService.release(items, user, license);
    }
    // Move the items
    for (Item item : items) {
      ImejiSPARQL.execUpdate(
          JenaCustomQueries.updateItemCollection(item.getCollection().toString(), colUri));
      itemsIndexer.updatePartial(item.getId().toString(),
          new ElasticForlderPartObject(colUri.toString()));
    }
    itemsIndexer.commit();
    // Notify to event queue
    items.stream().forEach(item -> messageService.add(
        new Message(MessageType.MOVE_ITEM, collection.getIdString(), createMessageMoveItem(item))));
    return items;
  }

  /**
   * Move collection to another collection
   * 
   * @param collection
   * @param parent
   * @param user
   * @param license
   * @throws ImejiException
   */
  public void moveCollection(CollectionImeji collection, CollectionImeji parent, User user,
      License license) throws ImejiException {
    checkIfUserCanMoveObjectsToCollection(Arrays.asList(collection), collection, user);
    if (parent.getStatus().equals(Status.RELEASED)) {
      // release the collection
      collectionService.release(collection, user, license);
    }
    // Move collection
    ImejiSPARQL.execUpdate(JenaCustomQueries.updateCollectionParent(collection.getId().toString(),
        parent.getId().toString()));
    collectionIndexer.updatePartial(collection.getId().toString(),
        new ElasticForlderPartObject(parent.getId().toString()));
    collectionIndexer.commit();
    messageService.add(createMessageMoveCollection(collection, parent));
  }


  /**
   * Find all Subcollection of the collection/subcollection
   * 
   * @param collectionUri
   * @return
   */
  public List<String> findAllSubcollections(String collectionUri) {
    return hierarchy.getTree().get(collectionUri) != null ? hierarchy.getTree().get(collectionUri)
        .stream().flatMap(s -> Stream.concat(Stream.of(s), findAllSubcollections(s).stream()))
        .collect(Collectors.toList()) : new ArrayList<>();
  }

  /**
   * Find the list of all parents of the objects
   * 
   * @param o
   * @return
   */
  public List<String> findAllParents(Object o) {
    List<String> l = new ArrayList<>();
    String uri = getParentUri(o);
    if (uri != null) {
      l.addAll(findAllParents(uri));
      l.add(uri);
      return l;
    }
    return l;
  }

  /**
   * Return the list of all parents of the object with this uri
   * 
   * @param parentUri
   * @return
   */
  public List<String> findAllParents(String uri) {
    List<String> l = new ArrayList<>();
    String parent = getParent(uri);
    if (parent != null) {
      l.addAll(findAllParents(parent));
      l.add(parent);
    }
    return l;
  }

  /**
   * Return the Parent uri of the passed uri
   * 
   * @param uri
   * @return
   */
  public String getParent(String uri) {
    Node n = hierarchy.getNodes().get(uri);
    return n != null ? n.getParent() : null;
  }


  /**
   * Return the last parent of the object
   * 
   * @param o
   * @return
   */
  public String getLastParent(Object o) {
    String uri = getParentUri(o);
    if (uri != null) {
      return getLastParent(uri);
    }
    return null;
  }

  /**
   * Get the last parent of the object according to its first parent
   * 
   * @param firstParent
   * @return
   */
  public String getLastParent(String firstParent) {
    Node parentNode = hierarchy.getNodes().get(firstParent);
    if (parentNode != null) {
      return getLastParent(parentNode.getParent());
    }
    return firstParent;
  }

  /**
   * Filter out items with a file which already exists in the target collection<br/>
   * 
   * @param items
   * @param collection
   * @return
   * @throws ImejiException
   */
  private List<Item> filterAlreadyExists(List<Item> items, CollectionImeji collection)
      throws ImejiException {
    final List<ContentVO> contents = retrieveContentBatchLazy(items).stream()
        .filter(content -> !itemService.checksumExistsInCollection(collection.getId(),
            content.getChecksum()))
        .collect(Collectors.toList());
    return items
        .stream().filter(item -> contents.stream()
            .filter(c -> c.getItemId().equals(item.getId().toString())).findAny().isPresent())
        .collect(Collectors.toList());
  }

  /**
   * Retrieve the contents of the Items
   * 
   * @param items
   * @return
   * @throws ImejiException
   */
  private List<ContentVO> retrieveContentBatchLazy(List<Item> items) throws ImejiException {
    ContentService contentService = new ContentService();
    List<String> contentIds =
        items.stream().map(item -> contentService.findContentId(item.getId().toString()))
            .collect(Collectors.toList());
    return contentService.retrieveBatchLazy(contentIds);
  }

  /**
   * If the object is an Item or a collection and has a parent, return the uri of its parent
   * 
   * @param o
   * @return
   */
  private String getParentUri(Object o) {
    if (o instanceof Item) {
      return ((Item) o).getCollection().toString();
    }
    if (o instanceof CollectionImeji) {
      return ((CollectionImeji) o).getCollection() != null
          ? ((CollectionImeji) o).getCollection().toString() : null;
    }
    return null;
  }

  /**
   * Create the Message content for moved items
   * 
   * @param item
   * @return
   */
  private Map<String, String> createMessageMoveItem(Item item) {
    return ImmutableMap.of(NotifyUsersOnFileUploadAggregation.COUNT, "1",
        NotifyUsersOnFileUploadAggregation.FILENAME,
        item.getFilename() != null ? item.getFilename() : "",
        NotifyUsersOnFileUploadAggregation.ITEM_ID, item.getIdString());
  }

  /**
   * Create the message for collection moved
   * 
   * @param collection
   * @return
   */
  private Message createMessageMoveCollection(CollectionImeji collection, CollectionImeji parent) {
    Map<String, String> content = ImmutableMap.of("parent", parent.getId().toString());
    return new Message(MessageType.MOVE_COLLECTION, collection.getIdString(), content);
  }

  /**
   * Throw {@link NotAllowedError} if the move operation is not possible, i.e if:
   * <li>the user can not update the collection and therefore not add new objects
   * <li>the user can not delete the objects
   * <li>if the collection is released, the user has no admin rights on the objects
   * <li>The objects can't be deleted (for instance released)
   * 
   * @param items
   * @param collection
   * @param user
   * @throws NotAllowedError
   * @throws WorkflowException
   */
  private void checkIfUserCanMoveObjectsToCollection(List<Object> objects,
      CollectionImeji collection, User user) throws NotAllowedError, WorkflowException {
    final Authorization authorization = new Authorization();
    if (!authorization.update(user, collection)
        || objects.stream().filter(o -> !authorization.delete(user, o)).findAny().isPresent()
        || (collection.getStatus().equals(Status.RELEASED) && objects.stream()
            .filter(o -> !authorization.administrate(user, o)).findAny().isPresent())) {
      throw new NotAllowedError(user.getEmail()
          + " is not allowed to moved the items to the collection " + collection.getIdString());
    }
    for (Object p : objects) {
      workflowValidator.isDeleteAllowed((Properties) p);
    }
  }


  private class ElasticForlderPartObject {
    private final String folder;

    public ElasticForlderPartObject(String collectionUri) {
      this.folder = collectionUri;
    }

    public String getFolder() {
      return folder;
    }
  }
}
