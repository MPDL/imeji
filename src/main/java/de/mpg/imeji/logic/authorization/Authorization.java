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
package de.mpg.imeji.logic.authorization;

import java.io.Serializable;
import java.net.URI;
import java.util.Collection;
import java.util.function.Predicate;

import org.apache.log4j.Logger;

import de.mpg.imeji.exceptions.NotAllowedError;
import de.mpg.imeji.logic.Imeji;
import de.mpg.imeji.logic.vo.CollectionImeji;
import de.mpg.imeji.logic.vo.Container;
import de.mpg.imeji.logic.vo.Grant;
import de.mpg.imeji.logic.vo.Grant.GrantType;
import de.mpg.imeji.logic.vo.Item;
import de.mpg.imeji.logic.vo.Organization;
import de.mpg.imeji.logic.vo.Person;
import de.mpg.imeji.logic.vo.Properties.Status;
import de.mpg.imeji.logic.vo.Space;
import de.mpg.imeji.logic.vo.User;
import de.mpg.imeji.logic.vo.UserGroup;

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

  /**
   * True if the {@link Grant} is found in the collection
   * 
   * @param grants
   * @param grant
   * @return
   */
  private boolean hasGrant(Collection<Grant> grants, Grant grant) {
    return grants.stream().anyMatch(new Predicate<Grant>() {
      @Override
      public boolean test(Grant t) {
        return t.getGrantFor().equals(grant.getGrantFor())
            && t.getGrantType().equals(grant.getGrantType());
      }
    });
  }


  /**
   * True if the User has either Read, Edit of Admin Grant for this uri
   * 
   * @param grants
   * @param uri
   * @return
   */
  private boolean hasReadGrant(Collection<Grant> grants, String uri) {
    return grants.stream().anyMatch(new Predicate<Grant>() {
      @Override
      public boolean test(Grant t) {
        return t.getGrantFor().equals(uri);
      }
    });
  }

  /**
   * True if the User has either Edit of Admin Grant for this uri
   * 
   * @param grants
   * @param uri
   * @return
   */
  private boolean hasEditGrant(Collection<Grant> grants, String uri) {
    return grants.stream().anyMatch(new Predicate<Grant>() {
      @Override
      public boolean test(Grant t) {
        return t.getGrantFor().equals(uri) && (t.getGrantType().equals(GrantType.EDIT.toString())
            || t.getGrantType().equals(GrantType.ADMIN.toString()));
      }
    });
  }

  /**
   * True if the User has Admin Grant for this uri
   * 
   * @param grants
   * @param uri
   * @return
   */
  private boolean hasAdminGrant(Collection<Grant> grants, String uri) {
    return hasGrant(grants, new Grant(GrantType.ADMIN, uri));
  }


  /**
   * True if the {@link User} has the grant to create a collection in imeji
   * 
   * @param user
   * @return
   */
  public boolean hasCreateCollectionGrant(User user) {
    return hasGrant(user.getGrants(),
        new Grant(GrantType.CREATE, AuthorizationPredefinedRoles.IMEJI_GLOBAL_URI));
  }

  /**
   * True if the {@link User} is a system administrator
   * 
   * @param user
   * @return
   */
  public boolean isSysAdmin(User user) {
    return hasGrant(user.getGrants(),
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
    return isPublic(obj, user) || isSysAdmin(user) || hasReadGrant(user.getGrants(), getId(obj));
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
    return isSysAdmin(user) || hasEditGrant(user.getGrants(), getId(obj));
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
    return update(user, obj);
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
    return isSysAdmin(user) || hasAdminGrant(user.getGrants(), getId(obj));
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
        return ((Item) obj).getCollection().toString();
      }
      if (obj instanceof Container) {
        return ((Container) obj).getId().toString();
      }
      if (obj instanceof User) {
        return ((User) obj).getId().toString();
      }
      if (obj instanceof URI) {
        return obj.toString();
      }
      if (obj instanceof String) {
        return obj.toString();
      }
      if (obj instanceof UserGroup) {
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
    } else if (obj instanceof Container) {
      return isPublicStatus(((Container) obj).getStatus());
    } else if (obj instanceof Space) {
      return isPublicStatus(((Space) obj).getStatus());
    } else if (obj instanceof Person) {
      return true;
    }
    return false;
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
    } else if (obj instanceof Container) {
      return isDiscardedStatus(((Container) obj).getStatus());
    } else if (obj instanceof Space) {
      return isDiscardedStatus(((Space) obj).getStatus());
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
