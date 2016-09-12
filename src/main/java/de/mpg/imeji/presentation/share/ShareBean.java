package de.mpg.imeji.presentation.share;

import java.io.Serializable;
import java.net.URI;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;

import org.apache.log4j.Logger;

import com.ocpsoft.pretty.PrettyContext;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.Imeji;
import de.mpg.imeji.logic.controller.resource.AlbumController;
import de.mpg.imeji.logic.controller.resource.CollectionController;
import de.mpg.imeji.logic.controller.resource.ItemController;
import de.mpg.imeji.logic.security.util.AuthUtil;
import de.mpg.imeji.logic.user.collaboration.email.EmailMessages;
import de.mpg.imeji.logic.user.collaboration.email.EmailService;
import de.mpg.imeji.logic.user.collaboration.invitation.InvitationBusinessController;
import de.mpg.imeji.logic.user.collaboration.share.ShareBusinessController.ShareRoles;
import de.mpg.imeji.logic.user.controller.GroupBusinessController;
import de.mpg.imeji.logic.util.UrlHelper;
import de.mpg.imeji.logic.vo.Album;
import de.mpg.imeji.logic.vo.CollectionImeji;
import de.mpg.imeji.logic.vo.Item;
import de.mpg.imeji.logic.vo.Properties;
import de.mpg.imeji.logic.vo.User;
import de.mpg.imeji.logic.vo.UserGroup;
import de.mpg.imeji.logic.vo.util.ObjectHelper;
import de.mpg.imeji.presentation.beans.Navigation;
import de.mpg.imeji.presentation.beans.SuperBean;
import de.mpg.imeji.presentation.history.HistoryUtil;
import de.mpg.imeji.presentation.user.UserGroupsBean;
import de.mpg.imeji.presentation.util.BeanHelper;

@ManagedBean(name = "ShareBean")
@ViewScoped
public class ShareBean extends SuperBean implements Serializable {
  private static final long serialVersionUID = 8106762709528360926L;
  private static final Logger LOGGER = Logger.getLogger(ShareBean.class);
  private String id;
  private URI uri;
  // The object (collection, album or item) which is going to be shared
  private Object shareTo;
  // the user whom the shared object belongs
  private URI owner;
  private String title;
  private String backUrl;
  private String profileUri;
  private boolean isAdmin;
  private boolean sendEmail = false;
  private UserGroup userGroup;
  private SharedObjectType type;
  // The url of the current share page (used for back link)
  private String pageUrl;
  @ManagedProperty("#{UserGroups}")
  private UserGroupsBean userGroupsBean;
  @ManagedProperty("#{SessionBean.instanceName}")
  private String instanceName;
  private ShareInput input;
  private ShareList shareList;
  private ShareList shareListCollection;
  private String collectionShareUrl;
  private String collectionName;
  private Object sharedObject;

  public enum SharedObjectType {
    COLLECTION, ALBUM, ITEM
  }

  /**
   * Init {@link ShareBean} for {@link CollectionImeji}
   *
   * @throws ImejiException
   *
   * @throws Exception
   */
  public void initShareCollection() throws ImejiException {
    this.shareTo = null;
    this.profileUri = null;
    this.type = SharedObjectType.COLLECTION;
    this.uri = ObjectHelper.getURI(CollectionImeji.class, getId());
    CollectionImeji collection = new CollectionController().retrieveLazy(uri, getSessionUser());
    if (collection != null) {
      this.shareTo = collection;
      this.profileUri = collection.getProfile() != null ? collection.getProfile().toString() : null;
      this.title = collection.getMetadata().getTitle();
      this.owner = collection.getCreatedBy();
      this.backUrl = getNavigation().getCollectionUrl() + collection.getIdString();
      this.sharedObject = collection;
    }
    this.init();
  }

  /**
   * Init {@link ShareBean} for {@link Album}
   *
   * @throws Exception
   */
  public void initShareAlbum() throws ImejiException {
    this.type = SharedObjectType.ALBUM;
    this.shareTo = null;
    this.profileUri = null;
    this.uri = ObjectHelper.getURI(Album.class, getId());
    Album album = new AlbumController().retrieveLazy(uri, getSessionUser());
    if (album != null) {
      this.shareTo = album;
      this.title = album.getMetadata().getTitle();
      this.owner = album.getCreatedBy();
      this.backUrl = getNavigation().getAlbumUrl() + album.getIdString();
      this.sharedObject = album;
    }
    this.init();
  }

