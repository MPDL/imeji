/**
 * License: src/main/resources/license/escidoc.license
 */
package de.mpg.imeji.logic.vo;

import java.io.Serializable;
import java.net.URI;

import de.mpg.imeji.j2j.annotations.j2jId;
import de.mpg.imeji.j2j.annotations.j2jResource;
import de.mpg.imeji.logic.util.IdentifierUtil;

/**
 * Grant of one {@link GrantType} for one {@link User} used for imeji authorization
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
@j2jResource("http://imeji.org/terms/grant")
@j2jId(getMethod = "getId", setMethod = "setId")
public class Grant implements Serializable {
  private static final long serialVersionUID = -6318969286926194883L;

  /**
   * The types of possible {@link Grant} in imeji
   *
   * @author saquet (initial creation)
   * @author $Author$ (last modification)
   * @version $Revision$ $LastChangedDate$
   */
  public enum GrantType {
    CREATE, READ, UPDATE, DELETE, ADMIN, UPDATE_CONTENT, DELETE_CONTENT, ADMIN_CONTENT;
  }

  @j2jResource("http://imeji.org/terms/grantType")
  private URI grantType;
  @j2jResource("http://imeji.org/terms/grantFor")
  private URI grantFor;
  private URI id;

  /**
   * Constructor: no ids is created with this constructor
   */
  public Grant() {}

  /**
   * Create a {@link Grant} of type {@link GrantType} for the object with the {@link URI} grantfor.
   * Define the id
   *
   * @param gt
   * @param gf
   */
  public Grant(GrantType gt, URI gf) {
    id = IdentifierUtil.newURI(Grant.class);
    if (gt == null || gf == null) {
      throw new NullPointerException("Impossible to created a grant with a null value! Granttype: "
          + gt + " , and GrantFor: " + gf);
    }
    this.setGrantType(toGrantTypeURI(gt));
    this.grantFor = gf;
  }

  /**
   * REturn the {@link Grant} as a {@link GrantType}
   *
   * @return
   */
  public GrantType asGrantType() {
    if (grantType != null) {
      return GrantType.valueOf(grantType.getFragment());
    }
    return null;
  }

  /*
   * (non-Javadoc)
   *
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object obj) {
    if (obj instanceof Grant) {
      return grantFor.equals(((Grant) obj).getGrantFor())
          && grantType.equals(((Grant) obj).getGrantType());
    }
    return false;
  }

  @Override
  public int hashCode() {
    // TODO Auto-generated method stub
    return super.hashCode();
  }

  public void setGrantFor(URI grantFor) {
    this.grantFor = grantFor;
  }

  public URI getGrantFor() {
    return grantFor;
  }

  public void setId(URI id) {
    this.id = id;
  }

  public URI getId() {
    return id;
  }

  public void setGrantType(URI grantType) {
    this.grantType = grantType;
  }

  public URI getGrantType() {
    return grantType;
  }

  /**
   * Transform a {@link GrantType} into an {@link URI}
   *
   * @param type
   * @return
   */
  public static URI toGrantTypeURI(GrantType type) {
    return URI.create("http://imeji.org/terms/grantType#" + type.name());
  }

  public static String getGrantTypeName(GrantType gt) {
    switch (gt) {
      case CREATE:
        return "create";
      case DELETE:
        return "delete";
      case UPDATE:
        return "update";
      case READ:
        return "read";
      case ADMIN:
        return "admin";
      case UPDATE_CONTENT:
        return "update content";
      case DELETE_CONTENT:
        return "delete content";
      case ADMIN_CONTENT:
        return "administer content";
    }
    return "action";
  }


}
