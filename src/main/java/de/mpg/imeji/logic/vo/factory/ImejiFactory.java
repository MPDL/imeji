/**
 * License: src/main/resources/license/escidoc.license
 */
package de.mpg.imeji.logic.vo.factory;

import org.apache.log4j.Logger;

import de.mpg.imeji.exceptions.UnprocessableError;
import de.mpg.imeji.logic.vo.Album;
import de.mpg.imeji.logic.vo.CollectionImeji;
import de.mpg.imeji.logic.vo.ContainerMetadata;
import de.mpg.imeji.logic.vo.Item;
import de.mpg.imeji.logic.vo.Metadata;
import de.mpg.imeji.logic.vo.Organization;
import de.mpg.imeji.logic.vo.Person;
import de.mpg.imeji.logic.vo.Properties.Status;
import de.mpg.imeji.logic.vo.Statement;
import de.mpg.imeji.logic.vo.User;

/**
 * Create objects ready to be displayed in JSF
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public class ImejiFactory {
  private static final Logger LOGGER = Logger.getLogger(ImejiFactory.class);

  /**
   * Private Constructor
   */
  private ImejiFactory() {
    // avoid creation
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


  public static Album newAlbum() {
    final Album album = new Album();
    return album;
  }

  public static ContainerMetadata newContainerMetadata() {
    final ContainerMetadata cm = new ContainerMetadata();
    cm.getPersons().add(newPerson());
    return cm;
  }

  public static ContainerMetadata newContainerMetadata(String title, String firstAuthorFamilyName,
      String firstAuthorGivenName, String firstAuthorOrganization) {
    final ContainerMetadata cm = new ContainerMetadata();
    cm.setTitle(title);
    cm.getPersons()
        .add(newPerson(firstAuthorFamilyName, firstAuthorGivenName, firstAuthorOrganization));
    return cm;
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
  public static Item newItem(CollectionImeji collection, User user, String storageId, String title,
      String filetype) throws UnprocessableError {
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
  public static Item newItem(Item item, CollectionImeji collection, User user, String storageId,
      String title, String filetype) {
    item.setFilename(title);
    item.setFiletype(filetype);
    if (collection.getStatus() == Status.RELEASED) {
      item.setStatus(Status.RELEASED);
    }
    return item;
  }



}
