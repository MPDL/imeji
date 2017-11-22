package de.mpg.imeji.presentation.notification;

import static com.google.common.base.Strings.isNullOrEmpty;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.apache.log4j.Logger;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.config.Imeji;
import de.mpg.imeji.logic.core.collection.CollectionService;
import de.mpg.imeji.logic.export.ExportAbstract;
import de.mpg.imeji.logic.model.CollectionImeji;
import de.mpg.imeji.logic.model.Item;
import de.mpg.imeji.logic.model.User;
import de.mpg.imeji.logic.notification.email.EmailMessages;
import de.mpg.imeji.logic.notification.email.EmailService;
import de.mpg.imeji.logic.security.user.UserService;
import de.mpg.imeji.logic.util.ObjectHelper;
import de.mpg.imeji.logic.util.UrlHelper;
import de.mpg.imeji.presentation.session.BeanHelper;
import de.mpg.imeji.presentation.session.SessionBean;

/**
 * Created by vlad on 13.03.15.
 */
public class NotificationUtils {

  private static final Logger LOGGER = Logger.getLogger(NotificationUtils.class);

  private static EmailMessages msgs = new EmailMessages();
  private static final EmailService emailClient = new EmailService();

  /**
   * Send email notifications to all users which checked "Send notification email by item download"
   * feature for collection of item
   *
   * @param user
   * @param fileItem
   * @param session
   * @throws de.mpg.imeji.exceptions.ImejiException
   * @throws java.io.IOException
   * @throws java.net.URISyntaxException
   */
  public static void notifyByItemDownload(User user, Item fileItem, Locale locale)
      throws ImejiException, IOException, URISyntaxException {
    final UserService uc = new UserService();
    final CollectionService cc = new CollectionService();
    final CollectionImeji c = cc.retrieve(fileItem.getCollection(), Imeji.adminUser);
    for (final User u : uc.searchUsersToBeNotified(user, c)) {
      emailClient.sendMail(u.getEmail(), null,
          EmailMessages.getEmailOnItemDownload_Subject(fileItem, locale),
          EmailMessages.getEmailOnItemDownload_Body(u, user, fileItem, c, locale));
      LOGGER.info("Sent notification email to user: " + u.getPerson().getCompleteName() + "<"
          + u.getEmail() + ">" + " by item " + fileItem.getId() + " download");
    }
  }

  /**
   * Send email notifications to all users which checked "Send notification email by item download"
   * feature for collection of item <br/>
   * Method runs asynchronously
   *
   * @param user
   * @param export
   * @param session
   * @throws de.mpg.imeji.exceptions.ImejiException
   * @throws java.io.IOException
   * @throws java.net.URISyntaxException
   */
  public static void notifyByExport(ExportAbstract export, SessionBean session, String query,
      String col) {
    new Thread(() -> {
      final UserService uc = new UserService();
      final CollectionService cc = new CollectionService();
      final Map<String, String> msgsPerEmail = new HashMap<>();
      final Map<String, User> usersPerEmail = new HashMap<>();
      final String q = !isNullOrEmpty(query) ? "/browse?q=" + query : "";
      for (final Map.Entry<String, Integer> entry : export.getExportedItemsPerCollection()
          .entrySet()) {
        try {
          CollectionImeji c = cc.retrieve(URI.create(entry.getKey()), Imeji.adminUser);
          for (final User u : uc.searchUsersToBeNotified(session.getUser(), c)) {
            final String key = u.getEmail();
            msgsPerEmail.put(key,
                (msgsPerEmail.containsKey(key) ? msgsPerEmail.get(key) + "\r\n" : "")
                    + "XXX_COLLECTION_XXX URI" + (isNullOrEmpty(q) ? ": " : " (XXX_FILTERED_XXX): ")
                    + UrlHelper.encodeQuery(Imeji.PROPERTIES.getApplicationURL() + "collection/"
                        + ObjectHelper.getId(URI.create(entry.getKey())) + q)
                    + ", XXX_ITEMS_COUNT_XXX: " + entry.getValue().intValue());
            usersPerEmail.put(key, u);
          }
        } catch (ImejiException e) {
          LOGGER.error("Error retrieving collection", e);
        }
      }
      final String url = reconstructQueryUrl(export, session, query, col);
      for (final Map.Entry<String, String> entry : msgsPerEmail.entrySet()) {
        final User u = usersPerEmail.get(entry.getKey());
        try {
          emailClient.sendMail(u.getEmail(), null,
              EmailMessages.getEmailOnZipDownload_Subject(Locale.ENGLISH),
              EmailMessages.getEmailOnZipDownload_Body(u, session.getUser(), entry.getValue(), url,
                  Locale.ENGLISH));
        } catch (ImejiException e) {
          LOGGER.error("Error sending Email", e);
        }
        LOGGER.info("Sent notification email to user: " + u.getPerson().getCompleteName() + "<"
            + u.getEmail() + ">;" + " zip download query: <" + url + ">; message: <"
            + entry.getValue().replaceAll("[\\r\\n]]", ";") + ">");
      }
    }).start();
  }

  /**
   * Reconstructs Query Url on hand of request parameters saved in export instance
   *
   * @param export
   * @param session
   * @return
   */
  private static String reconstructQueryUrl(ExportAbstract export, SessionBean session,
      String query, String col) {
    String q = "?q=", path = "browse";
    if (!isNullOrEmpty(query)) {
      q += query;
    }
    if (!isNullOrEmpty(col)) {
      path = "collection/" + col;
    }
    return session.getApplicationUrl() + path + q;
  }


  /**
   * Send account activation email to current admin
   */
  public static void sendActivationNotification(User user, Locale locale, boolean invitation) {
    // EmailClient emailClient = new EmailClient();
    // EmailMessages emailMessages = new EmailMessages();
    try {
      // send to support
      emailClient.sendMail(Imeji.CONFIG.getContactEmail(), null,
          EmailMessages.getEmailOnAccountActivation_Subject(user, locale),
          EmailMessages.getEmailOnAccountActivation_Body(user, locale));
    } catch (final Exception e) {
      BeanHelper.info(Imeji.RESOURCE_BUNDLE.getMessage("error", locale)
          + ": Account activation email not sent");
      LOGGER.info("Error sending account activation email", e);
    }
  }


}
