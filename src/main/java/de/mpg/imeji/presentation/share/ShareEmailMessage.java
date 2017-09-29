package de.mpg.imeji.presentation.share;

import java.util.Locale;

import de.mpg.imeji.logic.config.Imeji;
import de.mpg.imeji.logic.model.User;
import de.mpg.imeji.logic.security.sharing.ShareService.ShareRoles;
import de.mpg.imeji.logic.security.sharing.email.EmailMessages;

/**
 * Email Message when sharing an object
 *
 * @author bastiens
 *
 */
public class ShareEmailMessage {
  private String body = "";
  private final String subject = "";
  private final Locale locale;
  private final User user;

  /**
   * Constructor
   *
   * @param shareToUri
   * @param profileUri
   * @param grants
   * @param type
   * @param session
   */
  public ShareEmailMessage(String addresseeName, String sharedObjectName, String sharedObjectLink,
      String shareToUri, String role, User user, Locale locale) {
    this.user = user;
    this.locale = locale;
    body = EmailMessages.getSharedCollectionMessage(user.getPerson().getFirstnameLastname(),
        addresseeName, sharedObjectName, sharedObjectLink, locale);
    final String messageRoles = getMessageForShareCollection(role);
    body = body.replaceAll("XXX_RIGHTS_XXX", messageRoles.trim());

  }

  private String getMessageForShareCollection(String role) {
    switch (ShareRoles.valueOf(role)) {
      case READ:
        return "- " + Imeji.RESOURCE_BUNDLE.getLabel("collection_share_read", locale) + "\n";
      case EDIT:
        return "- " + Imeji.RESOURCE_BUNDLE.getLabel("collection_share_collection_edit", locale)
            + "\n";
      case ADMIN:
        return "- " + Imeji.RESOURCE_BUNDLE.getLabel("collection_share_admin", locale) + "\n";
    }
    return "";
  }


  public String getBody() {
    return body;
  }

  public String getSubject() {
    return subject;
  }

}
