package de.mpg.imeji.logic.model;

import java.io.Serializable;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;

import de.mpg.imeji.j2j.annotations.j2jId;
import de.mpg.imeji.j2j.annotations.j2jList;
import de.mpg.imeji.j2j.annotations.j2jLiteral;
import de.mpg.imeji.j2j.annotations.j2jModel;
import de.mpg.imeji.j2j.annotations.j2jResource;
import de.mpg.imeji.logic.util.IdentifierUtil;

/**
 * A User group
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
@j2jResource("http://imeji.org/terms/userGroup")
@j2jModel("userGroup")
@j2jId(getMethod = "getId", setMethod = "setId")
public class UserGroup implements Serializable {
  private static final long serialVersionUID = 7770992777121385741L;
  private URI id = IdentifierUtil.newURI(UserGroup.class);
  @j2jLiteral("http://xmlns.com/foaf/0.1/name")
  private String name;
  @j2jList("http://imeji.org/terms/grant")
  private Collection<String> grants = new ArrayList<String>();
  @j2jList("http://xmlns.com/foaf/0.1/member")
  private Collection<URI> users = new ArrayList<URI>();

  /**
   * @return the name
   */
  public String getName() {
    return name;
  }

  /**
   * @param name the name to set
   */
  public void setName(String name) {
    this.name = name;
  }

  /**
   * @return the grants
   */
  public Collection<String> getGrants() {
    return grants;
  }

  /**
   * @param grants the grants to set
   */
  public void setGrants(Collection<String> grants) {
    this.grants = grants;
  }

  /**
   * @return the users
   */
  public Collection<URI> getUsers() {
    return users;
  }

  /**
   * @param users the users to set
   */
  public void setUsers(Collection<URI> users) {
    this.users = users;
  }

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
}
