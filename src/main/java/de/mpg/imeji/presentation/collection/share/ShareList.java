package de.mpg.imeji.presentation.collection.share;

import java.io.Serializable;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.config.Imeji;
import de.mpg.imeji.logic.model.SearchFields;
import de.mpg.imeji.logic.model.User;
import de.mpg.imeji.logic.model.UserGroup;
import de.mpg.imeji.logic.search.Search;
import de.mpg.imeji.logic.search.model.SearchOperators;
import de.mpg.imeji.logic.search.model.SearchPair;
import de.mpg.imeji.logic.search.model.SearchQuery;
import de.mpg.imeji.logic.security.sharing.invitation.Invitation;
import de.mpg.imeji.logic.security.sharing.invitation.InvitationService;
import de.mpg.imeji.logic.security.user.UserService;
import de.mpg.imeji.logic.security.usergroup.UserGroupService;

/**
 * List of all entities with grant for one resource
 *
 * @author bastiens
 *
 */
public final class ShareList implements Serializable {
  private static final long serialVersionUID = -3986021952970961215L;
  private final List<ShareListItem> items = new ArrayList<ShareListItem>();
  private final List<ShareListItem> invitations = new ArrayList<ShareListItem>();
  private final List<ShareListItem> ownerItems = new ArrayList<ShareListItem>();

  /**
   * Create a list of all entities with grant for one resource
   *
   * @param ownerUri
   * @param sharedObjectUri
   * @param profileUri
   * @param type
   * @param currentUser
   * @throws ImejiException
   */
  public ShareList(URI ownerUri, String sharedObjectUri, User currentUser, Locale locale) throws ImejiException {
    retrieveUsers(ownerUri, sharedObjectUri, currentUser, locale);
    retrieveGroups(ownerUri, sharedObjectUri, currentUser, locale);
    retrieveInvitations(sharedObjectUri, currentUser, locale);
  }

  /**
   * Retrieve the user groups having a grant for the this resource
   *
   * @param ownerUri
   * @param sharedObjectUri
   * @param profileUri
   * @param type
   * @param currentUser
   */
  private void retrieveGroups(URI ownerUri, String sharedObjectUri, User currentUser, Locale locale) {
    final UserGroupService userGroupService = new UserGroupService();
    final Collection<UserGroup> groups = userGroupService.searchAndRetrieve(
        SearchQuery.toSearchQuery(new SearchPair(SearchFields.read, SearchOperators.EQUALS, sharedObjectUri, false)), null, Imeji.adminUser,
        Search.SEARCH_FROM_START_INDEX, Search.GET_ALL_RESULTS);
    for (final UserGroup group : groups) {
      items.add(new ShareListItem(group, sharedObjectUri, null, currentUser, locale));
    }
  }

  /**
   * Retrieve all Users having a grant for this resource
   *
   * @param ownerUri
   * @param sharedObjectUri
   * @param profileUri
   * @param type
   * @param currentUser
   */
  private void retrieveUsers(URI ownerUri, String sharedObjectUri, User currentUser, Locale locale) {
    final UserService userService = new UserService();
    final Collection<User> allUser = userService.searchAndRetrieve(
        SearchQuery.toSearchQuery(new SearchPair(SearchFields.read, SearchOperators.EQUALS, sharedObjectUri, false)), null, Imeji.adminUser,
        Search.SEARCH_FROM_START_INDEX, Search.GET_ALL_RESULTS);
    for (final User user : allUser) {
      boolean isOwner = user.getId().toString().equals(ownerUri.toString());
      ShareListItem userItem = new ShareListItem(user, sharedObjectUri, null, currentUser, locale, isOwner);
      if (isOwner) {
        ownerItems.add(userItem);
      } else {
        items.add(userItem);
      }
    }
  }

  /**
   * Retrieve all Pending invitations for this resource
   *
   * @param sharedObjectUri
   * @param profileUri
   * @param type
   * @param currentUser
   * @throws ImejiException
   */
  private void retrieveInvitations(String sharedObjectUri, User currentUser, Locale locale) throws ImejiException {
    final InvitationService invitationBC = new InvitationService();
    for (final Invitation invitation : invitationBC.retrieveInvitationsOfObject(sharedObjectUri)) {
      invitations.add(new ShareListItem(invitation, sharedObjectUri, currentUser, locale));
    }
  }

  public List<ShareListItem> getItems() {
    return items;
  }

  public List<ShareListItem> getInvitations() {
    return invitations;
  }

  public List<ShareListItem> getOwnerItems() {
    return ownerItems;
  }

}
