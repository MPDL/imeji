package de.mpg.imeji.logic.model;

import java.io.Serializable;
import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.function.BiFunction;

import de.mpg.imeji.j2j.annotations.j2jResource;
import de.mpg.imeji.logic.util.StringHelper;

/**
 * Grant of one {@link GrantType} for one {@link User} used for imeji authorization
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
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
    READ,
    EDIT,
    ADMIN;

    /**
     * True if the role is the same or bigger than the granttype
     * 
     * @param role
     * @return
     */
    public boolean isSameOrBigger(GrantType role) {
      switch (this) {
        case READ:
          return true;
        case EDIT:
          return role == EDIT || role == ADMIN;
        case ADMIN:
          return role == ADMIN;
        default:
          return false;
      }
    }

    /**
     * True if the role is the same or bigger than the granttype
     * 
     * @param role
     * @return
     */
    public boolean isSameOrBigger(String role) {
      return isSameOrBigger(valueOf(role));
    }
  }

  @j2jResource("http://imeji.org/terms/grantType")
  private String grantType;
  @j2jResource("http://imeji.org/terms/grantFor")
  private String grantFor;

  /**
   * Constructor: no ids is created with this constructor
   */
  public Grant() {

  }

  /**
   * Create a {@link Grant} of type {@link GrantType} for the object with the {@link URI} grantfor.
   * Define the id
   *
   * @param gt
   * @param gf
   */
  public Grant(GrantType grantType, String grantFor) {
    if (grantType != null) {
      this.grantType = grantType.name();
    }
    this.grantFor = grantFor;
  }

  /**
   * Create a Grant with a String with the format: GrantType,GrantFor
   *
   * @param grantString
   */
  public Grant(String grantString) {
    if (!StringHelper.isNullOrEmptyTrim(grantString)) {
      final String[] s = grantString.split(",");
      if (s.length == 2) {
        this.grantType = s[0];
        this.grantFor = s[1];
      }
    }
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

  /**
   * Return a {@link Grant} serialized as a String
   *
   * @return
   */
  public String toGrantString() {
    if (grantType != null) {
      return grantType + "," + grantFor;
    } else {
      return " " + "," + grantFor;
    }
  }

  /**
   * De-construct GrantString created by function above.
   * 
   * @param grantString
   * @return The grant
   */
  public static String extractGrantURIFromGrantString(String grantString) {
    if (grantString.contains(",")) {
      int indexOfComma = grantString.indexOf(",");
      String grantPart = grantString.substring(indexOfComma);
      if (grantPart.length() > 0) {
        return grantPart;
      }
    }
    return null;
  }


  public static int getGrantPositionInGrantsList(List<String> grantStringsList, String grantString) {

    for (String storedGrant : grantStringsList) {
      if (compareGrantStringURIs(storedGrant, grantString)) {
        int indexOfFoundGrantURI = grantStringsList.indexOf(storedGrant);
        return indexOfFoundGrantURI;
      }
    }
    return -1;
  }


  public static boolean compareGrantStringURIs(String grantString1, String grantString2) {

    String grantURI1 = extractGrantURIFromGrantString((String) grantString1);
    String grantURI2 = extractGrantURIFromGrantString((String) grantString2);
    if (grantURI1 != null && grantURI2 != null) {
      return grantURI1.equals(grantURI2);
    }

    return false;
  }


  /**
   * Get a function that compares two GrantStrings which were created by function above
   * (toGrantString()).
   * 
   * @return true if grants of GrantStrings are the same.
   */
  public static BiFunction<Object, Object, Boolean> getGrantStringCompareFunction() {

    return (Object s1, Object s2) -> {
      if (s1 instanceof String && s2 instanceof String) {
        String grantString1 = extractGrantURIFromGrantString((String) s1);
        String grantString2 = extractGrantURIFromGrantString((String) s2);
        if (grantString1 != null && grantString2 != null) {
          return grantString1.equals(grantString2);
        }
      }
      return false;
    };
  }



  /*
   * (non-Javadoc)
   *
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object obj) {
    if (obj instanceof Grant) {
      return grantFor.equals(((Grant) obj).getGrantFor()) && grantType.equals(((Grant) obj).getGrantType());
    }
    return false;
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
