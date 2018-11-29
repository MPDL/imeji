package de.mpg.imeji.logic.notification.email;

import java.util.Date;
import java.util.Locale;

import org.apache.logging.log4j.LogManager;

import de.mpg.imeji.logic.config.Imeji;
import de.mpg.imeji.logic.model.CollectionImeji;
import de.mpg.imeji.logic.model.Item;
import de.mpg.imeji.logic.model.User;
import de.mpg.imeji.logic.security.authorization.util.SecurityUtil;
import de.mpg.imeji.logic.util.ObjectHelper;
import de.mpg.imeji.logic.util.UrlHelper;
import de.mpg.imeji.presentation.navigation.Navigation;

/**
 * List of text (messages) sent from imeji to users via email
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public class EmailMessages {

  /**
   * Denotes whether a text for sending an email is a subject text or a message text
   * 
   * @author breddin
   *
   */
  private static enum TextType {
    SUBJECT,
    MESSAGE
  }

  // ---------------------------------------------------------------------------------------
  // --- Section File Access --------------------------------
  // ---------------------------------------------------------------------------------------

  /**
   * Read a message text from an .xml file in the file system (for example emailMessages_en.xml)
   * 
   * @param identifier the identifier of the text (i.e. )
   * @param textType is the text a email subject or an email message
   * @param locale the user's language
   * @return
   */
  private static String getContent(String identifier, TextType textType, Locale locale) {

    switch (textType) {
      case SUBJECT:
        return Imeji.EMAIL_CONFIG.getMessageSubject(identifier, locale);
      case MESSAGE:
        return Imeji.EMAIL_CONFIG.getMessageBody(identifier, locale);
      default:
        return "";
    }
  }

  /**
   * Reads a text (GUI message) from a .properties file in the .war distribution (for example:
   * messages_en.properties)
   * 
   * @param identifier
   * @param locale
   * @return
   */
  private static String getBundle(String identifier, Locale locale) {
    return Imeji.RESOURCE_BUNDLE.getMessage(identifier, locale);
  }

  // -------------------------------------------------------------------------------
  // --- SECTION: Public functions for constructing email messages -------------
  // -------------------------------------------------------------------------------

  // ------------------- GUI
  // -----------------------------------------------------------

  // function probably not used any more
  /**
   * Get system message that indicates successful removal of a collection
   * 
   * @param collectionName
   * @param locale
   * @return
   */
  public static String getSuccessCollectionDeleteMessage(String collectionName, Locale locale) {
    return getBundle("success_collection_delete", locale).replace("XXX_collectionName_XXX", collectionName);
  }

  // -------------- Replacements of variables ------------------------------

  public static String replaceInstanceNameVariable(String textWithVariable, String instanceName) {
    return textWithVariable.replaceAll("XXX_INSTANCE_NAME_XXX", instanceName);
  }

  // ----------------- EMail texts
  // -------------------------------------------------

  // --------------------- UsersBean: account activation
  // ------------------------------------------------------------

  /**
   * Email content when a new password is sent
   *
   * @param password
   * @param email
   * @param username
   * @return
   */
  public static String getNewPasswordMessage(String password, String email, String username, Locale locale) {
    final String msg = "";
    try {
      final String instanceName = Imeji.CONFIG.getInstanceName();
      return getEmailOnAccountAction_Body(password, email, username, "email_new_password", TextType.MESSAGE, locale)
          .replace("XXX_INSTANCE_NAME_XXX", instanceName);
    } catch (final Exception e) {
      LogManager.getLogger(EmailMessages.class).info("Will return empty message, due to some error", e);
      return msg;
    }
  }

  /**
   * Create the content of an email according to the parameters
   *
   * @param password
   * @param email
   * @param username
   * @param identifier
   * @return
   */
  private static String getEmailOnAccountAction_Body(String password, String email, String username, String identifier, TextType textType,
      Locale locale) {

    final String userPage = Imeji.PROPERTIES.getApplicationURL() + "user?email=" + email;
    String emailMessage = getContent(identifier, textType, locale);

    if ("email_new_user".equals(identifier)) {
      emailMessage = emailMessage.replace("XXX_LINK_TO_APPLICATION_XXX", Imeji.PROPERTIES.getApplicationURL());
    }
    emailMessage = emailMessage.replace("XXX_USER_NAME_XXX", username).replace("XXX_LOGIN_XXX", email).replace("XXX_PASSWORD_XXX", password)
        .replace("XXX_LINK_TO_USER_PAGE_XXX", userPage).replaceAll("XXX_INSTANCE_NAME_XXX", Imeji.CONFIG.getInstanceName());
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
      emailsubject = getContent("email_new_user", TextType.SUBJECT, locale);
    } else {
      emailsubject = getContent("email_new_password", TextType.SUBJECT, locale);
    }
    return emailsubject.replaceAll("XXX_INSTANCE_NAME_XXX", Imeji.CONFIG.getInstanceName());
  }

  // ------------------------ ShareBean via ShareEmailMessage: user shares
  // collection with other user -------------------

  /**
   * Email content when a collection has been shared with the addressee by the sender
   *
   * @param sender
   * @param dest
   * @param collectionName
   * @param collectionLink
   * @return
   */
  public static String getSharedCollectionMessage(String sender, String dest, String collectionName, String collectionLink, Locale locale) {
    String message = getContent("email_shared_collection", TextType.MESSAGE, locale);
    message = message.replace("XXX_USER_NAME_XXX", dest).replace("XXX_NAME_XXX", collectionName).replace("XXX_LINK_XXX", collectionLink)
        .replace("XXX_SENDER_NAME_XXX", sender);
    return message;
  }

  // message text for subject exists but is not used in project

  // ------------------------------ UserCreationBean: user account created
  // -----------------------------------

  /**
   * 
   * @param locale
   * @return
   */
  public static String getEmailToCreatedUser_Subject(Locale locale) {
    return getContent("email_new_user", TextType.SUBJECT, locale).replaceAll("XXX_INSTANCE_NAME_XXX", Imeji.CONFIG.getInstanceName());
  }

  /**
   * 
   * @param locale
   * @return
   */
  public static String getEmailToCreatedUser_Body(Locale locale, String userName, String linkToSetPassword) {
    return getContent("email_new_user", TextType.MESSAGE, locale).replaceAll("XXX_INSTANCE_NAME_XXX", Imeji.CONFIG.getInstanceName())
        .replaceAll("XXX_USER_NAME_XXX", userName).replaceAll("XXX_SET_PASSWORD_LINK_XXX", linkToSetPassword);
  }

  // -------------------------------- RegistrationBean: user can now activate
  // her/his account -------------------------

  /**
   * Create the body of the registration request email
   *
   * @param to
   * @param password
   * @param contactEmail
   * @param session
   * @return
   * @param navigationUrl
   */
  public static String getEmailOnRegistrationRequest_Body(User to, String url, String contactEmail, String expirationDate, Locale locale,
      String navigationUrl) {
    return getContent("email_registration_request", TextType.MESSAGE, locale)
        .replace("XXX_USER_NAME_XXX", to.getPerson().getFirstnameLastname())
        .replaceAll("XXX_INSTANCE_NAME_XXX", Imeji.CONFIG.getInstanceName()).replaceAll("XXX_CONTACT_EMAIL_XXX", contactEmail)
        .replace("XXX_ACTIVATION_LINK_XXX", url).replaceAll("XXX_EXPIRATION_DATE_XXX", expirationDate);
  }

  /**
   * Create the subject of the registration request email
   *
   * @return
   */
  public static String getEmailOnRegistrationRequest_Subject(Locale locale) {
    return getContent("email_registration_request", TextType.SUBJECT, locale).replaceAll("XXX_INSTANCE_NAME_XXX",
        Imeji.CONFIG.getInstanceName());
  }

  // ------ NotificationUtils, RegistrationBean: Mail to support team that an
  // account has been activated by user ------------------------

  /**
   * Create the subject of an account activation email
   *
   * @param session
   * @return
   */
  public static String getEmailOnAccountActivation_Subject(User u, Locale locale) {
    return getContent("email_account_activation", TextType.SUBJECT, locale).replace("XXX_USER_NAME_XXX",
        u.getPerson().getFirstnameLastname());
  }

  public static String getEmailOnAccountActivation_Body(User u, Locale locale) {
    return getContent("email_account_activation", TextType.MESSAGE, locale)
        .replaceAll("XXX_INSTANCE_NAME_XXX", Imeji.CONFIG.getInstanceName())
        .replace("XXX_USER_NAME_XXX", u.getPerson().getFirstnameLastname()).replace("XXX_USER_EMAIL_XXX", u.getEmail())
        .replace("XXX_ORGANIZATION_XXX", u.getPerson().getOrganizationString()).replace("XXX_TIME_XXX", new Date().toString())
        .replace("XXX_CREATE_COLLECTIONS_XXX", Boolean.toString(SecurityUtil.authorization().hasCreateCollectionGrant(u)));
  }

  // ------------------------- An item of a collection has been downloaded
  // --------------------
  // currently not used in project
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
  public static String getEmailOnItemDownload_Body(User to, User actor, Item item, CollectionImeji c, Locale locale) {
    return getContent("email_item_downloaded", TextType.MESSAGE, locale).replace("XXX_USER_NAME_XXX", to.getPerson().getFirstnameLastname())
        .replace("XXX_ITEM_ID_XXX", ObjectHelper.getId(item.getId()))
        .replace("XXX_ITEM_LINK_XXX", Imeji.PROPERTIES.getApplicationURL() + "item/" + ObjectHelper.getId(item.getId()))
        .replace("XXX_COLLECTION_NAME_XXX", c.getTitle())
        .replace("XXX_COLLECTION_LINK_XXX", Imeji.PROPERTIES.getApplicationURL() + "collection/" + ObjectHelper.getId(c.getId()))
        .replace("XXX_ACTOR_NAME_XXX", (actor != null ? actor.getPerson().getCompleteName() : "non_logged_in_user"))
        .replace("XXX_ACTOR_EMAIL_XXX", (actor != null ? actor.getEmail() : "")).replace("XXX_TIME_XXX", new Date().toString());
  }

  /**
   * Generate email subject for "Send notification email by item download" feature
   *
   * @param item
   * @param session
   * @return
   */
  public static String getEmailOnItemDownload_Subject(Item item, Locale locale) {
    return getContent("email_item_downloaded", TextType.SUBJECT, locale).replace("XXX_ITEM_ID_XXX", item.getIdString());
  }

  // ------------------- Imeji items have been downloaded in zip format
  // -----------------------
  // currently not used in project
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
  public static String getEmailOnZipDownload_Body(User to, User actor, String itemsDownloaded, String url, Locale locale) {
    return getContent("email_zip_images_downloaded", TextType.MESSAGE, locale)
        .replace("XXX_USER_NAME_XXX", to.getPerson().getFirstnameLastname())
        .replace("XXX_ACTOR_NAME_XXX", (actor != null ? actor.getPerson().getCompleteName() : "non_logged_in_user"))
        .replace("XXX_ACTOR_EMAIL_XXX", (actor != null ? actor.getEmail() : "")).replace("XXX_TIME_XXX", new Date().toString())
        .replace("XXX_ITEMS_DOWNLOADED_XXX", itemsDownloaded).replaceAll("XXX_COLLECTION_XXX", getBundle("collection", locale))
        .replaceAll("XXX_FILTERED_XXX", getBundle("filtered", locale)).replaceAll("XXX_ITEMS_COUNT_XXX", getBundle("items_count", locale))
        .replace("XXX_QUERY_URL_XXX", UrlHelper.encodeQuery(url));
  }

  /**
   * Generate email subject for "Send notification email by item download" feature
   *
   * @param session
   * @return
   */
  public static String getEmailOnZipDownload_Subject(Locale locale) {
    return getContent("email_zip_images_downloaded", TextType.SUBJECT, locale);
  }

  // ----------------- ShareBean: items have been unshared
  // ----------------------------------
  // currently not used
  /**
   * Email content when a collection has been shared with the addressee by the sender
   *
   * @param sender
   * @param dest
   * @param collectionName
   * @param collectionLink
   * @return
   */
  public static String getUnshareMessage(String sender, String dest, String title, String collectionLink, Locale locale) {
    String message = getContent("email_unshared_object", TextType.MESSAGE, locale);
    message = message.replace("XXX_USER_NAME_XXX", dest).replace("XXX_NAME_XXX", title).replace("XXX_LINK_XXX", collectionLink)
        .replace("XXX_SENDER_NAME_XXX", sender);
    return message;
  }

  // ---------- PasswordChangeBean: Answer to password reset request
  // -----------------------------------------------

  public static String getResetRequestEmailSubject(Locale locale) {
    return getContent("email_password_reset", TextType.SUBJECT, locale).replaceAll("XXX_INSTANCE_NAME_XXX", Imeji.CONFIG.getInstanceName());
  }

  public static String getResetRequestEmailBody(String url, User user, String expirationDate, Locale locale) {
    return getContent("email_password_reset", TextType.MESSAGE, locale)
        .replace("XXX_USER_NAME_XXX", user.getPerson().getFirstnameLastname())
        .replaceAll("XXX_INSTANCE_NAME_XXX", Imeji.CONFIG.getInstanceName())
        .replaceAll("XXX_CONTACT_EMAIL_XXX", Imeji.CONFIG.getContactEmail()).replaceAll("XXX_PWD_RESET_LINK_XXX", url)
        .replaceAll("XXX_EXPIRATION_DATE_XXX", expirationDate);
  }

  // ---------- PasswordChangeBean: Send new password to user after password reset
  // ----------------

  public static String getResetConfirmEmailSubject(Locale locale) {
    return getContent("email_new_password", TextType.SUBJECT, locale).replaceAll("XXX_INSTANCE_NAME_XXX", Imeji.CONFIG.getInstanceName());
  }

  public static String getResetConfirmEmailBody(User user, Locale locale) {
    return getContent("email_new_password", TextType.MESSAGE, locale).replaceAll("XXX_INSTANCE_NAME_XXX", Imeji.CONFIG.getInstanceName())
        .replace("XXX_USER_NAME_XXX", user.getPerson().getFirstnameLastname());
  }

  // ----------- SubscriptionAggregation: Send list of changes (in subscribed
  // collections) to user --------------------

  public static String getSubscriptionEmailSubject(Locale locale) {
    return getContent("email_subscribtion", TextType.SUBJECT, locale).replace("XXX_INSTANCE_NAME_XXX", Imeji.CONFIG.getInstanceName());
  }

  public static String getSubscriptionEmailBody(User user, String collectionSummaries, Locale locale) {

    String body =
        getContent("email_subscribtion", TextType.MESSAGE, locale).replaceAll("XXX_INSTANCE_NAME_XXX", Imeji.CONFIG.getInstanceName())
            .replaceAll("XXX_USER_NAME_XXX", user.getPerson().getFirstnameLastname()).replace("XXX_TEXT_XXX", collectionSummaries);
    return body;
  }

  // --------------- ShareInput: invite others to see shared items on imeji (and
  // register) ---------------------------

  /**
   * @return the invitation message
   */
  public static String getInvitationEmailBody(String email, Locale locale, User user, String instanceName) {
    final Navigation nav = new Navigation();
    return getContent("email_invitation", TextType.MESSAGE, locale).replace("XXX_SENDER_NAME_XXX", user.getPerson().getCompleteName())
        .replace("XXX_INSTANCE_NAME_XXX", instanceName).replace("XXX_REGISTRATION_LINK_XXX", nav.getRegistrationUrl() + "?login=" + email)
        .replace("XXX_SENDER_EMAIL", user.getEmail());

  }

  public static String getInvitationEmailSubject(User user, Locale locale, String instanceName) {
    return getContent("email_invitation", TextType.SUBJECT, locale).replace("XXX_SENDER_NAME_XXX", user.getPerson().getCompleteName())
        .replace("XXX_INSTANCE_NAME_XXX", instanceName);
  }

}
