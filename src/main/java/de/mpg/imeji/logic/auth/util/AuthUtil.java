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
package de.mpg.imeji.logic.auth.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import de.mpg.imeji.logic.Imeji;
import de.mpg.imeji.logic.auth.authorization.Authorization;
import de.mpg.imeji.logic.vo.Album;
import de.mpg.imeji.logic.vo.CollectionImeji;
import de.mpg.imeji.logic.vo.Grant;
import de.mpg.imeji.logic.vo.Grant.GrantType;
import de.mpg.imeji.logic.vo.Item;
import de.mpg.imeji.logic.vo.User;
import de.mpg.imeji.logic.vo.UserGroup;

/**
 * Utility class for the package auth
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public class AuthUtil {
  private static final Authorization authorization = new Authorization();

  /**
   * Return the {@link Authorization} as static
   *
   * @return
   */
  public static Authorization staticAuth() {
    return authorization;
  }


  /**
   * True if the user is Administrator of Imeji
   *
   * @param user
   * @return
   */
  public static boolean isSysAdmin(User user) {
    return authorization.administrate(user, Imeji.PROPERTIES.getBaseURI());
  }

  /**
   * True if a {@link User} can create an collection
   *
   * @param user
   * @return
   */
  public static boolean isAllowedToCreateCollection(User user) {
    return authorization.create(user, Imeji.PROPERTIES.getBaseURI());
  }

  /**
   * True if the {@link User} has read Grant for a single {@link Item} but not to its
   * {@link CollectionImeji}
   *
   * @param user
   * @param item
   * @return
   */
  public static boolean canReadItemButNotCollection(User user, Item item) {
    return staticAuth().read(user, item)
        && !staticAuth().read(user, item.getCollection().toString());
  }

  /**
   * Return the {@link List} of uri of all {@link CollectionImeji}, the {@link User} is allowed to
   * see
   *
   * @param user
   * @return
   */
  public static List<String> getListOfAllowedCollections(User user) {
    List<String> uris = new ArrayList<>();
    for (Grant g : getAllGrantsOfUser(user)) {
      if (g.getGrantFor().toString().contains("/collection/")
          && g.getGrantType().equals(Grant.toGrantTypeURI(GrantType.READ))) {
        uris.add(g.getGrantFor().toString());
      }

    }
    return uris;
  }

  /**
   * Return the {@link List} of uri of all {@link de.mpg.imeji.logic.vo.MetadataProfile}, the
   * {@link User} is allowed to see
   *
   * @param user
   * @return
   */
  public static List<String> getListOfAllowedProfiles(User user) {
    List<String> uris = new ArrayList<>();
    for (Grant g : getAllGrantsOfUser(user)) {
      if (g.getGrantFor().toString().contains("/metadataProfile/")
          && g.getGrantType().equals(Grant.toGrantTypeURI(GrantType.READ))) {
        uris.add(g.getGrantFor().toString());
      }

    }
    return uris;
  }

  /**
   * Return the {@link List} of uri of all {@link Item}, , the {@link User} has got an extra read
   * {@link Grant} for.
   *
   * @param user
   * @return
   */
  public static List<String> getListOfAllowedItem(User user) {
    List<String> uris = new ArrayList<>();
    for (Grant g : getAllGrantsOfUser(user)) {
      if (g.getGrantFor().toString().contains("/item/")
          && g.getGrantType().equals(Grant.toGrantTypeURI(GrantType.READ))) {
        uris.add(g.getGrantFor().toString());
      }
    }
    return uris;
  }

  /**
   * Return the {@link List} of uri of all {@link Album}, the {@link User} is allowed to see
   *
   * @param user
   * @return
   */
  public static List<String> getListOfAllowedAlbums(User user) {
    List<String> uris = new ArrayList<>();
    for (Grant g : getAllGrantsOfUser(user)) {
      if (g.getGrantFor().toString().contains("/album/")
          && g.getGrantType().equals(Grant.toGrantTypeURI(GrantType.READ))) {
        uris.add(g.getGrantFor().toString());
      }
    }
    return uris;
  }

  /**
   * Return all {@link Grant} of {@link User} including those from the {@link UserGroup} he is
   * member of.
   *
   * @param u
   * @return
   */
  public static List<Grant> getAllGrantsOfUser(User user) {
    if (user != null) {
      List<Grant> l = new ArrayList<>(filterUnvalidGrants(user.getGrants()));
      for (UserGroup ug : user.getGroups()) {
        l.addAll(filterUnvalidGrants(ug.getGrants()));
      }
      return l;
    }
    return new ArrayList<>();
  }

  /**
   * Return all {@link Grant} which have the passed grant for
   *
   * @param hasgrant
   * @param grantForUri
   * @return
   */
  public static List<Grant> extractGrantsFor(List<Grant> grants, String grantForUri) {
    List<Grant> l = new ArrayList<Grant>();
    for (Grant g : filterUnvalidGrants(grants)) {
      if (g.getGrantFor().toString().equals(grantForUri)) {
        l.add(g);
      }
    }
    return l;
  }

  /**
   * {@link ReturnaddressType} the Grant which is exactly define by the pass parameter. If not found
   * return null;
   *
   * @param grants
   * @param grantForUri
   * @param profileUri
   * @param type
   * @return
   */
  public static Grant extractGrant(List<Grant> grants, String grantForUri, GrantType type) {
    for (Grant g : AuthUtil.extractGrantsFor(grants, Imeji.PROPERTIES.getBaseURI())) {
      if (g.getGrantType().compareTo(Grant.toGrantTypeURI(type)) == 0) {
        return g;
      }
    }
    return null;
  }

  /**
   * Remove the grants which are not valid to avoid error in further methods
   *
   * @param hasgrant
   * @return
   */
  private static Collection<Grant> filterUnvalidGrants(Collection<Grant> l) {
    Collection<Grant> nl = new ArrayList<Grant>();
    for (Grant g : l) {
      if (g.getGrantFor() != null && g.getGrantType() != null) {
        nl.add(g);
      }
    }
    return nl;
  }
}