  /**
   * Loaded when the shre component is called from the item page
   *
   * @return
   * @throws Exception
   */
  public String initShareItem() throws ImejiException {
    this.type = SharedObjectType.ITEM;
    this.profileUri = null;
    this.shareTo = null;
    this.uri =
        HistoryUtil.extractURI(PrettyContext.getCurrentInstance().getRequestURL().toString());
    Item item = new ItemController().retrieveLazy(uri, getSessionUser());
    if (item != null) {
      this.shareTo = item;
      this.title = item.getFilename();
      this.owner = item.getCreatedBy();
      this.backUrl = getNavigation().getItemUrl() + item.getIdString();
      this.shareListCollection = new ShareList(owner, item.getCollection().toString(), profileUri,
          SharedObjectType.COLLECTION, getSessionUser(), getLocale());
      this.collectionShareUrl = getNavigation().getCollectionUrl()
          + ObjectHelper.getId(item.getCollection()) + "/" + Navigation.SHARE.getPath();
      this.collectionName = new CollectionController()
          .retrieve(item.getCollection(), getSessionUser()).getMetadata().getTitle();
      this.sharedObject = item;
    }
    this.init();
    return "";
  }

  /**
   * Init method for {@link ShareBean}
   *
   * @throws ImejiException
   */
  public void init() throws ImejiException {
    input = new ShareInput(uri.toString(), type, profileUri, getSessionUser(), getLocale(),
        instanceName);
    shareList =
        new ShareList(owner, uri.toString(), profileUri, type, getSessionUser(), getLocale());
    isAdmin = AuthUtil.staticAuth().administrate(getSessionUser(), shareTo);
    pageUrl = PrettyContext.getCurrentInstance().getRequestURL().toString()
        + PrettyContext.getCurrentInstance().getRequestQueryString();
    pageUrl = pageUrl.split("[&\\?]group=")[0];
    initShareWithGroup();
  }

  /**
   * Check in the url if a {@link UserGroup} should be shared with the currentContainer
   */
  private void initShareWithGroup() {
    this.userGroup = null;
    String groupToShareWithUri = UrlHelper.getParameterValue("group");
    if (groupToShareWithUri != null) {
      UserGroup group = retrieveGroup(groupToShareWithUri);
      if (group != null) {
        userGroup = group;
      }
    }
  }

  /**
   * Update the page accodring to new changes
   *
   * @return
   * @throws ImejiException
   */
  public void update() throws ImejiException {
    for (ShareListItem item : shareList.getItems()) {
      boolean modified = item.update();
      if (sendEmail && modified) {
        sendEmailForShare(item, title);
      }
    }
    for (ShareListItem item : shareList.getInvitations()) {
      item.updateInvitation();
    }
    reloadPage();
  }

  /**
   * Check the input and add all correct entry to the list of elements to be saved
   */
  public void share() {
    boolean reload = input.share();
    sendEmailForInput();
    if (reload) {
      reloadPage();
    }
  }

  /**
   * Unshare...
   * 
   * @param item
   * @throws ImejiException
   */
  public void unshare(ShareListItem item) throws ImejiException {
    item.getRoles().clear();
    if (item.getInvitation() != null) {
      item.updateInvitation();
      shareList.getInvitations().remove(item);
    } else {
      item.update();
      shareList.getItems().remove(item);
    }
  }

  /**
   * Invite the new users
   */
  public void invite() {
    input.sendInvitations();
    reloadPage();
  }

  /**
   * When input is triggered, check if email should sent. If yes, proceed
   */
  private void sendEmailForInput() {
    if (sendEmail) {
      for (ShareListItem item : input.getExistingUsersAsShareListItems()) {
        sendEmailForShare(item, title);
      }
    }
  }

