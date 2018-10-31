package de.mpg.imeji.presentation.status;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import javax.faces.component.FacesComponent;
import javax.faces.component.UINamingContainer;

import de.mpg.imeji.logic.config.Imeji;
import de.mpg.imeji.logic.hierarchy.HierarchyService;
import de.mpg.imeji.logic.model.CollectionImeji;
import de.mpg.imeji.logic.model.Item;
import de.mpg.imeji.logic.model.Properties;
import de.mpg.imeji.logic.model.Properties.Status;
import de.mpg.imeji.logic.model.SearchFields;
import de.mpg.imeji.logic.model.User;
import de.mpg.imeji.logic.model.UserGroup;
import de.mpg.imeji.logic.model.factory.ImejiFactory;
import de.mpg.imeji.logic.search.jenasearch.ImejiSPARQL;
import de.mpg.imeji.logic.search.jenasearch.JenaCustomQueries;
import de.mpg.imeji.logic.search.model.SearchOperators;
import de.mpg.imeji.logic.search.model.SearchPair;
import de.mpg.imeji.logic.search.model.SearchQuery;
import de.mpg.imeji.logic.security.authorization.util.SecurityUtil;
import de.mpg.imeji.logic.security.user.UserService;
import de.mpg.imeji.logic.security.usergroup.UserGroupService;
import de.mpg.imeji.logic.util.ObjectHelper;
import de.mpg.imeji.presentation.collection.list.CollectionListItem;
import de.mpg.imeji.presentation.navigation.Navigation;

/**
 * JSF Component Status informations (Status + Shared)
 *
 * @author bastiens
 *
 */
@FacesComponent("StatusComponent")
public class StatusComponent extends UINamingContainer {
	private Status status;
	private String owner;
	private boolean show = false;
	private boolean allowedToManage = false;
	private List<String> users = new ArrayList<>();
	private List<String> groups = new ArrayList<>();
	private String linkToSharePage;
	private static final int COLLABORATOR_LIST_MAX_SIZE = 5;
	private int collaboratorListSize = 0;
	private boolean hasMoreCollaborator = false;
	private User sessionUser;
	private Locale locale;
	private String applicationUrl;
	private boolean sharedWithUser = false;

	public StatusComponent() {
		// do nothing
	}

	/**
	 * Method called from the JSF component
	 *
	 * @param o
	 */
	public void init(Object o, User sessionUser, Locale locale, String applicationUrl) {
		this.sessionUser = sessionUser;
		this.locale = locale;
		this.applicationUrl = applicationUrl;
		this.show = false;
		this.allowedToManage = false;
		this.users = new ArrayList<>();
		this.groups = new ArrayList<>();
		this.collaboratorListSize = 0;
		this.hasMoreCollaborator = false;
		if (o instanceof Properties) {
			initialize((Properties) o);
		} else if (o instanceof CollectionListItem) {
			initialize(((CollectionListItem) o).getCollection());
		}
	}

	/**
	 * Initialize the AbstractBean
	 */
	private void initialize(Properties properties) {
		reset();
		if (properties != null) {
			CollectionImeji parentCollection = getLastParent(properties);
			status = properties.getStatus();
			// find out if this item is shared with this user
			sharedWithUser = SecurityUtil.authorization().isShared(sessionUser, parentCollection);
			if (isSharedWithUser() || SecurityUtil.authorization().isSysAdmin(sessionUser)) {
				users = getUserSharedWith(parentCollection);
				groups = getGroupSharedWith(parentCollection);
				// show a link to manage the access to this item (share page) if
				// - the user is the owner of the collection
				// - the user is system administrator
				allowedToManage = SecurityUtil.authorization().administrate(sessionUser, parentCollection);
			}
			linkToSharePage = initLinkToSharePage(parentCollection.getId());
			show = true;
		}
	}

	/**
	 * Get the last parent of the Item/collection, which is relevant for the share
	 * status
	 * 
	 * @param p
	 * @return
	 */
	private CollectionImeji getLastParent(Properties p) {
		String uri = new HierarchyService().getLastParent(p);
		return uri != null
				? ImejiFactory.newCollection().setUri(uri).setCreatedBy(findOwner(uri)).build()
				: (CollectionImeji) p;
	}

	/**
	 * Find Owner of the collection
	 * 
	 * @param collectionUri
	 * @return
	 */
	private URI findOwner(String collectionUri) {
		return URI.create(ImejiSPARQL.exec(JenaCustomQueries.selectCreatedBy(collectionUri), null).get(0));
	}

	/**
	 * Reset this bean
	 */
	private void reset() {
		status = null;
		owner = null;
		show = false;
		allowedToManage = false;
		users = new ArrayList<>();
		groups = new ArrayList<>();
		linkToSharePage = null;
	}

	/**
	 * Find all users the object is shared with
	 *
	 * @param p
	 * @return
	 */
	private List<String> getUserSharedWith(Properties p) {
		final List<String> l = new ArrayList<>();
		for (final User user : findAllUsersWithReadGrant(p)) {
			if (!l.contains(user.getPerson().getCompleteName())) {
				if (collaboratorListSize >= COLLABORATOR_LIST_MAX_SIZE) {
					hasMoreCollaborator = true;
					return l;
				}
				if (!p.getCreatedBy().toString().equals(user.getId().toString())) {
					l.add(user.getPerson().getCompleteName());
					collaboratorListSize++;
				} else {
					owner = user.getPerson().getCompleteName();
				}
			}
		}
		return l;
	}

