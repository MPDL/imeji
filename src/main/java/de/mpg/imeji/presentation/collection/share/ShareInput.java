package de.mpg.imeji.presentation.collection.share;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import org.apache.logging.log4j.Logger; 
import org.apache.logging.log4j.LogManager;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.config.Imeji;
import de.mpg.imeji.logic.model.User;
import de.mpg.imeji.logic.notification.email.EmailMessages;
import de.mpg.imeji.logic.notification.email.EmailService;
import de.mpg.imeji.logic.security.sharing.invitation.Invitation;
import de.mpg.imeji.logic.security.sharing.invitation.InvitationService;
import de.mpg.imeji.logic.security.user.UserService;
import de.mpg.imeji.presentation.session.BeanHelper;

/**
 * The Input for Share Page
 *
 * @author bastiens
 *
 */
public class ShareInput implements Serializable {
  private static final long serialVersionUID = 3979846119253696328L;
  private static final Logger LOGGER = LogManager.getLogger(ShareInput.class);
  private String input = "";
  private ShareListItem menu;
  private List<String> validEmails = new ArrayList<>();
  private List<String> invalidEntries = new ArrayList<>();
  private List<String> unknownEmails = new ArrayList<>();
  private final String objectUri;
  private final Locale locale;
  private final User user;
  private final String instanceName;

  /**
   * Constructor
   *
   * @param objectUri
   */
  public ShareInput(String objectUri, User user, Locale locale, String instanceName) {
    this.objectUri = objectUri;
    this.user = user;
    this.locale = locale;
    this.instanceName = instanceName;
    this.menu = new ShareListItem(objectUri, user, locale);
  }

  /**
   * Share to valid emails
   */
  public boolean share() {
    parseInput();
    if (invalidEntries.isEmpty()) {
      shareWithValidEmails();
      BeanHelper.info(Imeji.RESOURCE_BUNDLE.getMessage("success_share", locale));
      return this.unknownEmails.isEmpty();
    } else {
      BeanHelper.error(Imeji.RESOURCE_BUNDLE.getLabel("invalid_email", locale) + ": "
          + invalidEntries.stream().collect(Collectors.joining(", ")));
    }
    return false;
  }

  /**
   * Send Invitations to unknown Emails
   */
  public void sendInvitations() {
    final InvitationService invitationBC = new InvitationService();
    final EmailService emailService = new EmailService();
    for (final String invitee : unknownEmails) {
      try {
        invitationBC.invite(new Invitation(invitee, objectUri, menu.getRole()));
        emailService.sendMail(invitee, null, EmailMessages.getInvitationEmailSubject(this.user, this.locale, this.instanceName),
            EmailMessages.getInvitationEmailBody(invitee, this.locale, this.user, this.instanceName));
      } catch (final ImejiException e) {
        BeanHelper.error(Imeji.RESOURCE_BUNDLE.getMessage("error_send_invitation", locale));
        LOGGER.error("Error sending invitation:", e);
      }
    }
    BeanHelper.info(Imeji.RESOURCE_BUNDLE.getMessage("success_share", locale));
  }

  


  /**
   * Remove an unknow Email from the list (no invitation will be sent to him)
   *
   * @param pos
   */
  public String removeUnknownEmail(int pos) {
    unknownEmails.remove(pos);
    return unknownEmails.isEmpty() ? "pretty:" : "";
  }

  /**
   * Share with existing users
   */
  private void shareWithValidEmails() {
    for (final ShareListItem shareListItem : asShareListItem()) {
      shareListItem.update();
    }
  }

  public List<ShareListItem> asShareListItem() {
    final List<ShareListItem> listItems = new ArrayList<ShareListItem>();
    for (final String email : validEmails) {
      final ShareListItem item =
          new ShareListItem(retrieveUser(email), objectUri, null, user, locale, false);
      item.setRole(menu.getRole());
      listItems.add(item);
    }
    return listItems;
  }



  /**
   * Parse the Input to a list of Emails. Add Unknown emails to externaluser list and invalid Emails
   * to invalideEntries
   *
   * @return
   */
  private void parseInput() {
    validEmails.clear();
    unknownEmails.clear();
    invalidEntries.clear();
    for (final String value : input.split("\\s*[|,;\\n]\\s*")) {
      if (EmailService.isValidEmail(value) && !value.equalsIgnoreCase(user.getEmail())) {
        final boolean exists = retrieveUser(value) != null;
        if (exists) {
          validEmails.add(value);
        } else {
          unknownEmails.add(value);
        }
      } else {
        invalidEntries.add(value);
      }
    }
  }

  /**
   * Retrieve the user. If not existing, return null
   *
   * @param email
   * @return
   */
  private User retrieveUser(String email) {
    final UserService controller = new UserService();
    try {
      return controller.retrieve(email, Imeji.adminUser);
    } catch (final Exception e) {
      return null;
    }
  }

  /**
   * @return the input
   */
  public String getInput() {
    return input;
  }

  /**
   * @param input the input to set
   */
  public void setInput(String input) {
    this.input = input;
  }

  /**
   * @return the invalidEntries
   */
  public List<String> getInvalidEntries() {
    return invalidEntries;
  }

  public String getInvalidEntriesAsString() {
    return invalidEntries.stream().collect(Collectors.joining(", "));
  }

  /**
   * @param invalidEntries the invalidEntries to set
   */
  public void setInvalidEntries(List<String> invalidEntries) {
    this.invalidEntries = invalidEntries;
  }


  /**
   * @return the validEmails
   */
  public List<String> getValidEmails() {
    return validEmails;
  }

  /**
   * @param validEmails the validEmails to set
   */
  public void setValidEmails(List<String> validEmails) {
    this.validEmails = validEmails;
  }

  /**
   * @return the unknownEmails
   */
  public List<String> getUnknownEmails() {
    return unknownEmails;
  }

  /**
   * @param unknownEmails the unknownEmails to set
   */
  public void setUnknownEmails(List<String> unknownEmails) {
    this.unknownEmails = unknownEmails;
  }

  public ShareListItem getMenu() {
    return menu;
  }

  public void setMenu(ShareListItem menu) {
    this.menu = menu;
  }
}
