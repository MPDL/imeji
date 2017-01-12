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
    CREATE, READ, EDIT, ADMIN;
  }

  @j2jResource("http://imeji.org/terms/grantType")
  private String grantType;
  @j2jResource("http://imeji.org/terms/grantFor")
  private String grantFor;
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
  public Grant(GrantType grantType, String grantFor) {
    id = IdentifierUtil.newURI(Grant.class);
    this.grantType = grantType.name();
    this.grantFor = grantFor;
  }

  /**
   * REturn the {@link Grant} as a {@link GrantType}
   *
   * @return
   */
  public GrantType asGrantType() {
    if (grantType != null) {
      return GrantType.valueOf(grantType);
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

  public void setId(URI id) {
    this.id = id;
  }

  public URI getId() {
    return id;
  }

  /**
   * @return the grantType
   */
  public String getGrantType() {
    return grantType;
  }

  /**
   * @param grantType the grantType to set
   */
  public void setGrantType(String grantType) {
    this.grantType = grantType;
  }

  /**
   * @return the grantFor
   */
  public String getGrantFor() {
    return grantFor;
  }

  /**
   * @param grantFor the grantFor to set
   */
  public void setGrantFor(String grantFor) {
    this.grantFor = grantFor;
  }

}
