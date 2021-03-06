package de.mpg.imeji.logic.model.factory;

import de.mpg.imeji.exceptions.UnprocessableError;
import de.mpg.imeji.logic.model.CollectionImeji;
import de.mpg.imeji.logic.model.Item;
import de.mpg.imeji.logic.model.Metadata;
import de.mpg.imeji.logic.model.Organization;
import de.mpg.imeji.logic.model.Person;
import de.mpg.imeji.logic.model.Properties.Status;
import de.mpg.imeji.logic.model.SearchFields;
import de.mpg.imeji.logic.model.Statement;
import de.mpg.imeji.logic.model.User;
import de.mpg.imeji.logic.search.model.SortCriterion;
import de.mpg.imeji.logic.search.model.SortCriterion.SortOrder;

/**
 * Create objects ready to be displayed in JSF
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public class ImejiFactory {

  /**
   * Private Constructor
   */
  private ImejiFactory() {
    // avoid creation
  }

  public static SubscriptionFactory newSubscription() {
    return new SubscriptionFactory();
  }

  /**
   * Return the Factory to create item
   *
   * @return
   */
  public static ItemFactory newItem() {
    return new ItemFactory();
  }

  /**
   * Return the Factory to create user
   * 
   * @return
   */
  public static UserFactory newUser() {
    return new UserFactory();
  }

  /**
   * Return the Factory to create ContentVO
   * 
   * @return
   */
  public static ContentFactory newContent() {
    return new ContentFactory();
  }

  /**
   * Return a {@link CollectionFactory}
   *
   * @return
   */
  public static CollectionFactory newCollection() {
    return new CollectionFactory();
  }

  /**
   * Return a {@link StatementFactory}
   *
   * @return
   */
  public static StatementFactory newStatement() {
    return new StatementFactory();
  }

  /**
   * Return the factory for a {@link Metadata}
   *
   * @param s
   * @return
   */
  public static MetadataFactory newMetadata(Statement s) {
    return new MetadataFactory().setStatementId(s.getIndex());
  }

  /**
   * Return the factory for a {@link Metadata}
   *
   * @return
   */
  public static MetadataFactory newMetadata() {
    return new MetadataFactory();
  }

  public static Person newPerson() {
    final Person pers = new Person();
    pers.setFamilyName("");
    pers.setGivenName("");
    pers.getOrganizations().add(newOrganization());
    return pers;
  }

  public static Person newPerson(String familyName, String givenName, String firstOrganization) {
    final Person pers = new Person();
    pers.setFamilyName(familyName);
    pers.setGivenName(givenName);
    pers.getOrganizations().add(newOrganization(firstOrganization));
    return pers;
  }

  public static Organization newOrganization() {
    final Organization org = new Organization();
    org.setName("");
    return org;
  }

  public static Organization newOrganization(String name) {
    final Organization org = new Organization();
    org.setName(name);
    return org;
  }

  /**
   * Create a {@link SortCriterion}. If the sortBy is not valid, return null;
   * 
   * @param sortBy
   * @param order
   * @return
   */
  public static SortCriterion newSortCriterion(String sortBy, String order) {
    try {
      return new SortCriterion(SearchFields.valueOf(sortBy), newSortOrder(order));
    } catch (Exception e) {
      return null;
    }

  }

  /**
   * Create a Sortorder: allowed values: ascending or descending. Default value = ascending
   * 
   * @param order
   * @return
   */
  public static SortOrder newSortOrder(String order) {
    try {
      return SortOrder.valueOf(order);
    } catch (Exception e) {
      return SortOrder.ASCENDING;
    }
  }

  /**
   * Create a new emtpy {@link Item}
   *
   * @param collection
   * @return
   * @throws UnprocessableError
   */
  public static Item newItem(CollectionImeji collection) throws UnprocessableError {
    final Item item = new Item();
    if (collection == null || collection.getId() == null) {
      return item;
    }
    item.setCollection(collection.getId());
    return item;
  }

  /**
   * Factory Method used during the upload
   *
   * @param collection
   * @param user
   * @param storageId
   * @param title
   * @param fullImageURI
   * @param thumbnailURI
   * @param webURI
   * @return
   * @throws UnprocessableError
   */
  public static Item newItem(CollectionImeji collection, User user, String storageId, String title, String filetype)
      throws UnprocessableError {
    final Item item = ImejiFactory.newItem(collection);
    return newItem(item, collection, user, storageId, title, filetype);
  }

  /**
   * Copy the params into the item
   *
   * @param item
   * @param collection
   * @param user
   * @param storageId
   * @param title
   * @param fullImageURI
   * @param thumbnailURI
   * @param webURI
   * @param filetype
   * @return
   */
  public static Item newItem(Item item, CollectionImeji collection, User user, String storageId, String title, String filetype) {
    item.setFilename(title);
    item.setFiletype(filetype);
    if (collection.getStatus() == Status.RELEASED) {
      item.setStatus(Status.RELEASED);
    }
    return item;
  }

  public static UserGroupFactory newUserGroup() {
    return new UserGroupFactory();
  }

}
