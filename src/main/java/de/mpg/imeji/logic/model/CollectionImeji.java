package de.mpg.imeji.logic.model;

import java.io.Serializable;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import de.mpg.imeji.j2j.annotations.j2jId;
import de.mpg.imeji.j2j.annotations.j2jList;
import de.mpg.imeji.j2j.annotations.j2jLiteral;
import de.mpg.imeji.j2j.annotations.j2jModel;
import de.mpg.imeji.j2j.annotations.j2jResource;
import de.mpg.imeji.logic.model.aspects.CloneURI;
import de.mpg.imeji.logic.util.ObjectHelper.ObjectType;

/**
 * imeji collection has one {@link MetadataProfile} and contains {@link Item}
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
@j2jResource("http://imeji.org/terms/collection")
@j2jModel("collection")
@j2jId(getMethod = "getId", setMethod = "setId")
public class CollectionImeji extends Properties implements Serializable, CollectionElement, CloneURI {
  private static final long serialVersionUID = -4689209760815149573L;
  @j2jResource("http://imeji.org/terms/collection")
  private URI collection;
  @j2jLiteral("http://purl.org/dc/elements/1.1/title")
  private String title;
  @j2jLiteral("http://purl.org/dc/elements/1.1/description")
  private String description;
  @j2jList("http://xmlns.com/foaf/0.1/person")
  protected Collection<Person> persons = new ArrayList<Person>();
  @j2jList("http://imeji.org/AdditionalInfo")
  private List<ContainerAdditionalInfo> additionalInformations = new ArrayList<>();
  @j2jLiteral("http://imeji.org/terms/doi")
  private String doi;
  @j2jResource("http://imeji.org/terms/logoUrl")
  private URI logoUrl;
  @j2jList("http://purl.org/dc/terms/type")
  private List<String> types = new ArrayList<>();
  private Collection<URI> images = new ArrayList<URI>();


  @Override
  public Object cloneURI() {
    CollectionImeji newCollection = new CollectionImeji();
    newCollection.setId(this.getId());
    return newCollection;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public Collection<Person> getPersons() {
    return persons;
  }

  public void setPersons(Collection<Person> person) {
    this.persons = person;
  }

  public List<ContainerAdditionalInfo> getAdditionalInformations() {
    return additionalInformations;
  }

  public void setAdditionalInformations(List<ContainerAdditionalInfo> additionalInformations) {
    this.additionalInformations = additionalInformations;
  }

  public URI getLogoUrl() {
    return this.logoUrl;
  }

  public void setLogoUrl(URI logoUrl) {
    this.logoUrl = logoUrl;
  }

  public void setDoi(String doi) {
    this.doi = doi;
  }

  public String getDoi() {
    return doi;
  }

  public void setImages(Collection<URI> images) {
    this.images = images;
  }

  @Deprecated
  // TODO remove
  public Collection<URI> getImages() {
    return images;
  }

  /**
   * @return the collection
   */
  public URI getCollection() {
    return collection;
  }

  /**
   * @param collection the collection to set
   */
  public void setCollection(URI collection) {
    this.collection = collection;
  }

  /**
   * True if this collection is subcollection, i.e. if it has a parent collection
   * 
   * @return
   */
  public boolean isSubCollection() {
    return getCollection() != null;
  }

  @Override
  public String getName() {
    return title;
  }

  @Override
  public String getUri() {
    return getId().toString();
  }

  @Override
  public ObjectType getType() {
    return ObjectType.COLLECTION;
  }

  public List<String> getTypes() {
    return types;
  }

  public void setTypes(List<String> types) {
    this.types = types;
  }


}
