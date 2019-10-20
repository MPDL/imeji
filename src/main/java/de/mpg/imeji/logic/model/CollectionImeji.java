package de.mpg.imeji.logic.model;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.mpg.imeji.j2j.annotations.j2jId;
import de.mpg.imeji.j2j.annotations.j2jList;
import de.mpg.imeji.j2j.annotations.j2jLiteral;
import de.mpg.imeji.j2j.annotations.j2jModel;
import de.mpg.imeji.j2j.annotations.j2jResource;
import de.mpg.imeji.logic.model.aspects.AccessMember;
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
public class CollectionImeji extends Properties implements Serializable, CollectionElement, CloneURI, AccessMember {
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

  @j2jList("http://imeji.org/linkedCollection")
  private List<LinkedCollection> linkedCollections = new ArrayList<LinkedCollection>();

  private static final Logger LOGGER = LogManager.getLogger(CollectionImeji.class);



  @Override
  public Object cloneURI() {
    CollectionImeji newCollection = new CollectionImeji();
    newCollection.setId(this.getId());
    return newCollection;
  }


  @Override
  public void accessMember(ChangeMember changeMember) {

    // member field that can be set unsynchronized: 
    //collection's parent, "collection"
    // linked collections: remove a internal linked collection (after it has been deleted)

    super.accessMember(changeMember);

    try {
      Field parentOfCollectionField = CollectionImeji.class.getDeclaredField("collection");
      Field linkedCollectionsField = CollectionImeji.class.getDeclaredField("linkedCollections");
      if (changeMember.getField().equals(parentOfCollectionField) && changeMember.getAction().equals(ActionType.ADD_OVERRIDE)
          && changeMember.getValue() instanceof URI) {
        this.collection = (URI) changeMember.getValue();

      } else if (changeMember.getField().equals(linkedCollectionsField) && changeMember.getAction().equals(ActionType.REMOVE)
          && changeMember.getValue() instanceof URI) {
        String uriToDelete = (String) changeMember.getValue();
        for (LinkedCollection linkedCollection : this.linkedCollections) {
          if (linkedCollection.isInternalCollectionType() && linkedCollection.getInternalCollectionUri().equals(uriToDelete)) {
            this.linkedCollections.remove(linkedCollection);
          }
        }

      } else {
        LOGGER.debug("Did not set member in CollectionImeji. Please check and implement your case");
      }
    } catch (NoSuchFieldException | SecurityException e) {
      LOGGER.error("Did not set member in CollectionImeji. Member does not exist in class.", e);
    }


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

  // Section linked collections

  public void setLinkedCollections(List<LinkedCollection> linkedCollections) {
    this.linkedCollections = linkedCollections;
  }

  public List<LinkedCollection> getLinkedCollections() {
    return this.linkedCollections;
  }

}
