package de.mpg.imeji.presentation.userGroup;

import java.io.IOException;
import java.net.URI;
import java.util.Collection;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;

import org.apache.logging.log4j.Logger; 
import org.apache.logging.log4j.LogManager;

import de.mpg.imeji.logic.config.Imeji;
import de.mpg.imeji.logic.model.User;
import de.mpg.imeji.logic.model.UserGroup;
import de.mpg.imeji.logic.security.usergroup.UserGroupService;
import de.mpg.imeji.logic.util.UrlHelper;
import de.mpg.imeji.presentation.beans.SuperBean;
import de.mpg.imeji.presentation.session.BeanHelper;

/**
 * JSF Bean to browse {@link UserGroup}
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
@ManagedBean(name = "UserGroups")
@ViewScoped
public class UserGroupsBean extends SuperBean {
  private static final long serialVersionUID = -7449016567355739362L;
  private Collection<UserGroup> userGroups;
  @ManagedProperty(value = "#{SessionBean.user}")
  private User sessionUser;
  private String query;
  private static final Logger LOGGER = LogManager.getLogger(UserGroupsBean.class);
  private String backContainerUrl;

  @PostConstruct
  public void init() {
    final String q = UrlHelper.getParameterValue("q");
    final String back = UrlHelper.getParameterValue("back");
    backContainerUrl = back == null || "".equals(back) ? null : back;
    if (backContainerUrl != null) {
      if (URI.create(back).getQuery() != null) {
        backContainerUrl += "&";
      } else {
        backContainerUrl += "?";
      }
    }
    query = q == null ? "" : q;
    doSearch();
  }

  /**
   * Trigger the search to users Groups
   */
  public void search() {
    try {

      String redirectTo = getNavigation().getApplicationUrl() + "usergroups?q=" + query
          + (backContainerUrl != null ? "&back=" + backContainerUrl : "");

      if (redirectTo.endsWith("?")) {
        redirectTo = redirectTo.substring(0, redirectTo.lastIndexOf("?"));
      }

      if (redirectTo.endsWith("&")) {
        redirectTo = redirectTo.substring(0, redirectTo.lastIndexOf("?"));
      }

      redirect(redirectTo);
    } catch (final IOException e) {
      BeanHelper.error(e.getMessage());
      LOGGER.error(e);
    }
  }

  /**
   * Do the search
   */
  public void doSearch() {
    final UserGroupService controller = new UserGroupService();
    userGroups = controller.searchByName(query, Imeji.adminUser);
  }

  /**
   * Remove a {@link UserGroup}
   *
   * @param group
   * @return
   */
  public String remove() {
    final String id = FacesContext.getCurrentInstance().getExternalContext()
        .getRequestParameterMap().get("group");
    final UserGroupService c = new UserGroupService();
    UserGroup group;
    try {
      group = c.retrieve(id, sessionUser);
      if (group != null) {
        final UserGroupService controller = new UserGroupService();
        controller.delete(group, sessionUser);
      }
    } catch (final Exception e) {
      BeanHelper.error("Error removing group");
      LOGGER.error(e);
    }
    return "pretty:";
  }

  /**
   * @return the userGroups
   */
  public Collection<UserGroup> getUserGroups() {
    return userGroups;
  }

  /**
   * @param userGroups the userGroups to set
   */
  public void setUserGroups(Collection<UserGroup> userGroups) {
    this.userGroups = userGroups;
  }

  /**
   * @return the sessionUser
   */
  public User getSessionUser() {
    return sessionUser;
  }

  /**
   * @param sessionUser the sessionUser to set
   */
  public void setSessionUser(User sessionUser) {
    this.sessionUser = sessionUser;
  }

  /**
   * @return the query
   */
  public String getQuery() {
    return query;
  }

  /**
   * @param query the query to set
   */
  public void setQuery(String query) {
    this.query = query;
  }

  /**
   * @return the backContainerUrl
   */
  public String getBackContainerUrl() {
    return backContainerUrl;
  }

  /**
   * @param backContainerUrl the backContainerUrl to set
   */
  public void setBackContainerUrl(String backContainerUrl) {
    this.backContainerUrl = backContainerUrl;
  }
}
