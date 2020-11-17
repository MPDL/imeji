package de.mpg.imeji.presentation.security;

import java.io.IOException;
import java.util.Calendar;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.RequestScoped;
import javax.faces.bean.ViewScoped;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.config.Imeji;
import de.mpg.imeji.logic.model.User;
import de.mpg.imeji.logic.notification.email.EmailMessages;
import de.mpg.imeji.logic.notification.email.EmailService;
import de.mpg.imeji.logic.security.user.UserService;
import de.mpg.imeji.logic.security.user.pwdreset.PasswordResetController;
import de.mpg.imeji.logic.util.StringHelper;
import de.mpg.imeji.logic.util.UrlHelper;
import de.mpg.imeji.presentation.beans.SuperBean;
import de.mpg.imeji.presentation.session.BeanHelper;
import de.mpg.imeji.presentation.session.SessionBean;
import de.mpg.imeji.util.DateHelper;

@ManagedBean(name = "ForgotPasswordBean")
@RequestScoped
public class ForgotPasswordBean extends SuperBean {
  private static final long serialVersionUID = 2461713268070189067L;

  private String resetEmail;

  private final PasswordResetController passwordresetService = new PasswordResetController();

  /**
   * Send Email to reset password
   * 
   * @throws ImejiException
   * @throws IOException
   */
  public void sendResetEmail() throws ImejiException, IOException {

    if (!EmailService.isValidEmail(resetEmail)) {
      BeanHelper.error(resetEmail + " " + Imeji.RESOURCE_BUNDLE.getLabel("reset_invalid_email", getLocale()));
      redirect(getNavigation().getLoginUrl());
    } else {
      User user;
      try {
        user = new UserService().retrieve(resetEmail, Imeji.adminUser);
      } catch (ImejiException e) {
        BeanHelper.info(Imeji.RESOURCE_BUNDLE.getMessage("email_password_reset_sent", getLocale()));
        redirect(getNavigation().getLoginUrl());
        return;
      }
      String url = getNavigation().getApplicationUrl() + "pwdreset?token=" + passwordresetService.generateResetToken(user);
      Calendar expirationDate = DateHelper.getCurrentDate();
      expirationDate.add(Calendar.DAY_OF_MONTH, Integer.valueOf(Imeji.CONFIG.getRegistrationTokenExpiry()));

      new EmailService().sendMail(resetEmail, null, EmailMessages.getResetRequestEmailSubject(getLocale()),
          EmailMessages.getResetRequestEmailBody(url, user, DateHelper.printDate(expirationDate), getLocale()));

      BeanHelper.info(Imeji.RESOURCE_BUNDLE.getMessage("email_password_reset_sent", getLocale()));
      redirect(getNavigation().getLoginUrl());
    }
  }



  public String getResetEmail() {
    return resetEmail;
  }

  public void setResetEmail(String resetEmail) {
    this.resetEmail = resetEmail;
  }


}