  /**
   * Cancel Invitation
   *
   * @throws ImejiException
   */
  public void cancelInvitation(ShareListItem item) throws ImejiException {
    new InvitationBusinessController().cancel(item.getInvitation().getId());
    reloadPage();
  }

  /**
   * Unselect all roles
   *
   * @param sh
   */
  public void selectNone(ShareListItem item) {
    item.getRoles().clear();
  }

  /**
   * Select all roles, i.e give admin role
   * 
   * @param item
   */
  public void selectAll(ShareListItem item) {
    item.getRoles().add(ShareRoles.ADMIN.toString());
    item.checkRoles(true);
  }

  /**
   * Called when user share with a group
   */
  public void shareWithGroup() {
    ShareListItem groupListItem = new ShareListItem(userGroup, type, uri.toString(), profileUri,
        null, getSessionUser(), getLocale());
    groupListItem.setRoles(input.getMenu().getRoles());
    if (groupListItem.update() && sendEmail) {
      sendEmailForShare(groupListItem, title);
    }
    reloadPage();
  }


  /**
   * Remove an unknow Email from the list (no invitation will be sent to him)
   *
   * @param pos
   */
  public void removeUnknowEmail(int pos) {
    input.getUnknownEmails().remove(pos);
  }


  /**
   * Reload the current page
   */
  public void reloadPage() {
    try {
      if (AuthUtil.staticAuth().administrate(getSessionUser(), uri.toString())) {
        // user has still rights to read the collection
        FacesContext.getCurrentInstance().getExternalContext()
            .redirect(getNavigation().getApplicationUri() + pageUrl);
      } else if (AuthUtil.staticAuth().read(getSessionUser(), uri.toString())) {
        FacesContext.getCurrentInstance().getExternalContext()
            .redirect(getNavigation().getApplicationUri() + pageUrl.replace("share", ""));
      } else {
        // user has not right anymore to read the collection
        switch (type) {
          case COLLECTION:
            FacesContext.getCurrentInstance().getExternalContext()
                .redirect(getNavigation().getCollectionsUrl());
            break;
          case ALBUM:
            FacesContext.getCurrentInstance().getExternalContext()
                .redirect(getNavigation().getAlbumsUrl());
            break;
          case ITEM:
            FacesContext.getCurrentInstance().getExternalContext()
                .redirect(getNavigation().getBrowseUrl());
            break;
        }
      }
    } catch (Exception e) {
      LOGGER.error("Error reloading page " + pageUrl, e);
    }
  }

  /**
   * Send an Email...
   *
   * @param email
   * @param subject
   * @param body
   */
  private void sendEmail(String email, String subject, String body) {
    try {
      new EmailService().sendMail(email, null,
          subject.replaceAll("XXX_INSTANCE_NAME_XXX", instanceName), body);
    } catch (Exception e) {
      LOGGER.error("Error sending email", e);
      BeanHelper.error("Error: Email not sent");
    }
  }

  /**
   * Send Email for each ShareListItem (User of Group) that object has been shared and with which
   * Grants
   *
   * @param item
   * @param subject
   */
  private void sendEmailForShare(ShareListItem item, String subject) {
    for (User user : item.getUsers()) {
      ShareEmailMessage emailMessage =
          new ShareEmailMessage(user.getPerson().getCompleteName(), title, getLinkToSharedObject(),
              getShareToUri(), profileUri, item.getRoles(), type, getSessionUser(), getLocale());
      sendEmail(user.getEmail(), subject.replaceAll("XXX_INSTANCE_NAME_XXX", instanceName),
          emailMessage.getBody());
    }
  }


  /**
   * Send email to the user(s) for which the object has been unshared
   *
   * @param dest
   * @param subject
   * @param grants
   */
  private void sendEmailUnshare(ShareListItem item, String subject) {
    subject = subject.replaceAll("XXX_INSTANCE_NAME_XXX", instanceName);
    for (User user : item.getUsers()) {
      String body = EmailMessages.getUnshareMessage(getSessionUser().getPerson().getCompleteName(),
          user.getPerson().getCompleteName(), title, getLinkToSharedObject(), getLocale());
      sendEmail(user.getEmail(), subject, body);
    }
  }

