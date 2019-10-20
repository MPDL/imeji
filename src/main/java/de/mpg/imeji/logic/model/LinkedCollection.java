package de.mpg.imeji.logic.model;

import java.io.Serializable;
import java.net.URI;

import de.mpg.imeji.j2j.annotations.j2jId;
import de.mpg.imeji.j2j.annotations.j2jLiteral;
import de.mpg.imeji.j2j.annotations.j2jModel;
import de.mpg.imeji.j2j.annotations.j2jReferencedResource;
import de.mpg.imeji.j2j.annotations.j2jResource;

/**
 * A collection can be linked to other collections that belong to the same experiment, study similar
 * data etc. Linked collections can be internal (i.e an imeji collection) or external (a collection
 * that is stored on another server)
 * 
 * @author breddin
 *
 */


@j2jResource("http://imeji.org/linkedCollection")
@j2jModel("collection")
@j2jId(getMethod = "getId", setMethod = "setId")
public class LinkedCollection implements Serializable {


  /**
   * Classify a linked collection as internal (an imeji collection) or external
   * 
   * @author breddin
   *
   */
  public enum LinkedCollectionType {
    INTERNAL,
    EXTERNAL;
  }

  /**
   * 
   */
  private static final long serialVersionUID = 7937559634271439100L;

  /**
   * Will be set by Jena
   */
  private URI id;

  @j2jLiteral("http://purl.org/dc/terms/type")
  private String linkedCollectionType = LinkedCollectionType.INTERNAL.name();
  @j2jLiteral("http://purl.org/dc/elements/1.1/description")
  private String description;

  // JSF fields, i.e. fields for information exchange with client
  private boolean internalCollectionType = true;

  // Internal collection fields
  @j2jLiteral("http://imeji.org/terms/uri")
  private String internalCollectionUri;
  @j2jReferencedResource(referencedClass = "de.mpg.imeji.logic.model.CollectionImeji", referencedResourceUri = "internalCollectionUri",
      referencedField = "title")
  private String internalCollectionName;

  // External collection fields
  @j2jLiteral("http://imeji.org/terms/text")
  private String externalCollectionUri;
  @j2jLiteral("http://purl.org/dc/elements/1.1/title")
  private String externalCollectionName;


  /**
   * @return the id
   */
  public URI getId() {
    return id;
  }

  /**
   * @param id the id to set
   */
  public void setId(URI id) {
    this.id = id;
  }

  /**
   * Returns if this linked collection has an id (i.e. has been saved in database before)
   * 
   * @return
   */
  public boolean hasId() {
    return this.id != null;
  }


  public void setLinkedCollectionType(String collectionType) {
    this.linkedCollectionType = collectionType;
    if (this.linkedCollectionType.equals(LinkedCollectionType.INTERNAL.name())) {
      this.internalCollectionType = true;
    } else {
      this.internalCollectionType = false;
    }
  }


  public String getLinkedCollectionType() {
    return this.linkedCollectionType;
  }

  /**
   * Called from JSF
   * 
   * @return
   */
  public boolean isInternalCollectionType() {
    return this.linkedCollectionType.equals(LinkedCollectionType.INTERNAL.name());
  }

  /**
   * Called from JSF when user changes status from internal to external or vice versa
   * 
   * @param internalCollectionType
   */
  public void setInternalCollectionType(boolean internalCollectionType) {
    this.internalCollectionType = internalCollectionType;
    if (internalCollectionType) {
      this.linkedCollectionType = LinkedCollectionType.INTERNAL.name();
      //this.resetExternalFields();
    } else {
      this.linkedCollectionType = LinkedCollectionType.EXTERNAL.name();
      //this.resetInternalFields();
    }
  }

  public String getDescription() {
    return this.description;
  }


  public void setDescription(String description) {
    this.description = description;
  }

  /**
   * Checks if the linked collection has consistent data, i.e. if for internal collections the
   * collection name was assigned
   * 
   * @return
   */
  public boolean dataConsistent() {

    // if linked collection is missing in database
    // the collection name is not assigned, yet the uri exists
    if (this.internalCollectionType && this.internalCollectionName == null && this.internalCollectionUri != null) {
      return false;
    }
    return true;

  }


  public void prepareJSFFields() {

    // type
    if (this.linkedCollectionType.equals(LinkedCollectionType.INTERNAL.name())) {
      this.internalCollectionType = true;
    } else {
      this.internalCollectionType = false;
    }

  }

  // internal collection setters/getters

  /**
   * Get the internal collection URI as a String (needed for JSF)
   * 
   * @param uriString
   */
  public void setInternalCollectionUri(String uriString) {
    this.internalCollectionUri = uriString;
  }

  public String getInternalCollectionUri() {
    return this.internalCollectionUri;
  }

  public void setInternalCollectionName(String linkedCollectionName) {
    this.internalCollectionName = linkedCollectionName;
  }

  public String getInternalCollectionName() {
    return this.internalCollectionName;
  }

  public void resetInternalFields() {
    this.internalCollectionName = null;
    this.internalCollectionUri = null;
  }

  // external collection setters/getters

  public void setExternalCollectionUri(String linkedCollectionUri) {
    this.externalCollectionUri = linkedCollectionUri;
  }

  public String getExternalCollectionUri() {
    return this.externalCollectionUri;
  }

  public void setExternalCollectionName(String linkedCollectionName) {
    this.externalCollectionName = linkedCollectionName;
  }

  public String getExternalCollectionName() {
    return this.externalCollectionName;
  }

  public void resetExternalFields() {
    this.externalCollectionUri = null;
    this.externalCollectionName = null;
  }


  public boolean isEmpty() {

    if (this.linkedCollectionType.equals(LinkedCollectionType.INTERNAL.name())) {
      if (internalCollectionUri == null) {
        return true;
      } else if (internalCollectionUri.toString().isEmpty()) {
        return true;
      }
      return false;
    } else {
      if (this.externalCollectionUri == null || this.externalCollectionName == null) {
        return true;
      } else {
        if (this.externalCollectionUri.isEmpty() || this.externalCollectionName.isEmpty()) {
          return true;
        }
      }
      return false;
    }
  }



}