	/**
	 * Find all groups the object is shared with
	 *
	 * @param properties
	 * @return
	 */
	private List<String> getGroupSharedWith(Properties properties) {
		final List<String> l = new ArrayList<>();
		for (final UserGroup group : findAllGroupsWithReadGrant(properties)) {
			if (collaboratorListSize >= COLLABORATOR_LIST_MAX_SIZE) {
				hasMoreCollaborator = true;
				return l;
			}
			if (!l.contains(group.getName())) {
				l.add(group.getName());
				collaboratorListSize++;
			}

		}
		return l;
	}

	/**
	 * Find all Users the object is shared with
	 *
	 * @param p
	 * @return
	 */
	private List<User> findAllUsersWithReadGrant(Properties p) {
		final UserService uc = new UserService();
		final List<User> l = new ArrayList<>();
		if (p instanceof Item) {
			l.addAll(uc.searchAndRetrieveLazy(getReadQuery(((Item) p).getCollection().toString()), null,
					Imeji.adminUser, 0, COLLABORATOR_LIST_MAX_SIZE));
		} else {
			l.addAll(uc.searchAndRetrieveLazy(getReadQuery(p.getId().toString()), null, Imeji.adminUser, 0,
					COLLABORATOR_LIST_MAX_SIZE));
		}
		return l;
	}

	/**
	 * Return query "read:objectId" to find all users or user groups with read
	 * rights on this object
	 *
	 * @param objectId
	 * @return
	 */
	private SearchQuery getReadQuery(String objectId) {
		return SearchQuery.toSearchQuery(new SearchPair(SearchFields.read, SearchOperators.EQUALS, objectId, false));
	}

	/**
	 * Find all Groups the object is shared with
	 *
	 * @param p
	 * @return
	 */
	private List<UserGroup> findAllGroupsWithReadGrant(Properties p) {
		final UserGroupService ugc = new UserGroupService();
		final List<UserGroup> l = ugc.searchAndRetrieveLazy(getReadQuery(p.getId().toString()), null, Imeji.adminUser,
				0, COLLABORATOR_LIST_MAX_SIZE);
		if (p instanceof Item) {
			l.addAll(ugc.searchAndRetrieveLazy(getReadQuery(((Item) p).getCollection().toString()), null,
					Imeji.adminUser, 0, COLLABORATOR_LIST_MAX_SIZE));
		}
		return l;
	}

	/**
	 * Initialize the link to the share page
	 *
	 * @param uri
	 * @return
	 */
	private String initLinkToSharePage(URI uri) {
		return applicationUrl + ObjectHelper.getObjectType(uri).name().toLowerCase() + "/" + ObjectHelper.getId(uri)
				+ "/" + Navigation.SHARE.getPath();
	}

	/**
	 * Return a label for the status
	 *
	 * @return
	 */
	public String getStatusLabel() {
		if (status == Status.RELEASED) {
			return Imeji.RESOURCE_BUNDLE.getLabel("published", locale);
		} else if (status == Status.WITHDRAWN) {
			return Imeji.RESOURCE_BUNDLE.getLabel("withdrawn", locale);
		}
		return Imeji.RESOURCE_BUNDLE.getLabel("private", locale);
	}

	/**
	 * @return the status
	 */
	public Status getStatus() {
		return status;
	}

	public List<String> getUsers() {
		return users;
	}

	public List<String> getGroups() {
		return groups;
	}

	public String getOwner() {
		return owner;
	}

	public String getLinkToSharePage() {
		return linkToSharePage;
	}

	public boolean isAllowedToManage() {
		return allowedToManage;
	}

	public boolean isShow() {
		return show;
	}

	public boolean isHasMoreCollaborator() {
		return hasMoreCollaborator;
	}

	public boolean isSharedWithUser() {
		return sharedWithUser;
	}

	public void setSharedWithUser(boolean sharedWithUser) {
		this.sharedWithUser = sharedWithUser;
	}

	/**
	 * Returns whether or not to show a shared icon for a collection
	 * 
	 * @return
	 */
	public boolean isSharedIconShown() {
		boolean showSharedIcon = (!this.users.isEmpty() || !this.groups.isEmpty())
				&& (this.sharedWithUser || this.allowedToManage);
		return showSharedIcon;
	}

	/**
	 * Returns whether or not to show a private icon for a collection
	 * 
	 * @return
	 */
	public boolean isPrivateIconShown() {
		boolean showPrivateIcon = this.users.isEmpty() && this.groups.isEmpty()
				&& (this.sharedWithUser || this.allowedToManage);
		return showPrivateIcon;

	}

	public String getSharedWithToString() {
		return users.stream().collect(Collectors.joining("\n"))
				+ groups.stream().map(g -> g + " [group]").collect(Collectors.joining("[\n", "\n", ""));
	}

}
