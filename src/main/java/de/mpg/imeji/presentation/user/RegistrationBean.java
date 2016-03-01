package de.mpg.imeji.presentation.user;

import static de.mpg.imeji.logic.util.StringHelper.isNullOrEmptyTrim;
import static de.mpg.imeji.presentation.beans.ConfigurationBean.getContactEmailStatic;
import static de.mpg.imeji.presentation.beans.ConfigurationBean.getEmailServerSenderStatic;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;

import org.apache.log4j.Logger;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.Imeji;
import de.mpg.imeji.logic.controller.UserController;
import de.mpg.imeji.logic.notification.NotificationUtils;
import de.mpg.imeji.logic.util.StringHelper;
import de.mpg.imeji.logic.util.UrlHelper;
import de.mpg.imeji.logic.vo.User;
import de.mpg.imeji.presentation.beans.Navigation;
import de.mpg.imeji.presentation.session.SessionBean;
import de.mpg.imeji.presentation.user.util.EmailClient;
import de.mpg.imeji.presentation.user.util.EmailMessages;
import de.mpg.imeji.presentation.user.util.PasswordGenerator;
import de.mpg.imeji.presentation.util.BeanHelper;

/**
 * Bean for registration workflow
 *
 * @author makarenko (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
@ManagedBean(name = "RegistrationBean")
@ViewScoped
public class RegistrationBean {
  private static final Logger LOGGER = Logger.getLogger(RegistrationBean.class);
  private UserController uc = new UserController(Imeji.adminUser);

  private SessionBean sb;
  private Navigation nb;
  private User user = new User();

  private String token = null;
  private boolean registration_submitted = false;

  private boolean registration_success = false;
  private boolean activation_submitted = false;
  private boolean activation_success = false;
  private String activation_message;
  private String redirect;


  @PostConstruct
  public void init() {
    sb = (SessionBean) BeanHelper.getSessionBean(SessionBean.class);
    nb = (Navigation) BeanHelper.getApplicationBean(Navigation.class);
    // get token etc
    this.token = UrlHelper.getParameterValue("token");
    if (sb.getUser() == null) {
      if (!isNullOrEmptyTrim(token)) {
        // if user is not yet activated, activate it
        activate();
      }
    } else
      // if is logged in, redirect to home page
      try {
        FacesContext.getCurrentInstance().getExternalContext().redirect(nb.getHomeUrl());
      } catch (IOException e) {
        BeanHelper.error(e.getLocalizedMessage());
      }

  }

  public void register() {
    String password = "";
    try {
      this.activation_submitted = false;
      this.registration_submitted = true;
      PasswordGenerator generator = new PasswordGenerator();
      password = generator.generatePassword();
      user.setEncryptedPassword(StringHelper.convertToMD5(password));
      user = uc.create(user, UserController.USER_TYPE.INACTIVE);
      this.registration_success = true;
    } catch (Exception e) {
      BeanHelper.cleanMessages();
      BeanHelper.error(sb.getMessage("error_during_user_registration"));
      List<String> listOfErrors = Arrays.asList(e.getMessage().split(";"));
      for (String errorM : listOfErrors) {
        BeanHelper.error(sb.getMessage(errorM));
      }
    }

    if (this.registration_success) {
      BeanHelper.cleanMessages();
      BeanHelper.info("Sending registration email and new password.");
      sendRegistrationNotification(password);
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
      this.user = uc.activate(this.token);
      sendActivationNotification();
      this.activation_success = true;
      this.activation_message = sb.getMessage("activation_success");
      LoginBean loginBean = (LoginBean) BeanHelper.getRequestBean(LoginBean.class);
      loginBean.setLogin(user.getEmail());
      this.redirect = nb.getHomeUrl();
    } catch (ImejiException e) {
      this.activation_success = false;
      this.activation_message = e.getLocalizedMessage();
    }
  }

  /**
   * Send registration email
   */
  private void sendRegistrationNotification(String password) {
    EmailClient emailClient = new EmailClient();
    EmailMessages emailMessages = new EmailMessages();
    try {
      // send to requester
      emailClient.sendMail(getUser().getEmail(), getEmailServerSenderStatic(),
          emailMessages.getEmailOnRegistrationRequest_Subject(sb),
          emailMessages.getEmailOnRegistrationRequest_Body(getUser(), password,
              getContactEmailStatic(), sb, nb.getRegistrationUrl()));
    } catch (Exception e) {
      LOGGER.error("Error sending email", e);
      BeanHelper.error(sb.getMessage("error") + ": Email not sent");
    }
  }

  /**
   * Send account activation email
   */
  private void sendActivationNotification() {
    NotificationUtils.sendActivationNotification(user, sb);
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
}
