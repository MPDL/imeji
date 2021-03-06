package de.mpg.imeji.presentation.user;

import java.io.IOException;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.exceptions.ImejiExceptionWithUserMessage;
import de.mpg.imeji.exceptions.UnprocessableError;
import de.mpg.imeji.logic.config.Imeji;
import de.mpg.imeji.logic.model.Organization;
import de.mpg.imeji.logic.model.User;
import de.mpg.imeji.logic.model.factory.ImejiFactory;
import de.mpg.imeji.logic.notification.email.EmailMessages;
import de.mpg.imeji.logic.notification.email.EmailService;
import de.mpg.imeji.logic.security.authorization.util.PasswordGenerator;
import de.mpg.imeji.logic.security.user.UserService;
import de.mpg.imeji.logic.security.user.UserService.USER_TYPE;
import de.mpg.imeji.logic.security.user.pwdreset.PasswordResetController;
import de.mpg.imeji.logic.security.user.util.QuotaUtil;
import de.mpg.imeji.logic.util.StringHelper;
import de.mpg.imeji.presentation.beans.SuperBean;
import de.mpg.imeji.presentation.session.BeanHelper;

/**
 * Java Bean for the Create new user page
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
@ManagedBean(name = "UserCreationBean")
@ViewScoped
public class UserCreationBean extends SuperBean {
  private static final long serialVersionUID = 7704653755005606808L;
  private User user;
  private boolean sendEmail = false;
  private static final Logger LOGGER = LogManager.getLogger(UserCreationBean.class);
  private boolean allowedToCreateCollection = true;
  private QuotaUICompoment quota;

  /**
   * Construct new bean
   */
  public UserCreationBean() {
    super();
    this.setUser(new User());
    user.getPerson().getOrganizations().add(new Organization());
  }

  @PostConstruct
  public void init() {
    quota = new QuotaUICompoment(user, getLocale());
  }

  /**
   * Method called when user create a new user
   *
   * @return
   * @throws Exception
   */
  public String create() {
    try {
      final User user = createNewUser();
      if (sendEmail) {
        String token = new PasswordResetController().generateResetToken(user);
        sendNewAccountEmail(user, token);
      }
      BeanHelper.info(Imeji.RESOURCE_BUNDLE.getMessage("success_user_create", getLocale()));
      reloadUserPage();
    } catch (final ImejiExceptionWithUserMessage exceptionWithMessage) {
      String userMessage = "Error creating user: " + exceptionWithMessage.getUserMessage(getLocale());
      BeanHelper.error(userMessage);
      if (exceptionWithMessage.getMessage() != null) {
        LOGGER.error(exceptionWithMessage.getMessage(), exceptionWithMessage);
      } else {
        LOGGER.error(userMessage, exceptionWithMessage);
      }
    } catch (final UnprocessableError e) {
      BeanHelper.error(e, getLocale());
      LOGGER.error("Error creating user", e);
    } catch (final Exception e) {
      LOGGER.error("Error creating user:", e);
      BeanHelper.error(Imeji.RESOURCE_BUNDLE.getMessage(e.getMessage(), getLocale()));
    }
    return "";
  }

  /**
   * Create a new {@link User}
   *
   * @throws Exception
   */
  private User createNewUser() throws ImejiException {
    final UserService uc = new UserService();
    final PasswordGenerator generator = new PasswordGenerator();
    final String password = generator.generatePassword();
    user.setEncryptedPassword(StringHelper.md5(password));
    user.setQuota(QuotaUtil.getQuotaInBytes(quota.getQuota()));
    user.setEmail(user.getEmail().trim());
    return uc.create(user, allowedToCreateCollection ? USER_TYPE.DEFAULT : USER_TYPE.RESTRICTED);
  }

  /**
   * Send an email to the current {@link User}
   *
   * @param password
   */
  public void sendNewAccountEmail(User user, String token) {
    final EmailService emailClient = new EmailService();
    String url = getNavigation().getPasswordReserUrl() + "?token=" + token;
    try {
      emailClient.sendMail(getUser().getEmail(), Imeji.CONFIG.getEmailServerSender(),
          EmailMessages.getEmailToCreatedUser_Subject(getLocale()),
          EmailMessages.getEmailToCreatedUser_Body(getLocale(), user.getPerson().getFirstnameLastname(), url));
    } catch (final Exception e) {
      LOGGER.error("Error sending email", e);
      BeanHelper.error("Error: Email not sent");
    }
  }

  /**
   * Add a new empty organization
   *
   * @param index
   */
  public void addOrganization(int index) {
    ((List<Organization>) this.user.getPerson().getOrganizations()).add(index, ImejiFactory.newOrganization());
  }

  /**
   * Remove an nth organization
   *
   * @param index
   */
  public void removeOrganization(int index) {
    final List<Organization> orgas = (List<Organization>) this.user.getPerson().getOrganizations();
    if (orgas.size() > 1) {
      orgas.remove(index);
    }
  }

  /**
   * setter
   *
   * @param user
   */
  public void setUser(User user) {
    this.user = user;
  }

  /**
   * getter
   *
   * @return
   */
  public User getUser() {
    return user;
  }

  /**
   * getter - True if the selectbox "send email to user" has been selected
   *
   * @return
   */
  public boolean isSendEmail() {
    return sendEmail;
  }

  /**
   * setter
   *
   * @param sendEmail
   */
  public void setSendEmail(boolean sendEmail) {
    this.sendEmail = sendEmail;
  }

  private void reloadUserPage() {
    try {
      redirect(getNavigation().getUserUrl() + "?email=\"" + UTF8(user.getEmail()) + "\"");
    } catch (final IOException e) {
      LogManager.getLogger(UserBean.class).info("Some reloadPage exception", e);
    }
  }

  public QuotaUICompoment getQuota() {
    return quota;
  }

  public void setQuota(QuotaUICompoment quota) {
    this.quota = quota;
  }

  public boolean isAllowedToCreateCollection() {
    return allowedToCreateCollection;
  }

  public void setAllowedToCreateCollection(boolean allowedToCreateCollection) {
    this.allowedToCreateCollection = allowedToCreateCollection;
  }
}
