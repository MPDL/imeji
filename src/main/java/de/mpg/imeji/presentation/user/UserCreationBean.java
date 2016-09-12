/**
 * License: src/main/resources/license/escidoc.license
 */
package de.mpg.imeji.presentation.user;

import java.io.IOException;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import org.apache.log4j.Logger;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.exceptions.UnprocessableError;
import de.mpg.imeji.logic.Imeji;
import de.mpg.imeji.logic.controller.util.ImejiFactory;
import de.mpg.imeji.logic.security.util.PasswordGenerator;
import de.mpg.imeji.logic.user.collaboration.email.EmailMessages;
import de.mpg.imeji.logic.user.collaboration.email.EmailService;
import de.mpg.imeji.logic.user.controller.UserBusinessController;
import de.mpg.imeji.logic.user.controller.UserBusinessController.USER_TYPE;
import de.mpg.imeji.logic.user.util.QuotaUtil;
import de.mpg.imeji.logic.util.StringHelper;
import de.mpg.imeji.logic.vo.Organization;
import de.mpg.imeji.logic.vo.User;
import de.mpg.imeji.presentation.beans.SuperBean;
import de.mpg.imeji.presentation.util.BeanHelper;

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
  private static final Logger LOGGER = Logger.getLogger(UserCreationBean.class);
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
      String password = createNewUser();
      if (sendEmail) {
        sendNewAccountEmail(password);
      }
      BeanHelper.info(Imeji.RESOURCE_BUNDLE.getMessage("success_user_create", getLocale()));
      reloadUserPage();
    } catch (UnprocessableError e) {
      BeanHelper.error(e, getLocale());
      LOGGER.error("Error creating user", e);
    } catch (Exception e) {
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
  private String createNewUser() throws ImejiException {
    UserBusinessController uc = new UserBusinessController();
    PasswordGenerator generator = new PasswordGenerator();
    String password = generator.generatePassword();
    user.setEncryptedPassword(StringHelper.convertToMD5(password));
    user.setQuota(QuotaUtil.getQuotaInBytes(quota.getQuota()));
    uc.create(user, allowedToCreateCollection ? USER_TYPE.DEFAULT : USER_TYPE.RESTRICTED);
    return password;
  }

  /**
   * Send an email to the current {@link User}
   *
   * @param password
   */
  public void sendNewAccountEmail(String password) {
    EmailService emailClient = new EmailService();
    try {
      emailClient.sendMail(user.getEmail(), null,
          EmailMessages.getEmailOnAccountAction_Subject(true, getLocale()),
          EmailMessages.getNewAccountMessage(password, user.getEmail(),
              user.getPerson().getCompleteName(), getLocale()));
    } catch (Exception e) {
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
      redirect(getNavigation().getUserUrl() + "?id=" + user.getEmail());
    } catch (IOException e) {
      Logger.getLogger(UserBean.class).info("Some reloadPage exception", e);
    }
  }

  public QuotaUICompoment getQuota() {
    return quota;
  }

  public void setQuota(QuotaUICompoment quota) {
    this.quota = quota;
  }
}


