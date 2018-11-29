package de.mpg.imeji.presentation.user;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import de.mpg.imeji.logic.config.Imeji;
import de.mpg.imeji.logic.model.User;
import de.mpg.imeji.logic.model.User.UserStatus;
import de.mpg.imeji.logic.model.UserGroup;
import de.mpg.imeji.logic.notification.email.EmailMessages;
import de.mpg.imeji.logic.notification.email.EmailService;
import de.mpg.imeji.logic.security.authorization.util.PasswordGenerator;
import de.mpg.imeji.logic.security.registration.RegistrationService;
import de.mpg.imeji.logic.security.sharing.invitation.Invitation;
import de.mpg.imeji.logic.security.sharing.invitation.InvitationService;
import de.mpg.imeji.logic.security.user.UserService;
import de.mpg.imeji.logic.security.usergroup.UserGroupService;
import de.mpg.imeji.logic.util.StringHelper;
import de.mpg.imeji.logic.util.UrlHelper;
import de.mpg.imeji.presentation.beans.SuperBean;
import de.mpg.imeji.presentation.session.BeanHelper;

/**
 * Java Bean for the view users page
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
@ManagedBean(name = "UsersBean")
@ViewScoped
public class UsersBean extends SuperBean {
  private static final long serialVersionUID = 909531319532057429L;
  private List<User> users;
  private List<User> inactiveUsers;
  private List<Invitation> invitations;
  private UserGroup group;
  private String query;
  private static final Logger LOGGER = LogManager.getLogger(UserBean.class);

  /**
   * Initialize the bean
   */
  @PostConstruct
  public void init() {
    final String q = UrlHelper.getParameterValue("q");
    query = q == null ? "" : q;
    doSearch();
    retrieveGroup();
  }

  /**
   * Trigger the search to users Groups
   */
  public void search() {
    try {
      redirect(getNavigation().getApplicationUrl() + "users?q=" + query + (group != null ? "&group=" + group.getId() : ""));
    } catch (final IOException e) {
      BeanHelper.error(e.getMessage());
      LOGGER.error(e);
    }
  }

  /**
   * Retrieve all users
   */
  public void doSearch() {
    users = (List<User>) new UserService().searchUserByName(query);
    inactiveUsers = new RegistrationService().searchInactiveUsers(query);
    setInvitations(new InvitationService().search(query));
  }

  /**
   * If the parameter group in the url is not null, try to retrieve this group. This happens when
   * the admin want to add a {@link User} to a {@link UserGroup}
   */
  public void retrieveGroup() {
    if (UrlHelper.getParameterValue("group") != null && !"".equals(UrlHelper.getParameterValue("group"))) {
      final UserGroupService c = new UserGroupService();
      try {
        setGroup(c.retrieve(UrlHelper.getParameterValue("group"), getSessionUser()));
      } catch (final Exception e) {
        BeanHelper.error("error loading user group " + UrlHelper.getParameterValue("group"));
        LOGGER.error(e);
      }
    }
  }

  public String getSimpleQuery() {
    return this.query;
  }

  /**
   * Method called when a new password is sent
   *
   * @return
   * @throws Exception
   */
  public String sendPassword() {
    final String email = UrlHelper.getParameterValue("email");
    final PasswordGenerator generator = new PasswordGenerator();
    final UserService controller = new UserService();
    try {
      final User user = controller.retrieve(email, getSessionUser());
      final String newPassword = generator.generatePassword();
      user.setEncryptedPassword(StringHelper.md5(newPassword));
      controller.update(user, getSessionUser());
      sendEmail(email, newPassword, user.getPerson().getFirstnameLastname());
      BeanHelper.info(Imeji.RESOURCE_BUNDLE.getMessage("success_change_user_password", getLocale()));
    } catch (final Exception e) {
      BeanHelper.error("Could not update or send new password!");
      LOGGER.error("Could not update or send new password", e);
    }
    return "";
  }

  /**
   * Send an Email to a {@link User} for its new password
   *
   * @param email
   * @param password
   * @param username
   * @throws URISyntaxException
   * @throws IOException
   */
  public void sendEmail(String email, String password, String username) {
    final EmailService emailClient = new EmailService();
    try {
      emailClient.sendMail(email, null, EmailMessages.getEmailOnAccountAction_Subject(false, getLocale()),
          EmailMessages.getNewPasswordMessage(password, email, username, getLocale()));
    } catch (final Exception e) {
      BeanHelper.info("Error: Password Email not sent");
      LOGGER.error("Error sending password email", e);
    }
  }

  /**
   * Delete a {@link User}
   *
   * @return
   */
  public String deleteUser() {
    final String email = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap().get("email");
    final UserService controller = new UserService();
    try {
      controller.delete(controller.retrieve(email, getSessionUser()));
      reload();
    } catch (final Exception e) {
      BeanHelper.error("Error Deleting user");
      LOGGER.error("Error Deleting user", e);
    }
    return "";
  }

  /**
   * Removes a {@link User}, but does not delete him
   */
  public void removeUser() {
    final String email = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap().get("email");
    final UserService service = new UserService();
    try {
      final User user = service.retrieve(email, getSessionUser());
      user.setUserStatus(UserStatus.REMOVED);
      service.update(user, getSessionUser());
      reload();
    } catch (Exception e) {
      LOGGER.error("Error removing user", e);
    }
  }

  /**
   * Removes a {@link User}, but does not delete him
   */
  public void reactivateUser() {
    String email = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap().get("email");
    UserService controller = new UserService();
    try {
      User user = controller.retrieve(email, getSessionUser());
      user.setUserStatus(UserStatus.ACTIVE);
      controller.update(user, getSessionUser());
      reload();
    } catch (Exception e) {
      LOGGER.error("Error reactivating user", e);
    }
  }

  /**
   * Cancel a pending invitation
   */
  public void revokeRegistration() {
    final String email = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap().get("email");
    final RegistrationService service = new RegistrationService();
    try {
      service.delete(service.retrieveByEmail(email));
      reload();
    } catch (final Exception e) {
      BeanHelper.error("Error Deleting registration");
      LOGGER.error("Error Deleting registration", e);
    }
  }

  public void confirmRegistration() {
    final String email = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap().get("email");
    final RegistrationService service = new RegistrationService();
    try {
      service.activate(service.retrieveByEmail(email));
      reload();
    } catch (final Exception e) {
      BeanHelper.error("Error Deleting registration");
      LOGGER.error("Error Deleting registration", e);
    }
  }

  /**
   * Add a {@link User} to a {@link UserGroup} and then redirect to the {@link UserGroup} page
   *
   * @param hasgrant
   */
  public String addToGroup() {
    final String email = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap().get("email");
    try {
      final UserService uc = new UserService();
      final User user = uc.retrieve(email, Imeji.adminUser);
      group.getUsers().add(user.getId());
      final UserGroupService c = new UserGroupService();
      c.update(group, getSessionUser());
      FacesContext.getCurrentInstance().getExternalContext().redirect(getNavigation().getApplicationUrl() + "users?group=" + group.getId());
    } catch (final Exception e) {
      BeanHelper.error(e.getMessage());
    }
    return "";
  }

  public String removeFromGroup() {
    final String email = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap().get("email");
    try {
      final UserService uc = new UserService();
      final User user = uc.retrieve(email, Imeji.adminUser);
      group.getUsers().remove(user.getId());
      final UserGroupService c = new UserGroupService();
      c.update(group, getSessionUser());
      FacesContext.getCurrentInstance().getExternalContext().redirect(getNavigation().getApplicationUrl() + "users?group=" + group.getId());
    } catch (final Exception e) {
      BeanHelper.error(e.getMessage());
    }
    return "";
  }

  /**
   * getter
   *
   * @return
   */
  public List<User> getUsers() {
    return users;
  }

  /**
   * setter
   *
   * @param users
   */
  public void setUsers(List<User> users) {
    this.users = users;
  }

  /**
   * @return the group
   */
  public UserGroup getGroup() {
    return group;
  }

  /**
   * @param group the group to set
   */
  public void setGroup(UserGroup group) {
    this.group = group;
  }

  /**
   * @return the query
   */
  public String getQuery() {
    return query;
  }

  /**
   * @param query the query to set
   */
  public void setQuery(String query) {
    this.query = query;
  }

  public List<User> getInactiveUsers() {
    return inactiveUsers;
  }

  /**
   * @return the invitations
   */
  public List<Invitation> getInvitations() {
    return invitations;
  }

  /**
   * @param invitations the invitations to set
   */
  public void setInvitations(List<Invitation> invitations) {
    this.invitations = invitations;
  }

}
