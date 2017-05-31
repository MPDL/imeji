package de.mpg.imeji.presentation.security;

import java.io.IOException;
import java.util.Calendar;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import org.apache.log4j.Logger;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.exceptions.UnprocessableError;
import de.mpg.imeji.logic.authorization.pwdreset.PasswordResetController;
import de.mpg.imeji.logic.config.Imeji;
import de.mpg.imeji.logic.share.email.EmailService;
import de.mpg.imeji.logic.user.UserService;
import de.mpg.imeji.logic.vo.User;
import de.mpg.imeji.presentation.beans.SuperBean;
import de.mpg.imeji.presentation.navigation.Navigation;
import de.mpg.imeji.presentation.session.BeanHelper;
import de.mpg.imeji.presentation.user.UserBean;
import de.mpg.imeji.util.DateHelper;

@ManagedBean(name = "PasswordResetBean")
@ViewScoped
public class PasswordResetBean extends SuperBean {
  private static final long serialVersionUID = 2461713268070189067L;
  private static final Logger LOGGER = Logger.getLogger(UserBean.class);

  private String newPassword;
  private String repeatedPassword;
  private String resetEmail;
  private String token;

  public PasswordResetBean() {

  }

  @PostConstruct
  public void init() {
    newPassword = null;
    repeatedPassword = null;
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

  public void sendResetEmail() throws ImejiException, IOException {
    User user;
    try {
      user = (new UserService()).retrieve(resetEmail, Imeji.adminUser);
    } catch (ImejiException e) {
      BeanHelper.error(Imeji.RESOURCE_BUNDLE.getMessage("error_user_not_found", getLocale()));
      redirect(getNavigation().getHomeUrl());
      return;
    }
    String url = (new PasswordResetController()).generateResetURL(user);
    Calendar expirationDate = DateHelper.getCurrentDate();
    expirationDate.add(Calendar.DAY_OF_MONTH,
        Integer.valueOf(Imeji.CONFIG.getRegistrationTokenExpiry()));
    (new EmailService()).sendMail(resetEmail, null, getEmailSubject(),
        getEmailBody(url, user, DateHelper.printDate(expirationDate)));
    BeanHelper.info(Imeji.RESOURCE_BUNDLE.getMessage("email_password_reset_sent", getLocale()));
  }


  public void changePassword() throws IOException, UnprocessableError {
    if (token == null || !(new PasswordResetController()).isValidToken(token)) {
      BeanHelper
          .error(Imeji.RESOURCE_BUNDLE.getMessage("error_password_link_invalid", getLocale()));
      redirect(getNavigation().getHomeUrl());
    }
    if (newPassword == null || newPassword.equals("")) {
      BeanHelper.error(Imeji.RESOURCE_BUNDLE.getMessage("error_empty_password", getLocale()));
      reloadPage();
    }
    if (!newPassword.equals(repeatedPassword)) {
      BeanHelper.error(Imeji.RESOURCE_BUNDLE.getMessage("error_user_repeat_password", getLocale()));
      reloadPage();
    }
    (new PasswordResetController()).resetPassword(token, newPassword);
    BeanHelper.info(Imeji.RESOURCE_BUNDLE.getMessage("password_changed", getLocale()));
    redirect(Navigation.applicationUrl);
  }

  private void reloadPage() throws IOException {
    redirect(getNavigation().getHomeUrl() + "/pwdreset?token=" + token);
  }

  private String getEmailSubject() {
    return Imeji.RESOURCE_BUNDLE.getMessage("email_password_reset_subject", getLocale())
        .replaceAll("XXX_INSTANCE_NAME_XXX", Imeji.CONFIG.getInstanceName());
  }

  private String getEmailBody(String url, User user, String expirationDate) {
    return Imeji.RESOURCE_BUNDLE.getMessage("email_password_reset_body", getLocale())
        .replace("XXX_USER_NAME_XXX", user.getPerson().getCompleteName())
        .replaceAll("XXX_INSTANCE_NAME_XXX", Imeji.CONFIG.getInstanceName())
        .replaceAll("XXX_CONTACT_EMAIL_XXX", Imeji.CONFIG.getContactEmail())
        .replaceAll("XXX_PWD_RESET_LINK_XXX", url)
        .replaceAll("XXX_EXPIRATION_DATE_XXX", expirationDate);
  }

}
