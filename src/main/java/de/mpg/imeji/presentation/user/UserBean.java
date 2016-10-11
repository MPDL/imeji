/**
 * License: src/main/resources/license/escidoc.license
 */
package de.mpg.imeji.presentation.user;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.ViewScoped;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.jose4j.lang.JoseException;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.exceptions.UnprocessableError;
import de.mpg.imeji.logic.Imeji;
import de.mpg.imeji.logic.search.jenasearch.ImejiSPARQL;
import de.mpg.imeji.logic.search.jenasearch.JenaCustomQueries;
import de.mpg.imeji.logic.security.util.SecurityUtil;
import de.mpg.imeji.logic.user.authentication.impl.APIKeyAuthentication;
import de.mpg.imeji.logic.user.collaboration.share.ShareBusinessController;
import de.mpg.imeji.logic.user.collaboration.share.ShareBusinessController.ShareRoles;
import de.mpg.imeji.logic.user.controller.UserBusinessController;
import de.mpg.imeji.logic.user.util.QuotaUtil;
import de.mpg.imeji.logic.util.StringHelper;
import de.mpg.imeji.logic.util.UrlHelper;
import de.mpg.imeji.logic.vo.Organization;
import de.mpg.imeji.logic.vo.User;
import de.mpg.imeji.logic.vo.UserGroup;
import de.mpg.imeji.logic.vo.util.ImejiFactory;
import de.mpg.imeji.presentation.beans.SuperBean;
import de.mpg.imeji.presentation.session.BeanHelper;
import de.mpg.imeji.presentation.share.ShareListItem;
import de.mpg.imeji.presentation.share.ShareUtil;

@ManagedBean(name = "UserBean")
@ViewScoped
public class UserBean extends SuperBean {
  private static final long serialVersionUID = 8339673964329354673L;
  private static final Logger LOGGER = Logger.getLogger(UserBean.class);
  private User user;
  private String newPassword = null;
  private String repeatedPassword = null;
  private String id;
  private List<ShareListItem> roles = new ArrayList<ShareListItem>();
  private boolean edit = false;
  private QuotaUICompoment quota;
  @ManagedProperty(value = "#{LoginBean}")
  private LoginBean loginBean;

  public UserBean() {
    // mandatory for JSF initialization
  }

  public UserBean(String email) {
    init(email);
  }

  @PostConstruct
  public void init() {
    init(UrlHelper.getParameterValue("id"));
  }

  /**
   * Initialize the bean
   * 
   * @param id
   */
  private void init(String id) {
    try {
      this.id = id;
      newPassword = null;
      repeatedPassword = null;
      retrieveUser();
      if (user != null) {
        this.roles = ShareUtil.getAllRoles(user, getSessionUser(), getLocale());
        this.setEdit(false);
        this.setQuota(new QuotaUICompoment(user, getLocale()));
      }
    } catch (Exception e) {
      LOGGER.error("Error initializing page", e);
      BeanHelper.error("Error initializing page");
    }
  }

  /**
   * Retrieve the current user
   * 
   * @throws ImejiException
   *
   * @throws Exception
   */
  public void retrieveUser() throws ImejiException {
    if (id != null && getSessionUser() != null) {
      user = new UserBusinessController().retrieve(id, getSessionUser());
      if (user.getPerson().getOrganizations() == null
          || user.getPerson().getOrganizations().isEmpty()) {
        user.getPerson().getOrganizations().add(new Organization());
      }
    } else if (id != null && getSessionUser() == null) {
      loginBean.setLogin(id);
    }
  }

  /**
   * Change the password of the user
   *
   * @throws Exception
   */
  public void changePassword() throws ImejiException {
    if (user != null && newPassword != null && !"".equals(newPassword)) {
      if (newPassword.equals(repeatedPassword)) {
        user.setEncryptedPassword(StringHelper.convertToMD5(newPassword));
        updateUser();
        BeanHelper
            .info(Imeji.RESOURCE_BUNDLE.getMessage("success_change_user_password", getLocale()));
        return;
      } else {
        BeanHelper
            .error(Imeji.RESOURCE_BUNDLE.getMessage("error_user_repeat_password", getLocale()));
      }
      reloadPage();
    }
  }

  public void toggleEdit() {
    this.edit = edit ? false : true;
  }

  /**
   * Generate a new API Key, and update the user
   *
   * @throws ImejiException
   * @throws NoSuchAlgorithmException
   * @throws UnsupportedEncodingException
   * @throws JoseException
   */
  public void generateNewApiKey() throws ImejiException {
    if (user != null) {
      try {
        user.setApiKey(APIKeyAuthentication.generateKey(user.getId(), Integer.MAX_VALUE));
      } catch (JoseException e) {
        LOGGER.error("Error generating API Key", e);
        throw new ImejiException("Error generating API Key", e);
      }
      new UserBusinessController().update(user, getSessionUser());
    }
  }

  /**
   * Add a new empty organization
   *
   * @param index
   */
  public void addOrganization(int index) {
    ((List<Organization>) this.user.getPerson().getOrganizations()).add(index,
        ImejiFactory.newOrganization());
  }

