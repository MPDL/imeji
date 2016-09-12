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
package de.mpg.imeji.presentation.user;

import java.io.IOException;
import java.io.Serializable;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import org.apache.log4j.Logger;

import com.hp.hpl.jena.sparql.pfunction.library.container;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.exceptions.UnprocessableError;
import de.mpg.imeji.logic.Imeji;
import de.mpg.imeji.logic.user.controller.GroupBusinessController;
import de.mpg.imeji.logic.user.controller.UserBusinessController;
import de.mpg.imeji.logic.util.UrlHelper;
import de.mpg.imeji.logic.vo.Container;
import de.mpg.imeji.logic.vo.Grant;
import de.mpg.imeji.logic.vo.User;
import de.mpg.imeji.logic.vo.UserGroup;
import de.mpg.imeji.presentation.beans.SuperBean;
import de.mpg.imeji.presentation.share.ShareListItem;
import de.mpg.imeji.presentation.share.ShareUtil;
import de.mpg.imeji.presentation.util.BeanHelper;

/**
 * Bean to create a
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
@ManagedBean(name = "UserGroup")
@ViewScoped
public class UserGroupBean extends SuperBean implements Serializable {
  private static final long serialVersionUID = -6501626930686020874L;
  private UserGroup userGroup = new UserGroup();
  private Collection<User> users;
  private static final Logger LOGGER = Logger.getLogger(UserGroupsBean.class);
  private List<ShareListItem> roles = new ArrayList<ShareListItem>();

  @PostConstruct
  public void init() {
    String groupId = UrlHelper.getParameterValue("id");
    if (groupId != null) {
      GroupBusinessController c = new GroupBusinessController();
      try {
        this.userGroup = c.retrieve(groupId, getSessionUser());
        this.users = loadUsers(userGroup);
        this.roles = ShareUtil.getAllRoles(userGroup, getSessionUser(), getLocale());
      } catch (ImejiException e) {
        BeanHelper.error("Error reading user group " + groupId);
        LOGGER.error("Error initializing UserGroupBean", e);
      }
    }
  }

  /**
   * Load the {@link User} of a {@link UserGroup}
   *
   * @param subject
   * @param f
   * @param object
   * @param position
   * @return
   */
  public Collection<User> loadUsers(UserGroup group) {
    Collection<User> users = new ArrayList<User>();
    UserBusinessController c = new UserBusinessController();
    for (URI uri : userGroup.getUsers()) {
      try {
        users.add(c.retrieve(uri, Imeji.adminUser));
      } catch (ImejiException e) {
        LOGGER.error("Error reading user: ", e);
      }
    }
    return users;
  }

  /**
   * Remove a {@link User} from a {@link UserGroup}
   *
   * @param remove
   * @return
   * @throws IOException
   */
  public void removeUserGromGroup(URI remove) throws IOException {
    userGroup.getUsers().remove(remove);
    save();
  }

  /**
   * Unshare the {@link Container} for one {@link UserGroup} (i.e, remove all {@link Grant} of this
   * {@link User} related to the {@link container})
   *
   * @param sh
   * @throws IOException
   */
  public void revokeGrants(ShareListItem sh) throws IOException {
    sh.getRoles().clear();
    sh.update();
    reload();
  }

  /**
   * Reload the page
   *
   * @throws IOException
   */
  private void reload() throws IOException {
    redirect(getNavigation().getApplicationUrl() + "usergroup?id=" + userGroup.getId());
  }

  /**
   * Create a new {@link UserGroup}
   */
  public String create() {
    GroupBusinessController c = new GroupBusinessController();
    try {
      c.create(userGroup, getSessionUser());
      reload();
    } catch (UnprocessableError e) {
      BeanHelper.error(e, getLocale());
      LOGGER.error("Error creating user group", e);
    } catch (Exception e) {
      BeanHelper.error("Error creating user group");
      LOGGER.error("Error creating user group", e);
    }
    return "";
  }

  /**
   * Update the current {@link UserGroup}
   *
   * @throws IOException
   */
  public void save() throws IOException {
    GroupBusinessController c = new GroupBusinessController();
    try {
      c.update(userGroup, getSessionUser());
    } catch (UnprocessableError e) {
      BeanHelper.error(e, getLocale());
      LOGGER.error("Error updating user group", e);
    } catch (Exception e) {
      BeanHelper.error("Error updating user group");
      LOGGER.error("Error updating user group", e);
    }
    reload();
  }

  /**
   * @return the userGroup
   */
  public UserGroup getUserGroup() {
    return userGroup;
  }

  /**
   * @param userGroup the userGroup to set
   */
  public void setUserGroup(UserGroup userGroup) {
    this.userGroup = userGroup;
  }

  /**
   * @return the users
   */
  public Collection<User> getUsers() {
    return users;
  }

  /**
   * @param users the users to set
   */
  public void setUsers(Collection<User> users) {
    this.users = users;
  }

  /**
   * @return the roles
   */
  public List<ShareListItem> getRoles() {
    return roles;
  }

  /**
   * @param roles the roles to set
   */
  public void setRoles(List<ShareListItem> roles) {
    this.roles = roles;
  }
}
