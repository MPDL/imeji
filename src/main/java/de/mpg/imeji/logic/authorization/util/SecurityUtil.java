/*
 *
 * CDDL HEADER START
 *
 * The contents of this file are subject to the terms of the Common Development and Distribution
 * License, Version 1.0 only (the "License"). You may not use this file except in compliance with
 * the License.
 *
 * You can obtain a copy of the license at license/ESCIDOC.LICENSE or http://www.escidoc.de/license.
 * See the License for the specific language governing permissions and limitations under the
 * License.
 *
 * When distributing Covered Code, include this CDDL HEADER in each file and include the License
 * file at license/ESCIDOC.LICENSE. If applicable, add the following below this CDDL HEADER, with
 * the fields enclosed by brackets "[]" replaced with your own identifying information: Portions
 * Copyright [yyyy] [name of copyright owner]
 *
 * CDDL HEADER END
 */
/*
 * Copyright 2006-2007 Fachinformationszentrum Karlsruhe Gesellschaft für
 * wissenschaftlich-technische Information mbH and Max-Planck- Gesellschaft zur Förderung der
 * Wissenschaft e.V. All rights reserved. Use is subject to license terms.
 */
package de.mpg.imeji.logic.authorization.util;

import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Collectors;

import de.mpg.imeji.logic.authorization.Authorization;
import de.mpg.imeji.logic.vo.Album;
import de.mpg.imeji.logic.vo.CollectionImeji;
import de.mpg.imeji.logic.vo.Grant;
import de.mpg.imeji.logic.vo.User;

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
   * Return the {@link List} of uri of all {@link Album}, the {@link User} is allowed to see
   *
   * @param user
   * @return
   */
  public static List<String> getListOfAllowedAlbums(User user) {
    return authorization().toGrantList(authorization().getAllGrants(user)).stream()
        .filter(g -> g.getGrantFor().contains("/album/")).map(Grant::getGrantFor)
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
