/**
 * License: src/main/resources/license/escidoc.license
 */
package de.mpg.imeji.logic.share;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.exceptions.NotAllowedError;
import de.mpg.imeji.logic.Imeji;
import de.mpg.imeji.logic.authorization.AuthorizationPredefinedRoles;
import de.mpg.imeji.logic.authorization.util.SecurityUtil;
import de.mpg.imeji.logic.search.jenasearch.ImejiSPARQL;
import de.mpg.imeji.logic.search.jenasearch.JenaCustomQueries;
import de.mpg.imeji.logic.user.UserService;
import de.mpg.imeji.logic.usergroup.UserGroupService;
import de.mpg.imeji.logic.util.ObjectHelper;
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
public class ShareService {
  private static final Logger LOGGER = Logger.getLogger(ShareService.class);

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
      final List<Grant> grants = transformRolesToGrants(roles, sharedObjectUri, fromUser);
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
      final List<Grant> grants = transformRolesToGrants(roles, sharedObjectUri, fromUser);
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
      checkSecurity(fromUser, grants);
      toUser = removeGrants(toUser, sharedObjectUri);
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
      checkSecurity(fromUser, grants);
      toGroup = removeGrants(toGroup, sharedObjectUri);
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
  public static List<Grant> transformRolesToGrants(List<String> roles, String uri, User user) {
    final List<Grant> grants = new ArrayList<Grant>();
    if (roles == null) {
      return grants;
    }
    final String profileUri = getProfileUri(uri);
    final boolean isProfileAdmin = SecurityUtil.staticAuth().administrate(user, profileUri);
    if (profileUri != null && isProfileAdmin) {
      // Only for sharing collection
      grants.addAll(AuthorizationPredefinedRoles.read(profileUri));
    }
    for (final String g : roles) {
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
          if (profileUri != null && isProfileAdmin) {
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
    final List<String> l = new ArrayList<String>(roles.length);
    for (final ShareRoles r : roles) {
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
  public static List<String> transformGrantsToRoles(List<Grant> grants, String uri,
      String profileUri) {
    final List<String> l = new ArrayList<>();
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
    if (profileUri != null && hasEditGrants(grants, profileUri)) {
      l.add(ShareRoles.EDIT_PROFILE.toString());
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
    toUser.getGrants().addAll(grants);
    return new UserService().update(toUser, Imeji.adminUser);

  }

  /**
   * Add to the {@link UserGroup} the {@link List} of {@link Grant} and update the user in the
   * database
   *
   * @param fromUser
   * @param toGroup
   * @param grants
   *
   * @throws ImejiException
   */
  private void addGrants(User fromUser, UserGroup toGroup, List<Grant> grants)
      throws ImejiException {
    toGroup.getGrants().addAll(grants);
    new UserGroupService().update(toGroup, Imeji.adminUser);
  }

  private User removeGrants(User toUser, String uri) throws ImejiException {
    toUser.setGrants(removeGrantsOfObject((List<Grant>) toUser.getGrants(), uri));
    return new UserService().update(toUser, Imeji.adminUser);
  }

  private UserGroup removeGrants(UserGroup toGroup, String uri) throws ImejiException {
    toGroup.setGrants(removeGrantsOfObject((List<Grant>) toGroup.getGrants(), uri));
    return new UserGroupService().update(toGroup, Imeji.adminUser);
  }

  /**
   * Remove all Grant with grantfor = uri
   *
   * @param grants
   * @param uri
   * @return
   */
  private List<Grant> removeGrantsOfObject(List<Grant> grants, String uri) {
    final List<Grant> l = new ArrayList<>();
    for (final Grant g : grants) {
      if (!g.getGrantFor().toString().equals(uri)) {
        l.add(g);
      }
    }
    return l;
  }

  /**
   * True if the {@link User} is allowed to share the {@link Grant} to another {@link User}
   *
   * @param user
   * @param g
   * @return
   * @throws NotAllowedError
   */
  private void checkSecurity(User user, List<Grant> grants) throws NotAllowedError {
    boolean allowed = true;
    for (final Grant g : grants) {
      switch (ObjectHelper.getObjectType(g.getGrantFor())) {
        case ITEM:
          final List<String> c = ImejiSPARQL
              .exec(JenaCustomQueries.selectCollectionIdOfItem(g.getGrantFor().toString()), null);
          if (!c.isEmpty()) {
            allowed = SecurityUtil.staticAuth().administrate(user, c.get(0));
          }
          break;
        default:
          allowed = SecurityUtil.staticAuth().administrate(user, g.getGrantFor());
          break;
      }
      if (!allowed) {
        throw new NotAllowedError(user.getEmail() + " not allowed to share " + g.getGrantFor());
      }
    }
  }

  private static boolean hasReadGrants(List<Grant> userGrants, String uri) {
    final List<Grant> grants = AuthorizationPredefinedRoles.read(uri);
    return !grantNotExist(userGrants, grants);
  }

  private static boolean hasUploadGrants(List<Grant> userGrants, String uri) {
    final List<Grant> grants = AuthorizationPredefinedRoles.upload(uri);
    return !grantNotExist(userGrants, grants);
  }

  private static boolean hasEditItemGrants(List<Grant> userGrants, String uri) {
    final List<Grant> grants = AuthorizationPredefinedRoles.editContent(uri);
    return !grantNotExist(userGrants, grants);
  }

  private static boolean hasDeleteItemGrants(List<Grant> userGrants, String uri) {
    final List<Grant> grants = AuthorizationPredefinedRoles.delete(uri);
    return !grantNotExist(userGrants, grants);
  }

  private static boolean hasEditGrants(List<Grant> userGrants, String uri) {
    final List<Grant> grants = AuthorizationPredefinedRoles.edit(uri);
    return !grantNotExist(userGrants, grants);
  }

  private static boolean hasAdminGrants(List<Grant> userGrants, String uri) {
    final List<Grant> grants = AuthorizationPredefinedRoles.admin(uri);
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
    for (final Grant g : grantList) {
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
      final List<String> r =
          ImejiSPARQL.exec(JenaCustomQueries.selectProfileIdOfCollection(collectionUri), null);
      if (!r.isEmpty()) {
        return r.get(0);
      }
    }
    return null;
  }
}
