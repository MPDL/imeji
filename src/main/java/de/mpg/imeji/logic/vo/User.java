/**
 * License: src/main/resources/license/escidoc.license
 */
package de.mpg.imeji.logic.vo;

import java.io.Serializable;
import java.net.URI;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlEnum;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import de.mpg.imeji.logic.Imeji;
import de.mpg.imeji.logic.ImejiNamespaces;
import de.mpg.imeji.logic.auth.util.AuthUtil;
import de.mpg.imeji.logic.controller.UserGroupController;
import de.mpg.imeji.logic.util.IdentifierUtil;
import de.mpg.imeji.presentation.session.SessionBean;
import de.mpg.imeji.presentation.util.BeanHelper;
import de.mpg.imeji.presentation.util.ImejiFactory;
import de.mpg.j2j.annotations.j2jId;
import de.mpg.j2j.annotations.j2jList;
import de.mpg.j2j.annotations.j2jLiteral;
import de.mpg.j2j.annotations.j2jModel;
import de.mpg.j2j.annotations.j2jResource;

/**
 * imeji user
 * 
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
@j2jResource("http://imeji.org/terms/user")
@j2jModel("user")
@j2jId(getMethod = "getId", setMethod = "setId")
public class User implements Serializable {
  private static final long serialVersionUID = -8961821901552709120L;
  // @j2jLiteral("http://xmlns.com/foaf/0.1/name")
  // private String name;
  // @j2jLiteral("http://xmlns.com/foaf/0.1/nickname")
  // private String nick;
  @j2jLiteral("http://xmlns.com/foaf/0.1/email")
  private String email;
  @j2jLiteral("http://xmlns.com/foaf/0.1/password")
  private String encryptedPassword;
  @j2jLiteral("http://xmlns.com/foaf/0.1/person")
  private Person person;
  @j2jList("http://imeji.org/terms/grant")
  private Collection<Grant> grants = new ArrayList<Grant>();
  @j2jLiteral("http://imeji.org/terms/quota")
  private long quota = -1;
  @j2jLiteral("http://imeji.org/terms/apiKey")
  private String apiKey;

  private URI id = IdentifierUtil.newURI(User.class);
  private List<UserGroup> groups = new ArrayList<>();

  // User properties for registration
  @j2jLiteral(ImejiNamespaces.DATE_CREATED)
  private Calendar created;

  // User properties for registration
  @j2jLiteral(ImejiNamespaces.LAST_MODIFICATION_DATE)
  private Calendar modified;;

  @j2jResource(ImejiNamespaces.USER_STATUS)
  private URI userStatus = URI.create(UserStatus.ACTIVE.getUriString());


  @j2jLiteral("http://imeji.org/terms/registrationToken")
  private String registrationToken;


  @j2jList("http://imeji.org/terms/observedCollections")
  private Collection<String> observedCollections = new ArrayList<String>();


  private static final Logger LOGGER = Logger.getLogger(User.class);

  /**
     * 
     */
  public User() {
    this.person = ImejiFactory.newPerson();
  }

  /**
   * Return a clone of this user, with a new email
   * 
   * @param email
   * @return
   */
  public User clone(String email) {
    User clone = new User();
    clone.setEmail(email);
    clone.encryptedPassword = encryptedPassword;
    clone.setQuota(quota);
    clone.grants = new ArrayList<Grant>();
    for (Grant g : grants) {
      if (g.asGrantType() != null && g.getGrantFor() != null) {
        if (g.getGrantFor().toString().equals(this.getId().toString()))
          clone.grants.add(new Grant(g.asGrantType(), clone.getId()));
        else
          clone.grants.add(new Grant(g.asGrantType(), g.getGrantFor()));
      }
    }
    // Updates group references
    for (UserGroup group : groups) {
      UserGroupController c = new UserGroupController();
      group.getUsers().remove(this.getId());
      group.getUsers().add(clone.getId());
      try {
        c.update(group, Imeji.adminUser);
      } catch (Exception e) {
        LOGGER.error("Could not update the user group i think", e);
      }
    }
    clone.person = person.clone();
    return clone;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public void setEncryptedPassword(String encryptedPassword) {
    this.encryptedPassword = encryptedPassword;
  }

  public String getEncryptedPassword() {
    return encryptedPassword;
  }

  public void setGrants(Collection<Grant> grants) {
    this.grants = grants;
  }

  public Collection<Grant> getGrants() {
    return grants;
  }

  public Collection<Grant> getGrantsWithoutUser() {
    Collection<Grant> grantsWithoutUser = new ArrayList<Grant>();
    for (Grant g : grants) {
      if (!g.getGrantFor().getPath().contains("user")) {
        grantsWithoutUser.add(new Grant(g.asGrantType(), g.getGrantFor()));
      }
    }
    return grantsWithoutUser;
  }

  public void setId(URI id) {
    this.id = id;
  }

  public URI getId() {
    return id;
  }

  /**
   * @return the groups
   */
  public List<UserGroup> getGroups() {
    return groups;
  }

  /**
   * @param groups the groups to set
   */
  public void setGroups(List<UserGroup> groups) {
    this.groups = groups;
  }

  /**
   * True if the current user is the system administrator
   * 
   * @return
   */
  public boolean isAdmin() {
    return AuthUtil.isSysAdmin(this);
  }

  public void setAdmin(boolean b) {
    // dummy method for jsf
  }

  public boolean isAllowedToCreateCollection() {
    return AuthUtil.isAllowedToCreateCollection(this);
  }

  /**
   * @return the person
   */
  public Person getPerson() {
    return person;
  }

  /**
   * @param person the person to set
   */
  public void setPerson(Person person) {
    this.person = person;
  }


  /**
   * @return
   */
  public Collection<String> getObservedCollections() {
    return observedCollections;
  }

  /**
   * @return
   */
  public String addObservedCollection(String id) {
    if (!this.observedCollections.contains(id)) {
      this.observedCollections.add(id);
    }
    return id;
  }

  /**
   * @return
   */
  public void removeObservedCollection(String id) {
    this.observedCollections.remove(id);
  }



  /**
   * @param observedCollections
   */
  public void setObservedCollections(Collection<String> observedCollections) {
    this.observedCollections = observedCollections;
  }

  /**
   *
   * @return
   */
  public long getQuota() {
    return quota;
  }

  public String getQuotaHumanReadable() {
    if (quota == Long.MAX_VALUE) {
      return ((SessionBean) BeanHelper.getSessionBean(SessionBean.class)).getLabel("unlimited");
    } else {
      return FileUtils.byteCountToDisplaySize(quota);
    }

  }

  /**
   *
   * @param quota
   */
  public void setQuota(long quota) {
    this.quota = quota;
  }

  @XmlEnum(String.class)
  public enum UserStatus {
    ACTIVE(new String(ImejiNamespaces.USER_STATUS + "#ACTIVE")), INACTIVE(
        new String(ImejiNamespaces.USER_STATUS + "#INACTIVE"));

    private String uri;

    private UserStatus(String uri) {
      this.uri = uri;
    }

    public String getUriString() {
      return uri;
    }

    public URI getURI() {
      return URI.create(uri);
    }
  }

  @XmlElement(name = "created", namespace = "http://purl.org/dc/terms/")
  public Calendar getCreated() {
    return created;
  }

  public void setCreated(Calendar created) {
    this.created = created;
  }

  public void setUserStatus(UserStatus status) {
    this.userStatus = URI.create(status.getUriString());
  }

  @XmlElement(name = "userStatus", namespace = "http://imeji.org/terms/")
  public UserStatus getUserStatus() {
    return UserStatus.valueOf(userStatus.getFragment());
  }


  @XmlElement(name = "registrationToken", namespace = "http://imeji.org/terms/")
  public String getRegistrationToken() {
    return registrationToken;
  }

  public void setRegistrationToken(String token) {
    this.registrationToken = token;
  }

  public boolean isActive() {
    return userStatus.equals(UserStatus.ACTIVE.getURI());
  }

  public Calendar getModified() {
    return modified;
  }

  public void setModified(Calendar modified) {
    this.modified = modified;
  }

  public String getApiKey() {
    return apiKey;
  }

  public void setApiKey(String apiKey) {
    this.apiKey = apiKey;
  }


}
