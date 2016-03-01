/**
 * License: src/main/resources/license/escidoc.license
 */
package de.mpg.imeji.presentation.user;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import javax.faces.context.FacesContext;

import org.apache.log4j.Logger;
import org.jose4j.lang.JoseException;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.exceptions.UnprocessableError;
import de.mpg.imeji.logic.Imeji;
import de.mpg.imeji.logic.ImejiSPARQL;
import de.mpg.imeji.logic.auth.authentication.impl.APIKeyAuthentication;
import de.mpg.imeji.logic.auth.util.AuthUtil;
import de.mpg.imeji.logic.controller.ShareController;
import de.mpg.imeji.logic.controller.ShareController.ShareRoles;
import de.mpg.imeji.logic.controller.UserController;
import de.mpg.imeji.logic.search.jenasearch.JenaCustomQueries;
import de.mpg.imeji.logic.util.QuotaUtil;
import de.mpg.imeji.logic.util.StringHelper;
import de.mpg.imeji.logic.vo.Organization;
import de.mpg.imeji.logic.vo.User;
import de.mpg.imeji.logic.vo.UserGroup;
import de.mpg.imeji.presentation.beans.Navigation;
import de.mpg.imeji.presentation.beans.PropertyBean;
import de.mpg.imeji.presentation.session.SessionBean;
import de.mpg.imeji.presentation.util.BeanHelper;
import de.mpg.imeji.presentation.util.ImejiFactory;
import de.mpg.imeji.presentation.util.ObjectLoader;

public class UserBean extends QuotaSuperBean {
  private User user;
  private String newPassword = null;
  private String repeatedPassword = null;
  private SessionBean session = (SessionBean) BeanHelper.getSessionBean(SessionBean.class);
  private String id;
  private List<SharedHistory> roles = new ArrayList<SharedHistory>();
  private boolean edit = false;

  public UserBean() {
    super();
  }

  public UserBean(String email) {
    super();
    init(email);
  }

  /**
   * Method called from the html page
   * 
   * @return
   */
  public String getInit() {
    init(FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap().get("id"));
    return "";
  }

  private void init(String id) {
    try {
      this.id = id;
      newPassword = null;
      repeatedPassword = null;
      retrieveUser();
      if (user != null) {
        this.roles = AuthUtil.getAllRoles(user, session.getUser());
        this.setEdit(false);
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Retrieve the current user
   * 
   * @throws Exception
   */
  public void retrieveUser() throws Exception {
    if (id != null && session.getUser() != null) {
      user = ObjectLoader.loadUser(id, session.getUser());
    } else if (id != null && session.getUser() == null) {
      LoginBean loginBean = (LoginBean) BeanHelper.getRequestBean(LoginBean.class);
      loginBean.setLogin(id);
    }
  }

  /**
   * Change the password of the user
   * 
   * @throws Exception
   */
  public void changePassword() throws Exception {
    if (user != null && newPassword != null && !"".equals(newPassword)) {
      if (newPassword.equals(repeatedPassword)) {
        user.setEncryptedPassword(StringHelper.convertToMD5(newPassword));
        updateUser();
        BeanHelper.info(session.getMessage("success_change_user_password"));
      } else {
        BeanHelper.error(session.getMessage("error_user_repeat_password"));
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
  public void generateNewApiKey()
      throws ImejiException, NoSuchAlgorithmException, UnsupportedEncodingException, JoseException {
    if (user != null) {
      user.setApiKey(APIKeyAuthentication.generateKey(user.getId(), Integer.MAX_VALUE));
      new UserController(session.getUser()).update(user, session.getUser());
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
  public void toggleAdmin() throws Exception {
    ShareController shareController = new ShareController();
    if (user.isAdmin()) {
      shareController.shareToUser(session.getUser(), user, PropertyBean.baseURI(),
          ShareController.rolesAsList(ShareRoles.CREATE));
    } else {
      shareController.shareToUser(session.getUser(), user, PropertyBean.baseURI(),
          ShareController.rolesAsList(ShareRoles.ADMIN));
    }
  }

  public boolean isUniqueAdmin() {
    return ImejiSPARQL.exec(JenaCustomQueries.selectUserSysAdmin(), Imeji.userModel).size() == 1;
  }

  /**
   * Toggle the create collction role of the {@link User}
   * 
   * @throws Exception
   */
  public void toggleCreateCollection() throws Exception {
    ShareController shareController = new ShareController();
    if (!user.isAdmin()) {
      // admin can not be forbidden to create collections
      if (user.isAllowedToCreateCollection()) {
        shareController.shareToUser(session.getUser(), user, PropertyBean.baseURI(), null);
      } else {
        shareController.shareToUser(session.getUser(), user, PropertyBean.baseURI(),
            ShareController.rolesAsList(ShareController.ShareRoles.CREATE));
      }
    }
  }

  /**
   * Update the user in jena
   * 
   * @throws ImejiException
   */
  public void updateUser() throws ImejiException {
    if (user != null) {
      UserController controller = new UserController(session.getUser());
      user.setQuota(QuotaUtil.getQuotaInBytes(getQuota()));
      try {
        controller.update(user, session.getUser());
        reloadPage();
      } catch (UnprocessableError e) {
        BeanHelper.cleanMessages();
        BeanHelper.error(session.getMessage("error_during_user_update"));
        for (String errorM : e.getMessages()) {
          BeanHelper.error(session.getMessage(errorM));
        }
      }
    }
  }

  /**
   * Reload the page with the current user
   * 
   * @throws IOException
   */
  private void reloadPage() {
    try {
      FacesContext.getCurrentInstance().getExternalContext().redirect(getUserPageUrl());
    } catch (IOException e) {
      Logger.getLogger(UserBean.class).info("Some reloadPage exception", e);
    }
  }

  /**
   * return the URL of the current user
   * 
   * @return
   */
  public String getUserPageUrl() {
    Navigation navigation = (Navigation) BeanHelper.getApplicationBean(Navigation.class);

    return navigation.getUserUrl() + "?id=" + user.getEmail();
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
  public List<SharedHistory> getRoles() {
    return roles;
  }

  public List<SharedHistory> getGroupRoles(UserGroup userGroup) throws Exception {
    if (userGroup != null)
      return AuthUtil.getAllRoles(userGroup, session.getUser());
    else
      return null;
  };



  /**
   * @param roles the roles to set
   */
  public void setRoles(List<SharedHistory> roles) {
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


}
