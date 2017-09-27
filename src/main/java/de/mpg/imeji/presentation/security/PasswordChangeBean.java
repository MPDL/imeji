package de.mpg.imeji.presentation.security;

import java.io.IOException;
import java.util.Calendar;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.ViewScoped;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.authorization.pwdreset.PasswordResetController;
import de.mpg.imeji.logic.config.Imeji;
import de.mpg.imeji.logic.share.email.EmailService;
import de.mpg.imeji.logic.user.UserService;
import de.mpg.imeji.logic.vo.User;
import de.mpg.imeji.presentation.beans.SuperBean;
import de.mpg.imeji.presentation.session.BeanHelper;
import de.mpg.imeji.presentation.session.SessionBean;
import de.mpg.imeji.util.DateHelper;

@ManagedBean(name = "PasswordChangeBean")
@ViewScoped
public class PasswordChangeBean extends SuperBean {
  private static final long serialVersionUID = 2461713268070189067L;
  private String newPassword;
  private String repeatedPassword;
  private String resetEmail;
  private String token;
  private boolean resetFinished;
  private final PasswordResetController passwordresetService = new PasswordResetController();

  @ManagedProperty(value = "#{SessionBean}")
  private SessionBean sessionBean;

  @PostConstruct
  public void init() {
    newPassword = null;
    repeatedPassword = null;
    resetFinished = false;
  }

  /**
   * Send Email to reset password
   * 
   * @throws ImejiException
   * @throws IOException
   */
  public void sendResetEmail() throws ImejiException, IOException {
    if (!EmailService.isValidEmail(resetEmail)) {
      BeanHelper.error(
          resetEmail + " " + Imeji.RESOURCE_BUNDLE.getLabel("reset_invalid_email", getLocale()));
      redirect(getNavigation().getLoginUrl());
    } else {
      User user;
      try {
        user = new UserService().retrieve(resetEmail, Imeji.adminUser);
      } catch (ImejiException e) {
        BeanHelper.error(resetEmail + ": "
            + Imeji.RESOURCE_BUNDLE.getMessage("error_user_not_found", getLocale()));
        redirect(getNavigation().getLoginUrl());
        return;
      }
      String url = getNavigation().getApplicationUrl() + "pwdreset?token="
          + passwordresetService.generateResetToken(user);
      Calendar expirationDate = DateHelper.getCurrentDate();
      expirationDate.add(Calendar.DAY_OF_MONTH,
          Integer.valueOf(Imeji.CONFIG.getRegistrationTokenExpiry()));
      new EmailService().sendMail(resetEmail, null, getResetRequestEmailSubject(),
          getResetRequestEmailBody(url, user, DateHelper.printDate(expirationDate)));
      BeanHelper.info(Imeji.RESOURCE_BUNDLE.getMessage("email_password_reset_sent", getLocale()));
      redirect(getNavigation().getHomeUrl());
    }
  }

  /**
   * Reset the password
   * 
   * @throws IOException
   * @throws ImejiException
   */
  public void resetPassword() throws IOException, ImejiException {
    if (!isValidPassword()) {
      reloadPage();
      return;
    }
    User user;
    if (getSessionUser() != null) {
      user = passwordresetService.resetPassword(getSessionUser(), newPassword);
    } else {
      if (!passwordresetService.isValidToken(token)) {
        BeanHelper
            .error(Imeji.RESOURCE_BUNDLE.getMessage("error_password_link_invalid", getLocale()));
        redirect(getNavigation().getLoginUrl());
        return;
      }
      user = passwordresetService.resetPassword(token, newPassword);
      sessionBean.setUser(user);
      BeanHelper.info(Imeji.RESOURCE_BUNDLE.getMessage("password_changed", getLocale()));
      new EmailService().sendMail(user.getEmail(), null, getResetConfirmEmailSubject(),
          getResetConfirmEmailBody(user));
    }
    resetFinished = true;
    redirect(getNavigation().getHomeUrl());
  }


  /**
   * Stop the password reset if the password is not valid
   * 
   * @throws IOException
   */
  private boolean isValidPassword() throws IOException {
    if (newPassword == null || newPassword.equals("")) {
      BeanHelper.error(Imeji.RESOURCE_BUNDLE.getMessage("error_empty_password", getLocale()));
      return false;
    }
    if (!newPassword.equals(repeatedPassword)) {
      BeanHelper.error(Imeji.RESOURCE_BUNDLE.getMessage("error_user_repeat_password", getLocale()));
      return false;
    }
    return true;
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

  public String getToken() {
    return token;
  }

  public void setToken(String token) {
    this.token = token;
  }

  public String getResetEmail() {
    return resetEmail;
  }

  public void setResetEmail(String resetEmail) {
    this.resetEmail = resetEmail;
  }


  public boolean isResetFinished() {
    return resetFinished;
  }

  public void setResetFinished(boolean resetFinished) {
    this.resetFinished = resetFinished;
  }



  public SessionBean getSessionBean() {
    return sessionBean;
  }

  public void setSessionBean(SessionBean sessionBean) {
    this.sessionBean = sessionBean;
  }



  private void reloadPage() throws IOException {
    redirect(getNavigation().getHomeUrl() + "/pwdreset?token=" + token);
  }

  private String getResetRequestEmailSubject() {
    return Imeji.RESOURCE_BUNDLE.getMessage("email_password_reset_subject", getLocale())
        .replaceAll("XXX_INSTANCE_NAME_XXX", Imeji.CONFIG.getInstanceName());
  }

  private String getResetRequestEmailBody(String url, User user, String expirationDate) {
    return Imeji.RESOURCE_BUNDLE.getMessage("email_password_reset_body", getLocale())
        .replace("XXX_USER_NAME_XXX", user.getPerson().getFirstnameLastname())
        .replaceAll("XXX_INSTANCE_NAME_XXX", Imeji.CONFIG.getInstanceName())
        .replaceAll("XXX_CONTACT_EMAIL_XXX", Imeji.CONFIG.getContactEmail())
        .replaceAll("XXX_PWD_RESET_LINK_XXX", url)
        .replaceAll("XXX_EXPIRATION_DATE_XXX", expirationDate);
  }

  private String getResetConfirmEmailSubject() {
    return Imeji.RESOURCE_BUNDLE.getMessage("email_new_password_subject", getLocale())
        .replaceAll("XXX_INSTANCE_NAME_XXX", Imeji.CONFIG.getInstanceName());
  }

  private String getResetConfirmEmailBody(User user) {
    return Imeji.RESOURCE_BUNDLE.getMessage("email_new_password", getLocale())
        .replaceAll("XXX_INSTANCE_NAME_XXX", Imeji.CONFIG.getInstanceName())
        .replace("XXX_USER_NAME_XXX", user.getPerson().getFirstnameLastname());
  }

}
