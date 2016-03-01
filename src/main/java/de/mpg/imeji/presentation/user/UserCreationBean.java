/**
 * License: src/main/resources/license/escidoc.license
 */
package de.mpg.imeji.presentation.user;

import java.io.IOException;
import java.util.List;

import javax.faces.context.FacesContext;

import org.apache.log4j.Logger;

import de.mpg.imeji.exceptions.NotFoundException;
import de.mpg.imeji.exceptions.UnprocessableError;
import de.mpg.imeji.logic.controller.UserController;
import de.mpg.imeji.logic.controller.UserController.USER_TYPE;
import de.mpg.imeji.logic.util.QuotaUtil;
import de.mpg.imeji.logic.util.StringHelper;
import de.mpg.imeji.logic.vo.Organization;
import de.mpg.imeji.logic.vo.User;
import de.mpg.imeji.presentation.beans.Navigation;
import de.mpg.imeji.presentation.session.SessionBean;
import de.mpg.imeji.presentation.user.util.EmailClient;
import de.mpg.imeji.presentation.user.util.EmailMessages;
import de.mpg.imeji.presentation.user.util.PasswordGenerator;
import de.mpg.imeji.presentation.util.BeanHelper;
import de.mpg.imeji.presentation.util.ImejiFactory;

/**
 * Java Bean for the Create new user page
 * 
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public class UserCreationBean extends QuotaSuperBean {
  private User user;
  private SessionBean sb;
  private boolean sendEmail = false;
  private static final Logger LOGGER = Logger.getLogger(UserCreationBean.class);
  private boolean allowedToCreateCollection = true;


  /**
   * Construct new bean
   */
  public UserCreationBean() {
    super();
    sb = (SessionBean) BeanHelper.getSessionBean(SessionBean.class);
    this.setUser(new User());
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
      BeanHelper.info(sb.getMessage("success_user_create"));
      reloadUserPage();
    } catch (UnprocessableError e) {
      BeanHelper.cleanMessages();
      BeanHelper.error(sb.getMessage("error_during_user_create"));
      for (String errorM : e.getMessages()) {
        BeanHelper.error(sb.getMessage(errorM));
      }
    } catch (Exception e) {
      LOGGER.error("Error creating user:", e);
      BeanHelper.error(sb.getMessage(e.getMessage()));
    }
    return "";
  }

  /**
   * Create a new {@link User}
   * 
   * @throws Exception
   */
  private String createNewUser() throws Exception {
    UserController uc = new UserController(sb.getUser());
    PasswordGenerator generator = new PasswordGenerator();
    String password = generator.generatePassword();
    user.setEncryptedPassword(StringHelper.convertToMD5(password));
    user.setQuota(QuotaUtil.getQuotaInBytes(getQuota()));
    uc.create(user, allowedToCreateCollection ? USER_TYPE.DEFAULT : USER_TYPE.RESTRICTED);
    return password;
  }



  /**
   * True if the {@link User} exists
   * 
   * @return
   * @throws Exception
   */
  public static boolean userAlreadyExists(User user) throws Exception {
    try {
      SessionBean session = (SessionBean) BeanHelper.getSessionBean(SessionBean.class);
      UserController uc = new UserController(session.getUser());
      uc.retrieve(user.getEmail());
      return true;
    } catch (NotFoundException e) {
      LOGGER.info("User not found: " + user.getEmail());
      return false;
    }
  }

  /**
   * Send an email to the current {@link User}
   * 
   * @param password
   */
  public void sendNewAccountEmail(String password) {
    EmailClient emailClient = new EmailClient();
    EmailMessages emailMessages = new EmailMessages();
    try {
      emailClient.sendMail(user.getEmail(), null,
          emailMessages.getEmailOnAccountAction_Subject(true), emailMessages
              .getNewAccountMessage(password, user.getEmail(), user.getPerson().getCompleteName()));
    } catch (Exception e) {
      LOGGER.error("Error sending email", e);
      BeanHelper.error(sb.getMessage("error") + ": Email not sent");
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

  /**
   * @return the allowedToCreateCollection
   */
  public boolean isAllowedToCreateCollection() {
    return allowedToCreateCollection;
  }

  /**
   * @param allowedToCreateCollection the allowedToCreateCollection to set
   */
  public void setAllowedToCreateCollection(boolean allowedToCreateCollection) {
    this.allowedToCreateCollection = allowedToCreateCollection;
  }
  
  
  private void reloadUserPage() {
    try {
      Navigation navigation = (Navigation) BeanHelper.getApplicationBean(Navigation.class);
      FacesContext.getCurrentInstance().getExternalContext().redirect(navigation.getUserUrl() + "?id=" + user.getEmail());
    } catch (IOException e) {
      Logger.getLogger(UserBean.class).info("Some reloadPage exception", e);
    }
  }
 

}


