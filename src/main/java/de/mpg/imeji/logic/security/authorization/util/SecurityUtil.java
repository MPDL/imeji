package de.mpg.imeji.logic.security.authorization.util;

import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Collectors;

import de.mpg.imeji.logic.model.CollectionImeji;
import de.mpg.imeji.logic.model.Grant;
import de.mpg.imeji.logic.model.User;
import de.mpg.imeji.logic.security.authorization.Authorization;

/**
 * Utility class for the package auth
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public class SecurityUtil {
  private static final Authorization authorization = new Authorization();

  /**
   * Return the {@link Authorization} as static
   *
   * @return
   */
  public static Authorization authorization() {
    return authorization;
  }

  /**
   * Return the {@link List} of uri of all {@link CollectionImeji}, the {@link User} is allowed to
   * see
   *
   * @param user
   * @return
   */
  public static List<String> getListOfAllowedCollections(User user) {
    return authorization().toGrantList(authorization().getAllGrants(user)).stream()
        .filter(g -> g.getGrantFor().contains("/collection/")).map(Grant::getGrantFor)
        .collect(Collectors.toList());
  }


  /**
   * Return the Grant for the object id. If no grant found, return null
   *
   * @param grants
   * @param id
   * @return
   */
  public static Grant getGrantForObject(Collection<String> grants, String id) {
    try {
      Optional<Grant> optional = grants.stream().map(s -> new Grant(s))
          .filter(s -> s.getGrantFor() != null && s.getGrantFor().equals(id)).findFirst();
      return optional.isPresent() ? optional.get() : null;
    } catch (final NoSuchElementException e) {
      return null;
    }
  }
}
