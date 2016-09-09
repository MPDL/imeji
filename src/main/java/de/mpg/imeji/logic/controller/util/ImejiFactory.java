/**
 * License: src/main/resources/license/escidoc.license
 */
package de.mpg.imeji.logic.controller.util;

import java.net.URI;

import org.apache.log4j.Logger;

import de.mpg.imeji.logic.vo.Album;
import de.mpg.imeji.logic.vo.CollectionImeji;
import de.mpg.imeji.logic.vo.ContainerMetadata;
import de.mpg.imeji.logic.vo.Item;
import de.mpg.imeji.logic.vo.Item.Visibility;
import de.mpg.imeji.logic.vo.MetadataProfile;
import de.mpg.imeji.logic.vo.MetadataSet;
import de.mpg.imeji.logic.vo.Organization;
import de.mpg.imeji.logic.vo.Person;
import de.mpg.imeji.logic.vo.Properties.Status;
import de.mpg.imeji.logic.vo.Space;
import de.mpg.imeji.logic.vo.Statement;
import de.mpg.imeji.logic.vo.User;
import de.mpg.imeji.logic.vo.predefinedMetadata.Metadata.Types;
import de.mpg.imeji.util.LocalizedString;

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

  public static Album newAlbum() {
    Album album = new Album();
    album.setMetadata(newContainerMetadata());
    return album;
  }

  public static CollectionImeji newCollection() {
    CollectionImeji coll = new CollectionImeji();
    coll.setMetadata(newContainerMetadata());
    return coll;
  }

  public static CollectionImeji newCollection(String title, String firstAuthorFamilyName,
      String firstAuthorGivenName, String firstAuthorOrganization) {
    CollectionImeji coll = new CollectionImeji();
    coll.setMetadata(newContainerMetadata(title, firstAuthorFamilyName, firstAuthorGivenName,
        firstAuthorOrganization));
    return coll;
  }

  public static MetadataProfile newProfile() {
    MetadataProfile p = new MetadataProfile();
    return p;
  }

  public static ContainerMetadata newContainerMetadata() {
    ContainerMetadata cm = new ContainerMetadata();
    cm.getPersons().add(newPerson());
    return cm;
  }

  public static ContainerMetadata newContainerMetadata(String title, String firstAuthorFamilyName,
      String firstAuthorGivenName, String firstAuthorOrganization) {
    ContainerMetadata cm = new ContainerMetadata();
    cm.setTitle(title);
    cm.getPersons()
        .add(newPerson(firstAuthorFamilyName, firstAuthorGivenName, firstAuthorOrganization));
    return cm;
  }

  public static Space newSpace() {
    Space s = new Space();
    return s;
  }

  /**
   * Crate a new emtpy {@link Statement}
   *
   * @return
   */
  public static Statement newStatement() {
    Statement s = new Statement();
    s.getLabels().add(new LocalizedString("", null));
    return s;
  }

  /**
   * Crate a new emtpy {@link Statement}
   *
   * @return
   */
  public static Statement newStatement(String label, String lang, Types type) {
    Statement s = new Statement();
    s.getLabels().add(new LocalizedString(label, lang));
    s.setType(URI.create(type.getClazzNamespace()));
    return s;
  }

  /**
   * Create an emtpy {@link Statement} as a child of another {@link Statement}
   *
   * @param parent
   * @return
   */
  public static Statement newStatement(URI parent) {
    Statement s = newStatement();
    s.setParent(parent);
    return s;
  }

  public static Person newPerson() {
    Person pers = new Person();
    pers.setAlternativeName("");
    pers.setFamilyName("");
    pers.setGivenName("");
    pers.getOrganizations().add(newOrganization());
    return pers;
  }

  public static Person newPerson(String familyName, String givenName, String firstOrganization) {
    Person pers = new Person();
    pers.setAlternativeName("");
    pers.setFamilyName(familyName);
    pers.setGivenName(givenName);
    pers.getOrganizations().add(newOrganization(firstOrganization));
    return pers;
  }

  public static Organization newOrganization() {
    Organization org = new Organization();
    org.setName("");
    return org;
  }

  public static Organization newOrganization(String name) {
    Organization org = new Organization();
    org.setName(name);
    return org;
  }

  public static MetadataSet newMetadataSet(URI profile) {
    MetadataSet mds = new MetadataSet();
    mds.setProfile(profile);
    return mds;
  }

  /**
   * Create a new emtpy {@link Item}
   *
   * @param collection
   * @return
   */
  public static Item newItem(CollectionImeji collection) {
    Item item = new Item();
    if (collection == null || collection.getId() == null) {
      throw new RuntimeException("Can not create item with a collection null");
    }
    item.setCollection(collection.getId());
    item.getMetadataSets().add(newMetadataSet(collection.getProfile()));
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
   */
  public static Item newItem(CollectionImeji collection, User user, String storageId, String title,
      URI fullImageURI, URI thumbnailURI, URI webURI, String filetype) {
    Item item = ImejiFactory.newItem(collection);
    return newItem(item, collection, user, storageId, title, fullImageURI, thumbnailURI, webURI,
        filetype);
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
      String title, URI fullImageURI, URI thumbnailURI, URI webURI, String filetype) {
    item.setFullImageUrl(fullImageURI);
    item.setThumbnailImageUrl(thumbnailURI);
    item.setWebImageUrl(webURI);
    item.setVisibility(Visibility.PUBLIC);
    item.setFilename(title);
    item.setFiletype(filetype);
    if (storageId != null) {
      item.setStorageId(storageId);
    }
    if (collection.getStatus() == Status.RELEASED) {
      item.setStatus(Status.RELEASED);
    }
    return item;
  }
}
