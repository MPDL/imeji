package de.mpg.imeji.logic.core.facade;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.net.URI;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.stream.Collectors;

import de.mpg.imeji.exceptions.AuthenticationError;
import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.exceptions.NotAllowedError;
import de.mpg.imeji.exceptions.NotFoundException;
import de.mpg.imeji.exceptions.UnprocessableError;
import de.mpg.imeji.logic.concurrency.Locks;
import de.mpg.imeji.logic.core.item.ItemService;
import de.mpg.imeji.logic.db.writer.WriterFacade;
import de.mpg.imeji.logic.hierarchy.HierarchyService;
import de.mpg.imeji.logic.model.CollectionImeji;
import de.mpg.imeji.logic.model.Item;
import de.mpg.imeji.logic.model.License;
import de.mpg.imeji.logic.model.Properties;
import de.mpg.imeji.logic.model.Properties.Status;
import de.mpg.imeji.logic.model.SearchFields;
import de.mpg.imeji.logic.model.User;
import de.mpg.imeji.logic.model.aspects.AccessMember.ActionType;
import de.mpg.imeji.logic.model.aspects.AccessMember.ChangeMember;
import de.mpg.imeji.logic.search.Search;
import de.mpg.imeji.logic.search.factory.SearchFactory;
import de.mpg.imeji.logic.search.model.SearchPair;
import de.mpg.imeji.logic.security.authorization.Authorization;
import de.mpg.imeji.logic.util.StringHelper;
import de.mpg.imeji.logic.workflow.WorkflowValidator;
import de.mpg.imeji.util.DateHelper;

/**
 * Facade to Release or Withdraw collections
 * 
 * @author saquet
 *
 */
public class WorkflowFacade implements Serializable {

  private static final long serialVersionUID = 3966446108673573909L;
  private final Authorization authorization = new Authorization();
  private final WorkflowValidator workflowValidator = new WorkflowValidator();


  /**
   * Release a collection and its item
   * 
   * @param collection
   * @param user
   * @param releaseLicense
   * @throws ImejiException
   */
  public void release(CollectionImeji collection, User user, License releaseLicense) throws ImejiException {

    preValidateRelease(collection, user, releaseLicense);

    ItemService itemService = new ItemService();
    // 1) all items of collection and it's sub collections
    final List<String> itemIds =
        itemService.search(collection.getId(), new SearchFactory().and(new SearchPair(SearchFields.filename, "*")).build(), null, user,
            Search.GET_ALL_RESULTS, Search.SEARCH_FROM_START_INDEX).getResults();
    preValidateCollectionItems(itemIds, user);

    // 2) collection ids 
    // Create a list with the collectionId, all the sub collection ids and all itemIds
    List<String> collectionIds = new ArrayList<>(new HierarchyService().findAllSubcollections(collection.getId().toString()));
    collectionIds.add(collection.getId().toString());

    List<ChangeMember> changeParts = new ArrayList<ChangeMember>(itemIds.size() + collectionIds.size());
    try {
      Field statusField = Properties.class.getDeclaredField("status");
      Field issuedField = Properties.class.getDeclaredField("versionDate");
      Field licensesField = Item.class.getDeclaredField("licenses");
      Calendar releaseDate = DateHelper.getCurrentDate();

      // items
      for (String itemId : itemIds) {
        URI itemURI = URI.create(itemId);
        Item item = new Item();
        item.setId(itemURI);
        // status, status issued
        ChangeMember changeItemStatus = new ChangeMember(ActionType.EDIT, item, statusField, Properties.Status.RELEASED);
        ChangeMember changeItemStatusIssued = new ChangeMember(ActionType.ADD, item, issuedField, releaseDate);
        // only add license to items that don't have a license yet
        License itemsLicense = releaseLicense.clone();
        itemsLicense.setStart(releaseDate.getTimeInMillis());
        ChangeMember addItemsLicense = new ChangeMember(ActionType.ADD, item, licensesField, itemsLicense);
        changeParts.add(changeItemStatus);
        changeParts.add(changeItemStatusIssued);
        changeParts.add(addItemsLicense);
      }
      // collection and sub collections
      for (String collectionId : collectionIds) {
        CollectionImeji collectionToChange = new CollectionImeji();
        collectionToChange.setId(URI.create(collectionId));
        ChangeMember changeCollectionStatus =
            new ChangeMember(ActionType.EDIT, collectionToChange, statusField, Properties.Status.RELEASED);
        ChangeMember changeCollectionStatusIssued = new ChangeMember(ActionType.ADD, collectionToChange, issuedField, releaseDate);
        changeParts.add(changeCollectionStatus);
        changeParts.add(changeCollectionStatusIssued);
      }

      // direct access to WriterFacade
      WriterFacade writerFacade = new WriterFacade();
      writerFacade.editElements(changeParts, user);
    } catch (NoSuchFieldException | SecurityException e) {
      // log error
      return;
    }
  }

