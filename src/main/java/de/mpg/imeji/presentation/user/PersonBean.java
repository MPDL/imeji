package de.mpg.imeji.presentation.user;

import java.io.Serializable;
import java.net.URI;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.el.ELContext;
import javax.el.ValueExpression;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;
import javax.faces.event.ValueChangeEvent;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import de.mpg.imeji.logic.config.Imeji;
import de.mpg.imeji.logic.user.UserService;
import de.mpg.imeji.logic.vo.CollectionImeji;
import de.mpg.imeji.logic.vo.Organization;
import de.mpg.imeji.logic.vo.Person;
import de.mpg.imeji.logic.vo.factory.ImejiFactory;
import de.mpg.imeji.presentation.beans.ContainerBean;
import de.mpg.imeji.presentation.beans.SuperBean;
import de.mpg.imeji.presentation.registration.RegistrationBean;
import de.mpg.imeji.presentation.session.BeanHelper;

/**
 * The JSF Composite for a {@link Person}
 *
 * @author saquet
 *
 */
@ManagedBean(name = "PersonBean")
@ViewScoped
public class PersonBean extends SuperBean implements Serializable {
  private static final long serialVersionUID = 2066560191381597873L;
  private String personURI;
  private String orgaURI;

  public PersonBean() {}

  /**
   * Change the person
   *
   * @return
   */
  public String changePerson(Object bean, int position) {
    if (personURI == null || personURI.isEmpty()) {
      return ":";
    }
    final Person person = loadPerson(personURI);
    if (bean instanceof UserCreationBean) {
      ((UserCreationBean) bean).getUser().setPerson(person.clone());
    } else if (bean instanceof ContainerBean) {
      final List<Person> l = (List<Person>) ((ContainerBean) bean).getContainer().getPersons();
      l.set(position, person.clone());
    } else if (bean instanceof UserBean) {
      ((UserBean) bean).getUser().setPerson(person.clone());
    }
    return ":";
  }

  /**
   * Change the {@link Organization}
   *
   * @param bean
   * @param positionUser
   * @param positionOrga
   * @return
   */
  public String changeOrga(Object bean, int positionUser, int positionOrga) {
    final Organization orga = loadOrga(orgaURI);
    if (bean instanceof UserCreationBean) {
      final List<Organization> l =
          (List<Organization>) ((UserCreationBean) bean).getUser().getPerson().getOrganizations();
      l.set(positionOrga, orga);
    } else if (bean instanceof ContainerBean) {
      final List<Person> pl = (List<Person>) ((ContainerBean) bean).getContainer().getPersons();
      final List<Organization> l = (List<Organization>) pl.get(positionUser).getOrganizations();
      l.set(positionOrga, orga.clone());
    } else if (bean instanceof UserBean) {
      final List<Organization> l =
          (List<Organization>) ((UserBean) bean).getUser().getPerson().getOrganizations();
      l.set(positionOrga, orga);
    } else if (bean instanceof RegistrationBean) {
      final List<Organization> l =
          (List<Organization>) ((RegistrationBean) bean).getUser().getPerson().getOrganizations();
      l.set(positionOrga, orga);
    }
    return ":";
  }

  /**
   * Load the {@link Person} with the passed uri
   *
   * @param uri
   * @return
   */
  private Person loadPerson(String uri) {
    if (uri != null) {
      try {
        URI.create(uri);
        // if not errors, then the person is intern to imeji
        final UserService uc = new UserService();
        return uc.retrievePersonById(personURI);
      } catch (final Exception e) {
        // is a cone person
        return parseConePersonJSON(uri);
      }
    }
    return null;
  }

  /**
   * Parse a json from cone for a person
   *
   * @param jsonString
   * @return
   */
  private Person parseConePersonJSON(String jsonString) {
    final Object json = JSONValue.parse(jsonString);
    final Person p = ImejiFactory.newPerson();
    if (json instanceof JSONObject) {

      p.setFamilyName((String) ((JSONObject) json).get("http_xmlns_com_foaf_0_1_family_name"));
      p.setGivenName((String) ((JSONObject) json).get("http_xmlns_com_foaf_0_1_givenname"));
      p.setIdentifier((String) ((JSONObject) json).get("id"));
      p.setOrganizations(parseConeOrgnanizationJson(
          ((JSONObject) json).get("http_purl_org_escidoc_metadata_terms_0_1_position").toString()));
      return p;
    }
    return null;
  }

