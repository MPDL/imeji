/**
 * License: src/main/resources/license/escidoc.license
 */
package de.mpg.imeji.presentation.util;

import java.net.URI;

import org.apache.log4j.Logger;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.exceptions.NotFoundException;
import de.mpg.imeji.logic.controller.AlbumController;
import de.mpg.imeji.logic.controller.CollectionController;
import de.mpg.imeji.logic.controller.ItemController;
import de.mpg.imeji.logic.controller.ProfileController;
import de.mpg.imeji.logic.controller.UserController;
import de.mpg.imeji.logic.controller.UserGroupController;
import de.mpg.imeji.logic.vo.Album;
import de.mpg.imeji.logic.vo.CollectionImeji;
import de.mpg.imeji.logic.vo.Item;
import de.mpg.imeji.logic.vo.MetadataProfile;
import de.mpg.imeji.logic.vo.User;
import de.mpg.imeji.logic.vo.UserGroup;
import de.mpg.imeji.presentation.session.SessionBean;

/**
 * imeji objects (item, collection, album, profile) loader. This loader should be used to loads
 * objects from a Java bean, since it include error message. Doesn't use caching (unlike
 * {@link ObjectCachedLoader})
 * 
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public class ObjectLoader {
  private static final Logger LOGGER = Logger.getLogger(ObjectLoader.class);

  /**
   * Private Constructor
   */
  private ObjectLoader() {}

  /**
   * Load a {@link CollectionImeji} with all its item
   * 
   * @param id
   * @param user
   * @return
   * @throws Exception
   */
  public static CollectionImeji loadCollection(URI id, User user) throws ImejiException {
    try {
      CollectionController cl = new CollectionController();
      return cl.retrieve(id, user);
    } catch (NotFoundException e) {
      writeErrorNotFound("collection", id);
    } catch (ImejiException e) {
      // LOGGER.error("Error Object loader for " + id, e);
      // Commented out no need to log handled ImejiException
      throw e;
    }
    return null;
  }

  /**
   * Load a {@link CollectionImeji} without its {@link Item}
   * 
   * @param id
   * @param user
   * @return
   * @throws Exception
   */
  public static CollectionImeji loadCollectionLazy(URI id, User user) throws ImejiException {
    try {
      CollectionController cl = new CollectionController();
      return cl.retrieveLazy(id, user);
    } catch (NotFoundException e) {
      writeErrorNotFound("collection", id);
    } catch (ImejiException e) {
      if (id != null) {
        writeException(e, id.toString());
      } else {
        writeException(e, null);
      }
    }
    return null;
  }

  /**
   * Load an {@link Album} with all tis {@link Item}
   * 
   * @param id
   * @param user
   * @return
   * @throws Exception
   */
  public static Album loadAlbum(URI id, User user) throws Exception {
    try {
      Album a = loadAlbumLazy(id, user);
      ItemController ic = new ItemController();
      return (Album) ic.searchAndSetContainerItems(a, user, -1, 0);
    } catch (ImejiException e) {
      writeException(e, id.toString());
    }
    return null;
  }

  /**
   * Load an {@link Album} without its {@link Item}
   * 
   * @param id
   * @param user
   * @return
   * @throws Exception
   */
  public static Album loadAlbumLazy(URI id, User user) throws ImejiException {
    try {
      AlbumController ac = new AlbumController();
      return ac.retrieveLazy(id, user);
    } catch (NotFoundException e) {
      writeErrorNotFound("album", id);
    } catch (ImejiException e) {
      writeException(e, id.toString());
    }
    return null;
  }

  /**
   * Load an {@link Item}
   * 
   * @param id
   * @param user
   * @return
   * @throws Exception
   */
  public static Item loadItem(URI id, User user) throws ImejiException {
    try {
      ItemController ic = new ItemController();
      return ic.retrieve(id, user);
    } catch (NotFoundException e) {
      writeErrorNotFound("image", id);
    } catch (ImejiException e) {
      writeException(e, id.toString());
    }
    return null;
  }


  /**
   * Load an {@link UserGroup}
   * 
   * @param id
   * @param user
   * @return
   * @throws Exception
   */
  public static UserGroup loadUserGroupLazy(URI id, User user) throws ImejiException {
    try {
      UserGroupController c = new UserGroupController();
      return c.read(id, user);
    } catch (NotFoundException e) {
      writeErrorNotFound("usergroup", id);
    } catch (ImejiException e) {
      writeException(e, id.toString());
    }
    return null;
  }

  /**
   * Load a {@link User}
   * 
   * @param email
   * @param user
   * @return
   * @throws Exception
   */
  public static User loadUser(URI uri, User user) throws ImejiException {
    try {
      UserController uc = new UserController(user);
      return uc.retrieve(uri);
    } catch (NotFoundException e) {
      writeErrorNotFound("user", uri);
    } catch (ImejiException e) {
      writeException(e, uri.toString());
    }
    return null;
  }

  /**
   * Load a {@link User}
   * 
   * @param email
   * @param user
   * @return
   * @throws Exception
   */
  public static User loadUser(String email, User user) throws ImejiException {
    try {
      UserController uc = new UserController(user);
      return uc.retrieve(email);
    } catch (NotFoundException e) {
      writeErrorNotFound("user", URI.create(email));
    } catch (ImejiException e) {
      writeException(e, email);
    }
    return null;
  }

  /**
   * Load a {@link MetadataProfile}
   * 
   * @param id
   * @param user
   * @return
   */
  public static MetadataProfile loadProfile(URI id, User user) {
    try {
      ProfileController pc = new ProfileController();
      MetadataProfile p = pc.retrieve(id, user);
      return p;
    } catch (Exception e) {
      LOGGER.info("There was a problem loading the profile with id " + id.toString());
    }
    return null;
  }

  /**
   * Write {@link NotFoundException} in JSF messages and in logs
   * 
   * @param objectType
   * @param id
   */
  private static void writeErrorNotFound(String objectType, URI id) {
    BeanHelper
        .error(((SessionBean) BeanHelper.getSessionBean(SessionBean.class)).getLabel(objectType)
            + " " + id + " "
            + ((SessionBean) BeanHelper.getSessionBean(SessionBean.class)).getLabel("not_found"));
  }

  /**
   * Write {@link Exception} in jsf messages and in logs
   * 
   * @param e
   * @param id
   * @throws Exception
   */
  private static void writeException(ImejiException e, String id) throws ImejiException {
    // LOGGER.error("Error Object loader for " + id, e);
    // No need to log handled ImejiException
    throw e;
  }
}
