package de.mpg.imeji.presentation.authorization;

import java.io.Serializable;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import de.mpg.imeji.logic.authorization.Authorization;
import de.mpg.imeji.logic.vo.User;
import de.mpg.imeji.presentation.beans.SuperBean;

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
public class AuthorizationBean extends SuperBean implements Serializable {
  private static final long serialVersionUID = 4905896901833448372L;
  private final Authorization authorization = new Authorization();
  private boolean read = false;
  private boolean update = false;
  private boolean delete = false;
  private boolean admin = false;
  private boolean sysadmin = false;
  private boolean createCollection = false;

  public void init() {
    System.out.println("INIT AUTH");
    sysadmin = isSysAdmin(getSessionUser());
    createCollection = createCollection(getSessionUser());
  }

  /**
   * Initialize the authorization for one object (better performance than calling each time the
   * Authorization)
   * 
   * @param obj
   */
  public void init(Object obj) {
    read = read(obj);
    update = update(obj);
    admin = admin(obj);
    delete = delete(obj);
  }

  /**
   * @return the read
   */
  public boolean isRead() {
    return read;
  }



  /**
   * @return the update
   */
  public boolean isUpdate() {
    return update;
  }

  /**
   * @return the delete
   */
  public boolean isDelete() {
    return delete;
  }

  /**
   * @return the admin
   */
  public boolean isAdmin() {
    return admin;
  }



  /**
   * True if the {@link User} can read the object
   *
   * @param user
   * @param url
   * @return
   */
  public boolean read(User user, Object obj) {
    return authorization.read(user, obj);
  }

  /**
   * True if the {@link User} can update the object
   *
   * @param user
   * @param url
   * @return
   */
  public boolean update(User user, Object obj) {
    return authorization.update(user, obj);
  }

  /**
   * True if the {@link User} can delete the object
   *
   * @param user
   * @param url
   * @return
   */
  public boolean delete(User user, Object obj) {
    return authorization.delete(user, obj);
  }

  /**
   * True if the {@link User} can administrate the object
   *
   * @param user
   * @param url
   * @return
   */
  public boolean admin(User user, Object obj) {
    return authorization.administrate(user, obj);
  }

  /**
   * True if the current {@link User} in the session can read the object
   *
   * @param hasgrant
   * @param url
   * @return
   */
  public boolean read(Object obj) {
    return authorization.read(getSessionUser(), obj);
  }

  /**
   * True if the current {@link User} in the session can create the object
   *
   * @param hasgrant
   * @param url
   * @return
   */
  public boolean create(Object obj) {
    return authorization.create(getSessionUser(), obj);
  }

  /**
   * True if the {@link User} can update the object
   *
   * @param hasgrant
   * @param url
   * @return
   */
  public boolean update(Object obj) {
    return authorization.update(getSessionUser(), obj);
  }

  /**
   * True if the current {@link User} in the session can delete the object
   *
   * @param hasgrant
   * @param url
   * @return
   */
  public boolean delete(Object obj) {
    return authorization.delete(getSessionUser(), obj);
  }

  /**
   * True if the current {@link User} in the session can administrate the object
   *
   * @param hasgrant
   * @param url
   * @return
   */
  public boolean admin(Object obj) {
    return authorization.administrate(getSessionUser(), obj);
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
    return sysadmin;
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
  public boolean createCollection(User user) {
    return authorization.hasCreateCollectionGrant(user);
  }

  /**
   * True if the user can create a new collection
   *
   * @param user
   * @return
   */
  public boolean isCreateCollection() {
    return createCollection;
  }

  /**
   * True if a user is currently logged in
   *
   * @return
   */
  public boolean isLoggedIn() {
    return getSessionUser() != null;
  }

}
