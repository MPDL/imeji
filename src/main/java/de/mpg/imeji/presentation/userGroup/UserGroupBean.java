package de.mpg.imeji.presentation.userGroup;

import java.io.IOException;
import java.io.Serializable;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.servlet.http.Cookie;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import org.apache.jena.sparql.pfunction.library.container;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.exceptions.UnprocessableError;
import de.mpg.imeji.logic.config.Imeji;
import de.mpg.imeji.logic.model.Grant;
import de.mpg.imeji.logic.model.User;
import de.mpg.imeji.logic.model.UserGroup;
import de.mpg.imeji.logic.security.user.UserService;
import de.mpg.imeji.logic.security.usergroup.UserGroupService;
import de.mpg.imeji.logic.util.UrlHelper;
import de.mpg.imeji.presentation.beans.SuperBean;
import de.mpg.imeji.presentation.collection.share.ShareListItem;
import de.mpg.imeji.presentation.collection.share.ShareUtil;
import de.mpg.imeji.presentation.session.BeanHelper;
import de.mpg.imeji.presentation.util.CookieUtils;

/**
 * Bean to create a
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
/**
 * @author jandura
 *
 */
@ManagedBean(name = "UserGroup")
@ViewScoped
public class UserGroupBean extends SuperBean implements Serializable {
	private static final long serialVersionUID = -6501626930686020874L;
	private UserGroup userGroup = new UserGroup();
	private Collection<User> users;
	private static final Logger LOGGER = LogManager.getLogger(UserGroupsBean.class);
	private List<ShareListItem> roles = new ArrayList<ShareListItem>();
	private boolean edit = false;

	// properties for add-user dialog
	private String index = "";

	@PostConstruct
	public void init() {
		final String groupId = UrlHelper.getParameterValue("groupId");
		if (groupId != null) {
			final UserGroupService c = new UserGroupService();
			try {
				this.userGroup = c.retrieve(groupId, getSessionUser());
				this.users = loadUsers(userGroup).stream().sorted(
						(u1, u2) -> u1.getPerson().getCompleteName().compareTo(u2.getPerson().getCompleteName()))
						.collect(Collectors.toList());
				this.roles = ShareUtil.getAllRoles(userGroup, getSessionUser(), getLocale());

			} catch (final ImejiException e) {
				BeanHelper.error("Error reading user group " + groupId);
				LOGGER.error("Error initializing UserGroupBean", e);
			}
		}
	}

	/**
	 * Load the {@link User} of a {@link UserGroup}
	 *
	 * @param subject
	 * @param f
	 * @param object
	 * @param position
	 * @return
	 */
	public Collection<User> loadUsers(UserGroup group) {
		final Collection<User> users = new ArrayList<User>();
		final UserService c = new UserService();
		for (final URI uri : userGroup.getUsers()) {
			try {
				users.add(c.retrieve(uri, Imeji.adminUser));
			} catch (final ImejiException e) {
				LOGGER.error("Error reading user: ", e);
			}
		}
		return users;
	}

	/**
	 * Remove a {@link User} from a {@link UserGroup}
	 *
	 * @param remove
	 * @return
	 * @throws IOException
	 */
	public void removeUserFromGroup(URI remove) throws IOException {
		userGroup.getUsers().remove(remove);
		save();
	}

	public boolean isUserInGroup(URI user) {
		for (User u : users) {
			if (u.getId().equals(user)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Unshare the {@link Container} for one {@link UserGroup} (i.e, remove all
	 * {@link Grant} of this {@link User} related to the {@link container})
	 *
	 * @param sh
	 * @throws IOException
	 */
	public void revokeGrants(ShareListItem sh) throws IOException {
		sh.setRole(null);
		sh.update();
		reload();
	}

	/**
	 * Reload the page
	 *
	 * @throws IOException
	 */
	@Override
	public void reload() throws IOException {
		redirect(getNavigation().getApplicationUrl() + "usergroup?groupId=" + userGroup.getId());
	}

	/**
	 * Create a new {@link UserGroup}
	 */
	public String create() {
		final UserGroupService c = new UserGroupService();
		try {
			c.create(userGroup, getSessionUser());
			reload();
		} catch (final UnprocessableError e) {
			BeanHelper.error(e, getLocale());
			LOGGER.error("Error creating user group", e);
		} catch (final Exception e) {
			BeanHelper.error("Error creating user group");
			LOGGER.error("Error creating user group", e);
		}
		return "";
	}

	/**
	 * Update the current {@link UserGroup}
	 *
	 * @throws IOException
	 */
	public void save() throws IOException {
		final UserGroupService c = new UserGroupService();
		try {
			c.update(userGroup, getSessionUser());
		} catch (final UnprocessableError e) {
			BeanHelper.error(e, getLocale());
			LOGGER.error("Error updating user group", e);
		} catch (final Exception e) {
			BeanHelper.error("Error updating user group");
			LOGGER.error("Error updating user group", e);
		}
		reload();
	}

	/**
	 * @return the userGroup
	 */
	public UserGroup getUserGroup() {
		return userGroup;
	}

	/**
	 * @param userGroup
	 *            the userGroup to set
	 */
	public void setUserGroup(UserGroup userGroup) {
		this.userGroup = userGroup;
	}

	/**
	 * @return the users
	 */
	public Collection<User> getUsers() {
		return users;
	}

	/**
	 * @param users
	 *            the users to set
	 */
	public void setUsers(Collection<User> users) {
		this.users = users;
	}

	/**
	 * @return the roles
	 */
	public List<ShareListItem> getRoles() {
		return roles;
	}

	/**
	 * @param roles
	 *            the roles to set
	 */
	public void setRoles(List<ShareListItem> roles) {
		this.roles = roles;
	}

	/**
	 * @return the edit
	 */
	public boolean isEdit() {
		return edit;
	}

	/**
	 * @param edit
	 *            the edit to set
	 */
	public void setEdit(boolean edit) {
		this.edit = edit;
	}

	public String getIndex() {
		return index;
	}

	public void setIndex(String index) {
		this.index = index;
	}

	public List<User> searchForIndex() throws ImejiException {
		Collection<User> allUsers = (new UserService()).searchUserByName("");
		return allUsers.stream().filter(u -> filter(u, index))
				.sorted((u1, u2) -> u1.getPerson().getCompleteName().compareTo(u2.getPerson().getCompleteName()))
				.collect(Collectors.toList());
	}

	private boolean filter(User user, String index) {
		return user.getEmail().toLowerCase().startsWith(index.toLowerCase())
				|| user.getPerson().getFamilyName().toLowerCase().startsWith(index.toLowerCase())
				|| user.getPerson().getGivenName().toLowerCase().startsWith(index.toLowerCase());
	}

	public String getUserText(String email) throws ImejiException {
		User user = (new UserService()).retrieve(email, getSessionUser());
		String text = user.getPerson().getCompleteName() + "(" + user.getEmail() + ")";
		if (isUserInGroup(user.getId())) {
			text += "   Already a member of the group";
		}
		return text;
	}

	public void addUser(User user) throws ImejiException, IOException {
		userGroup.getUsers().add(user.getId());
		save();
	}

	public String getHideUsersButtonStyle() {
		Cookie cookie = CookieUtils.readCookie("showUsers");
		return cookie == null || "true".equals(cookie.getValue()) ? "block" : "none";
	}

	public String getShowUsersButtonStyle() {
		Cookie cookie = CookieUtils.readCookie("showUsers");
		return cookie == null || "true".equals(cookie.getValue()) ? "none" : "block";
	}

}
