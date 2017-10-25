package de.mpg.imeji.logic.core.hierarchy;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableMap;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.exceptions.NotAllowedError;
import de.mpg.imeji.exceptions.WorkflowException;
import de.mpg.imeji.logic.batch.messageAggregations.NotifyUsersOnFileUploadAggregation;
import de.mpg.imeji.logic.core.content.ContentService;
import de.mpg.imeji.logic.core.item.ItemService;
import de.mpg.imeji.logic.messaging.Message;
import de.mpg.imeji.logic.messaging.Message.MessageType;
import de.mpg.imeji.logic.messaging.MessageService;
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
  private final MessageService messageService = new MessageService();
  private final Authorization authorization = new Authorization();
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
    // Move contents
    items = moveContentsOfItems(items, collection);
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
        new Message(MessageType.MOVE_ITEM, collection.getIdString(), createMessageContent(item))));
    return items;
  }

  /**
   * Move collection to another collection
   * 
   * @param collection
   * @param parent
   * @param user
   * @param license
   * @throws NotAllowedError
   * @throws WorkflowException
   */
  public void moveCollection(CollectionImeji collection, CollectionImeji parent, User user,
      License license) throws NotAllowedError, WorkflowException {
    checkIfUserCanMoveObjectsToCollection(Arrays.asList(collection), collection, user);
    if (parent.getCollection() == null) {

    }
    ImejiSPARQL.execUpdate(JenaCustomQueries.updateCollectionParent(collection.getId().toString(),
        parent.getId().toString()));
    itemsIndexer.updatePartial(collection.getId().toString(),
        new ElasticForlderPartObject(parent.getId().toString()));
    itemsIndexer.commit();
  }


  public List<String> findAllSubcollections(String collectionId) {
    return null;
  }


  public String getLastParent(String itemId) {
    return null;
  }

  /**
   * Move the contents of the passed items to a collection.<br/>
   * <strong>Important:</strong><br/>
   * <li>If a content already exists, it is not allowed to be moved. Therefore it is not returned in
   * the result list
   * <li>If the Item is moved to a sub-collection which belongs to same top parent collection, the
   * contents doens't need to be moved, but the content is returned in the result list, since
   * correct.
   * 
   * @param items
   * @param collection
   * @return
   * @throws ImejiException
   */
  private List<Item> moveContentsOfItems(List<Item> items, CollectionImeji collection)
      throws ImejiException {
    List<ContentVO> contents = retrieveContentBatchLazy(items);
    contents = filterAlreadyExists(contents, collection);
    List<ContentVO> contentsToMoved = filterContentToMovedInStorage(contents, collection);
    // Keep only contents which are already in collection and therefore not moved
    contents.removeAll(contentsToMoved);
    // Move the content
    contentsToMoved = new ContentService().move(contentsToMoved, collection.getIdString());
    // Merge moved contents with content which weren't move since already in collection
    final List<ContentVO> contentsReady = new ArrayList<>(contents);
    contentsReady.addAll(contentsToMoved);
    // Return the items of which the content is on the correct position
    return items.stream()
        .filter(item -> contentsReady.stream()
            .filter(c -> c.getItemId().equals(item.getId().toString())).findAny().isPresent())
        .collect(Collectors.toList());
  }

  /**
   * Return only contents which actually need to be moved, i.e. contents which are moved to a new
   * top parent collection
   * 
   * @param contents
   * @param collection
   * @return
   */
  private List<ContentVO> filterContentToMovedInStorage(List<ContentVO> contents,
      CollectionImeji collection) {
    String collectionLastParent = getLastParent(collection.getIdString());
    if (collectionLastParent != null) {
      // Filter out content moved within the same top parent collection
      return contents.stream()
          .filter(content -> !getLastParent(content.getItemId()).equals(collectionLastParent))
          .collect(Collectors.toList());
    }
    return new ArrayList<>(contents);
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
   * Create the Message content for moved items
   * 
   * @param item
   * @return
   */
  private Map<String, String> createMessageContent(Item item) {
    return ImmutableMap.of(NotifyUsersOnFileUploadAggregation.COUNT, "1",
        NotifyUsersOnFileUploadAggregation.FILENAME,
        item.getFilename() != null ? item.getFilename() : "",
        NotifyUsersOnFileUploadAggregation.ITEM_ID, item.getIdString());
  }

  /**
   * Throw {@link NotAllowedError} if the move operation is not possible, i.e if:
   * <li>the user can not update the collection and therefore not add new objects
   * <li>the user can not delete the objects
   * <li>if the collection is released, the user has no admin rights on the objects
   * <li>The objects can be deleted
   * 
   * @param items
   * @param collection
   * @param user
   * @throws NotAllowedError
   * @throws WorkflowException
   */
  private void checkIfUserCanMoveObjectsToCollection(List<Object> objects,
      CollectionImeji collection, User user) throws NotAllowedError, WorkflowException {
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
   * Filter out all content already exsting in the collection (i.e., if another content of the
   * collection has the same checksum)
   * 
   * @param items
   * @param col
   * @return
   */
  private List<ContentVO> filterAlreadyExists(List<ContentVO> contents, CollectionImeji col) {
    return contents.stream()
        .filter(
            content -> !itemService.checksumExistsInCollection(col.getId(), content.getChecksum()))
        .collect(Collectors.toList());
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