  public String getLabelConfirmInvitation() {
    return Imeji.RESOURCE_BUNDLE.getLabel("share_confirm_invitation", getLocale())
        .replace("XXX_INSTANCE_NAME_XXX", getInstanceName());
  }

  /**
   * Search a {@link UserGroup} by name
   *
   * @param uri
   * @return
   */
  private UserGroup retrieveGroup(String uri) {
    GroupBusinessController c = new GroupBusinessController();
    try {
      return c.retrieve(uri, Imeji.adminUser);
    } catch (Exception e) {
      return null;
    }
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public boolean isAdmin() {
    return isAdmin;
  }

  public void setAdmin(boolean isAdmin) {
    this.isAdmin = isAdmin;
  }

  public String getShareToUri() {
    if (shareTo instanceof Properties) {
      return ((Properties) shareTo).getId().toString();
    }
    return null;
  }

  private String getLinkToSharedObject() {
    switch (type) {
      case COLLECTION:
        return getNavigation().getCollectionUrl() + ((Properties) shareTo).getIdString();
      case ALBUM:
        return getNavigation().getAlbumUrl() + ((Properties) shareTo).getIdString();
      case ITEM:
        return getNavigation().getItemUrl() + ((Properties) shareTo).getIdString();
    }
    return null;
  }

  public Object getShareTo() {
    return shareTo;
  }

  public void setShareToUri(Object shareTo) {
    this.shareTo = shareTo;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  /**
   * @return the sendEmail
   */
  public boolean isSendEmail() {
    return sendEmail;
  }

  /**
   * @param sendEmail the sendEmail to set
   */
  public void setSendEmail(boolean sendEmail) {
    this.sendEmail = sendEmail;
  }

  /**
   * @return the userGroup
   */
  public UserGroup getUserGroup() {
    return userGroup;
  }

  /**
   * @param userGroup the userGroup to set
   */
  public void setUserGroup(UserGroup userGroup) {
    this.userGroup = userGroup;
  }

  public String getPageUrl() {
    return pageUrl;
  }

  public void setPageUrl(String pageUrl) {
    this.pageUrl = pageUrl;
  }

  public SharedObjectType getType() {
    return type;
  }

  public void setType(SharedObjectType type) {
    this.type = type;
  }

  public UserGroupsBean getUserGroupsBean() {
    return userGroupsBean;
  }

  public void setUserGroupsBean(UserGroupsBean ugroupsBean) {
    this.userGroupsBean = ugroupsBean;
  }

  /**
   * @return the input
   */
  public ShareInput getInput() {
    return input;
  }

  /**
   * @param input the input to set
   */
  public void setInput(ShareInput input) {
    this.input = input;
  }

  /**
   * @return the shareList
   */
  public ShareList getShareList() {
    return shareList;
  }

  /**
   * @param shareList the shareList to set
   */
  public void setShareList(ShareList shareList) {
    this.shareList = shareList;
  }

  /**
   * @return the backurl
   */
  public String getBackUrl() {
    return backUrl;
  }

  /**
   * @param backurl the backurl to set
   */
  public void setBackUrl(String backurl) {
    this.backUrl = backurl;
  }

  /**
   * @return the shareListCollection
   */
  public ShareList getShareListCollection() {
    return shareListCollection;
  }

  /**
   * @param shareListCollection the shareListCollection to set
   */
  public void setShareListCollection(ShareList shareListCollection) {
    this.shareListCollection = shareListCollection;
  }

  public String getCollectionShareUrl() {
    return collectionShareUrl;
  }

  /**
   * @return the instanceName
   */
  public String getInstanceName() {
    return instanceName;
  }

  /**
   * @param instanceName the instanceName to set
   */
  public void setInstanceName(String instanceName) {
    this.instanceName = instanceName;
  }

  /**
   * @return the collectionName
   */
  public String getCollectionName() {
    return collectionName;
  }

  /**
   * @param collectionName the collectionName to set
   */
  public void setCollectionName(String collectionName) {
    this.collectionName = collectionName;
  }

  public Object getSharedObject() {
    return sharedObject;
  }
}