  /**
   * Parse a json from cone for an organization
   *
   * @param jsonString
   * @return
   */
  private List<Organization> parseConeOrgnanizationJson(String jsonString) {
    final Object json = JSONValue.parse(jsonString);
    final List<Organization> l = new ArrayList<Organization>();
    if (json instanceof JSONArray) {
      for (final Iterator<?> iterator = ((JSONArray) json).iterator(); iterator.hasNext();) {
        l.addAll(parseConeOrgnanizationJson(iterator.next().toString()));
      }
    } else if (json instanceof JSONObject) {
      final Organization o = ImejiFactory.newOrganization();
      o.setName(
          (String) ((JSONObject) json).get("http_purl_org_eprint_terms_affiliatedInstitution"));
      l.add(o);
    }
    return l;
  }

  private Organization loadOrga(String uri) {
    if (uri != null) {
      try {
        final UserService uc = new UserService();
        return uc.retrieveOrganizationById(uri);
      } catch (final Exception e) {
        BeanHelper.error(e.getMessage());
      }
    }
    return null;
  }

  /**
   * Read a JSON Object as a String, whether it is an {@link JSONArray}, a {@link String} or a
   * {@link JSONObject}
   *
   * @param jsonObj
   * @param jsonName
   * @return
   */
  private String writeJsonArrayToOneString(Object jsonObj, String jsonName) {
    String str = "";
    if (jsonObj instanceof JSONArray) {
      for (final Iterator<?> iterator = ((JSONArray) jsonObj).iterator(); iterator.hasNext();) {
        if (!"".equals(str)) {
          str += ", ";
        }
        str += writeJsonArrayToOneString(iterator.next(), jsonName);
      }
    } else if (jsonObj instanceof JSONObject) {
      str = (String) ((JSONObject) jsonObj).get(jsonName);
    } else if (jsonObj instanceof String) {
      str = (String) jsonObj;
    }
    return str;
  }

  /**
   * Get the {@link Person} which comes from the parent bean
   */
  private Person getPersonFromParentBean() {
    final FacesContext facesContext = FacesContext.getCurrentInstance();
    final ELContext elContext = facesContext.getELContext();
    final ValueExpression valueExpression = facesContext.getApplication().getExpressionFactory()
        .createValueExpression(elContext, "#{cc.attrs.person}", Person.class);

    return (Person) valueExpression.getValue(elContext);
  }

  /**
   * Add an organization to an author of the {@link CollectionImeji}
   *
   * @param authorPosition
   * @param organizationPosition
   * @return
   */
  public String addOrganization(int organizationPosition) {
    final List<Organization> orgs =
        (List<Organization>) getPersonFromParentBean().getOrganizations();
    final Organization o = ImejiFactory.newOrganization();
    o.setPos(organizationPosition);
    orgs.add(organizationPosition, o);
    return "";
  }

  /**
   * Remove an organization to an author of the {@link CollectionImeji}
   *
   * @return
   */
  public String removeOrganization(int organizationPosition) {
    final List<Organization> orgs =
        (List<Organization>) getPersonFromParentBean().getOrganizations();
    if (orgs.size() > 1) {
      orgs.remove(organizationPosition);
    } else {
      BeanHelper.error(
          Imeji.RESOURCE_BUNDLE.getMessage("error_author_need_one_organization", getLocale()));
    }
    return "";
  }

  /**
   * Listener
   *
   * @param event
   */
  public void orgaListener(ValueChangeEvent event) {
    this.orgaURI = event.getNewValue().toString();
  }

  /**
   * Listener
   *
   * @param event
   */
  public void personListener(ValueChangeEvent event) {
    this.personURI = event.getNewValue().toString();
  }

  /**
   * Getter
   *
   * @return
   */
  public String getPersonURI() {
    return personURI;
  }

  /**
   * setter
   *
   * @param personURI
   */
  public void setPersonURI(String personURI) {
    this.personURI = personURI;
  }

  public String getOrgaURI() {
    return orgaURI;
  }

  public void setOrgaURI(String orgaURI) {
    this.orgaURI = orgaURI;
  }

}