  /**
   * Remove an nth organization
   *
   * @param index
   */
  public void removeOrganization(int index) {
    List<Organization> orgas = (List<Organization>) this.user.getPerson().getOrganizations();
    if (!orgas.isEmpty()) {
      orgas.remove(index);
    }
  }

  /**
   * Toggle the Admin Role of the {@link User}
   *
   * @throws Exception
   */
  public void toggleAdmin() throws ImejiException {
    ShareBusinessController shareController = new ShareBusinessController();
    if (SecurityUtil.isSysAdmin(user)) {
      shareController.shareToUser(getSessionUser(), user, Imeji.PROPERTIES.getBaseURI(),
          ShareBusinessController.rolesAsList(ShareRoles.CREATE));
    } else {
      shareController.shareToUser(getSessionUser(), user, Imeji.PROPERTIES.getBaseURI(),
          ShareBusinessController.rolesAsList(ShareRoles.ADMIN));
    }

    reloadPage();
  }

  public boolean isSysAdmin() {
    return SecurityUtil.isSysAdmin(user);
  }

  public boolean isUniqueAdmin() {
    return ImejiSPARQL.exec(JenaCustomQueries.selectUserSysAdmin(), Imeji.userModel).size() == 1;
  }

  /**
   * Toggle the create collction role of the {@link User}
   *
   * @throws Exception
   */
  public void toggleCreateCollection() throws ImejiException {
    ShareBusinessController shareController = new ShareBusinessController();
    if (!SecurityUtil.isSysAdmin(user)) {
      // admin can not be forbidden to create collections
      if (SecurityUtil.isAllowedToCreateCollection(user)) {
        shareController.shareToUser(getSessionUser(), user, Imeji.PROPERTIES.getBaseURI(), null);
      } else {
        shareController.shareToUser(getSessionUser(), user, Imeji.PROPERTIES.getBaseURI(),
            ShareBusinessController.rolesAsList(ShareBusinessController.ShareRoles.CREATE));
      }
    }
  }

  /**
   * Update the user in jena
   *
   * @throws ImejiException
   * @throws IOException
   */
  public void updateUser() throws ImejiException {
    if (user != null) {
      UserBusinessController controller = new UserBusinessController();
      user.setQuota(QuotaUtil.getQuotaInBytes(quota.getQuota()));
      try {
        controller.update(user, getSessionUser());
        reloadPage();
      } catch (UnprocessableError e) {
        BeanHelper.error(e, getLocale());
        LOGGER.error("Error updating user", e);
      }
    }
  }

  /**
   * Return the quota of the current user in a user friendly way
   * 
   * @param locale
   * @return
   */
  public String getQuotaHumanReadable(Locale locale) {
    if (user.getQuota() == Long.MAX_VALUE) {
      return Imeji.RESOURCE_BUNDLE.getLabel("unlimited", getLocale());
    } else {
      return FileUtils.byteCountToDisplaySize(user.getQuota());
    }
  }

  /**
   * Reload the page with the current user
   *
   * @throws IOException
   */
  private void reloadPage() {
    try {
      redirect(getUserPageUrl());
    } catch (IOException e) {
      LOGGER.error("Error reloading user page", e);
    }
  }

  /**
   * return the URL of the current user
   *
   * @return
   */
  public String getUserPageUrl() {
    return getNavigation().getUserUrl() + "?id=" + user.getEmail();
  }

  public User getUser() {
    return user;
  }

  public void setUser(User user) {
    this.user = user;
  }

  public String getNewPassword() {
    return newPassword;
  }

  public void setNewPassword(String newPassword) {
    this.newPassword = newPassword;
  }

  public String getRepeatedPassword() {
    return repeatedPassword;
  }

  public void setRepeatedPassword(String repeatedPassword) {
    this.repeatedPassword = repeatedPassword;
  }

  /**
   * @return the roles
   */
  public List<ShareListItem> getRoles() {
    return roles;
  }

  public List<ShareListItem> getGroupRoles(UserGroup userGroup) throws ImejiException {
    if (userGroup != null) {
      return ShareUtil.getAllRoles(userGroup, getSessionUser(), getLocale());
    } else {
      return null;
    }
  }



  /**
   * @param roles the roles to set
   */
  public void setRoles(List<ShareListItem> roles) {
    this.roles = roles;
  }

  /**
   * @return the edit
   */
  public boolean isEdit() {
    return edit;
  }

  /**
   * @param edit the edit to set
   */
  public void setEdit(boolean edit) {
    this.edit = edit;
  }

  /**
   * @return the quota
   */
  public QuotaUICompoment getQuota() {
    return quota;
  }

  /**
   * @param quota the quota to set
   */
  public void setQuota(QuotaUICompoment quota) {
    this.quota = quota;
  }

  public LoginBean getLoginBean() {
    return loginBean;
  }

  public void setLoginBean(LoginBean loginBean) {
    this.loginBean = loginBean;
  }
}
