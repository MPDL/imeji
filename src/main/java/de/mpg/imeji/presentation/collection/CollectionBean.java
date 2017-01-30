package de.mpg.imeji.presentation.collection;

import static com.google.common.base.Strings.isNullOrEmpty;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.exceptions.UnprocessableError;
import de.mpg.imeji.logic.config.Imeji;
import de.mpg.imeji.logic.doi.DoiService;
import de.mpg.imeji.logic.item.ItemService;
import de.mpg.imeji.logic.search.model.SearchIndex.SearchFields;
import de.mpg.imeji.logic.util.ObjectHelper;
import de.mpg.imeji.logic.search.model.SearchOperators;
import de.mpg.imeji.logic.search.model.SearchPair;
import de.mpg.imeji.logic.search.model.SearchQuery;
import de.mpg.imeji.logic.vo.CollectionImeji;
import de.mpg.imeji.logic.vo.ContainerAdditionalInfo;
import de.mpg.imeji.logic.vo.Item;
import de.mpg.imeji.logic.vo.Organization;
import de.mpg.imeji.logic.vo.Person;
import de.mpg.imeji.logic.vo.Properties.Status;
import de.mpg.imeji.logic.vo.User;
import de.mpg.imeji.logic.vo.factory.ImejiFactory;
import de.mpg.imeji.presentation.beans.SuperBean;
import de.mpg.imeji.presentation.session.BeanHelper;
import de.mpg.imeji.presentation.session.SessionBean;

