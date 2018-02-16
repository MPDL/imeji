package de.mpg.imeji.logic.core.facade;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.exceptions.NotAllowedError;
import de.mpg.imeji.exceptions.UnprocessableError;
import de.mpg.imeji.exceptions.WorkflowException;
import de.mpg.imeji.logic.core.collection.CollectionService;
import de.mpg.imeji.logic.core.content.ContentService;
import de.mpg.imeji.logic.core.item.ItemService;
import de.mpg.imeji.logic.events.MessageService;
import de.mpg.imeji.logic.events.messages.Message.MessageType;
import de.mpg.imeji.logic.events.messages.MoveCollectionMessage;
import de.mpg.imeji.logic.events.messages.MoveItemMessage;
import de.mpg.imeji.logic.hierarchy.HierarchyService;
import de.mpg.imeji.logic.model.CollectionImeji;
import de.mpg.imeji.logic.model.ContentVO;
import de.mpg.imeji.logic.model.Item;
import de.mpg.imeji.logic.model.License;
import de.mpg.imeji.logic.model.Properties;
import de.mpg.imeji.logic.model.Properties.Status;
import de.mpg.imeji.logic.model.User;
import de.mpg.imeji.logic.model.factory.ImejiFactory;
import de.mpg.imeji.logic.search.elasticsearch.ElasticIndexer;
import de.mpg.imeji.logic.search.elasticsearch.ElasticService;
import de.mpg.imeji.logic.search.elasticsearch.ElasticService.ElasticTypes;
import de.mpg.imeji.logic.search.jenasearch.ImejiSPARQL;
import de.mpg.imeji.logic.search.jenasearch.JenaCustomQueries;
import de.mpg.imeji.logic.security.authorization.Authorization;
import de.mpg.imeji.logic.util.ObjectHelper;
import de.mpg.imeji.logic.workflow.WorkflowValidator;

/**
 * Facade to manage to move operation on {@link Item} and {@link CollectionImeji}
 * 
 * @author saquet
 *
 */
public class MoveFacade implements Serializable {
  private static final long serialVersionUID = 6158359006910656203L;
  private final ElasticIndexer collectionIndexer =
      new ElasticIndexer(ElasticService.DATA_ALIAS, ElasticTypes.folders, ElasticService.ANALYSER);
  private final ElasticIndexer itemsIndexer =
      new ElasticIndexer(ElasticService.DATA_ALIAS, ElasticTypes.items, ElasticService.ANALYSER);
  private final CollectionService collectionService = new CollectionService();
  private final MessageService messageService = new MessageService();
  private final ItemService itemService = new ItemService();
  private final WorkflowValidator workflowValidator = new WorkflowValidator();

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
      ImejiSPARQL.execUpdate(JenaCustomQueries.updateCollection(item.getId().toString(),
          item.getCollection().toString(), colUri));
      itemsIndexer.updatePartial(item.getId().toString(), ImejiFactory.newItem()
          .setUri(item.getId().toString()).setCollection(colUri.toString()).build());
    }
    itemsIndexer.commit();
    // Notify to event queue
    items.stream().forEach(item -> messageService.add(new MoveItemMessage(MessageType.MOVE_ITEM,
        item, ObjectHelper.getId(collection.getId()), ObjectHelper.getId(item.getCollection()))));
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
    checkIfCollectionCanBeMovedToNewParent(collection, parent);
    if (parent.getStatus().equals(Status.RELEASED)) {
      // release the collection
      collectionService.release(collection, user, license);
    }
    // Move collection
    ImejiSPARQL.execUpdate(JenaCustomQueries.updateCollectionParent(collection.getId().toString(),
        collection.getCollection() != null ? collection.getCollection().toString() : null,
        parent.getId().toString()));
    collectionIndexer.updatePartial(collection.getId().toString(),
        new ElasticForlderPartObject(parent.getId().toString()));
    messageService.add(new MoveCollectionMessage(MessageType.MOVE_COLLECTION, collection,
        ObjectHelper.getId(parent.getId()), ObjectHelper.getId(collection.getCollection())));
    HierarchyService.reloadHierarchy();
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

  /**
   * Throw an Error if the new Parent is a child of the moved collection
   * 
   * @param c
   * @param newParent
   * @throws UnprocessableError
   */
  private void checkIfCollectionCanBeMovedToNewParent(CollectionImeji c, CollectionImeji newParent)
      throws UnprocessableError {
    if (new HierarchyService().isChildOf(newParent.getId().toString(), c.getId().toString())) {
      throw new UnprocessableError(newParent.getTitle() + " is a child of " + c.getTitle());
    }
    if (!c.isSubCollection()) {
      new UnprocessableError("Only subcollections can be moved");
    }
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
    List<ContentVO> l = retrieveContentBatchLazy(items);
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
   * Java objects used to update the collection of a folder
   * 
   * @author saquet
   *
   */
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
