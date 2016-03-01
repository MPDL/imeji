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
package de.mpg.imeji.logic.auth.authorization;

import java.io.Serializable;
import java.net.URI;
import java.util.List;

import de.mpg.imeji.exceptions.NotAllowedError;
import de.mpg.imeji.logic.ImejiSPARQL;
import de.mpg.imeji.logic.auth.util.AuthUtil;
import de.mpg.imeji.logic.search.jenasearch.JenaCustomQueries;
import de.mpg.imeji.logic.vo.Album;
import de.mpg.imeji.logic.vo.Container;
import de.mpg.imeji.logic.vo.Grant;
import de.mpg.imeji.logic.vo.Grant.GrantType;
import de.mpg.imeji.logic.vo.Item;
import de.mpg.imeji.logic.vo.MetadataProfile;
import de.mpg.imeji.logic.vo.Organization;
import de.mpg.imeji.logic.vo.Person;
import de.mpg.imeji.logic.vo.Properties.Status;
import de.mpg.imeji.logic.vo.Space;
import de.mpg.imeji.logic.vo.User;
import de.mpg.imeji.presentation.album.AlbumBean;
import de.mpg.imeji.presentation.beans.ConfigurationBean;
import de.mpg.imeji.presentation.beans.PropertyBean;
import de.mpg.imeji.presentation.collection.CollectionListItem;

