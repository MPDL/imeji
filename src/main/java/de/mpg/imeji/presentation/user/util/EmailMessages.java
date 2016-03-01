/**
 * License: src/main/resources/license/escidoc.license
 */
package de.mpg.imeji.presentation.user.util;

import java.util.Date;

import org.apache.log4j.Logger;

import de.mpg.imeji.logic.util.ObjectHelper;
import de.mpg.imeji.logic.util.UrlHelper;
import de.mpg.imeji.logic.vo.CollectionImeji;
import de.mpg.imeji.logic.vo.Item;
import de.mpg.imeji.logic.vo.User;
import de.mpg.imeji.presentation.beans.ConfigurationBean;
import de.mpg.imeji.presentation.beans.Navigation;
import de.mpg.imeji.presentation.session.SessionBean;
import de.mpg.imeji.presentation.util.BeanHelper;

/**
 * List of text (messages) sent from imeji to users via email
 * 
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public class EmailMessages {

  private static final Logger LOGGER = Logger.getLogger(EmailMessages.class);

  /**
   * Email content when a new Account is sent
   * 
   * @param password
   * @param email
   * @param username
   * @return
   */
  public String getNewAccountMessage(String password, String email, String username) {
    return getEmailOnAccountAction_Body(password, email, username, "email_new_user");
  }

  /**
   * Email content when a new password is sent
   * 
   * @param password
   * @param email
   * @param username
   * @return
   */
  public String getNewPasswordMessage(String password, String email, String username) {
    String msg = "";
    try {
      String name = ((ConfigurationBean) BeanHelper.getApplicationBean(ConfigurationBean.class))
          .getInstanceName();
      return getEmailOnAccountAction_Body(password, email, username, "email_new_password")
          .replace("XXX_INSTANCE_NAME_XXX", name);
    } catch (Exception e) {
      Logger.getLogger(EmailMessages.class).info("Will return empty message, due to some error", e);
      return msg;
    }
  }

  /**
   * Email content when a collection has been shared with the addressee by the sender
   * 
   * @param sender
   * @param dest
   * @param collectionName
   * @param collectionLink
   * @return
   */
  public String getSharedCollectionMessage(String sender, String dest, String collectionName,
      String collectionLink) {
    String message = getBundle("email_shared_collection");
    message = message.replace("XXX_USER_NAME_XXX,", dest).replace("XXX_NAME_XXX", collectionName)
        .replace("XXX_LINK_XXX", collectionLink).replace("XXX_SENDER_NAME_XXX", sender);
    return message;
  }

  /**
   * Email content when an item has been shared with the addressee by the sender
   * 
   * @param sender
   * @param dest
   * @param itemName
   * @param itemLink
   * @return
   */
  public String getSharedItemMessage(String sender, String dest, String itemName, String itemLink) {
    String message = getBundle("email_shared_item");
    message = message.replace("XXX_USER_NAME_XXX,", dest).replace("XXX_NAME_XXX", itemName)
        .replace("XXX_LINK_XXX", itemLink).replace("XXX_SENDER_NAME_XXX", sender);
    return message;
  }

  /**
   * Email content when an album has been shared with the addressee by the sender
   * 
   * @param sender
   * @param dest
   * @param collectionName
   * @param collectionLink
   * @return
   */
  public String getSharedAlbumMessage(String sender, String dest, String collectionName,
      String collectionLink) {
    String message = getBundle("email_shared_album");
    message = message.replace("XXX_USER_NAME_XXX,", dest).replace("XXX_NAME_XXX", collectionName)
        .replace("XXX_LINK_XXX", collectionLink).replace("XXX_SENDER_NAME_XXX", sender);
    return message;
  }

  /**
   * Read the message bundle (for example: messages_en.properties)
   * 
   * @param messageBundle
   * @return
   */
  private String getBundle(String messageBundle) {
    SessionBean session = (SessionBean) BeanHelper.getSessionBean(SessionBean.class);
    return session.getMessage(messageBundle);
  }

  /**
   * Create the content of an email according to the parameters
   * 
   * @param password
   * @param email
   * @param username
   * @param message_bundle
   * @return
   */
  private String getEmailOnAccountAction_Body(String password, String email, String username,
      String message_bundle) {
    Navigation navigation = (Navigation) BeanHelper.getApplicationBean(Navigation.class);
    String userPage = navigation.getApplicationUrl() + "user?id=" + email;
    SessionBean session = (SessionBean) BeanHelper.getSessionBean(SessionBean.class);
    String emailMessage = session.getMessage(message_bundle);
    if ("email_new_user".equals(message_bundle)) {
      emailMessage =
          emailMessage.replace("XXX_LINK_TO_APPLICATION_XXX", navigation.getApplicationUrl());
    }
    emailMessage =
        emailMessage.replace("XXX_USER_NAME_XXX", username).replace("XXX_LOGIN_XXX", email)
            .replace("XXX_PASSWORD_XXX", password).replace("XXX_LINK_TO_USER_PAGE_XXX", userPage)
            .replaceAll("XXX_INSTANCE_NAME_XXX", session.getInstanceName());
    return emailMessage;
  }

  /**
   * Create the subject of the email being send, for either new account or new password
   * 
   * @param newAccount
   * @return
   */
  public String getEmailOnAccountAction_Subject(boolean newAccount) {
    SessionBean session = (SessionBean) BeanHelper.getSessionBean(SessionBean.class);
    String emailsubject = "";
    if (newAccount) {
      emailsubject = session.getMessage("email_new_user_subject");
    } else {
      emailsubject = session.getMessage("email_new_password_subject");
    }
    return emailsubject.replaceAll("XXX_INSTANCE_NAME_XXX", session.getInstanceName());
  }

  /**
   * Create the subject of the registration request email
   *
   * @return
   */
  public String getEmailOnRegistrationRequest_Subject(SessionBean session) {
    return session.getMessage("email_registration_request_subject")
        .replaceAll("XXX_INSTANCE_NAME_XXX", session.getInstanceName());
  }


  /**
   * Create the body of the registration request email
   * 
   * @param to
   * @param password
   * @param contactEmail
   * @param session @return
   * @param navigationUrl
   */
  public String getEmailOnRegistrationRequest_Body(User to, String password, String contactEmail,
      SessionBean session, String navigationUrl) {
    return session.getMessage("email_registration_request_body")
        .replace("XXX_USER_NAME_XXX", to.getPerson().getCompleteName())
        .replace("XXX_LOGIN_XXX", to.getEmail())
        .replace("XXX_USER_PLAIN_TEXT_PASSWORD_XXX", password)
        .replaceAll("XXX_INSTANCE_NAME_XXX", session.getInstanceName())
        .replaceAll("XXX_CONTACT_EMAIL_XXX", contactEmail).replace("XXX_ACTIVATION_LINK_XXX",
            navigationUrl + "?token=" + to.getRegistrationToken() + "&login=" + to.getEmail());
  }

  /**
   * Create the subject of an account activation email
   *
   * @param session
   * @return
   */
  public String getEmailOnAccountActivation_Subject(User u, SessionBean session) {
    return session.getMessage("email_account_activation_subject").replace("XXX_USER_NAME_XXX",
        u.getPerson().getCompleteName());
  }

  public String getEmailOnAccountActivation_Body(User u, SessionBean session) {
    return session.getMessage("email_account_activation_body")
        .replaceAll("XXX_INSTANCE_NAME_XXX", session.getInstanceName())
        .replace("XXX_USER_NAME_XXX", u.getPerson().getCompleteName())
        .replace("XXX_USER_EMAIL_XXX", u.getEmail())
        .replace("XXX_ORGANIZATION_XXX", u.getPerson().getOrganizationString())
        .replace("XXX_TIME_XXX", new Date().toString());
  }


  /**
   * Generate email body for "Send notification email by item download" feature
   *
   * @param to
   * @param actor
   * @param item
   * @param c
   * @param session
   * @return
   */
  public String getEmailOnItemDownload_Body(User to, User actor, Item item, CollectionImeji c,
      SessionBean session) {
    return session.getMessage("email_item_downloaded_body")
        .replace("XXX_USER_NAME_XXX", to.getPerson().getCompleteName())
        .replace("XXX_ITEM_ID_XXX", ObjectHelper.getId(item.getId()))
        .replace("XXX_ITEM_LINK_XXX", item.getId().toString())
        .replace("XXX_COLLECTION_NAME_XXX", c.getMetadata().getTitle())
        .replace("XXX_COLLECTION_LINK_XXX", c.getId().toString())
        .replace("XXX_ACTOR_NAME_XXX",
            (actor != null ? actor.getPerson().getCompleteName() : "non_logged_in_user"))
        .replace("XXX_ACTOR_EMAIL_XXX", (actor != null ? actor.getEmail() : ""))
        .replace("XXX_TIME_XXX", new Date().toString());
  }

  /**
   * Generate email subject for "Send notification email by item download" feature
   * 
   * @param item
   * @param session
   * @return
   */
  public String getEmailOnItemDownload_Subject(Item item, SessionBean session) {
    return session.getMessage("email_item_downloaded_subject").replace("XXX_ITEM_ID_XXX",
        item.getIdString());
  }

  /**
   * Generate email body for "Send notification email by item download" feature
   *
   * @param to
   * @param actor
   * @param itemsDownloaded
   * @param url
   * @param session
   * @return
   */
  public String getEmailOnZipDownload_Body(User to, User actor, String itemsDownloaded, String url,
      SessionBean session) {
    return session.getMessage("email_zip_images_downloaded_body")
        .replace("XXX_USER_NAME_XXX", to.getPerson().getCompleteName())
        .replace("XXX_ACTOR_NAME_XXX",
            (actor != null ? actor.getPerson().getCompleteName() : "non_logged_in_user"))
        .replace("XXX_ACTOR_EMAIL_XXX", (actor != null ? actor.getEmail() : ""))
        .replace("XXX_TIME_XXX", new Date().toString())
        .replace("XXX_ITEMS_DOWNLOADED_XXX", itemsDownloaded)
        .replaceAll("XXX_COLLECTION_XXX", session.getMessage("collection"))
        .replaceAll("XXX_FILTERED_XXX", session.getMessage("filtered"))
        .replaceAll("XXX_ITEMS_COUNT_XXX", session.getMessage("items_count"))
        .replace("XXX_QUERY_URL_XXX", UrlHelper.encodeQuery(url));
  }

  /**
   * Generate email subject for "Send notification email by item download" feature
   * 
   * @param session
   * @return
   */
  public String getEmailOnZipDownload_Subject(SessionBean session) {
    return session.getMessage("email_zip_images_downloaded_subject");
  }
  
  /**
   * Email content when a collection has been shared with the addressee by the sender
   * 
   * @param sender
   * @param dest
   * @param collectionName
   * @param collectionLink
   * @return
   */
  public String getUnshareMessage(String sender, String dest, String title,
      String collectionLink) {
    String message = getBundle("email_unshared_object");
    message = message.replace("XXX_USER_NAME_XXX,", dest).replace("XXX_NAME_XXX", title)
        .replace("XXX_LINK_XXX", collectionLink).replace("XXX_SENDER_NAME_XXX", sender);
    return message;
  }

}
