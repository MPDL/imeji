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
package de.mpg.imeji.logic.security.authorization;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import de.mpg.imeji.logic.Imeji;
import de.mpg.imeji.logic.vo.Grant;
import de.mpg.imeji.logic.vo.User;
import de.mpg.imeji.logic.vo.Grant.GrantType;

/**
 * Defines the predefined roles (for instance the creator of collection) with a {@link List} of
 * {@link Grant}
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public class AuthorizationPredefinedRoles {
  /**
   * Roles for the share Page
   */
  // Can read a container (default role)
  private static final GrantType[] read = {GrantType.READ};
  // Can upload items in a collection or add/remove item in an album
  private static final GrantType[] upload = {GrantType.READ, GrantType.CREATE};
  // Can edit the item metadata
  private static final GrantType[] edit_items = {GrantType.READ, GrantType.UPDATE_CONTENT};
  // Can delete the items
  private static final GrantType[] delete_items = {GrantType.READ, GrantType.DELETE_CONTENT};
  // Can edit container metadata
  private static final GrantType[] edit_container = {GrantType.READ, GrantType.UPDATE};
  // Can edit a profile
  private static final GrantType[] edit_profile = {GrantType.READ, GrantType.UPDATE};
  // Can administrate an album
  private static final GrantType[] admin_album =
      {GrantType.READ, GrantType.ADMIN, GrantType.CREATE, GrantType.DELETE, GrantType.UPDATE};
  // Can administrate a collection
  private static final GrantType[] admin_collection =
      {GrantType.READ, GrantType.ADMIN, GrantType.ADMIN_CONTENT, GrantType.CREATE, GrantType.DELETE,
          GrantType.DELETE_CONTENT, GrantType.UPDATE, GrantType.UPDATE_CONTENT};
  public static final String IMEJI_GLOBAL_URI = Imeji.PROPERTIES.getBaseURI();

  /**
   * The default {@link User} role in imeji can create (collection/album) in imeji
   *
   * @param uri
   * @param allowedToCreateCollection
   * @return
   */
  public static List<Grant> defaultUser(String uri) {
    GrantType[] g = {GrantType.CREATE};
    List<Grant> l = toGrantList(g, IMEJI_GLOBAL_URI);
    l.addAll(restrictedUser(uri));
    return l;
  }

  /**
   * This user can not create a collection in imeji. He only has the {@link Grant} on his account
   *
   * @param uri
   * @return
   */
  public static List<Grant> restrictedUser(String uri) {
    GrantType[] g =
        {GrantType.CREATE, GrantType.READ, GrantType.UPDATE, GrantType.DELETE, GrantType.ADMIN};
    return toGrantList(g, uri);
  }

  /**
   * Return the {@link Grant} of a {@link User} who is an imeji system administrator
   *
   * @return
   */
  public static List<Grant> imejiAdministrator(String uri) {
    GrantType[] g = {GrantType.ADMIN};
    List<Grant> l = toGrantList(g, IMEJI_GLOBAL_URI);
    l.addAll(defaultUser(uri));
    return l;
  }

  public static List<Grant> read(String uri) {
    List<Grant> l = toGrantList(read, uri);
    return l;
  }

  public static List<Grant> upload(String uri) {
    List<Grant> l = toGrantList(upload, uri);
    return l;
  }

  public static List<Grant> editContent(String uri) {
    List<Grant> l = toGrantList(edit_items, uri);
    return l;
  }

  public static List<Grant> delete(String uri) {
    List<Grant> l = toGrantList(delete_items, uri);
    return l;
  }

  public static List<Grant> edit(String uri) {
    List<Grant> l = toGrantList(edit_container, uri);
    return l;
  }


  public static List<Grant> admin(String uri) {
    List<Grant> l = new ArrayList<Grant>();
    if (uri != null) {
      // add grant for the container (collection or album)
      GrantType[] g = uri.contains("/collection/") ? admin_collection : admin_album;
      l = toGrantList(g, uri);
    }
    return l;
  }

  /**
   * Transform an array of {@link GrantType} to a {@link List} of {@link Grant} for the given uri
   *
   * @param array
   * @param uri
   * @return
   */
  private static List<Grant> toGrantList(GrantType[] array, String uri) {
    List<Grant> l = new ArrayList<Grant>();
    for (GrantType gt : array) {
      l.add(new Grant(gt, URI.create(uri)));
    }
    return l;
  }
}
