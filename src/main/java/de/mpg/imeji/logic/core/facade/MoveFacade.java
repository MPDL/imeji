package de.mpg.imeji.logic.core.facade;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.exceptions.NotAllowedError;
import de.mpg.imeji.exceptions.UnprocessableError;
import de.mpg.imeji.logic.core.collection.CollectionService;
import de.mpg.imeji.logic.core.content.ContentService;
import de.mpg.imeji.logic.core.item.ItemService;
import de.mpg.imeji.logic.db.writer.WriterFacade;
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
import de.mpg.imeji.logic.model.aspects.ChangeMember;
import de.mpg.imeji.logic.model.aspects.ChangeMember.ActionType;
import de.mpg.imeji.logic.security.authorization.Authorization;
import de.mpg.imeji.logic.util.ObjectHelper;
import de.mpg.imeji.logic.workflow.WorkflowValidator;

/**
 * Facade to manage move operations with {@link Item} and {@link CollectionImeji}
 * 
 * @author saquet
 *
 */
public class MoveFacade implements Serializable {
  private static final long serialVersionUID = 6158359006910656203L;
  private final CollectionService collectionService = new CollectionService();
  private final MessageService messageService = new MessageService();
  private final ItemService itemService = new ItemService();
  private final WorkflowValidator workflowValidator = new WorkflowValidator();


  private static final Logger LOGGER = LogManager.getLogger(MoveFacade.class);

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
  public List<Item> moveItems(List<Item> items, CollectionImeji newParent, User user, License license) throws ImejiException {

    // Keep only private content
    items = items.stream().filter(item -> item.getStatus().equals(Status.PENDING)).collect(Collectors.toList());
    // Check if user can move the items
    checkIfUserCanMoveObjectsToCollection(new ArrayList<>(items), newParent, user);
    // Filters files already existing in the collection
    items = filterAlreadyExists(items, newParent);
    // Release items if necessary
    if (newParent.getStatus().equals(Status.RELEASED)) {
      items = itemService.release(items, user, license);
    }

    // Move items
    List<ChangeMember> changeParts = new ArrayList<ChangeMember>(items.size());
    try {
      Field parentCollectionField = Item.class.getDeclaredField("collection");
      for (Item item : items) {
        ChangeMember changeItemsParentCollection = new ChangeMember(ActionType.EDIT, item, parentCollectionField, newParent.getId());
        changeParts.add(changeItemsParentCollection);
      }
      WriterFacade writerFacade = new WriterFacade();
      List<Object> updatedItems = writerFacade.editElements(changeParts, user);
      items = itemService.toItemList(updatedItems);

      // Notify event queue
      items.stream().forEach(item -> messageService.add(new MoveItemMessage(MessageType.MOVE_ITEM, item,
          ObjectHelper.getId(newParent.getId()), ObjectHelper.getId(item.getCollection()))));

    } catch (NoSuchFieldException | SecurityException e) {
      LOGGER.error("Could not move items", e);
    }
    return items;
  }

  /**
   * Move collection to another collection
   * 
   * @param collection
   * @param newParent
   * @param user
   * @param license
   * @throws ImejiException
   */
  public void moveCollection(CollectionImeji collection, CollectionImeji newParent, User user, License license) throws ImejiException {
    checkIfUserCanMoveObjectsToCollection(Arrays.asList(collection), collection, user);
    checkIfCollectionCanBeMovedToNewParent(collection, newParent);
    if (newParent.getStatus().equals(Status.RELEASED)) {
      // release the collection
      collectionService.release(collection, user, license);
    }

    // Move collection 
    List<ChangeMember> changeParts = new ArrayList<ChangeMember>(1);
    try {
      Field parentCollectionField = CollectionImeji.class.getDeclaredField("collection");
      ChangeMember changeParent = new ChangeMember(ActionType.ADD_OVERRIDE, collection, parentCollectionField, newParent.getId());
      changeParts.add(changeParent);

      WriterFacade writerFacade = new WriterFacade();
      List<Object> updatedCollections = writerFacade.editElements(changeParts, user);
      if (updatedCollections != null && updatedCollections.size() == 1) {
        collection = (CollectionImeji) updatedCollections.get(0);
        messageService.add(new MoveCollectionMessage(MessageType.MOVE_COLLECTION, collection, ObjectHelper.getId(newParent.getId()),
            ObjectHelper.getId(collection.getCollection())));
      } else {
        LOGGER.error("Unexpected result while moving collection");
      }

      HierarchyService.reloadHierarchy();
    } catch (NoSuchFieldException | SecurityException e) {
      LOGGER.error("Could not move collection", e);
    }

  }

  /**
   * Throw {@link NotAllowedError} if the move operation is not possible, i.e if:
   * <li>the user can not update the collection and therefore not add new objects
   * <li>the user can not delete the objects
   * <li>if the collection is released, the user has no admin rights on the objects
   * <li>The objects can't be deleted (for instance released)
   * 
   * @param objects (items or sub collection)
   * @param targetCollection
   * @param user
   * @throws ImejiException
   */
  public void checkIfUserCanMoveObjectsToCollection(List<Object> objects, CollectionImeji targetCollection, User user)
      throws ImejiException {

    // reload collection
    CollectionService collectionService = new CollectionService();
    targetCollection = collectionService.retrieve(targetCollection.getId(), user);
    // workflow check for target collection
    workflowValidator.isUpdateAllowed(targetCollection);
    final Authorization authorization = new Authorization();
    authorization.reload();
    if (!authorization.update(user, targetCollection) || objects.stream().filter(o -> !authorization.delete(user, o)).findAny().isPresent()
        || (targetCollection.getStatus().equals(Status.RELEASED)
            && objects.stream().filter(o -> !authorization.administrate(user, o)).findAny().isPresent())) {
      throw new NotAllowedError(user.getEmail() + " is not allowed to moved the items to the collection " + targetCollection.getIdString());
    }
    for (Object p : objects) {
      workflowValidator.isDeleteAllowed((Properties) p);
    }
  }

  /**
   * Throw an Error if the new Parent is a child of the moved collection
   * 
   * @param collectionToMove
   * @param newParent
   * @throws UnprocessableError
   */
  private void checkIfCollectionCanBeMovedToNewParent(CollectionImeji collectionToMove, CollectionImeji newParent)
      throws UnprocessableError {
    if (new HierarchyService().isChildOf(newParent.getId().toString(), collectionToMove.getId().toString())) {
      throw new UnprocessableError(newParent.getTitle() + " is a child of " + collectionToMove.getTitle());
    }
    if (!collectionToMove.isSubCollection()) {
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
  private List<Item> filterAlreadyExists(List<Item> items, CollectionImeji collection) throws ImejiException {

    final List<ContentVO> contents = retrieveContentBatchLazy(items).stream()
        .filter(content -> !itemService.checksumExistsInCollection(collection.getId(), content.getChecksum())).collect(Collectors.toList());
    return items.stream().filter(item -> contents.stream().filter(c -> c.getItemId().equals(item.getId().toString())).findAny().isPresent())
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
        items.stream().map(item -> contentService.findContentId(item.getId().toString())).collect(Collectors.toList());
    return contentService.retrieveBatchLazy(contentIds);
  }


}
