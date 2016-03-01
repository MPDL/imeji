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
package de.mpg.imeji.presentation.auth;

import java.io.Serializable;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import de.mpg.imeji.logic.auth.authorization.Authorization;
import de.mpg.imeji.logic.vo.User;
import de.mpg.imeji.presentation.beans.PropertyBean;
import de.mpg.imeji.presentation.session.SessionBean;
import de.mpg.imeji.presentation.util.BeanHelper;

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
public class ImejiAuthBean implements Serializable {
  private static final long serialVersionUID = 4905896901833448372L;
  private Authorization auth = new Authorization();
  private User sessionUser;

  /**
     * 
     * 
     */
  public ImejiAuthBean() {
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
    return auth.read(user, obj);
  }

  /**
   * True if the {@link User} can create the object
   * 
   * @param user
   * @param url
   * @return
   */
  public boolean create(User user, Object obj) {
    return auth.create(user, obj);
  }

  /**
   * True if the {@link User} can update the object
   * 
   * @param user
   * @param url
   * @return
   */
  public boolean update(User user, Object obj) {
    return auth.update(user, obj);
  }

  /**
   * True if the {@link User} can delete the object
   * 
   * @param user
   * @param url
   * @return
   */
  public boolean delete(User user, Object obj) {
    return auth.delete(user, obj);
  }

  /**
   * True if the {@link User} can administrate the object
   * 
   * @param user
   * @param url
   * @return
   */
  public boolean admin(User user, Object obj) {
    return auth.administrate(user, obj);
  }

  /**
   * Return true if the user can create content in the object. For instance, upload an item in a
   * collection, or add/remove an item to an album
   * 
   * @param user
   * @param url
   * @return
   */
  public boolean createContent(User user, Object obj) {
    return auth.createContent(user, obj);
  }

  /**
   * True if the {@link User} can update the content of the object
   * 
   * @param user
   * @param url
   * @return
   */
  public boolean updateContent(User user, Object obj) {
    return auth.updateContent(user, obj);
  }

  /**
   * True if the {@link User} can delete the content of the object
   * 
   * @param user
   * @param url
   * @return
   */
  public boolean deleteContent(User user, Object obj) {
    return auth.deleteContent(user, obj);
  }

  /**
   * True if the current {@link User} in the session can read the object
   * 
   * @param hasgrant
   * @param url
   * @return
   */
  public boolean read(Object obj) {
    return auth.read(sessionUser, obj);
  }

  /**
   * True if the current {@link User} in the session can create the object
   * 
   * @param hasgrant
   * @param url
   * @return
   */
  public boolean create(Object obj) {
    return auth.create(sessionUser, obj);
  }

  /**
   * True if the {@link User} can update the object
   * 
   * @param hasgrant
   * @param url
   * @return
   */
  public boolean update(Object obj) {
    return auth.update(sessionUser, obj);
  }

  /**
   * True if the current {@link User} in the session can delete the object
   * 
   * @param hasgrant
   * @param url
   * @return
   */
  public boolean delete(Object obj) {
    return auth.delete(sessionUser, obj);
  }

  /**
   * True if the current {@link User} in the session can administrate the object
   * 
   * @param hasgrant
   * @param url
   * @return
   */
  public boolean admin(Object obj) {
    return auth.administrate(sessionUser, obj);
  }

  /**
   * Return true if the user can create content in the object. For instance, upload an item in a
   * collection, or add/remove an item to an album
   * 
   * @param hasgrant
   * @param url
   * @return
   */
  public boolean createContent(Object obj) {
    return auth.createContent(sessionUser, obj);
  }

  /**
   * True if the {@link User} can update the content of the object
   * 
   * @param hasgrant
   * @param url
   * @return
   */
  public boolean updateContent(Object obj) {
    return auth.updateContent(sessionUser, obj);
  }

  /**
   * True if the {@link User} can delete the content of the object
   * 
   * @param hasgrant
   * @param url
   * @return
   */
  public boolean deleteContent(Object obj) {
    return auth.deleteContent(sessionUser, obj);
  }

  /**
   * True if the current {@link User} in the session can administrate imeji (i.e. is system
   * administrator)
   * 
   * @param hasgrant
   * @param url
   * @return
   */
  public boolean isAdmin() {
    return auth.administrate(sessionUser, PropertyBean.baseURI());
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

}
