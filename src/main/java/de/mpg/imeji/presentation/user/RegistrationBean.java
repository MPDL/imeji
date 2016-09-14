package de.mpg.imeji.presentation.user;

import static de.mpg.imeji.logic.util.StringHelper.isNullOrEmptyTrim;

import java.io.IOException;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;

import org.apache.log4j.Logger;

import de.mpg.imeji.exceptions.AlreadyExistsException;
import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.exceptions.UnprocessableError;
import de.mpg.imeji.logic.Imeji;
import de.mpg.imeji.logic.registration.Registration;
import de.mpg.imeji.logic.registration.RegistrationBusinessController;
import de.mpg.imeji.logic.user.collaboration.email.EmailMessages;
import de.mpg.imeji.logic.user.collaboration.email.EmailService;
import de.mpg.imeji.logic.user.collaboration.invitation.InvitationBusinessController;
import de.mpg.imeji.logic.util.StringHelper;
import de.mpg.imeji.logic.util.UrlHelper;
import de.mpg.imeji.logic.vo.User;
import de.mpg.imeji.logic.vo.util.ImejiFactory;
import de.mpg.imeji.presentation.beans.SuperBean;
import de.mpg.imeji.presentation.notification.NotificationUtils;
import de.mpg.imeji.presentation.session.BeanHelper;

/**
 * Bean for registration workflow
 *
 * @author makarenko (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
@ManagedBean(name = "RegistrationBean")
@ViewScoped
public class RegistrationBean extends SuperBean {
  private static final long serialVersionUID = -993770106648303808L;
  private static final Logger LOGGER = Logger.getLogger(RegistrationBean.class);
  private final RegistrationBusinessController registrationBC =
      new RegistrationBusinessController();

  private User user = new User();

  private String token = null;
  private boolean registration_submitted = false;

  private boolean registration_success = false;
  private boolean activation_submitted = false;
  private boolean activation_success = false;
  private String activation_message;
  private String redirect;
  private boolean isInvited = false;
  private boolean termsAccepted = StringHelper.isNullOrEmptyTrim(Imeji.CONFIG.getTermsOfUse())
      && StringHelper.isNullOrEmptyTrim(Imeji.CONFIG.getTermsOfUseUrl());
  @ManagedProperty(value = "#{LoginBean}")
  private LoginBean loginBean;

  @PostConstruct
  public void init() {
    this.user.setPerson(ImejiFactory.newPerson());;
    // get token etc
    this.token = UrlHelper.getParameterValue("token");
    this.user.setEmail(UrlHelper.getParameterValue("login"));
    this.isInvited = checkInvitations();
    if (getSessionUser() == null) {
      if (!isNullOrEmptyTrim(token)) {
        // if user is not yet activated, activate it
        activate();
      }
    } else {
      // if is logged in, redirect to home page
      try {
        redirect(getNavigation().getHomeUrl());
      } catch (IOException e) {
        BeanHelper.error(e.getLocalizedMessage());
        LOGGER.error("Error redirect", e);
      }
    }
  }

  public void register() {
    if (!termsAccepted) {
      BeanHelper.error(Imeji.RESOURCE_BUNDLE.getMessage("error_accept_terms_of_use", getLocale()));
      return;
    }
    Registration registration = null;
    try {
      activation_submitted = false;
      registration_submitted = true;
      registration = registrationBC.register(user);
      registration_success = true;
    } catch (UnprocessableError e) {
      BeanHelper.error(e, getLocale());
      LOGGER.error("error registering user", e);
    } catch (AlreadyExistsException e) {
      BeanHelper.error(Imeji.RESOURCE_BUNDLE.getMessage("error_user_already_exists", getLocale()));
      LOGGER.error("error registering user", e);
    } catch (Exception e) {
      BeanHelper
          .error(Imeji.RESOURCE_BUNDLE.getMessage("error_during_user_registration", getLocale()));
      LOGGER.error("error registering user", e);
    }
    if (registration_success) {
      BeanHelper.cleanMessages();
      BeanHelper.info("Sending registration email and new password.");
      sendRegistrationNotification(registration.getToken(), registration.getPassword());
      if (FacesContext.getCurrentInstance().getMessageList().size() > 1) {
        BeanHelper.cleanMessages();
        BeanHelper.info(
            "User account has been registered, but verification email could not be sent! Please contact service administrators!");
      }
    }
  }

  private void activate() {
    try {
      this.activation_submitted = true;
      this.registration_submitted = false;
      this.user = registrationBC.activate(registrationBC.retrieveByToken(token));
      sendActivationNotification();
      this.activation_success = true;
      this.activation_message = Imeji.RESOURCE_BUNDLE.getMessage("activation_success", getLocale());
      loginBean.setLogin(user.getEmail());
      this.redirect = getNavigation().getHomeUrl();
    } catch (ImejiException e) {
      this.activation_success = false;
      this.activation_message = e.getLocalizedMessage();
      LOGGER.error("Error activating user", e);
    }
  }

  /**
   * True if the user registering has been invited
   *
   * @return
   * @throws ImejiException
   */
  private boolean checkInvitations() {
    try {
      return !new InvitationBusinessController().retrieveInvitationOfUser(user.getEmail())
          .isEmpty();
    } catch (ImejiException e) {
      LOGGER.error("Error checking user invitations", e);
      return false;
    }
  }

  /**
   * Send registration email
   */
  private void sendRegistrationNotification(String token, String password) {
    EmailService emailClient = new EmailService();
    try {
      // send to requester
      emailClient.sendMail(getUser().getEmail(), Imeji.CONFIG.getEmailServerSender(),
          EmailMessages.getEmailOnRegistrationRequest_Subject(getLocale()),
          EmailMessages.getEmailOnRegistrationRequest_Body(getUser(), token, password,
              Imeji.CONFIG.getContactEmail(), getLocale(), getNavigation().getRegistrationUrl()));
    } catch (Exception e) {
      LOGGER.error("Error sending email", e);
      BeanHelper.error("Error: Email not sent");
    }
  }

  /**
   * Send account activation email
   */
  private void sendActivationNotification() {
    NotificationUtils.sendActivationNotification(user, getLocale(), isInvited);
  }

  public boolean isRegistration_submitted() {
    return registration_submitted;
  }

  public void setRegistration_submitted(boolean registration_submitted) {
    this.registration_submitted = registration_submitted;
  }

  public User getUser() {
    return user;
  }

  public void setUser(User user) {
    this.user = user;
  }

  public boolean isActivation_submitted() {
    return activation_submitted;
  }

  public void setActivation_submitted(boolean activation_submitted) {
    this.activation_submitted = activation_submitted;
  }

  public boolean isActivation_success() {
    return activation_success;
  }

  public void setActivation_success(boolean activation_success) {
    this.activation_success = activation_success;
  }

  public String getActivation_message() {
    return activation_message;
  }

  public void setActivation_message(String activation_message) {
    this.activation_message = activation_message;
  }

  public boolean isRegistration_success() {
    return registration_success;
  }

  public void setRegistration_success(boolean registration_success) {
    this.registration_success = registration_success;
  }

  public String getRedirect() {
    return redirect;
  }

  public boolean isRegistrationEnabled() {
    return Imeji.CONFIG.isRegistrationEnabled() || isInvited;
  }

  public boolean isTermsAccepted() {
    return termsAccepted;
  }

  public void setTermsAccepted(boolean termsAccepted) {
    this.termsAccepted = termsAccepted;
  }

  public LoginBean getLoginBean() {
    return loginBean;
  }

  public void setLoginBean(LoginBean loginBean) {
    this.loginBean = loginBean;
  }
}
