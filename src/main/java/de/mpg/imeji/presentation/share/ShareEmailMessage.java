package de.mpg.imeji.presentation.share;

import java.util.List;
import java.util.Locale;

import de.mpg.imeji.logic.Imeji;
import de.mpg.imeji.logic.user.collaboration.email.EmailMessages;
import de.mpg.imeji.logic.user.collaboration.share.ShareBusinessController;
import de.mpg.imeji.logic.vo.Grant;
import de.mpg.imeji.logic.vo.User;
import de.mpg.imeji.presentation.share.ShareBean.SharedObjectType;

/**
 * Email Message when sharing an object
 *
 * @author bastiens
 *
 */
public class ShareEmailMessage {
  private String body = "";
  private String subject = "";
  private Locale locale;
  private User user;

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
      String shareToUri, String profileUri, List<String> roles, SharedObjectType type, User user,
      Locale locale) {
    this.user = user;
    this.locale = locale;
    body = initBody(addresseeName, sharedObjectName, sharedObjectLink, type);
    String messageRoles = "";
    switch (type) {
      case ALBUM:
        messageRoles = getMessageForShareAlbum(roles);
        break;
      case COLLECTION:
        messageRoles = getMessageForShareCollection(roles, profileUri);
        break;
      case ITEM:
        messageRoles = getMessageForShareItem(roles);
        break;
    }
    body = body.replaceAll("XXX_RIGHTS_XXX", messageRoles.trim());
  }

  private String initBody(String addresseeName, String sharedObjectName, String sharedObjectLink,
      SharedObjectType type) {
    String from = user.getPerson().getCompleteName();
    switch (type) {
      case ALBUM:
        return EmailMessages.getSharedAlbumMessage(from, addresseeName, sharedObjectName,
            sharedObjectLink, locale);
      case COLLECTION:
        return EmailMessages.getSharedCollectionMessage(from, addresseeName, sharedObjectName,
            sharedObjectLink, locale);
      case ITEM:
        return EmailMessages.getSharedItemMessage(from, addresseeName, sharedObjectName,
            sharedObjectLink, locale);
    }
    return null;
  }

  private List<String> toRoleList(String shareToUri, String profileUri, List<Grant> grants) {
    List<String> roles = ShareBusinessController.transformGrantsToRoles(grants, shareToUri);
    if (profileUri != null) {
      for (String profileRole : ShareBusinessController.transformGrantsToRoles(grants,
          profileUri)) {
        if (profileRole.equals("EDIT")) {
          roles.add("EDIT_PROFILE");
        }
      }
    }
    return roles;
  }

  private String getMessageForShareItem(List<String> roles) {
    for (String role : roles) {
      if ("READ".equals(role)) {
        return "- " + Imeji.RESOURCE_BUNDLE.getLabel("collection_share_read", locale);
      }
    }
    return "";
  }

  private String getMessageForShareCollection(List<String> roles, String profileUri) {
    String message = "";
    for (String role : roles) {
      switch (role) {
        case "READ":
          message += "- " + Imeji.RESOURCE_BUNDLE.getLabel("collection_share_read", locale) + "\n";
          break;
        case "CREATE":
          message +=
              "- " + Imeji.RESOURCE_BUNDLE.getLabel("collection_share_image_upload", locale) + "\n";
          break;
        case "EDIT_ITEM":
          message +=
              "- " + Imeji.RESOURCE_BUNDLE.getLabel("collection_share_image_edit", locale) + "\n";
          break;
        case "DELETE_ITEM":
          message +=
              "- " + Imeji.RESOURCE_BUNDLE.getLabel("collection_share_image_delete", locale) + "\n";
          break;
        case "EDIT":
          message += "- "
              + Imeji.RESOURCE_BUNDLE.getLabel("collection_share_collection_edit", locale) + "\n";
          break;
        case "EDIT_PROFILE":
          message += "- " + Imeji.RESOURCE_BUNDLE.getLabel("collection_share_profile_edit", locale)
              + ":  " + profileUri + "\n";
          break;
        case "ADMIN":
          message += "- " + Imeji.RESOURCE_BUNDLE.getLabel("collection_share_admin", locale) + "\n";
          break;
      }
    }
    return message;
  }

  private String getMessageForShareAlbum(List<String> roles) {
    String message = "";
    for (String role : roles) {
      switch (role) {
        case "READ":
          message += "- " + Imeji.RESOURCE_BUNDLE.getLabel("album_share_read", locale) + "\n";
          break;
        case "CREATE":
          message += "- " + Imeji.RESOURCE_BUNDLE.getLabel("album_share_image_add", locale) + "\n";
          break;
        case "EDIT":
          message += "- " + Imeji.RESOURCE_BUNDLE.getLabel("album_share_album_edit", locale) + "\n";
          break;
        case "ADMIN":
          message += "- " + Imeji.RESOURCE_BUNDLE.getLabel("album_share_admin", locale) + "\n";
          break;
      }
    }
    return message;
  }

  public String getBody() {
    return body;
  }

  public String getSubject() {
    return subject;
  }

}
