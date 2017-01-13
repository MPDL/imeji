package de.mpg.imeji.presentation.authorization;

import java.io.Serializable;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import de.mpg.imeji.logic.authorization.Authorization;
import de.mpg.imeji.logic.vo.User;
import de.mpg.imeji.presentation.album.AlbumBean;
import de.mpg.imeji.presentation.collection.CollectionListItem;
import de.mpg.imeji.presentation.session.BeanHelper;
import de.mpg.imeji.presentation.session.SessionBean;

/**
 * JSF Bean for imeji authorization. Can be call in the xhtml pages by: <br/>
 * <code>#{Auth.readUri(SessionBean.user, uri)}</code> or <br/>
 * <code>#{Auth.readUri(uri)}</code> (equivalent as before) or <br/>
 * <code>#{Auth.read(item)}</code> (equivalent as before) or <br/>
 * <code>#{Auth.isAdmin()}</code>
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
@ManagedBean(name = "Auth")
@ViewScoped
public class AuthorizationBean implements Serializable {
  private static final long serialVersionUID = 4905896901833448372L;
  private final Authorization authorization = new Authorization();
  private User sessionUser;

  public AuthorizationBean() {
    this.sessionUser = ((SessionBean) BeanHelper.getSessionBean(SessionBean.class)).getUser();
  }

  /**
   * True if the {@link User} can read the object
   *
   * @param user
   * @param url
   * @return
   */
  public boolean read(User user, Object obj) {
    return authorization.read(user, extractVO(obj));
  }

  /**
   * True if the {@link User} can update the object
   *
   * @param user
   * @param url
   * @return
   */
  public boolean update(User user, Object obj) {
    return authorization.update(user, extractVO(obj));
  }

  /**
   * True if the {@link User} can delete the object
   *
   * @param user
   * @param url
   * @return
   */
  public boolean delete(User user, Object obj) {
    return authorization.delete(user, extractVO(obj));
  }

  /**
   * True if the {@link User} can administrate the object
   *
   * @param user
   * @param url
   * @return
   */
  public boolean admin(User user, Object obj) {
    return authorization.administrate(user, extractVO(obj));
  }

  /**
   * True if the current {@link User} in the session can read the object
   *
   * @param hasgrant
   * @param url
   * @return
   */
  public boolean read(Object obj) {
    return authorization.read(sessionUser, extractVO(obj));
  }

  /**
   * True if the current {@link User} in the session can create the object
   *
   * @param hasgrant
   * @param url
   * @return
   */
  public boolean create(Object obj) {
    return authorization.create(sessionUser, extractVO(obj));
  }

  /**
   * True if the {@link User} can update the object
   *
   * @param hasgrant
   * @param url
   * @return
   */
  public boolean update(Object obj) {
    return authorization.update(sessionUser, extractVO(obj));
  }

  /**
   * True if the current {@link User} in the session can delete the object
   *
   * @param hasgrant
   * @param url
   * @return
   */
  public boolean delete(Object obj) {
    return authorization.delete(sessionUser, extractVO(obj));
  }

  /**
   * True if the current {@link User} in the session can administrate the object
   *
   * @param hasgrant
   * @param url
   * @return
   */
  public boolean admin(Object obj) {
    return authorization.administrate(sessionUser, extractVO(obj));
  }



  /**
   * True if the current {@link User} in the session can administrate imeji (i.e. is system
   * administrator)
   *
   * @param hasgrant
   * @param url
   * @return
   */
  public boolean isSysAdmin() {
    return authorization.isSysAdmin(sessionUser);
  }

  /**
   * True if the user is sysadmin
   *
   * @param user
   * @return
   */
  public boolean isSysAdmin(User user) {
    return authorization.isSysAdmin(user);
  }

  /**
   * True if the user ca create a new collection
   *
   * @param user
   * @return
   */
  public boolean isAllowedToCreateCollection(User user) {
    return authorization.hasCreateCollectionGrant(user);
  }

  /**
   * True if a user is currently logged in
   *
   * @return
   */
  public boolean isLoggedIn() {
    return sessionUser != null;
  }

  /**
   * @return the sessionUser
   */
  public User getSessionUser() {
    return sessionUser;
  }

  /**
   * @param sessionUser the sessionUser to set
   */
  public void setSessionUser(User sessionUser) {
    this.sessionUser = sessionUser;
  }

  /**
   * Extract the VO out of the object, to be abble to use {@link Authorization}
   *
   * @param obj
   * @return
   */
  private Object extractVO(Object obj) {
    if (obj instanceof CollectionListItem) {
      return ((CollectionListItem) obj).getCollection();
    }
    if (obj instanceof AlbumBean) {
      return ((AlbumBean) obj).getAlbum();
    }
    return obj;
  }

}