  /**
   * Release items
   * 
   * @param items
   * @param user
   * @param defaultLicense
   * @throws ImejiException
   */
  public void releaseItems(List<Item> items, User user, License defaultLicense) throws ImejiException {
    final List<String> itemIds = items.stream().map(item -> item.getId().toString()).collect(Collectors.toList());
    preValidateReleaseItems(items, user, defaultLicense);
    List<ChangeMember> changeParts = new ArrayList<ChangeMember>(itemIds.size());
    try {
      Field statusField = Properties.class.getDeclaredField("status");
      Field issuedField = Properties.class.getDeclaredField("versionDate");
      Field licensesField = Item.class.getDeclaredField("licenses");
      Calendar releaseDate = DateHelper.getCurrentDate();

      for (String itemId : itemIds) {
        Item item = new Item();
        item.setId(URI.create(itemId));
        ChangeMember changeItemStatus = new ChangeMember(ActionType.EDIT, item, statusField, Properties.Status.RELEASED);
        ChangeMember changeItemStatusIssued = new ChangeMember(ActionType.ADD, item, issuedField, releaseDate);
        License itemsLicense = defaultLicense.clone();
        itemsLicense.setStart(releaseDate.getTimeInMillis());
        ChangeMember addItemsLicense = new ChangeMember(ActionType.ADD, item, licensesField, itemsLicense);
        changeParts.add(changeItemStatus);
        changeParts.add(changeItemStatusIssued);
        changeParts.add(addItemsLicense);
      }

      // direct access to WriterFacade
      WriterFacade writerFacade = new WriterFacade();
      writerFacade.editElements(changeParts, user);

    } catch (NoSuchFieldException | SecurityException e) {
      // log error
      return;
    }
  }

  /**
   * Withdraw the collection and its items
   * 
   * @param collection
   * @param comment
   * @param user
   * @throws ImejiException
   */
  public void withdraw(CollectionImeji collection, String comment, User user) throws ImejiException {

    prevalidateWithdraw(collection, comment, user);
    final List<String> itemIds = getItemIds(collection, user);
    if (itemIds != null && itemIds.size() > 0) {
      preValidateCollectionItems(itemIds, user);
    }

    // Create a list with the collectionId, all the subcollectionIds and all itemIds
    List<String> collectionIds = new ArrayList<>(new HierarchyService().findAllSubcollections(collection.getId().toString()));
    collectionIds.add(collection.getId().toString());

    // set for items and collections: status, versionDate, discardComment
    List<ChangeMember> changeParts = new ArrayList<ChangeMember>(itemIds.size() + collectionIds.size());
    try {
      Field statusField = Properties.class.getDeclaredField("status");
      Field issuedField = Properties.class.getDeclaredField("versionDate");
      Field discardCommentField = Properties.class.getDeclaredField("discardComment");
      Calendar withdrawDate = DateHelper.getCurrentDate();

      // items
      for (String itemId : itemIds) {

        Item item = new Item();
        item.setId(URI.create(itemId));
        ChangeMember changeItemStatus = new ChangeMember(ActionType.EDIT, item, statusField, Properties.Status.WITHDRAWN);
        ChangeMember changeItemStatusIssued = new ChangeMember(ActionType.EDIT, item, issuedField, withdrawDate);
        ChangeMember changeItemDiscardComment = new ChangeMember(ActionType.ADD, item, discardCommentField, comment);
        changeParts.add(changeItemStatus);
        changeParts.add(changeItemStatusIssued);
        changeParts.add(changeItemDiscardComment);

      }

      // collections
      for (String collectionId : collectionIds) {

        CollectionImeji collectionToUpdate = new CollectionImeji();
        collectionToUpdate.setId(URI.create(collectionId));
        ChangeMember changeCollectionStatus =
            new ChangeMember(ActionType.EDIT, collectionToUpdate, statusField, Properties.Status.WITHDRAWN);
        ChangeMember changeCollectionStatusIssued = new ChangeMember(ActionType.EDIT, collectionToUpdate, issuedField, withdrawDate);
        ChangeMember changeCollectionDiscardComment = new ChangeMember(ActionType.ADD, collectionToUpdate, discardCommentField, comment);
        changeParts.add(changeCollectionStatus);
        changeParts.add(changeCollectionStatusIssued);
        changeParts.add(changeCollectionDiscardComment);

      }


      // direct access to WriterFacade
      WriterFacade writerFacade = new WriterFacade();
      writerFacade.editElements(changeParts, user);

    } catch (NoSuchFieldException | SecurityException e) {
      // log error
      return;
    }

  }

