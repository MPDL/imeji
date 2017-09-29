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
import de.mpg.imeji.logic.core.collection.CollectionService;
import de.mpg.imeji.logic.model.CollectionImeji;
import de.mpg.imeji.logic.model.Organization;
import de.mpg.imeji.logic.model.Person;
import de.mpg.imeji.logic.util.ObjectHelper;
import de.mpg.imeji.logic.util.UrlHelper;

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
      setCollection(new CollectionService()
          .retrieve(ObjectHelper.getURI(CollectionImeji.class, getId()), getSessionUser()));
      countItems();
      if (getSessionUser() != null) {
        setSendEmailNotification(getSessionUser().getObservedCollections().contains(getId()));
      }
      if (getCollection() != null) {
        persons = new ArrayList<Person>(getCollection().getPersons().size());
        for (final Person p : getCollection().getPersons()) {
          final List<Organization> orgs = new ArrayList<Organization>(p.getOrganizations().size());
          for (final Organization o : p.getOrganizations()) {
            orgs.add(o);
          }
          p.setOrganizations(orgs);
          persons.add(p);
        }
        getCollection().setPersons(persons);
        setActionMenu(new CollectionActionMenu(getCollection(), getSessionUser(), getLocale()));
      }
    } catch (final ImejiException e) {
      LOGGER.error("Error initializing Bean", e);
    }
  }

  public List<Person> getPersons() {
    return persons;
  }

  public void setPersons(List<Person> persons) {
    this.persons = persons;
  }

  protected String getNavigationString() {
    return "pretty:collectionInfos";
  }

  public String getSmallDescription() {
    if (this.getCollection() == null || this.getCollection().getDescription() == null) {
      return "No Description";
    }
    if (this.getCollection().getDescription().length() > 100) {
      return this.getCollection().getDescription().substring(0, 100) + "...";
    } else {
      return this.getCollection().getDescription();
    }
  }

  @Override
  protected List<URI> getSelectedCollections() {
    return new ArrayList<>();
  }

}
