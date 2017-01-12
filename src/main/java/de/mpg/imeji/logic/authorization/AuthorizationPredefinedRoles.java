package de.mpg.imeji.logic.authorization;

import java.util.Arrays;
import java.util.List;

import de.mpg.imeji.logic.Imeji;
import de.mpg.imeji.logic.vo.Grant;
import de.mpg.imeji.logic.vo.Grant.GrantType;
import de.mpg.imeji.logic.vo.User;

/**
 * Defines the predefined roles (for instance the creator of collection) with a {@link List} of
 * {@link Grant}
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public class AuthorizationPredefinedRoles {
  public static final String IMEJI_GLOBAL_URI = Imeji.PROPERTIES.getBaseURI();

  /**
   * The default {@link User} role in imeji can create (collection/album) in imeji
   *
   * @param uri
   * @param allowedToCreateCollection
   * @return
   */
  public static List<Grant> defaultUser(String uri) {
    return Arrays.asList(new Grant(GrantType.CREATE, IMEJI_GLOBAL_URI),
        new Grant(GrantType.ADMIN, uri));
  }

  /**
   * This user can not create a collection in imeji. He only has the {@link Grant} on his account
   *
   * @param uri
   * @return
   */
  public static List<Grant> restrictedUser(String uri) {
    return Arrays.asList(new Grant(GrantType.ADMIN, uri));
  }

  /**
   * Return the {@link Grant} of a {@link User} who is an imeji system administrator
   *
   * @return
   */
  public static List<Grant> imejiAdministrator(String uri) {
    return Arrays.asList(new Grant(GrantType.CREATE, IMEJI_GLOBAL_URI),
        new Grant(GrantType.ADMIN, uri), new Grant(GrantType.ADMIN, IMEJI_GLOBAL_URI));
  }
}
