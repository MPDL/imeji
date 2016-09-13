/**
 * License: src/main/resources/license/escidoc.license
 */
package de.mpg.imeji.logic.user.collaboration.email;

import java.util.Date;
import java.util.Locale;

import org.apache.log4j.Logger;

import de.mpg.imeji.logic.Imeji;
import de.mpg.imeji.logic.security.util.SecurityUtil;
import de.mpg.imeji.logic.util.UrlHelper;
import de.mpg.imeji.logic.vo.CollectionImeji;
import de.mpg.imeji.logic.vo.Item;
import de.mpg.imeji.logic.vo.User;
import de.mpg.imeji.logic.vo.util.ObjectHelper;

/**
 * List of text (messages) sent from imeji to users via email
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public class EmailMessages {


  /**
   * Email content when a new Account is sent
   *
   * @param password
   * @param email
   * @param username
   * @return
   */
  public static String getNewAccountMessage(String password, String email, String username,
      Locale locale) {
    return getEmailOnAccountAction_Body(password, email, username, "email_new_user", locale);
  }


  public static String getSuccessCollectionDeleteMessage(String collectionName, Locale locale) {
    return getBundle("success_collection_delete", locale).replace("XXX_collectionName_XXX",
        collectionName);
  }

  /**
   * Email content when a new password is sent
   *
   * @param password
   * @param email
   * @param username
   * @return
   */
  public static String getNewPasswordMessage(String password, String email, String username,
      Locale locale) {
    String msg = "";
    try {
      String name = Imeji.CONFIG.getInstanceName();
      return getEmailOnAccountAction_Body(password, email, username, "email_new_password", locale)
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
  public static String getSharedCollectionMessage(String sender, String dest, String collectionName,
      String collectionLink, Locale locale) {
    String message = getBundle("email_shared_collection", locale);
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
  public static String getSharedItemMessage(String sender, String dest, String itemName,
      String itemLink, Locale locale) {
    String message = getBundle("email_shared_item", locale);
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
  public static String getSharedAlbumMessage(String sender, String dest, String collectionName,
      String collectionLink, Locale locale) {
    String message = getBundle("email_shared_album", locale);
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
  private static String getBundle(String messageBundle, Locale locale) {
    return Imeji.RESOURCE_BUNDLE.getMessage(messageBundle, locale);
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
  private static String getEmailOnAccountAction_Body(String password, String email, String username,
      String message_bundle, Locale locale) {
    String userPage = Imeji.PROPERTIES.getApplicationURL() + "user?id=" + email;
    String emailMessage = getBundle(message_bundle, locale);
    if ("email_new_user".equals(message_bundle)) {
      emailMessage =
          emailMessage.replace("XXX_LINK_TO_APPLICATION_XXX", Imeji.PROPERTIES.getApplicationURL());
    }
    emailMessage =
        emailMessage.replace("XXX_USER_NAME_XXX", username).replace("XXX_LOGIN_XXX", email)
            .replace("XXX_PASSWORD_XXX", password).replace("XXX_LINK_TO_USER_PAGE_XXX", userPage)
            .replaceAll("XXX_INSTANCE_NAME_XXX", Imeji.CONFIG.getInstanceName());
    return emailMessage;
  }

  /**
   * Create the subject of the email being send, for either new account or new password
   *
   * @param newAccount
   * @return
   */
  public static String getEmailOnAccountAction_Subject(boolean newAccount, Locale locale) {
    String emailsubject = "";
    if (newAccount) {
      emailsubject = getBundle("email_new_user_subject", locale);
    } else {
      emailsubject = getBundle("email_new_password_subject", locale);
    }
    return emailsubject.replaceAll("XXX_INSTANCE_NAME_XXX", Imeji.CONFIG.getInstanceName());
  }

  /**
   * Create the subject of the registration request email
   *
   * @return
   */
  public static String getEmailOnRegistrationRequest_Subject(Locale locale) {
    return getBundle("email_registration_request_subject", locale)
        .replaceAll("XXX_INSTANCE_NAME_XXX", Imeji.CONFIG.getInstanceName());
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
  public static String getEmailOnRegistrationRequest_Body(User to, String token, String password,
      String contactEmail, Locale locale, String navigationUrl) {
    return getBundle("email_registration_request_body", locale)
        .replace("XXX_USER_NAME_XXX", to.getPerson().getCompleteName())
        .replace("XXX_LOGIN_XXX", to.getEmail())
        .replace("XXX_USER_PLAIN_TEXT_PASSWORD_XXX", password)
        .replaceAll("XXX_INSTANCE_NAME_XXX", Imeji.CONFIG.getInstanceName())
        .replaceAll("XXX_CONTACT_EMAIL_XXX", contactEmail).replace("XXX_ACTIVATION_LINK_XXX",
            navigationUrl + "?token=" + token + "&login=" + to.getEmail());
  }

  /**
   * Create the subject of an account activation email
   *
   * @param session
   * @return
   */
  public static String getEmailOnAccountActivation_Subject(User u, Locale locale) {
    return getBundle("email_account_activation_subject", locale).replace("XXX_USER_NAME_XXX",
        u.getPerson().getCompleteName());
  }

  public static String getEmailOnAccountActivation_Body(User u, Locale locale, boolean invitation) {
    return getBundle("email_account_activation_body", locale)
        .replaceAll("XXX_INSTANCE_NAME_XXX", Imeji.CONFIG.getInstanceName())
        .replace("XXX_USER_NAME_XXX", u.getPerson().getCompleteName())
        .replace("XXX_USER_EMAIL_XXX", u.getEmail())
        .replace("XXX_ORGANIZATION_XXX", u.getPerson().getOrganizationString())
        .replace("XXX_TIME_XXX", new Date().toString())
        .replace("XXX_CREATE_COLLECTIONS_XXX",
            Boolean.toString(SecurityUtil.isAllowedToCreateCollection(u)))
        .replace("XXX_INVITATION_XXX", Boolean.toString(invitation));
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
  public static String getEmailOnItemDownload_Body(User to, User actor, Item item,
      CollectionImeji c, Locale locale) {
    return getBundle("email_item_downloaded_body", locale)
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
  public static String getEmailOnItemDownload_Subject(Item item, Locale locale) {
    return getBundle("email_item_downloaded_subject", locale).replace("XXX_ITEM_ID_XXX",
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
  public static String getEmailOnZipDownload_Body(User to, User actor, String itemsDownloaded,
      String url, Locale locale) {
    return getBundle("email_zip_images_downloaded_body", locale)
        .replace("XXX_USER_NAME_XXX", to.getPerson().getCompleteName())
        .replace("XXX_ACTOR_NAME_XXX",
            (actor != null ? actor.getPerson().getCompleteName() : "non_logged_in_user"))
        .replace("XXX_ACTOR_EMAIL_XXX", (actor != null ? actor.getEmail() : ""))
        .replace("XXX_TIME_XXX", new Date().toString())
        .replace("XXX_ITEMS_DOWNLOADED_XXX", itemsDownloaded)
        .replaceAll("XXX_COLLECTION_XXX", getBundle("collection", locale))
        .replaceAll("XXX_FILTERED_XXX", getBundle("filtered", locale))
        .replaceAll("XXX_ITEMS_COUNT_XXX", getBundle("items_count", locale))
        .replace("XXX_QUERY_URL_XXX", UrlHelper.encodeQuery(url));
  }

  /**
   * Generate email subject for "Send notification email by item download" feature
   *
   * @param session
   * @return
   */
  public static String getEmailOnZipDownload_Subject(Locale locale) {
    return getBundle("email_zip_images_downloaded_subject", locale);
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
  public static String getUnshareMessage(String sender, String dest, String title,
      String collectionLink, Locale locale) {
    String message = getBundle("email_unshared_object", locale);
    message = message.replace("XXX_USER_NAME_XXX,", dest).replace("XXX_NAME_XXX", title)
        .replace("XXX_LINK_XXX", collectionLink).replace("XXX_SENDER_NAME_XXX", sender);
    return message;
  }

}
