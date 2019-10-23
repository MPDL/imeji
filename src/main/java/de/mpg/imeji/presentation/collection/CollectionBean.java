package de.mpg.imeji.presentation.collection;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import javax.faces.model.SelectItem;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.config.Imeji;
import de.mpg.imeji.logic.config.ImejiConfiguration;
import de.mpg.imeji.logic.core.collection.CollectionService;
import de.mpg.imeji.logic.model.CollectionImeji;
import de.mpg.imeji.logic.model.ContainerAdditionalInfo;
import de.mpg.imeji.logic.model.LinkedCollection;
import de.mpg.imeji.logic.model.LinkedCollection.LinkedCollectionType;
import de.mpg.imeji.logic.model.Organization;
import de.mpg.imeji.logic.model.Person;
import de.mpg.imeji.logic.model.SearchFields;
import de.mpg.imeji.logic.model.factory.ImejiFactory;
import de.mpg.imeji.logic.search.Search;
import de.mpg.imeji.logic.search.factory.SearchFactory;
import de.mpg.imeji.logic.search.model.SearchLogicalRelation.LOGICAL_RELATIONS;
import de.mpg.imeji.logic.search.model.SearchOperators;
import de.mpg.imeji.logic.search.model.SearchPair;
import de.mpg.imeji.logic.search.model.SortCriterion;
import de.mpg.imeji.logic.search.model.SortCriterion.SortOrder;
import de.mpg.imeji.presentation.beans.SuperBean;
import de.mpg.imeji.presentation.session.BeanHelper;

