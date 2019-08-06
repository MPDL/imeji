package de.mpg.imeji.presentation.collection;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import javax.faces.model.SelectItem;

import de.mpg.imeji.logic.config.Imeji;
import de.mpg.imeji.logic.config.ImejiConfiguration;
import de.mpg.imeji.logic.model.CollectionImeji;
import de.mpg.imeji.logic.model.ContainerAdditionalInfo;
import de.mpg.imeji.logic.model.Organization;
import de.mpg.imeji.logic.model.Person;
import de.mpg.imeji.logic.model.factory.ImejiFactory;
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
  private CollectionImeji collection;
  private String id;
  private int authorPosition;
  private int organizationPosition;

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
}
