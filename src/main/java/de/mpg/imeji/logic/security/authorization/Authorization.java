package de.mpg.imeji.logic.security.authorization;

import java.io.Serializable;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;

import de.mpg.imeji.exceptions.NotAllowedError;
import de.mpg.imeji.logic.config.Imeji;
import de.mpg.imeji.logic.hierarchy.HierarchyService;
import de.mpg.imeji.logic.model.CollectionImeji;
import de.mpg.imeji.logic.model.Grant;
import de.mpg.imeji.logic.model.Grant.GrantType;
import de.mpg.imeji.logic.model.Item;
import de.mpg.imeji.logic.model.Organization;
import de.mpg.imeji.logic.model.Person;
import de.mpg.imeji.logic.model.Properties.Status;
import de.mpg.imeji.logic.model.Statement;
import de.mpg.imeji.logic.model.User;
import de.mpg.imeji.logic.model.UserGroup;

/**
 * Authorization rules for imeji objects (defined by their uri) for one {@link User}
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public class Authorization implements Serializable {
  private static final long serialVersionUID = -4745899890554497793L;
  private static final Logger LOGGER = Logger.getLogger(Authorization.class);
  private HierarchyService hierarchyService = new HierarchyService();

  /**
   * True if the {@link User} has the grant to create a collection in imeji
   *
   * @param user
   * @return
   */
  public boolean hasCreateCollectionGrant(User user) {
    return isSysAdmin(user) || (user != null && hasGrant(user.getGrants(),
        new Grant(GrantType.EDIT, AuthorizationPredefinedRoles.IMEJI_GLOBAL_URI)));
  }

  /**
   * True if the {@link User} is a system administrator
   *
   * @param user
   * @return
   */
  public boolean isSysAdmin(User user) {
    return user != null && hasGrant(user.getGrants(),
        new Grant(GrantType.ADMIN, AuthorizationPredefinedRoles.IMEJI_GLOBAL_URI));
  }

  /**
   * True if the user can create the object
   *
   * @param user
   * @param obj
   * @return
   */
  public boolean create(User user, Object obj) {
    if (obj instanceof Item) {
      return update(user, ((Item) obj).getCollection());
    }
    if (obj instanceof CollectionImeji) {
      return hasCreateCollectionGrant(user);
    }
    return isSysAdmin(user);
  }

  /**
   * Return true if the {@link User} can read the object
   *
   * @param user
   * @param url
   * @return
   * @throws NotAllowedError
   */
  public boolean read(User user, Object obj) {
    return isPublic(obj, user) || isSysAdmin(user) || hasReadGrant(getAllGrants(user), getId(obj));
  }


  /**
   * Return true if the {@link User} can update the object
   *
   * @param user
   * @param url
   * @return
   * @throws NotAllowedError
   */
  public boolean update(User user, Object obj) {
    return isSysAdmin(user) || hasEditGrant(getAllGrants(user), getId(obj));
  }

  /**
   * Return true if the {@link User} can delete the object
   *
   * @param user
   * @param url
   * @return
   * @throws NotAllowedError
   */
  public boolean delete(User user, Object obj) {
    if (obj instanceof Item) {
      return update(user, obj);
    }
    return administrate(user, obj);
  }

  /**
   * Return true if the {@link User} can administrate the object
   *
   * @param user
   * @param url
   * @return
   * @throws NotAllowedError
   */
  public boolean administrate(User user, Object obj) {
    return !isSubcollection(obj)
        && (isSysAdmin(user) || hasAdminGrant(getAllGrants(user), getId(obj)));
  }

  /**
   * Transform a list of String (with the format: GrantType,GrantFor) to a list of {@link Grant}
   *
   * @param grants
   * @return
   */
  public Collection<Grant> toGrantList(Collection<String> grants) {
    return grants.stream().map(s -> new Grant(s)).collect(Collectors.toList());
  }

  /**
   * True if the object is shared with the user. Even if the object is public, this does not have to
   * be true.
   * 
   * @param user
   * @param obj
   * @return
   */
  public boolean isShared(User user, Object obj) {
    return hasReadGrant(getAllGrants(user), getId(obj))
        || hasEditGrant(getAllGrants(user), getId(obj))
        || hasAdminGrant(getAllGrants(user), getId(obj));
  }

  /**
   * True if the {@link Grant} is found in the collection
   *
   * @param grants
   * @param grant
   * @return
   */
  private boolean hasGrant(Collection<String> grants, Grant grant) {
    return grants != null && grants.contains(grant.toGrantString());
  }


  /**
   * True if the User has either Read, Edit of Admin Grant for this uri
   *
   * @param grants
   * @param uri
   * @return
   */
  private boolean hasReadGrant(Collection<String> grants, String uri) {
    return toGrantList(grants).stream().anyMatch(g -> g.getGrantFor().equals(uri));
  }

  /**
   * True if the User has either Edit of Admin Grant for this uri
   *
   * @param grants
   * @param uri
   * @return
   */
  private boolean hasEditGrant(Collection<String> grants, String uri) {
    return toGrantList(grants).stream().anyMatch(
        g -> g.getGrantFor().equals(uri) && (g.getGrantType().equals(GrantType.EDIT.toString())
            || g.getGrantType().equals(GrantType.ADMIN.toString())));
  }

  /**
   * True if the User has Admin Grant for this uri
   *
   * @param grants
   * @param uri
   * @return
   */
  private boolean hasAdminGrant(Collection<String> grants, String uri) {
    return hasGrant(grants, new Grant(GrantType.ADMIN, uri));
  }

  /**
   * Return all {@link Grant} of {@link User} including those from the {@link UserGroup} he is
   * member of.
   *
   * @param uu
   * @return
   */
  public List<String> getAllGrants(User user) {
    if (user == null) {
      return new ArrayList<>();
    }
    final List<String> grants = new ArrayList<>(user.getGrants());
    grants.addAll(getAllUserGroupGrants(user));
    return grants;
  }

  /**
   * Return all Grants the user have via its Usergroups
   *
   * @param user
   * @return
   */
  public List<String> getAllUserGroupGrants(User user) {
    final List<String> grants = new ArrayList<>();
    for (final UserGroup group : user.getGroups()) {
      grants.addAll(group.getGrants());
    }
    return grants;
  }


  /**
   * Return the uri which is relevant for the {@link Authorization}
   *
   * @param obj
   * @param hasItemGrant
   * @param getContext
   * @param isReadGrant
   * @return
   */
  private String getId(Object obj) {
    try {
      if (obj == null) {
        return AuthorizationPredefinedRoles.IMEJI_GLOBAL_URI;
      }
      if (obj instanceof Item) {
        return getLastParent(obj);
      }
      if (obj instanceof CollectionImeji) {
        return getLastParent(obj);
      }
      if (obj instanceof User) {
        return ((User) obj).getId().toString();
      }
      if (obj instanceof URI) {
        return getLastParent(obj.toString());
      }
      if (obj instanceof String) {
        return getLastParent(obj.toString());
      }
      if (obj instanceof UserGroup) {
        return AuthorizationPredefinedRoles.IMEJI_GLOBAL_URI;
      }
      if (obj instanceof Statement) {
        return AuthorizationPredefinedRoles.IMEJI_GLOBAL_URI;
      }
      LOGGER.fatal("Invalid Object type: " + obj.getClass());
      return AuthorizationPredefinedRoles.IMEJI_GLOBAL_URI;
    } catch (final Exception e) {
      LOGGER.error("Error get security URI", e);
      return AuthorizationPredefinedRoles.IMEJI_GLOBAL_URI;
    }
  }

  /**
   * Return the last parent. Works only for items and collections
   * 
   * @param obj
   * @return
   */
  private String getLastParent(Object obj) {
    if (obj instanceof Item) {
      return hierarchyService.getLastParent(((Item) obj).getCollection().toString());
    }
    if (obj instanceof CollectionImeji) {
      return hierarchyService.getLastParent(((CollectionImeji) obj).getId().toString());
    }
    return null;
  }

  private String getLastParent(String uri) {
    return hierarchyService.getLastParent(uri);
  }

  /**
   * True if the {@link Object} is public (i.e. has been released)
   *
   * @param obj
   * @return
   */
  private boolean isPublic(Object obj, User user) {
    if (obj instanceof Organization) {
      return true;
    } else if (Imeji.CONFIG.getPrivateModus() && user == null) {
      return false;
    } else if (obj instanceof Item) {
      return isPublicStatus(((Item) obj).getStatus());
    } else if (obj instanceof CollectionImeji) {
      return isPublicStatus(((CollectionImeji) obj).getStatus());
    } else if (obj instanceof Person) {
      return true;
    }
    return false;
  }

  /**
   * True if the object is a subcollection
   * 
   * @param o
   * @return
   */
  private boolean isSubcollection(Object o) {
    return o instanceof CollectionImeji && ((CollectionImeji) o).isSubCollection();
  }

  /**
   * True if an object is discarded
   *
   * @param obj
   * @return
   */
  private boolean isDiscarded(Object obj) {
    if (obj instanceof Item) {
      return isDiscardedStatus(((Item) obj).getStatus());
    } else if (obj instanceof CollectionImeji) {
      return isDiscardedStatus(((CollectionImeji) obj).getStatus());
    } else if (obj instanceof Person) {
      return false;
    } else if (obj instanceof Organization) {
      return false;
    }
    return false;
  }

  /**
   * True if the {@link Status} is a public status(i.e. not need to have special grants to read the
   * object)
   *
   * @param status
   * @return
   */
  private boolean isPublicStatus(Status status) {
    return status.equals(Status.RELEASED) || status.equals(Status.WITHDRAWN);
  }

  /**
   * True if the {@link Status} is discarded status
   *
   * @param status
   * @return
   */
  private boolean isDiscardedStatus(Status status) {
    return status.equals(Status.WITHDRAWN);
  }
}
