package de.mpg.imeji.presentation.collection.share;

import java.io.Serializable;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import javax.faces.model.SelectItem;

import org.apache.logging.log4j.Logger; 
import org.apache.logging.log4j.LogManager;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.config.Imeji;
import de.mpg.imeji.logic.model.User;
import de.mpg.imeji.logic.model.UserGroup;
import de.mpg.imeji.logic.security.sharing.ShareService;
import de.mpg.imeji.logic.security.sharing.ShareService.ShareRoles;
import de.mpg.imeji.logic.security.sharing.invitation.Invitation;
import de.mpg.imeji.logic.security.sharing.invitation.InvitationService;
import de.mpg.imeji.logic.security.user.UserService;
import de.mpg.imeji.presentation.session.BeanHelper;

/**
 * A Item of the list of shared person
 *
 * @author bastiens
 *
 */
public class ShareListItem implements Serializable {
  private static final long serialVersionUID = -1637916656299359982L;
  private static final Logger LOGGER = LogManager.getLogger(ShareListItem.class);
  private final User currentUser;
  private Invitation invitation;
  private User user;
  private UserGroup group;
  private final String shareToUri;
  private String role = ShareRoles.READ.name();
  private List<SelectItem> rolesMenu;
  private String title;
  private Locale locale;
  private boolean creator = false;

  /**
   * Constructor without User of Group (To be used as menu)
   *
   * @param user
   * @param isCollection
   * @param containerUri
   * @param profileUri
   * @param roles
   */
  public ShareListItem(String uri, User currentUser, Locale locale) {
    this.shareToUri = uri;
    this.currentUser = currentUser;
    init(ShareRoles.READ.name(), uri, locale);
  }

  /**
   * Constructor with a {@link Invitation}
   *
   * @param user
   * @param isCollection
   * @param containerUri
   * @param profileUri
   * @param roles
   */
  public ShareListItem(Invitation invitation, String uri, User currentUser, Locale locale) {
    this.invitation = invitation;
    this.shareToUri = uri;
    this.currentUser = currentUser;
    init(invitation.getRole(), uri, locale);
  }

  /**
   * Constructor with a {@link User}
   *
   * @param user
   * @param isCollection
   * @param containerUri
   * @param profileUri
   * @param roles
   */
  public ShareListItem(User user, String uri, String title, User currentUser, Locale locale,
      boolean creator) {
    this.user = user;
    this.title = title;
    this.currentUser = currentUser;
    this.creator = creator;
    this.shareToUri = uri;
    init(ShareService.findRole(user.getGrants(), uri), uri, locale);
  }

  /**
   * Constructor with a {@link UserGroup}
   *
   * @param group
   * @param isCollection
   * @param containerUri
   * @param profileUri
   * @param roles
   */
  public ShareListItem(UserGroup group, String uri, String title, User currentUser, Locale locale) {
    this.setGroup(group);
    this.shareToUri = uri;
    this.title = title;
    this.currentUser = currentUser;
    init(ShareService.findRole(group.getGrants(), uri), uri, locale);
  }

  /**
   * Initialize the menu
   *
   * @param grants
   * @param uri
   * @param profileUri
   */
  private void init(String role, String uri, Locale locale) {
    this.role = role;
    this.locale = locale;
    this.rolesMenu = Arrays.asList(
        new SelectItem(ShareRoles.READ.name(),
            Imeji.RESOURCE_BUNDLE.getLabel("collection_share_read", locale)),
        new SelectItem(ShareRoles.EDIT.name(), Imeji.RESOURCE_BUNDLE.getLabel("edit", locale)),
        new SelectItem(ShareRoles.ADMIN.name(), Imeji.RESOURCE_BUNDLE.getLabel("admin", locale)));
  }

  /**
   * Update {@link Grants} the {@link ShareListItem} according to the new roles. Return true if the
   * user grant have been modified
   *
   * @return
   */
  public boolean update() {
    final ShareService sbc = new ShareService();
    String roleBefore = null;
    try {
      if (user != null) {
        roleBefore = ShareService.findRole(user.getGrants(), shareToUri);
        sbc.shareToUser(currentUser, user, shareToUri, role);
      } else if (group != null) {
        roleBefore = ShareService.findRole(group.getGrants(), shareToUri);
        sbc.shareToGroup(currentUser, group, shareToUri, role);
      }
      return role == null || !role.equals(roleBefore);
    } catch (final ImejiException e) {
      LOGGER.error("Error updating grants: ", e);
      BeanHelper.error("Error during sharing: " + e.getMessage());
    }
    return false;
  }

  /**
   * Update the invitation
   *
   * @throws ImejiException
   */
  public void updateInvitation() throws ImejiException {
    if (invitation != null) {
      final InvitationService invitationService = new InvitationService();
      if (role == null) {
        invitationService.cancel(invitation.getId());
      } else {
        final Invitation newInvitation =
            new Invitation(invitation.getInviteeEmail(), invitation.getObjectUri(), role);
        invitationService.invite(newInvitation);
      }
    }
  }

  /**
   * Revoke all grants for the current object. Called from the user page
   */
  public String revokeGrants() {
    role = null;
    update();
    return "";
  }

  /**
   * Return all users in this items. This might be many user if the item contains a group
   *
   * @return
   */
  public List<User> getUsers() {
    final UserService controller = new UserService();
    final List<User> users = new ArrayList<>();
    if (group != null) {
      for (final URI uri : group.getUsers()) {
        try {
          users.add(controller.retrieve(uri, Imeji.adminUser));
        } catch (final ImejiException e) {
          LOGGER.error("Error retrieving user:" + uri);
        }
      }
    }
    if (user != null) {
      users.add(user);
    }
    return users;
  }

  /**
   * @return the group
   */
  public UserGroup getGroup() {
    return group;
  }

  /**
   * @param group the group to set
   */
  public void setGroup(UserGroup group) {
    this.group = group;
  }

  /**
   * @return the shareToUri
   */
  public String getShareToUri() {
    return shareToUri;
  }

  public String getShareToUriString() {
    String[] split = shareToUri.split("/");
    return split[split.length - 1];
  }

  /**
   * @return the title
   */
  public String getTitle() {
    return title;
  }

  /**
   * @param title the title to set
   */
  public void setTitle(String title) {
    this.title = title;
  }

  public User getUser() {
    return user;
  }

  public void setUser(User user) {
    this.user = user;
  }

  public Invitation getInvitation() {
    return invitation;
  }

  public boolean isCreator() {
    return creator;
  }

  /**
   * @return the role
   */
  public String getRole() {
    return role;
  }

  /**
   * @param role the role to set
   */
  public void setRole(String role) {
    this.role = role;
  }

  /**
   * @return the rolesMenu
   */
  public List<SelectItem> getRolesMenu() {
    return rolesMenu;
  }

  /**
   * @param rolesMenu the rolesMenu to set
   */
  public void setRolesMenu(List<SelectItem> rolesMenu) {
    this.rolesMenu = rolesMenu;
  }


}
