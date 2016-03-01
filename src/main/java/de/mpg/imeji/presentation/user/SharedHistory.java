package de.mpg.imeji.presentation.user;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import de.mpg.imeji.logic.vo.User;
import de.mpg.imeji.logic.vo.UserGroup;
import de.mpg.imeji.presentation.session.SessionBean;
import de.mpg.imeji.presentation.user.ShareBean.SharedObjectType;
import de.mpg.imeji.presentation.util.BeanHelper;

public class SharedHistory implements Serializable {
  private static final long serialVersionUID = -1637916656299359982L;
  private static final Logger LOGGER = Logger.getLogger(SharedHistory.class);
  private User user;
  private UserGroup group;
  private String shareToUri;
  private String profileUri;
  private SharedObjectType type;
  private List<String> sharedType = new ArrayList<String>();
  private String title;

  /**
   * Constructor with a {@link User}
   * 
   * @param user
   * @param isCollection
   * @param containerUri
   * @param profileUri
   * @param sharedType
   */
  public SharedHistory(User user, SharedObjectType type, String containerUri, String profileUri,
      String title) {
    this.user = user;
    this.type = type;
    this.shareToUri = containerUri;
    this.profileUri = profileUri;
    this.title = title;
    this.sharedType = ShareBean.initShareMenu(user, null, containerUri, profileUri);
  }

  /**
   * Constructor with a {@link UserGroup}
   * 
   * @param group
   * @param isCollection
   * @param containerUri
   * @param profileUri
   * @param sharedType
   */
  public SharedHistory(UserGroup group, SharedObjectType type, String containerUri,
      String profileUri, String title) {
    this.setGroup(group);
    this.type = type;
    this.shareToUri = containerUri;
    this.profileUri = profileUri;
    this.title = title;
    this.sharedType = ShareBean.initShareMenu(null, group, containerUri, profileUri);
  }

  public void revokeGrants() {
    this.getSharedType().clear();
    this.update();
  }

  public User getUser() {
    return user;
  }

  public void setUser(User user) {
    this.user = user;
  }

  public List<String> getSharedType() {
    return sharedType;
  }

  public void setSharedType(List<String> sharedType) {
    this.sharedType = ShareBean.checkGrants(type, sharedType);
  }

  /**
   * Update {@link Grants} the {@link SharedHistory} according to the new roles
   * 
   * @return
   */
  public String update() {
    SessionBean session = (SessionBean) BeanHelper.getSessionBean(SessionBean.class);
    try {
      ShareBean.share(session.getUser(), user, group, shareToUri, profileUri, sharedType);
    } catch (Exception e) {
      LOGGER.error(e);
    }
    // TODO: CHECK PRETTY PAGE
    return ((SessionBean) BeanHelper.getSessionBean(SessionBean.class))
        .getPrettySpacePage("pretty:shareCollection");
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

  /**
   * @param shareToUri the shareToUri to set
   */
  public void setShareToUri(String shareToUri) {
    this.shareToUri = shareToUri;
  }

  /**
   * @return the type
   */
  public SharedObjectType getType() {
    return type;
  }

  /**
   * @param type the type to set
   */
  public void setType(SharedObjectType type) {
    this.type = type;
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
}