/**
 * Authorization rules for imeji objects (defined by their uri) for one {@link User}
 * 
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public class Authorization implements Serializable {
  private static final long serialVersionUID = -4745899890554497793L;

  /**
   * Return true if the {@link User} can create the object
   * 
   * @param user
   * @param url
   * @return
   * @throws NotAllowedError
   */
  public boolean create(User user, Object obj) {
    if (obj instanceof Album) {
      return true; // everybody is allowed to create albums
    }
    if (hasGrant(user,
        toGrant(getRelevantURIForSecurity(obj, false, true, false), GrantType.CREATE))
        && !isDiscarded(obj)) {
      return true;
    }
    return false;
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
    if (isPublic(obj, user)) {
      return true;
    } else if (hasGrant(user, toGrant(getRelevantURIForSecurity(obj, true, false, false),
        getGrantTypeAccordingToObjectType(obj, GrantType.READ)))) {
      return true;
    } else if (hasGrant(user, toGrant(getRelevantURIForSecurity(obj, false, false, false),
        getGrantTypeAccordingToObjectType(obj, GrantType.READ)))) {
      return true;
    }
    return false;
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
    if (hasGrant(user, toGrant(getRelevantURIForSecurity(obj, false, false, false),
        getGrantTypeAccordingToObjectType(obj, GrantType.UPDATE)))) {
      return true;
    }
    return false;
  }

  public boolean isItem(Object obj) {
    return (obj instanceof Item || isItemUri(obj.toString()));
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
    if (AuthUtil.isSysAdmin(user)) {
      return true;
    }

    if (!isPublic(obj, user)
        && hasGrant(user, toGrant(getRelevantURIForSecurity(obj, false, false, false),
            getGrantTypeAccordingToObjectType(obj, GrantType.DELETE)))) {
      return true;
    }
    return false;
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
    if (!isDiscarded(obj)
        && hasGrant(user, toGrant(getRelevantURIForSecurity(obj, false, false, false),
            getGrantTypeAccordingToObjectType(obj, GrantType.ADMIN)))) {
      return true;
    }
    return false;
  }

  /**
   * Return true if the user can create content in the object. For instance, upload an item in a
   * collection, or add/remove an item to an album
   * 
   * @param user
   * @param obj
   * @return
   */
  public boolean createContent(User user, Object obj) {
    if (hasGrant(user,
        toGrant(getRelevantURIForSecurity(obj, false, false, false), GrantType.CREATE))
        && !isDiscarded(obj)) {
      return true;
    }
    return false;
  }

  /**
   * Return true if the user can update the content of the object
   * 
   * @param user
   * @param url
   * @return
   */
  public boolean updateContent(User user, Object obj) {
    if (hasGrant(user,
        toGrant(getRelevantURIForSecurity(obj, false, false, false), GrantType.UPDATE_CONTENT))) {
      return true;
    }
    return false;
  }

  /**
   * Return true if the user can delete the content of the object
   * 
   * @param user
   * @param url
   * @return
   */
  public boolean deleteContent(User user, Object obj) {
    if (AuthUtil.isSysAdmin(user)) {
      return true;
    }
    if (!isPublic(obj, user) && hasGrant(user,
        toGrant(getRelevantURIForSecurity(obj, false, false, false), GrantType.DELETE_CONTENT))) {
      return true;
    }
    return false;
  }

  /**
   * Return true if the user can administrate the content of the object
   * 
   * @param user
   * @param url
   * @return
   */
  public boolean adminContent(User user, Object obj) {
    if (hasGrant(user,
        toGrant(getRelevantURIForSecurity(obj, false, false, false), GrantType.ADMIN_CONTENT))) {
      return true;
    }
    return false;
  }

  /**
   * True if the {@link User} has the given {@link Grant} or if the {@link User} is system
   * Administrator
   * 
   * @param user
   * @param g
   * @return
   */
  private boolean hasGrant(User user, Grant g) {
    List<Grant> all = AuthUtil.getAllGrantsOfUser(user);
    if (all.contains(g)) {
      return true;
    }
    if (all.contains(toGrant(PropertyBean.baseURI(), GrantType.ADMIN))) {
      return true;
    }
    return false;
  }

  /**
   * Create a {@link Grant} out of the {@link GrantType} and the given uri
   * 
   * @param uri
   * @param type
   * @return
   */
  private Grant toGrant(String uri, GrantType type) {
    if (uri == null) {
      return null;
    }
    return new Grant(type, URI.create(uri));
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
  public String getRelevantURIForSecurity(Object obj, boolean hasItemGrant, boolean getContext,
      boolean isReadGrant) {
    if (obj instanceof Item) {
      return hasItemGrant ? ((Item) obj).getId().toString()
          : ((Item) obj).getCollection().toString();
    } else if (obj instanceof Container) {
      return getContext ? AuthorizationPredefinedRoles.IMEJI_GLOBAL_URI
          : ((Container) obj).getId().toString();
    } else if (obj instanceof CollectionListItem) {
      return ((CollectionListItem) obj).getUri().toString();
    } else if (obj instanceof AlbumBean) {
      return ((AlbumBean) obj).getAlbum().getId().toString();
    } else if (obj instanceof MetadataProfile) {
      return getContext ? AuthorizationPredefinedRoles.IMEJI_GLOBAL_URI
          : ((MetadataProfile) obj).getId().toString();
    } else if (obj instanceof User) {
      return ((User) obj).getId().toString();
    } else if (obj instanceof URI) {
      return getCollectionUri(obj.toString(), isReadGrant);
    } else if (obj instanceof String) {
      return getCollectionUri((String) obj, isReadGrant);
    }
    return PropertyBean.baseURI();
  }

  /**
   * Return the collection of an Item. Useful for Non Read Operation, since it is not possible to
   * give a non read grant to an item. Thus, if the authorization is called with an Item Id
   * 
   * @param itemUri
   * @return
   */
  private String getCollectionUri(String uri, boolean isReadGrant) {
    if (!isReadGrant && uri.contains("/item/")) {
      List<String> c = ImejiSPARQL.exec(JenaCustomQueries.selectCollectionIdOfItem(uri), null);
      if (!c.isEmpty()) {
        return c.get(0);
      }
    }
    return uri;
  }

  /**
   * If the Object is an {@link Item} then the {@link GrantType} must be changed to fit the
   * authorization on container level
   * 
   * @param obj
   * @param type
   * @return
   */
  private GrantType getGrantTypeAccordingToObjectType(Object obj, GrantType type) {
    if (obj == null)
      return type;
    if (obj instanceof Item || isItemUri(obj.toString())) {
      switch (type) {
        case UPDATE:
          return GrantType.UPDATE_CONTENT;
        case DELETE:
          return GrantType.DELETE_CONTENT;
        case ADMIN:
          return GrantType.ADMIN_CONTENT;
        default:
          return type;
      }
    }
    return type;
  }

  /**
   * True if the uri is the uri of an {@link Item}
   * 
   * @param uri
   * @return
   */
  private boolean isItemUri(String uri) {
    return uri.contains("/item/");
  }

  /**
   * True if the {@link Object} is public (i.e. has been released)
   * 
   * @param obj
   * @return
   */
  private boolean isPublic(Object obj, User user) {
    if (ConfigurationBean.getPrivateModusStatic() && user == null) {
      return false;
    } else if (obj instanceof Item) {
      return isPublicStatus(((Item) obj).getStatus());
    } else if (obj instanceof Container) {
      return isPublicStatus(((Container) obj).getStatus());
    } else if (obj instanceof Space) {
      return isPublicStatus(((Space) obj).getStatus());
    } else if (obj instanceof MetadataProfile) {
      return isPublicStatus(((MetadataProfile) obj).getStatus());
    } else if (obj instanceof Person) {
      return true;
    } else if (obj instanceof Organization) {
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
    } else if (obj instanceof MetadataProfile) {
      return isDiscardedStatus(((MetadataProfile) obj).getStatus());
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
