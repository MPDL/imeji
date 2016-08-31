/**
 * License: src/main/resources/license/escidoc.license
 */
package de.mpg.imeji.presentation.collection;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;

import org.apache.log4j.Logger;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.exceptions.NotFoundException;
import de.mpg.imeji.logic.controller.resource.CollectionController;
import de.mpg.imeji.logic.util.ObjectHelper;
import de.mpg.imeji.logic.util.UrlHelper;
import de.mpg.imeji.logic.vo.CollectionImeji;
import de.mpg.imeji.logic.vo.Organization;
import de.mpg.imeji.logic.vo.Person;
import de.mpg.imeji.presentation.session.SessionBean;

/**
 * Bean for the pages "CollectionEntryPage" and "ViewCollection"
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
@ManagedBean(name = "ViewCollectionBean")
@RequestScoped
public class ViewCollectionBean extends CollectionBean {
  private static final long serialVersionUID = 6473181109648137472L;
  private List<Person> persons;
  private static final Logger LOGGER = Logger.getLogger(ViewCollectionBean.class);
  /**
   * Maximum number of items displayed on collection start page
   */
  private static final int MAX_ITEM_NUM_VIEW = 13;


  /**
   * Initialize all elements of the page.
   *
   * @throws NotFoundException
   *
   * @throws Exception
   */
  @PostConstruct
  public void init() {
    setId(UrlHelper.getParameterValue("id"));
    try {
      setCollection(new CollectionController()
          .retrieveLazy(ObjectHelper.getURI(CollectionImeji.class, getId()), getSessionUser()));
      if (getSessionUser() != null) {
        setSendEmailNotification(getSessionUser().getObservedCollections().contains(getId()));
      }
      if (getCollection() != null) {
        initCollectionProfile();
        persons = new ArrayList<Person>(getCollection().getMetadata().getPersons().size());
        for (Person p : getCollection().getMetadata().getPersons()) {
          List<Organization> orgs = new ArrayList<Organization>(p.getOrganizations().size());
          for (Organization o : p.getOrganizations()) {
            orgs.add(o);
          }
          p.setOrganizations(orgs);
          persons.add(p);
        }
        getCollection().getMetadata().setPersons(persons);
      }
    } catch (ImejiException e) {
      LOGGER.error("Error initializing Bean", e);
    }
  }

  public List<Person> getPersons() {
    return persons;
  }

  public void setPersons(List<Person> persons) {
    this.persons = persons;
  }

  @Override
  protected String getNavigationString() {
    return SessionBean.getPrettySpacePage("pretty:collectionInfos", getSpace());
  }

  public String getSmallDescription() {
    if (this.getCollection() == null
        || this.getCollection().getMetadata().getDescription() == null) {
      return "No Description";
    }
    if (this.getCollection().getMetadata().getDescription().length() > 100) {
      return this.getCollection().getMetadata().getDescription().substring(0, 100) + "...";
    } else {
      return this.getCollection().getMetadata().getDescription();
    }
  }

  @Override
  protected List<URI> getSelectedCollections() {
    return new ArrayList<>();
  }

}