  /**
   * Withdraw a list of items
   * 
   * @param items
   * @param comment
   * @param user
   * @throws ImejiException
   */
  public void withdrawItems(List<Item> items, String comment, User user) throws ImejiException {
    prevalidateWithdrawItems(items, comment, user);
    List<String> itemIds = items.stream().map(item -> item.getId().toString()).collect(Collectors.toList());
    preValidateCollectionItems(itemIds, user);

    // set for items and collections: status, versionDate, discardComment
    List<ChangeMember> changeParts = new ArrayList<ChangeMember>(itemIds.size());
    try {
      Field statusField = Properties.class.getDeclaredField("status");
      Field issuedField = Properties.class.getDeclaredField("versionDate");
      Field discardCommentField = Properties.class.getDeclaredField("discardComment");
      Calendar withdrawDate = DateHelper.getCurrentDate();

      // items
      for (String itemId : itemIds) {

        Item item = new Item();
        item.setId(URI.create(itemId));
        ChangeMember changeItemStatus = new ChangeMember(ActionType.EDIT, item, statusField, Properties.Status.RELEASED);
        ChangeMember changeItemStatusIssued = new ChangeMember(ActionType.EDIT, item, issuedField, withdrawDate);
        ChangeMember changeItemDiscardComment = new ChangeMember(ActionType.ADD, item, discardCommentField, comment);
        changeParts.add(changeItemStatus);
        changeParts.add(changeItemStatusIssued);
        changeParts.add(changeItemDiscardComment);

      }

      // direct access to WriterFacade
      WriterFacade writerFacade = new WriterFacade();
      writerFacade.editElements(changeParts, user);
    } catch (NoSuchFieldException | SecurityException e) {
      // log error
      return;
    }


  }


  /**
   * Perform prevalidation on the collection to check if the user can proceed to the workflow
   * operation
   * 
   * @param collection
   * @param user
   * @param defaultLicense
   * @throws ImejiException
   */
  private void preValidateRelease(CollectionImeji collection, User user, License defaultLicense) throws ImejiException {
    if (user == null) {
      throw new AuthenticationError(AuthenticationError.USER_MUST_BE_LOGGED_IN);
    }
    if (!authorization.administrate(user, collection)) {
      throw new NotAllowedError(NotAllowedError.NOT_ALLOWED);
    }
    if (collection == null) {
      throw new NotFoundException("collection object does not exists");
    }
    if (defaultLicense == null) {
      throw new UnprocessableError("A default license is needed to release a collection");
    }
    workflowValidator.isReleaseAllowed(collection);
  }

  /**
   * Perform prevalidation on the collection to check if the user can proceed to the withdraw
   * operation
   * 
   * @param collection
   * @param user
   * @throws ImejiException
   */
  private void prevalidateWithdraw(CollectionImeji collection, String comment, User user) throws ImejiException {
    workflowValidator.isWithdrawAllowed(collection);
    if (user == null) {
      throw new AuthenticationError(AuthenticationError.USER_MUST_BE_LOGGED_IN);
    }
    if (!authorization.administrate(user, collection)) {
      throw new NotAllowedError(NotAllowedError.NOT_ALLOWED);
    }
    if (collection == null) {
      throw new NotFoundException("collection object does not exists");
    }
    if (StringHelper.isNullOrEmptyTrim(comment)) {
      throw new UnprocessableError("Missing discard comment");
    }
  }

