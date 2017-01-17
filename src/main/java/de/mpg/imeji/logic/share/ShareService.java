/**
 * License: src/main/resources/license/escidoc.license
 */
package de.mpg.imeji.logic.share;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.exceptions.NotAllowedError;
import de.mpg.imeji.logic.Imeji;
import de.mpg.imeji.logic.authorization.AuthorizationPredefinedRoles;
import de.mpg.imeji.logic.authorization.util.SecurityUtil;
import de.mpg.imeji.logic.user.UserService;
import de.mpg.imeji.logic.usergroup.UserGroupService;
import de.mpg.imeji.logic.vo.Grant;
import de.mpg.imeji.logic.vo.Grant.GrantType;
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
    READ, EDIT, ADMIN;
  }


  /**
   * Makes user Sysadmin
   *
   * @param fromUser
   * @param toUser
   * @throws ImejiException
   */
  public void shareSysAdmin(User fromUser, User toUser) throws ImejiException {
    checkSecurity(fromUser, AuthorizationPredefinedRoles.IMEJI_GLOBAL_URI);
    addGrant(toUser, new Grant(GrantType.ADMIN, AuthorizationPredefinedRoles.IMEJI_GLOBAL_URI),
        AuthorizationPredefinedRoles.IMEJI_GLOBAL_URI);
  }

  /**
   * Revoke Sysadmin grant
   *
   * @param fromUser
   * @param toUser
   * @throws ImejiException
   */
  public void unshareSysAdmin(User fromUser, User toUser) throws ImejiException {
    shareCreateCollection(fromUser, toUser);
  }


  /**
   * Makes user Sysadmin
   *
   * @param fromUser
   * @param toUser
   * @throws ImejiException
   */
  public void shareCreateCollection(User fromUser, User toUser) throws ImejiException {
    checkSecurity(fromUser, AuthorizationPredefinedRoles.IMEJI_GLOBAL_URI);
    addGrant(toUser, new Grant(GrantType.EDIT, AuthorizationPredefinedRoles.IMEJI_GLOBAL_URI),
        AuthorizationPredefinedRoles.IMEJI_GLOBAL_URI);
  }

  /**
   * Revoke Sysadmin grant
   *
   * @param fromUser
   * @param toUser
   * @throws ImejiException
   */
  public void unshareCreateCollection(User fromUser, User toUser) throws ImejiException {
    checkSecurity(fromUser, AuthorizationPredefinedRoles.IMEJI_GLOBAL_URI);
    addGrant(toUser, new Grant(GrantType.READ, AuthorizationPredefinedRoles.IMEJI_GLOBAL_URI),
        AuthorizationPredefinedRoles.IMEJI_GLOBAL_URI);
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
  public User shareToUser(User fromUser, User toUser, String sharedObjectUri, String role)
      throws ImejiException {
    if (toUser != null) {
      final Grant grant = toGrant(role, sharedObjectUri);
      toUser = addGrantToUser(fromUser, toUser, sharedObjectUri, grant);
    }
    return toUser;
  }

  /**
   * Share an object (Item, Collection, Album) to a {@link UserGroup}
   *
   * @param fromUser - The User sharing the object
   * @param toGroup - The group the object is shared to
   * @param sharedObjectUri - The uri of the shared object
   * @param role - The roles given to the shared user
   * @throws ImejiException
   */
  public void shareToGroup(User fromUser, UserGroup toGroup, String sharedObjectUri, String role)
      throws ImejiException {
    if (toGroup != null) {
      final Grant grant = toGrant(role, sharedObjectUri);
      shareGrantsToGroup(fromUser, toGroup, sharedObjectUri, grant);
    }
  }

  /**
   * Share an object (Item, Collection, Album) to a {@link User}
   *
   * @param fromUser - The User sharing the object
   * @param toUser - The user the object is shared to
   * @param sharedObjectUri - The uri of the shared object
   * @param grant - The grants given to the shared user
   * @throws ImejiException
   */
  private User addGrantToUser(User fromUser, User toUser, String sharedObjectUri, Grant grant)
      throws ImejiException {
    if (toUser != null) {
      checkSecurity(fromUser, sharedObjectUri);
      toUser = addGrant(toUser, grant, sharedObjectUri);
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
      Grant grant) throws ImejiException {
    if (toGroup != null) {
      checkSecurity(fromUser, sharedObjectUri);
      addGrant(toGroup, grant, sharedObjectUri);
    }
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
  public static ShareRoles transformGrantToRole(Grant g) {
    if (g != null) {
      switch (GrantType.valueOf(g.getGrantType())) {
        case READ:
          return ShareRoles.READ;
        case EDIT:
          return ShareRoles.EDIT;
        case ADMIN:
          return ShareRoles.ADMIN;
      }
    }
    return null;
  }

  /**
   * Transform a Role to a {@link Grant}
   *
   * @param role
   * @param grantFor
   * @return
   */
  public static Grant toGrant(String role, String grantFor) {
    if (role != null) {
      switch (ShareRoles.valueOf(role)) {
        case READ:
          return new Grant(GrantType.READ, grantFor);
        case EDIT:
          return new Grant(GrantType.EDIT, grantFor);
        case ADMIN:
          return new Grant(GrantType.ADMIN, grantFor);
      }
    }
    return null;
  }

  /**
   * Find the Role for this id from the list
   *
   * @param grants
   * @param id
   * @return
   */
  public static String findRole(Collection<String> grants, String id) {
    final ShareRoles role =
        ShareService.transformGrantToRole(SecurityUtil.getGrantForObject(grants, id));
    return role != null ? role.name() : null;
  }

  /**
   * Add to the {@link User} the {@link List} of {@link Grant} and update the user in the database
   *
   * @param toUser
   * @param g
   * @throws ImejiException
   */
  private User addGrant(User toUser, Grant grant, String uri) throws ImejiException {
    toUser.setGrants(addGrant(toUser.getGrants(), grant, uri));
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
  private UserGroup addGrant(UserGroup toGroup, Grant grant, String uri) throws ImejiException {
    toGroup.setGrants(addGrant(toGroup.getGrants(), grant, uri));
    return new UserGroupService().update(toGroup, Imeji.adminUser);
  }

  /**
   * Add a grant to a list of grant if the grant is not null. All grants with the same grantFor with
   * be removed
   *
   * @param grants
   * @param grant
   * @param uri
   * @return
   */
  private List<String> addGrant(Collection<String> grants, Grant grant, String uri) {
    // Remove the grant for this id
    final List<String> l =
        grants.stream().filter(g -> !g.endsWith(uri)).collect(Collectors.toList());
    if (grant != null) {
      l.add(grant.toGrantString());
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
  private void checkSecurity(User user, String uri) throws NotAllowedError {
    final boolean allowed = SecurityUtil.authorization().administrate(user, uri);
    if (!allowed) {
      throw new NotAllowedError(user.getEmail() + " not allowed to share " + uri);
    }
  }
}
