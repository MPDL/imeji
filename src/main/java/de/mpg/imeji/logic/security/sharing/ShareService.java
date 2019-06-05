package de.mpg.imeji.logic.security.sharing;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.exceptions.NotAllowedError;
import de.mpg.imeji.logic.config.Imeji;
import de.mpg.imeji.logic.events.MessageService;
import de.mpg.imeji.logic.events.messages.Message.MessageType;
import de.mpg.imeji.logic.events.messages.ShareMessage;
import de.mpg.imeji.logic.model.Grant;
import de.mpg.imeji.logic.model.Grant.GrantType;
import de.mpg.imeji.logic.model.User;
import de.mpg.imeji.logic.model.UserGroup;
import de.mpg.imeji.logic.security.authorization.AuthorizationPredefinedRoles;
import de.mpg.imeji.logic.security.authorization.util.SecurityUtil;
import de.mpg.imeji.logic.security.user.UserService;
import de.mpg.imeji.logic.security.usergroup.UserGroupService;

/**
 * Controller for {@link Grant}
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public class ShareService {
  private static final Logger LOGGER = LogManager.getLogger(ShareService.class);
  private final MessageService messageService = new MessageService();

  /**
   * The Roles which can be shared to every object
   *
   * @author saquet
   *
   */
  public enum ShareRoles {
    READ,
    EDIT,
    ADMIN,
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
    addGrantToUser(toUser, new Grant(GrantType.ADMIN, AuthorizationPredefinedRoles.IMEJI_GLOBAL_URI),
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
    addGrantToUser(toUser, new Grant(GrantType.EDIT, AuthorizationPredefinedRoles.IMEJI_GLOBAL_URI),
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
    addGrantToUser(toUser, new Grant(GrantType.READ, AuthorizationPredefinedRoles.IMEJI_GLOBAL_URI),
        AuthorizationPredefinedRoles.IMEJI_GLOBAL_URI);
  }

  /**
   * Share an object (Item, Collection, Album) to a {@link User}
   *
   * @param fromUser - The User sharing the object
   * @param toUser - The user the object is shared to
   * @param sharedObjectUri - The uri of the shared object
   * @param profileUri - (only for collections) the uri of the profile of the collection
   * @param roles - The roles given to the shared user, null if a grant is revoked
   * @throws ImejiException
   */
  public User shareToUser(User fromUser, User toUser, String sharedObjectUri, String role) throws ImejiException {
    if (toUser != null) {
      fromUser = new UserService().retrieve(fromUser.getEmail(), Imeji.adminUser);
      // unshare
      if (role == null) {
        toUser = removeGrantFromUser(fromUser, toUser, sharedObjectUri);
        messageService.add(new ShareMessage(MessageType.UNSHARE, sharedObjectUri, toUser));
      }
      // share
      else {
        final Grant grant = toGrant(role, sharedObjectUri);
        toUser = addGrantToUser(fromUser, toUser, sharedObjectUri, grant);
      }
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
  public void shareToGroup(User fromUser, UserGroup toGroup, String sharedObjectUri, String role) throws ImejiException {
    if (toGroup != null) {
      fromUser = new UserService().retrieve(fromUser.getEmail(), Imeji.adminUser);
      // unshare
      if (role == null) {
        this.removeGrantFromGroup(fromUser, toGroup, sharedObjectUri);
        messageService.add(new ShareMessage(MessageType.UNSHARE, sharedObjectUri, toGroup));
      }
      // share
      else {
        final Grant grant = toGrant(role, sharedObjectUri);
        shareGrantsToGroup(fromUser, toGroup, sharedObjectUri, grant);
      }
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
  private User addGrantToUser(User fromUser, User toUser, String sharedObjectUri, Grant grant) throws ImejiException {
    if (toUser != null) {
      checkSecurity(fromUser, sharedObjectUri);
      toUser = addGrantToUser(toUser, grant, sharedObjectUri);
    }
    return toUser;
  }


  private User removeGrantFromUser(User fromUser, User toUser, String sharedObjectUri) throws ImejiException {
    if (toUser != null) {
      checkSecurity(fromUser, sharedObjectUri);
      toUser = removeGrantFromUser(toUser, sharedObjectUri);
    }
    return toUser;
  }

  /**
   * Share an object (Item, Collection, Album) to a {@link UserGroup}
   *
   * @param issuingUser - The User sharing the object
   * @param addToThisGroup - The group the object is shared to
   * @param sharedObjectUri - The uri of the shared object
   * @param profileUri - (only for collections) the uri of the profile of the collection
   * @param grants - The grants given to the shared user
   * @throws ImejiException
   */
  private UserGroup shareGrantsToGroup(User issuingUser, UserGroup addToThisGroup, String sharedObjectUri, Grant grant)
      throws ImejiException {
    if (addToThisGroup != null) {
      checkSecurity(issuingUser, sharedObjectUri);
      UserGroupService userGroupService = new UserGroupService();
      UserGroup updatedUserGroup =
          userGroupService.addEditGrantToGroup(Imeji.adminUser, addToThisGroup, new Grant(grant.asGrantType(), sharedObjectUri));
      return updatedUserGroup;
    }
    return addToThisGroup;
  }

  private UserGroup removeGrantFromGroup(User issuingUser, UserGroup removeFromThisGroup, String unsharedObjectUri) throws ImejiException {
    if (removeFromThisGroup != null) {
      checkSecurity(issuingUser, unsharedObjectUri);
      UserGroupService userGroupService = new UserGroupService();
      UserGroup updatedUserGroup =
          userGroupService.removeGrantFromGroup(Imeji.adminUser, removeFromThisGroup, new Grant(null, unsharedObjectUri));
      return updatedUserGroup;
    }
    return removeFromThisGroup;
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
    final ShareRoles role = ShareService.transformGrantToRole(SecurityUtil.getGrantForObject(grants, id));
    return role != null ? role.name() : null;
  }

  /**
   * Add to the {@link User} the {@link List} of {@link Grant} and update the user in the database
   *
   * @param toUser
   * @param g
   * @throws ImejiException
   */
  private User addGrantToUser(User toUser, Grant grant, String grantedUri) throws ImejiException {
    UserService userService = new UserService();
    User userWithNewGrant = userService.addEditGrantToUser(Imeji.adminUser, toUser, new Grant(grant.asGrantType(), grantedUri));
    return userWithNewGrant;
  }


  private User removeGrantFromUser(User toUser, String grantedUri) throws ImejiException {
    UserService userService = new UserService();
    User userWithoutGrant = userService.removeGrantsFromUser(Imeji.adminUser, toUser, new Grant(null, grantedUri));
    return userWithoutGrant;
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