/**
 * Abstract bean for all collection beans
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public abstract class CollectionBean extends SuperBean {
  private static final long serialVersionUID = -3071769388574710503L;
  static final Logger LOGGER = LogManager.getLogger(CollectionBean.class);
  private CollectionImeji collection;
  private String id;
  private int authorPosition;
  private int organizationPosition;

  private List<CollectionImeji> internalCollectionsToLink;
  private List<LinkedCollection> linkedCollectionsToEdit;

  protected String getErrorMessageNoAuthor() {
    return "error_collection_need_one_author";
  }

  /**
   * @return the collection
   */
  public CollectionImeji getCollection() {
    return collection;
  }

  /**
   * @param collection the collection to set
   */
  public void setCollection(CollectionImeji collection) {
    this.collection = collection;
  }

  /**
   * @return the id
   */
  public String getId() {
    return id;
  }

  /**
   * @param id the id to set
   */
  public void setId(String id) {
    this.id = id;
  }

  protected abstract List<URI> getSelectedCollections();

  public String getPageUrl() {
    return getNavigation().getCollectionUrl() + id;
  }


  public int numberOfAdditionalInformationLabels(String label) {
    int i = 0;
    for (ContainerAdditionalInfo cai : collection.getAdditionalInformations()) {
      if (label.equals(cai.getLabel())) {
        i++;
      }
    }
    return i;
  }

  /**
   * Add an additionial Info at the passed position
   *
   * @param pos
   */
  public void addAdditionalInfo(int pos, String label) {

    collection.getAdditionalInformations().add(pos, new ContainerAdditionalInfo(label, "", ""));
  }

  /**
   * Remove the nth additional Info
   *
   * @param pos
   */
  public void removeAdditionalInfo(int pos) {
    collection.getAdditionalInformations().remove(pos);
  }

  /**
   * Get the appropriate placeholder for an input field by its label. <br/>
   * Used for additional informations, where only some of the additional information have a
   * placeholder.
   * 
   * @param inputFiledLabel The label of the input field.
   * @return The placeholder or an empty String if no placeholder exists.
   */
  public String getPlaceholder(String inputFiledLabel) {
    if (ImejiConfiguration.COLLECTION_METADATA_GEO_COORDINATES_LABEL.equals(inputFiledLabel)) {
      return Imeji.RESOURCE_BUNDLE.getLabel("placeholder_geocoordinates", getLocale());
    } else if (ImejiConfiguration.COLLECTION_METADATA_KEYWORDS_LABEL.equals(inputFiledLabel)) {
      return Imeji.RESOURCE_BUNDLE.getLabel("placeholder_keywords", getLocale());
    } else {
      return "";
    }
  }

  /**
   * Add a new author to the {@link CollectionImeji}
   *
   * @param authorPosition
   * @return
   */
  public String addAuthor(int authorPosition) {
    final List<Person> c = (List<Person>) collection.getPersons();
    final Person p = ImejiFactory.newPerson();

    //Preselect Organizations/Department with Organizations/Department from the first person (author)
    List<Organization> organizationsOfFirstPerson = (List<Organization>) c.get(0).getOrganizations();
    List<Organization> preselectedOrganizations = new ArrayList<>();
    for (Organization organization : organizationsOfFirstPerson) {
      preselectedOrganizations.add(organization.clone());
    }
    p.setOrganizations(preselectedOrganizations);

    p.setPos(authorPosition + 1);
    c.add(authorPosition + 1, p);
    return "";
  }

  /**
   * Remove an author of the {@link CollectionImeji}
   *
   * @return
   */
  public String removeAuthor(int authorPosition) {
    final List<Person> c = (List<Person>) collection.getPersons();
    if (c.size() > 1) {
      c.remove(authorPosition);
    } else {
      BeanHelper.error(getErrorMessageNoAuthor());
    }
    return "";
  }

  /**
   * Add an organization to an author of the {@link CollectionImeji}
   *
   * @param authorPosition
   * @param organizationPosition
   * @return
   */
  public String addOrganization(int authorPosition, int organizationPosition) {
    final List<Person> persons = (List<Person>) collection.getPersons();
    final List<Organization> orgs = (List<Organization>) persons.get(authorPosition).getOrganizations();
    final Organization o = ImejiFactory.newOrganization();
    o.setPos(organizationPosition + 1);
    orgs.add(organizationPosition + 1, o);
    return "";
  }

  /**
   * Remove an organization to an author of the {@link CollectionImeji}
   *
   * @return
   */
  public String removeOrganization(int authorPosition, int organizationPosition) {
    final List<Person> persons = (List<Person>) collection.getPersons();
    final List<Organization> orgs = (List<Organization>) persons.get(authorPosition).getOrganizations();
    if (orgs.size() > 1) {
      orgs.remove(organizationPosition);
    } else {
      BeanHelper.error(Imeji.RESOURCE_BUNDLE.getMessage("error_author_need_one_organization", getLocale()));
    }
    return "";
  }

  /**
   * getter
   *
   * @return
   */
  public int getAuthorPosition() {
    return authorPosition;
  }

  /**
   * setter
   *
   * @param pos
   */
  public void setAuthorPosition(int pos) {
    this.authorPosition = pos;
  }

  /**
   * @return the collectionPosition
   */
  public int getOrganizationPosition() {
    return organizationPosition;
  }

  /**
   * @param collectionPosition the collectionPosition to set
   */
  public void setOrganizationPosition(int organizationPosition) {
    this.organizationPosition = organizationPosition;
  }

  /**
   * Remove an author of the {@link CollectionImeji}
   *
   * @return
   */
  public String removeContainerLogo() {
    collection.setLogoUrl(null);
    return "";
  }


  public List<SelectItem> getCollectionTypesSelectItems() {
    List<SelectItem> selectItemList = new ArrayList<SelectItem>();
    for (String entry : Imeji.CONFIG.getCollectionTypesAsList()) {
      selectItemList.add(new SelectItem(entry, entry));
    }

    return selectItemList;
  }

  // -- Linked collections JSF setter/getter

  /**
   * Get a list of existing linked collections including a placeholder for new entries
   * 
   * @return
   */
  public List<LinkedCollection> getLinkedCollectionsToEdit() {
    return this.linkedCollectionsToEdit;
  }


  public void setLinkedCollectionsToEdit(List<LinkedCollection> linkedCollectionsToEdit) {
    this.linkedCollectionsToEdit = linkedCollectionsToEdit;
  }

  /**
   * Show radio button in GUI to switch between internal nad external collections
   * 
   * @param linkedCollection
   */
  public boolean showRadioButton(LinkedCollection linkedCollection) {

    // case 1: always show button if collections to link are available
    if (internalCollecionsToLinkAreAvailable()) {
      return true;
    }
    // case 2: in case that no collections are available to link
    // but there is a stored linked internal collection (i.e. to a discarded collection) left
    else if (linkedCollection.isInternalCollectionType()) {
      return true;
    }
    // case 3: don't show button if there are no collections are available to link
    // and the current collection is an external collection
    else {
      return false;
    }
  }

  /**
   * Called to add a new collection via GUI
   * 
   * @param position
   */
  public void addNewLinkedCollection(int position) {

    LinkedCollection newLinkedCollection = new LinkedCollection();
    if (!internalCollecionsToLinkAreAvailable()) {
      newLinkedCollection.setLinkedCollectionType(LinkedCollectionType.EXTERNAL.name());
    }
    this.linkedCollectionsToEdit.add(position, newLinkedCollection);
  }

  /**
   * Called to remove a linked collection via GUI
   * 
   * @param position
   */
  public void removeLinkedCollection(int position) {

    if (position >= 0 && position < this.linkedCollectionsToEdit.size()) {
      this.linkedCollectionsToEdit.remove(position);
    }
    this.checkIfLinkedCollectionsAreEmptyAndPutPlaceholder();
  }



  /**
   * Called in bean and sub-bean initiation. Initiate local list of editable linked collections.
   */
  public void initLinkedCollections() {

    this.linkedCollectionsToEdit = new ArrayList<LinkedCollection>();
    for (LinkedCollection linkedCollection : getCollection().getLinkedCollections()) {
      linkedCollection.prepareJSFFields();
      this.linkedCollectionsToEdit.add(linkedCollection);
    }

    // (2) load all imeji collections that the user may access (for autocomplete functionality)
    this.retrieveInternalCollectionsToLink();

    // (3) put GUI placeholder in case there are no linked collections yet
    this.checkIfLinkedCollectionsAreEmptyAndPutPlaceholder();
  }


  /**
   * Called when saving the collection
   */
  public void saveLinkedCollections() {

    // (1) clean gui placeholder and newly created empty links
    this.cleanLinkedCollections();
    // clean external fields for internal collections and vice versa
    for (LinkedCollection linkedCollection : this.linkedCollectionsToEdit) {
      if (linkedCollection.isInternalCollectionType()) {
        linkedCollection.resetExternalFields();
      } else {
        linkedCollection.resetInternalFields();
      }
    }
    getCollection().setLinkedCollections(this.linkedCollectionsToEdit);
  }

  /**
   * Checks if the list of editable linked collections is empty. If yes puts a placeholder
   */
  private void checkIfLinkedCollectionsAreEmptyAndPutPlaceholder() {

    if (checkIfLinkedCollectionsAreEmpty()) {
      LinkedCollection placeholder = new LinkedCollection();
      if (this.internalCollectionsToLink.isEmpty()) {
        // set placeholder for external collection
        placeholder.setInternalCollectionType(false);
      }
      this.linkedCollectionsToEdit.add(placeholder);
    }
  }


  /**
   * 
   * @return
   */
  private boolean checkIfLinkedCollectionsAreEmpty() {
    return this.linkedCollectionsToEdit.isEmpty();
  }

  /**
   * 
   */
  private void cleanLinkedCollections() {

    // Remove placeholder and empty entries from list
    List<LinkedCollection> emptyCollections = new LinkedList<LinkedCollection>();

    for (LinkedCollection editedLinkedColletion : this.linkedCollectionsToEdit) {
      if (editedLinkedColletion.isEmpty()) {
        emptyCollections.add(editedLinkedColletion);
      }
    }
    this.linkedCollectionsToEdit.removeAll(emptyCollections);
  }


  /**
   * Retrieves a list of all imeji collections that can be linked to a given collection
   */
  private void retrieveInternalCollectionsToLink() {

    try {
      final CollectionService collectionService = new CollectionService();

      //Search for all public collections which are no subcollections
      SearchFactory searchFactory = new SearchFactory();
      searchFactory.addElement(new SearchPair(SearchFields.status, SearchOperators.EQUALS, "public", false), LOGICAL_RELATIONS.AND);
      searchFactory.addElement(new SearchPair(SearchFields.folder, SearchOperators.EQUALS, "*", true), LOGICAL_RELATIONS.AND);

      this.internalCollectionsToLink =
          collectionService.searchAndRetrieve(searchFactory.build(), new SortCriterion(SearchFields.collection_title, SortOrder.DESCENDING),
              getSessionUser(), Search.GET_ALL_RESULTS, Search.SEARCH_FROM_START_INDEX);
    } catch (ImejiException e) {
      // not a severe problem
      this.internalCollectionsToLink = Collections.emptyList();
      LOGGER.error("Error loading internal Collections-To-Link list.", e);
    }
  }

  /**
   * Returns a list with all collections the user can access (except this collection). Format:
   * String that contains a javascript array specification. Use this list for showing a list of
   * collections that can be linked to this collection.
   * 
   * @return
   */
  public String getAvailableCollectionsToLinkForAutocomplete() {
    return createRepresentationForAutocomplete();
  }


  /**
   * Returns whether there are currently internal collections to link
   * 
   * @return
   */
  public boolean internalCollecionsToLinkAreAvailable() {
    return !this.internalCollectionsToLink.isEmpty();
  }


  /**
   * Uri and name of the available collections-to-link will be passed to jQuery autocomplete
   * function in GUI. For this: Create a String that contains a semicolon-separated list of label1;
   * value1; label2; value2 etc entries
   * 
   * If there are no collections to link return an empty String
   * 
   * @return
   */
  private String createRepresentationForAutocomplete() {

    if (this.internalCollectionsToLink.isEmpty()) {
      return "[]";
    }

    String representation = "";
    int index = 0;
    for (CollectionImeji collection : this.internalCollectionsToLink) {

      representation += collection.getTitle();
      representation += ";";
      representation += collection.getUri();

      if (index < this.internalCollectionsToLink.size() - 1) {
        representation += ";";
      }
      index = index + 1;
    }
    return representation;
  }

}
