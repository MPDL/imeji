package de.mpg.imeji.logic.authorization;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import de.mpg.imeji.logic.config.Imeji;
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
  public static List<String> defaultUser(String uri) {
    return new ArrayList<String>(
        Arrays.asList(new Grant(GrantType.EDIT, IMEJI_GLOBAL_URI).toGrantString(),
            new Grant(GrantType.ADMIN, uri).toGrantString()));
  }

  /**
   * This user can not create a collection in imeji. He only has the {@link Grant} on his account
   *
   * @param uri
   * @return
   */
  public static List<String> restrictedUser(String uri) {
    return new ArrayList<String>(
        Arrays.asList(new Grant(GrantType.READ, IMEJI_GLOBAL_URI).toGrantString(),
            new Grant(GrantType.ADMIN, uri).toGrantString()));
  }

  /**
   * Return the {@link Grant} of a {@link User} who is an imeji system administrator
   *
   * @return
   */
  public static List<String> imejiAdministrator(String uri) {
    return new ArrayList<String>(Arrays.asList(new Grant(GrantType.ADMIN, uri).toGrantString(),
        new Grant(GrantType.ADMIN, IMEJI_GLOBAL_URI).toGrantString()));
  }
}
