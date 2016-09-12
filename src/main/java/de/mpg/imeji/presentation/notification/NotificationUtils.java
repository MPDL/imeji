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
import de.mpg.imeji.logic.Imeji;
import de.mpg.imeji.logic.controller.resource.CollectionController;
import de.mpg.imeji.logic.export.format.Export;
import de.mpg.imeji.logic.export.format.ZIPExport;
import de.mpg.imeji.logic.user.collaboration.email.EmailMessages;
import de.mpg.imeji.logic.user.collaboration.email.EmailService;
import de.mpg.imeji.logic.user.controller.UserBusinessController;
import de.mpg.imeji.logic.util.UrlHelper;
import de.mpg.imeji.logic.vo.CollectionImeji;
import de.mpg.imeji.logic.vo.Item;
import de.mpg.imeji.logic.vo.User;
import de.mpg.imeji.presentation.session.SessionBean;
import de.mpg.imeji.presentation.util.BeanHelper;

/**
 * Created by vlad on 13.03.15.
 */
public class NotificationUtils {

  private static final Logger LOGGER = Logger.getLogger(NotificationUtils.class);

  private static EmailMessages msgs = new EmailMessages();
  private static final EmailService emailClient = new EmailService();
  private static UserBusinessController uc = new UserBusinessController();
  private static CollectionController cc = new CollectionController();

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
    final CollectionImeji c = cc.retrieve(fileItem.getCollection(), Imeji.adminUser);
    for (User u : uc.searchUsersToBeNotified(user, c)) {
      emailClient.sendMail(u.getEmail(), null,
          EmailMessages.getEmailOnItemDownload_Subject(fileItem, locale),
          EmailMessages.getEmailOnItemDownload_Body(u, user, fileItem, c, locale));
      LOGGER.info("Sent notification email to user: " + u.getPerson().getCompleteName() + "<"
          + u.getEmail() + ">" + " by item " + fileItem.getId() + " download");
    }
  }

  /**
   * Send email notifications to all users which checked "Send notification email by item download"
   * feature for collection of item
   *
   * @param user
   * @param export
   * @param session
   * @throws de.mpg.imeji.exceptions.ImejiException
   * @throws java.io.IOException
   * @throws java.net.URISyntaxException
   */
  public static void notifyByExport(User user, Export export, SessionBean session)
      throws ImejiException, IOException, URISyntaxException {
    // notify by zip export
    if ("zip".equals(export.getParam("format"))) {
      // only for images
      if ("image".equals(export.getParam("type"))) {
        Map<String, String> msgsPerEmail = new HashMap<>();
        Map<String, User> usersPerEmail = new HashMap<>();
        String q = !isNullOrEmpty(export.getParam("q")) ? "/browse?q=" + export.getParam("q") : "";
        for (Map.Entry<URI, Integer> entry : ((ZIPExport) export).getItemsPerCollection()
            .entrySet()) {
          final CollectionImeji c = cc.retrieve(entry.getKey(), Imeji.adminUser);
          for (User u : uc.searchUsersToBeNotified(user, c)) {
            String key = u.getEmail();
            msgsPerEmail.put(key,
                (msgsPerEmail.containsKey(key) ? msgsPerEmail.get(key) + "\r\n" : "")
                    + "XXX_COLLECTION_XXX URI" + (isNullOrEmpty(q) ? ": " : " (XXX_FILTERED_XXX): ")
                    + UrlHelper.encodeQuery(entry.getKey().toString() + q)
                    + ", XXX_ITEMS_COUNT_XXX: " + entry.getValue().intValue());
            usersPerEmail.put(key, u);
          }
        }
        String url = reconstructQueryUrl((ZIPExport) export, session);
        for (Map.Entry<String, String> entry : msgsPerEmail.entrySet()) {
          User u = usersPerEmail.get(entry.getKey());
          emailClient.sendMail(u.getEmail(), null,
              EmailMessages.getEmailOnZipDownload_Subject(session.getLocale()), EmailMessages
                  .getEmailOnZipDownload_Body(u, user, entry.getValue(), url, session.getLocale()));
          LOGGER.info("Sent notification email to user: " + u.getPerson().getCompleteName() + "<"
              + u.getEmail() + ">;" + " zip download query: <" + url + ">; message: <"
              + entry.getValue().replaceAll("[\\r\\n]]", ";") + ">");
        }
      }
    }
  }


  /**
   * Reconstructs Query Url on hand of request parameters saved in export instance
   *
   * @param export
   * @param session
   * @return
   */
  private static String reconstructQueryUrl(ZIPExport export, SessionBean session) {
    String q = "?q=", path = "browse";
    if (!isNullOrEmpty(export.getParam("q"))) {
      q += export.getParam("q");
    }
    if (!isNullOrEmpty(export.getParam("col"))) {
      path = "collection/" + export.getParam("col") + "/" + path;
    } else if (!isNullOrEmpty(export.getParam("album"))) {
      path = "album/" + export.getParam("album") + "/" + path;
    }
    return session.getApplicationUrl() + path + q;
  }


  /**
   * Send account activation email
   */
  public static void sendActivationNotification(User user, Locale locale, boolean invitation) {
    // EmailClient emailClient = new EmailClient();
    // EmailMessages emailMessages = new EmailMessages();
    try {
      // send to support
      emailClient.sendMail(user.getEmail(), null,
          EmailMessages.getEmailOnAccountActivation_Subject(user, locale),
          EmailMessages.getEmailOnAccountActivation_Body(user, locale, invitation));
    } catch (Exception e) {
      BeanHelper.info(Imeji.RESOURCE_BUNDLE.getMessage("error", locale)
          + ": Account activation email not sent");
      LOGGER.info("Error sending account activation email", e);
    }
  }


}