  /**
   * Prevalidate the witdthraw pf an item
   * 
   * @param items
   * @param comment
   * @param user
   * @throws ImejiException
   */
  private void prevalidateWithdrawItems(List<Item> items, String comment, User user) throws ImejiException {
    for (Item item : items) {
      workflowValidator.isWithdrawAllowed(item);
      if (user == null) {
        throw new AuthenticationError(AuthenticationError.USER_MUST_BE_LOGGED_IN);
      }
      if (!authorization.administrate(user, item)) {
        throw new NotAllowedError(NotAllowedError.NOT_ALLOWED);
      }
      if (StringHelper.isNullOrEmptyTrim(comment)) {
        throw new UnprocessableError("Missing discard comment");
      }
    }
  }

  /**
   * Check if the items can be released
   * 
   * @param items
   * @param user
   * @param defaultLicense
   * @throws ImejiException
   */
  private void preValidateReleaseItems(List<Item> items, User user, License defaultLicense) throws ImejiException {
    for (Item item : items) {
      workflowValidator.isReleaseAllowed(item);
      if (user == null) {
        throw new AuthenticationError(AuthenticationError.USER_MUST_BE_LOGGED_IN);
      }
      if (!authorization.administrate(user, item)) {
        throw new NotAllowedError(NotAllowedError.NOT_ALLOWED);
      }
    }
    if (defaultLicense == null) {
      throw new UnprocessableError("A default license is needed to release a collection");
    }
  }

  /**
   * Perform prevalidation on the collection items to check if the user can proceed to the workflow
   * operation
   * 
   * @param itemIds
   * @param user
   * @throws ImejiException
   */
  private void preValidateCollectionItems(List<String> itemIds, User user) throws ImejiException {
    if (hasImageLocked(itemIds, user)) {
      throw new UnprocessableError("Collection has locked items: can not be released");
    }
    if (itemIds.isEmpty()) {
      throw new UnprocessableError("An empty collection can not be released!");
    }
  }

  /**
   * Return the ids of all items of the collection and its subcollection
   * 
   * @param c
   * @param user
   * @return
   * @throws UnprocessableError
   */
  private List<String> getItemIds(CollectionImeji c, User user) throws UnprocessableError {
    return new ItemService().search(c.getId(), new SearchFactory().and(new SearchPair(SearchFields.filename, "*")).build(), null, user,
        Search.GET_ALL_RESULTS, Search.SEARCH_FROM_START_INDEX).getResults();
  }


  /**
   * True if at least one {@link Item} is locked by another {@link User}
   *
   * @param uris
   * @param user
   * @return
   */
  protected boolean hasImageLocked(List<String> uris, User user) {
    for (final String uri : uris) {
      if (Locks.isLocked(uri.toString(), user.getEmail())) {
        return true;
      }
    }
    return false;
  }

  /**
   * Part object to update status
   * 
   * @author saquet
   *
   */
  public class StatusPart implements Serializable {
    private static final long serialVersionUID = 7167344369483590830L;
    private Calendar modified;
    private final String status;
    private final String id;
    private final String comment;

    public StatusPart(String id, Status status, Calendar modified, String comment) {
      this.modified = modified;
      this.status = status.name();
      this.id = id;
      this.comment = comment;
    }

    public Calendar getModified() {
      return modified;
    }

    public String getStatus() {
      return status;
    }

    public String getId() {
      return id;
    }

    public String getComment() {
      return comment;
    }

  }

  /**
   * Part Object to update item license
   * 
   * @author saquet
   *
   */
  public class LicensePart implements Serializable {
    private static final long serialVersionUID = -8457457810566054732L;
    private final String id;
    private final String license;

    public LicensePart(String id, License license) {
      this.id = id;
      this.license = !StringHelper.isNullOrEmptyTrim(license.getName()) ? license.getName() : license.getUrl();
    }

    public String getLicense() {
      return license;
    }

    public String getId() {
      return id;
    }

  }

}
