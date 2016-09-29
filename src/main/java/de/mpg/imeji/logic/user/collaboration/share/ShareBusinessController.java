/**
 * License: src/main/resources/license/escidoc.license
 */
package de.mpg.imeji.logic.user.collaboration.share;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.log4j.Logger;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.Imeji;
import de.mpg.imeji.logic.search.jenasearch.ImejiSPARQL;
import de.mpg.imeji.logic.search.jenasearch.JenaCustomQueries;
import de.mpg.imeji.logic.security.authorization.AuthorizationPredefinedRoles;
import de.mpg.imeji.logic.security.util.SecurityUtil;
import de.mpg.imeji.logic.user.controller.GroupBusinessController;
import de.mpg.imeji.logic.user.controller.UserBusinessController;
import de.mpg.imeji.logic.vo.Grant;
import de.mpg.imeji.logic.vo.User;
import de.mpg.imeji.logic.vo.UserGroup;

/**
 * Controller for {@link Grant}
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public class ShareBusinessController {
  private static final Logger LOGGER = Logger.getLogger(ShareBusinessController.class);

  /**
   * The Roles which can be shared to every object
   *
   * @author saquet
   *
   */
  public enum ShareRoles {
    READ, CREATE, EDIT_ITEM, DELETE_ITEM, EDIT, ADMIN, EDIT_PROFILE;
  }

  /**
   * Share an object (Item, Collection, Album) to a {@link User}
   *
   * @param fromUser - The User sharing the object
   * @param toUser - The user the object is shared to
   * @param sharedObjectUri - The uri of the shared object
   * @param profileUri - (only for collections) the uri of the profile of the collection
   * @param roles - The roles given to the shared user
   * @throws ImejiException
   */
  public User shareToUser(User fromUser, User toUser, String sharedObjectUri, List<String> roles)
      throws ImejiException {
    if (toUser != null) {
      List<Grant> grants = transformRolesToGrants(roles, sharedObjectUri);
      toUser = shareGrantsToUser(fromUser, toUser, sharedObjectUri, grants);
    }
    return toUser;
  }

  /**
   * Share an object (Item, Collection, Album) to a {@link UserGroup}
   *
   * @param fromUser - The User sharing the object
   * @param toGroup - The group the object is shared to
   * @param sharedObjectUri - The uri of the shared object
   * @param profileUri - (only for collections) the uri of the profile of the collection
   * @param roles - The roles given to the shared user
   * @throws ImejiException
   */
  public void shareToGroup(User fromUser, UserGroup toGroup, String sharedObjectUri,
      List<String> roles) throws ImejiException {
    if (toGroup != null) {
      List<Grant> grants = transformRolesToGrants(roles, sharedObjectUri);
      shareGrantsToGroup(fromUser, toGroup, sharedObjectUri, grants);
    }
  }

  /**
   * Share an object (Item, Collection, Album) to a {@link User}
   *
   * @param fromUser - The User sharing the object
   * @param toUser - The user the object is shared to
   * @param sharedObjectUri - The uri of the shared object
   * @param profileUri - (only for collections) the uri of the profile of the collection
   * @param grants - The grants given to the shared user
   * @throws ImejiException
   */
  private User shareGrantsToUser(User fromUser, User toUser, String sharedObjectUri,
      List<Grant> grants) throws ImejiException {
    if (toUser != null) {
      cleanUserGrants(fromUser, toUser, grants, sharedObjectUri);
      addGrants(toUser, fromUser, grants);
    }
    return toUser;
  }

  /**
   * Share an object (Item, Collection, Album) to a {@link UserGroup}
   *
   * @param fromUser - The User sharing the object
   * @param toGroup - The group the object is shared to
   * @param sharedObjectUri - The uri of the shared object
   * @param profileUri - (only for collections) the uri of the profile of the collection
   * @param grants - The grants given to the shared user
   * @throws ImejiException
   */
  private void shareGrantsToGroup(User fromUser, UserGroup toGroup, String sharedObjectUri,
      List<Grant> grants) throws ImejiException {
    if (toGroup != null) {
      cleanGroupGrants(fromUser, toGroup, grants, sharedObjectUri);
      addGrants(Imeji.adminUser, toGroup, grants);
    }
  }

  /**
   * Transform a list of {@link ShareRoles} into a list of {@link Grant}
   *
   * @param roles
   * @param uri
   * @param profileUri
   * @return
   */
  public static List<Grant> transformRolesToGrants(List<String> roles, String uri) {
    List<Grant> grants = new ArrayList<Grant>();
    if (roles == null) {
      return grants;
    }
    String profileUri = getProfileUri(uri);
    if (profileUri != null) {
      // Only for sharing collection
      grants.addAll(AuthorizationPredefinedRoles.read(profileUri));
    }
    for (String g : roles) {
      switch (ShareRoles.valueOf(g)) {
        case READ:
          grants.addAll(AuthorizationPredefinedRoles.read(uri));
          break;
        case CREATE:
          grants.addAll(AuthorizationPredefinedRoles.upload(uri));
          break;
        case EDIT_ITEM:
          grants.addAll(AuthorizationPredefinedRoles.editContent(uri));
          break;
        case DELETE_ITEM:
          grants.addAll(AuthorizationPredefinedRoles.delete(uri));
          break;
        case EDIT:
          grants.addAll(AuthorizationPredefinedRoles.edit(uri));
          break;
        case ADMIN:
          grants.addAll(AuthorizationPredefinedRoles.admin(uri));
          break;
        case EDIT_PROFILE:
          if (profileUri != null) {
            grants.addAll(AuthorizationPredefinedRoles.edit(profileUri));
          }
      }
    }
    return grants;
  }

  /**
   * TRansform a list of Roles into a {@link List} of {@link String}
   *
   * @param roles
   * @return
   */
  public static List<String> rolesAsList(ShareRoles... roles) {
    List<String> l = new ArrayList<String>(roles.length);
    for (ShareRoles r : roles) {
      l.add(r.toString());
    }
    return l;
  }

  /**
   * Transform a list of {@link Grant} into a list of {@link ShareRoles}
   *
   * @param grants
   * @param uri
   * @param profileUri
   * @param type
   * @return
   */
  public static List<String> transformGrantsToRoles(List<Grant> grants, String uri) {
    List<String> l = new ArrayList<>();
    if (hasReadGrants(grants, uri)) {
      l.add(ShareRoles.READ.toString());
    }
    if (hasUploadGrants(grants, uri)) {
      l.add(ShareRoles.CREATE.toString());
    }
    if (hasEditItemGrants(grants, uri)) {
      l.add(ShareRoles.EDIT_ITEM.toString());
    }
    if (hasDeleteItemGrants(grants, uri)) {
      l.add(ShareRoles.DELETE_ITEM.toString());
    }
    if (hasEditGrants(grants, uri)) {
      l.add(ShareRoles.EDIT.toString());
    }
    if (hasAdminGrants(grants, uri)) {
      l.add(ShareRoles.ADMIN.toString());
    }
    return l;
  }

  /**
   * Add to the {@link User} the {@link List} of {@link Grant} and update the user in the database
   *
   * @param toUser
   * @param g
   * @throws ImejiException
   */
  private User addGrants(User toUser, User fromUser, List<Grant> grants) throws ImejiException {
    toUser.getGrants().addAll(filterNewGrants(toUser.getGrants(), grants, fromUser));
    UserBusinessController c = new UserBusinessController();
    // Admin is the only User which can update other users
    c.update(toUser, Imeji.adminUser);
    return toUser;

  }

  /**
   * Add to the {@link UserGroup} the {@link List} of {@link Grant} and update the user in the
   * database
   *
   * @param toUser
   * @param toGroup
   * @param grants
   *
   * @throws ImejiException
   */
  private void addGrants(User toUser, UserGroup toGroup, List<Grant> grants) throws ImejiException {
    toGroup.getGrants().addAll(filterNewGrants(toGroup.getGrants(), grants, toUser));
    GroupBusinessController c = new GroupBusinessController();
    // Admin is the only User which can update other users
    c.update(toGroup, Imeji.adminUser);
  }

  /**
   * Clean all {@link Grant} for an object
   *
   * @param hasgrant
   * @param uri
   * @param currentUser
   */
  private void cleanUserGrants(User fromUser, User toUser, List<Grant> grants, String uri) {
    List<Grant> notRemovedGrants = new ArrayList<Grant>();
    List<Grant> removedGrants = new ArrayList<Grant>();
    List<String> grantFor = getGrantFor(grants);
    for (Grant g : toUser.getGrants()) {
      if (g.getGrantFor() == null || grantFor.contains(g.getGrantFor().toString())
          || uri.equals(g.getGrantFor().toString())) {
        removedGrants.add(g);
      } else {
        notRemovedGrants.add(g);
      }
    }
    toUser.setGrants(notRemovedGrants);
    UserBusinessController c = new UserBusinessController();
    try {
      c.update(toUser, Imeji.adminUser);
    } catch (Exception e) {
      LOGGER.error(e);
    }
  }

  /**
   * Clean all {@link Grant} for an object
   *
   * @param hasgrant
   * @param uri
   * @param currentUser
   */
  private void cleanGroupGrants(User fromUser, UserGroup toGroup, List<Grant> grants, String uri) {
    List<Grant> notRemovedGrants = new ArrayList<Grant>();
    List<Grant> removedGrants = new ArrayList<Grant>();
    List<String> grantFor = getGrantFor(grants);
    for (Grant g : toGroup.getGrants()) {
      if (grantFor.contains(g.getGrantFor().toString()) || uri.equals(g.getGrantFor().toString())) {
        removedGrants.add(g);
      } else {
        notRemovedGrants.add(g);
      }
    }
    toGroup.setGrants(notRemovedGrants);
    GroupBusinessController c = new GroupBusinessController();
    try {
      c.update(toGroup, Imeji.adminUser);
      // new UserController().deleteGrants(removedGrants);
    } catch (Exception e) {
      LOGGER.error("Error cleaning grants", e);
    }
  }

  /**
   * Get all Objects uri which are in this list of {@link Grant}
   *
   * @param grants
   * @return
   */
  private List<String> getGrantFor(List<Grant> grants) {
    List<String> l = new ArrayList<String>();
    for (Grant g : grants) {
      if (!l.contains(g.getGrantFor().toString())) {
        l.add(g.getGrantFor().toString());
      }
    }
    return l;
  }


  /**
   * Return the {@link Grant} which are new for the {@link User}
   *
   * @param current
   * @param toAdd
   * @return
   */
  private List<Grant> filterNewGrants(Collection<Grant> current, List<Grant> toAdd, User user) {
    List<Grant> newGrants = new ArrayList<>();
    for (Grant g : toAdd) {
      if (!current.contains(g) && !newGrants.contains(g) && isAllowedToAddGrant(user, g)) {
        newGrants.add(g);
      } else if (!current.contains(g) && !newGrants.contains(g) && !isAllowedToAddGrant(user, g)) {
        LOGGER
            .error(user.getPerson().getCompleteName() + " NOT ALLOWED TO share " + g.getGrantFor());
      }
    }
    return newGrants;
  }

  /**
   * True if the {@link User} is allowed to share the {@link Grant} to another {@link User}
   *
   * @param user
   * @param g
   * @return
   */
  private boolean isAllowedToAddGrant(User user, Grant g) {
    if (g.getGrantFor().toString().contains("/item/")) {
      // If the grantFor is an Item, the collection is needed to know if
      // the user can administrate it
      List<String> c = ImejiSPARQL
          .exec(JenaCustomQueries.selectCollectionIdOfItem(g.getGrantFor().toString()), null);
      if (!c.isEmpty()) {
        return SecurityUtil.staticAuth().administrate(user, c.get(0));
      }

    }
    return SecurityUtil.staticAuth().administrate(user, g.getGrantFor());
  }

  private static boolean hasReadGrants(List<Grant> userGrants, String uri) {
    List<Grant> grants = AuthorizationPredefinedRoles.read(uri);
    return !grantNotExist(userGrants, grants);
  }

  private static boolean hasUploadGrants(List<Grant> userGrants, String uri) {
    List<Grant> grants = AuthorizationPredefinedRoles.upload(uri);
    return !grantNotExist(userGrants, grants);
  }

  private static boolean hasEditItemGrants(List<Grant> userGrants, String uri) {
    List<Grant> grants = AuthorizationPredefinedRoles.editContent(uri);
    return !grantNotExist(userGrants, grants);
  }

  private static boolean hasDeleteItemGrants(List<Grant> userGrants, String uri) {
    List<Grant> grants = AuthorizationPredefinedRoles.delete(uri);
    return !grantNotExist(userGrants, grants);
  }

  private static boolean hasEditGrants(List<Grant> userGrants, String uri) {
    List<Grant> grants = AuthorizationPredefinedRoles.edit(uri);
    return !grantNotExist(userGrants, grants);
  }

  private static boolean hasAdminGrants(List<Grant> userGrants, String uri) {
    List<Grant> grants = AuthorizationPredefinedRoles.admin(uri);
    return !grantNotExist(userGrants, grants);
  }

  /**
   * True if ???
   *
   * @param userGrants
   * @param grantList
   * @return
   */
  private static boolean grantNotExist(List<Grant> userGrants, List<Grant> grantList) {
    boolean b = false;
    for (Grant g : grantList) {
      if (!userGrants.contains(g)) {
        b = true;
      }
    }
    return b;
  }

  /**
   * Find the profile of a collection
   *
   * @param collectionUri
   * @return
   */
  private static String getProfileUri(String collectionUri) {
    if (collectionUri.contains("/collection/")) {
      List<String> r =
          ImejiSPARQL.exec(JenaCustomQueries.selectProfileIdOfCollection(collectionUri), null);
      if (!r.isEmpty()) {
        return r.get(0);
      }
    }
    return null;
  }
}