/**
 * Abstract bean for all collection beans
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public abstract class CollectionBean extends SuperBean {
  private static final long serialVersionUID = -3071769388574710503L;
  private static final Logger LOGGER = Logger.getLogger(CollectionBean.class);
  private CollectionImeji collection;
  private String id;
  private String profileId;
  private boolean selected;

  private boolean sendEmailNotification = false;
  private boolean collectionCreateMode = true;
  private boolean profileSelectMode = false;
  private CollectionActionMenu actionMenu;
  private int authorPosition;
  private int organizationPosition;
  private int size;
  private List<Item> items;
  private List<Item> discardedItems;
  private int sizeDiscarded;

  /**
   * New default {@link CollectionBean}
   */
  public CollectionBean() {
    collection = new CollectionImeji();
  }

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

  /**
   * @return the selected
   */
  public boolean getSelected() {
    if (getSelectedCollections().contains(collection.getId())) {
      selected = true;
    } else {
      selected = false;
    }
    return selected;
  }

  /**
   * @param selected the selected to set
   */
  public void setSelected(boolean selected) {
    if (selected) {
      if (!(getSelectedCollections().contains(collection.getId()))) {
        getSelectedCollections().add(collection.getId());
      }
    } else {
      getSelectedCollections().remove(collection.getId());
    }
    this.selected = selected;
  }

  protected abstract List<URI> getSelectedCollections();

  /**
   * getter
   *
   * @return
   */
  public String getProfileId() {
    return profileId;
  }

  /**
   * setter
   *
   * @param profileId
   */
  public void setProfileId(String profileId) {
    this.profileId = profileId;
  }

  public String getPageUrl() {
    return getNavigation().getCollectionUrl() + id;
  }


  public boolean isSendEmailNotification() {
    return sendEmailNotification;
  }

  public void setSendEmailNotification(boolean sendEmailNotification) {
    this.sendEmailNotification = sendEmailNotification;
    // check if id already set
    if (!isNullOrEmpty(id)) {
      if (sendEmailNotification) {
        getSessionUser().addObservedCollection(id);
      } else {
        getSessionUser().removeObservedCollection(id);
      }
    }
  }

  public boolean isCollectionCreateMode() {
    return collectionCreateMode;
  }

  public void setCollectionCreateMode(boolean collectionCreateMode) {
    this.collectionCreateMode = collectionCreateMode;
  }

  public boolean isProfileSelectMode() {
    return profileSelectMode;
  }

  public void setProfileSelectMode(boolean profileSelectMode) {
    this.profileSelectMode = profileSelectMode;
  }

  /**
   * @return the actionMenu
   */
  public CollectionActionMenu getActionMenu() {
    return actionMenu;
  }

  /**
   * @param actionMenu the actionMenu to set
   */
  public void setActionMenu(CollectionActionMenu actionMenu) {
    this.actionMenu = actionMenu;
  }

  /**
   * Find the first {@link Item} of the current {@link Container} (fast method)
   *
   * @param user
   * @param size
   */
  protected void findItems(User user, int size) {
    final ItemService ic = new ItemService();
    ic.searchAndSetContainerItems(collection, user, size, 0);
  }

  /**
   * Count the size the {@link Container}
   *
   * @param hasgrant
   * @return
   */
  protected void countItems() {
    final ItemService ic = new ItemService();
    size = ic.search(collection.getId(), null, null, Imeji.adminUser, 0, 0).getNumberOfRecords();
  }

  /**
   * Load the {@link Item} of the {@link Container}
   *
   * @throws ImejiException
   */
  protected void loadItems(User user, int size) {
    setItems(new ArrayList<Item>());
    if (collection != null) {
      final List<String> uris = new ArrayList<String>();
      for (final URI uri : collection.getImages()) {
        uris.add(uri.toString());
      }
      final ItemService ic = new ItemService();
      try {
        setItems((List<Item>) ic.retrieveBatchLazy(uris, size, 0, user));
      } catch (final ImejiException e) {
        LOGGER.error("Error loading items of container");
        BeanHelper.error("Error reading items of " + collection.getTitle());
      }
    }
  }


  /**
   * Load the {@link Item} of the {@link Container}
   */
  public void countDiscardedItems(User user) {
    if (collection != null) {
      final ItemService ic = new ItemService();
      final SearchQuery q = new SearchQuery();
      try {
        q.addPair(new SearchPair(SearchFields.status, SearchOperators.EQUALS,
            Status.WITHDRAWN.getUriString(), false));
      } catch (final UnprocessableError e) {
        LOGGER.error("Error creating query to search for discarded items of a container", e);
      }
      setSizeDiscarded(ic.search(collection.getId(), q, null, user, -1, 0).getNumberOfRecords());
    } else {
      setSizeDiscarded(0);
    }
  }

  /**
   * Get Person String
   *
   * @return
   */
  public String getPersonString() {
    String personString = "";
    for (final Person p : collection.getPersons()) {
      if (!"".equalsIgnoreCase(personString)) {
        personString += ", ";
      }
      personString += p.getFamilyName() + " " + p.getGivenName() + " ";
    }
    return personString;
  }

  /**
   * @return
   */
  public String getAuthorsWithOrg() {
    String personString = "";
    for (final Person p : collection.getPersons()) {
      if (!"".equalsIgnoreCase(personString)) {
        personString += ", ";
      }
      personString += p.getCompleteName();
      if (!p.getOrganizationString().equals("")) {
        personString += " (" + p.getOrganizationString() + ")";
      }
    }
    return personString;
  }

  public String getCitation() {
    final String url = getDoiUrl().isEmpty() ? getPageUrl() : getDoiUrl();
    return getAuthorsWithOrg() + ". " + collection.getTitle() + ". <a href=\"" + url + "\">" + url
        + "</a>";
  }

  /**
   * The Url to view the DOI
   *
   * @return
   */
  public String getDoiUrl() {
    return collection.getDoi().isEmpty() ? "" : DoiService.DOI_URL_RESOLVER + collection.getDoi();
  }

  /**
   * Add an addtionial Info at the passed position
   *
   * @param pos
   */
  public void addAdditionalInfo(int pos) {
    collection.getAdditionalInformations().add(pos, new ContainerAdditionalInfo("", "", ""));
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
   * Add a new author to the {@link CollectionImeji}
   *
   * @param authorPosition
   * @return
   */
  public String addAuthor(int authorPosition) {
    final List<Person> c = (List<Person>) collection.getPersons();
    final Person p = ImejiFactory.newPerson();
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
    final List<Organization> orgs =
        (List<Organization>) persons.get(authorPosition).getOrganizations();
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
    final List<Organization> orgs =
        (List<Organization>) persons.get(authorPosition).getOrganizations();
    if (orgs.size() > 1) {
      orgs.remove(organizationPosition);
    } else {
      BeanHelper.error(
          Imeji.RESOURCE_BUNDLE.getMessage("error_author_need_one_organization", getLocale()));
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
   * @return the items
   */
  public List<Item> getItems() {
    return items;
  }

  /**
   * @return the discarded items
   */
  public List<Item> getDiscardedItems() {
    return discardedItems;
  }

  /**
   * @param items the items to set
   */
  public void setItems(List<Item> items) {
    this.items = items;
  }

  /**
   * @param discarded items setter
   */
  public void setDiscardedItems(List<Item> items) {
    this.discardedItems = items;
  }

  /**
   * @return the size
   */
  public int getSize() {
    return size;
  }

  /**
   * @param size the size to set
   */
  public void setSize(int size) {
    this.size = size;
  }

  /**
   * @param size the size to set
   */
  public void setSizeDiscarded(int size) {
    this.sizeDiscarded = size;
  }

  public int getSizeDiscarded() {
    return sizeDiscarded;
  }

  /**
   * True if the current {@link User} is the creator of the {@link Container}
   *
   * @return
   */
  public boolean isOwner() {
    final SessionBean sessionBean = (SessionBean) BeanHelper.getSessionBean(SessionBean.class);
    if (collection != null && collection.getCreatedBy() != null && sessionBean.getUser() != null) {
      return collection.getCreatedBy()
          .equals(ObjectHelper.getURI(User.class, sessionBean.getUser().getEmail()));
    }
    return false;
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
}
