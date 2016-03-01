package de.mpg.imeji.presentation.user;

import static de.mpg.imeji.presentation.user.util.EmailClient.isValidEmail;

import java.io.Serializable;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;
import javax.faces.model.SelectItem;
import javax.validation.constraints.AssertTrue;

import org.apache.http.auth.AUTH;
import org.apache.jena.atlas.lib.ListUtils;
import org.apache.log4j.Logger;

import com.hp.hpl.jena.sparql.pfunction.library.container;
import com.ocpsoft.pretty.PrettyContext;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.Imeji;
import de.mpg.imeji.logic.auth.util.AuthUtil;
import de.mpg.imeji.logic.controller.ItemController;
import de.mpg.imeji.logic.controller.ShareController;
import de.mpg.imeji.logic.controller.ShareController.ShareRoles;
import de.mpg.imeji.logic.controller.UserController;
import de.mpg.imeji.logic.controller.UserGroupController;
import de.mpg.imeji.logic.util.ObjectHelper;
import de.mpg.imeji.logic.util.UrlHelper;
import de.mpg.imeji.logic.vo.Album;
import de.mpg.imeji.logic.vo.CollectionImeji;
import de.mpg.imeji.logic.vo.Container;
import de.mpg.imeji.logic.vo.Grant;
import de.mpg.imeji.logic.vo.Item;
import de.mpg.imeji.logic.vo.Properties;
import de.mpg.imeji.logic.vo.User;
import de.mpg.imeji.logic.vo.UserGroup;
import de.mpg.imeji.presentation.beans.Navigation;
import de.mpg.imeji.presentation.history.HistoryUtil;
import de.mpg.imeji.presentation.session.SessionBean;
import de.mpg.imeji.presentation.user.util.EmailClient;
import de.mpg.imeji.presentation.user.util.EmailMessages;
import de.mpg.imeji.presentation.util.BeanHelper;
import de.mpg.imeji.presentation.util.ObjectLoader;

@ManagedBean(name = "ShareBean")
@ViewScoped
public class ShareBean implements Serializable {
  private static final long serialVersionUID = 8106762709528360926L;
  private static final Logger LOGGER = Logger.getLogger(ShareBean.class);
  @ManagedProperty(value = "#{SessionBean.user}")
  private User user;
  private String id;
  private URI uri;
  // The object (collection, album or item) which is going to be shared
  private Object shareTo;
  // the user whom the shareto object belongs
  private URI owner;
  private String title;
  private String profileUri;
  private String emailInput;
  private List<String> emailList = new ArrayList<String>();
  private List<String> errorList = new ArrayList<String>();
  private List<SharedHistory> sharedWith = new ArrayList<SharedHistory>();
  private boolean isAdmin;
  private List<String> selectedRoles = new ArrayList<String>();
  private boolean sendEmail = false;
  private UserGroup userGroup;
  private SharedObjectType type;
  // The url of the current share page (used for back link)
  private String pageUrl;
  private boolean hasContent = false;
  @ManagedProperty("#{UserGroups}")
  private UserGroupsBean userGroupsBean;
  @ManagedProperty("#{SessionBean}")
  private SessionBean sb;

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
    CollectionImeji collection = ObjectLoader.loadCollectionLazy(uri, user);
    if (collection != null) {
      this.shareTo = collection;
      this.profileUri = collection.getProfile() != null ? collection.getProfile().toString() : null;
      this.title = collection.getMetadata().getTitle();
      this.owner = collection.getCreatedBy();
      this.hasContent = hasContent(collection);
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
    Album album = ObjectLoader.loadAlbumLazy(uri, user);
    if (album != null) {
      this.shareTo = album;
      this.title = album.getMetadata().getTitle();
      this.owner = album.getCreatedBy();
      this.hasContent = hasContent(album);
    }
    this.init();
  }

  /**
   * Loaded when the shre component is called from the item page
   * 
   * @return
   * @throws Exception
   */
  public String getInitShareItem() throws ImejiException {
    this.type = SharedObjectType.ITEM;
    this.profileUri = null;
    this.shareTo = null;
    this.uri =
        HistoryUtil.extractURI(PrettyContext.getCurrentInstance().getRequestURL().toString());
    Item item = new ItemController().retrieveLazy(uri, user);
    if (item != null) {
      this.shareTo = item;
      this.title = item.getFilename();
      this.owner = item.getCreatedBy();
    }
    this.init();
    return "";
  }

  /**
   * Init method for {@link ShareBean}
   */
  public void init() {
    this.sharedWith = new ArrayList<SharedHistory>();
    this.emailList = new ArrayList<String>();
    this.errorList = new ArrayList<String>();
    this.selectedRoles = checkGrants(type, new ArrayList<String>());
    this.retrieveSharedUserWithGrants();
    this.emailInput = "";
    this.isAdmin = AuthUtil.staticAuth().administrate(this.user, shareTo);
    this.pageUrl = PrettyContext.getCurrentInstance().getRequestURL().toString()
        + PrettyContext.getCurrentInstance().getRequestQueryString();
    this.pageUrl = this.pageUrl.split("[&\\?]group=")[0];
    this.initShareWithGroup();
  }

  /**
   * Check in the url if a {@link UserGroup} should be shared with the current {@link Container}
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
   */
  public void update() {

    for (SharedHistory sh : sharedWith) {
      //Find out which were old permissions (only in real update eMail should be sent) and share should be updated
      List<String> oldSharedType = initShareMenu(sh.getUser(), sh.getGroup(), sh.getShareToUri(), profileUri);

      Comparator<String> descPriceComp = (String b1, String b2) -> b2.compareTo(b1);
      oldSharedType.sort(descPriceComp);
      List<String> newSharedType = sh.getSharedType(); 
      newSharedType.sort(descPriceComp);
      
      if (! oldSharedType.equals(newSharedType)) {
          sh.update();
          List<Grant> grants = ShareController.transformRolesToGrants(selectedRoles, getShareToUri());
          if (sendEmail) {
              sendEmail(sh.getUser(), title, grants);
          }
      }
    }
    reloadPage();
  }

  /**
   * Check the input and add all correct entry to the list of elements to be saved
   */
  public void share() {
    
    List<String> emailsToShare = new ArrayList<String>();
    emailsToShare = checkInput();
    if (isEmptyErrorList()){
      shareTo(emailsToShare);
      reloadPage();
    }
    
  }

  /**
   * Unshare the {@link Container} for one {@link User} (i.e, remove all {@link Grant} of this
   * {@link User} related to the {@link container})
   * 
   * @param sh
   */
  public void unshare(SharedHistory sh) {
    sh.getSharedType().clear();
    sh.update();
    
    if (sendEmail) {
       if (sh.getUser() != null ) {
           sendEmailUnshare(sh.getUser(), title );
       }
       else if (sh.getGroup() != null)
       {
         UserController c = new UserController(Imeji.adminUser);
         for (URI uriUser:sh.getGroup().getUsers()){
            try {
              sendEmailUnshare(c.retrieve(uriUser), title);
            } catch (ImejiException e) {
              // TODO Auto-generated catch block
              LOGGER.info("Error retrieving user "+uriUser+" who is member of the user group "+sh.getGroup().getName());
            }
         }
         
       }
       
    }
    reloadPage();
  }

  /**
   * Called when user share with a group
   */
  public void shareWithGroup() {
    List<String> l = new ArrayList<String>();
    l.add(userGroup.getId().toString());
    shareTo(l);
    reloadPage();
  }


  /**
   * Reload the current page
   */
  private void reloadPage() {
    try {
      UserController c = new UserController(user);
      user = c.retrieve(user.getId());
      Navigation navigation = (Navigation) BeanHelper.getApplicationBean(Navigation.class);
      if (AuthUtil.staticAuth().administrate(user, uri.toString())) {
        // user has still rights to read the collection
        FacesContext.getCurrentInstance().getExternalContext()
            .redirect(navigation.getApplicationUri() + pageUrl);
      } else if (AuthUtil.staticAuth().read(user, uri.toString())) {
        FacesContext.getCurrentInstance().getExternalContext()
            .redirect(navigation.getApplicationUri() + pageUrl.replace("share", ""));
      } else {
        // user has not right anymore to read the collection
        switch (type) {
          case COLLECTION:
            FacesContext.getCurrentInstance().getExternalContext()
                .redirect(navigation.getCollectionsUrl());
            break;
          case ALBUM:
            FacesContext.getCurrentInstance().getExternalContext()
                .redirect(navigation.getAlbumsUrl());
            break;
          case ITEM:
            FacesContext.getCurrentInstance().getExternalContext()
                .redirect(navigation.getBrowseUrl());
            break;
        }
      }
    } catch (Exception e) {
      throw new RuntimeException("Error reloading page " + pageUrl);
    }
  }

  /**
   * Check the input values with the emails
   * 
   * @return
   */
  private List<String> checkInput() {
    List<String> emails = new ArrayList<>();
    //Empty the error list from old failures
    setErrorList(new ArrayList<String>());

    if (getEmailInput() != null) {
      List<String> inputValues = Arrays.asList(getEmailInput().split("\\s*[|,;\\n]\\s*"));
      for (String value : inputValues) {
        if (isValidEmail(value) && !value.equalsIgnoreCase(sb.getUser().getEmail())) {
          try {
            UserController uc = new UserController(Imeji.adminUser);
            uc.retrieve(value);
            emails.add(value);

          } catch (Exception e) {
            this.errorList
                .add(sb.getMessage("error_share_invalid_user").replace("XXX_VALUE_XXX", value));
            BeanHelper
                .error(sb.getMessage("error_share_invalid_user").replace("XXX_VALUE_XXX", value));
            LOGGER.error(sb.getMessage("error_share_invalid_user").replace("XXX_VALUE_XXX", value));
          }
        } else {
          this.errorList
              .add(sb.getMessage("error_share_invalid_email").replace("XXX_VALUE_XXX", value));
          BeanHelper
              .error(sb.getMessage("error_share_invalid_email").replace("XXX_VALUE_XXX", value));
          LOGGER.error(sb.getMessage("error_share_invalid_email").replace("XXX_VALUE_XXX", value));
        }
      }
    }
    return emails;
  }

  /**
   * Retrieve all {@link User} with {@link Grant} for the current {@link Container}
   */
  public void retrieveSharedUserWithGrants() {
    UserController uc = new UserController(Imeji.adminUser);
    Collection<User> allUser = uc.searchByGrantFor(getShareToUri());
    sharedWith = new ArrayList<>();
    for (User u : allUser) {
      // Do not display the creator of this collection here
      if (!u.getId().toString().equals(owner.toString())) {
        sharedWith.add(new SharedHistory(u, type, getShareToUri(), profileUri, null));
      }
    }
    UserGroupController ugc = new UserGroupController();
    Collection<UserGroup> groups = ugc.searchByGrantFor(getShareToUri(), Imeji.adminUser);
    for (UserGroup group : groups) {
      sharedWith.add(new SharedHistory(group, type, getShareToUri(), profileUri, null));
    }
  }

  /**
   * Send email to the person to share with
   * 
   * @param dest
   * @param subject
   * @param grants
   */
  private void sendEmail(User dest, String subject, List<Grant> grants) {
    EmailClient emailClient = new EmailClient();
    SessionBean sb = (SessionBean) BeanHelper.getSessionBean(SessionBean.class);
    if (grants != null && grants.size() > 0) {
      try {
        this.getEmailMessage(this.user.getPerson().getCompleteName(),
            dest.getPerson().getCompleteName(), title, getLinkToSharedObject());
      } catch (Exception e) {
        BeanHelper.error(sb.getMessage("error") + ": Email not sent\n" + "User: " + user
            + "\nDestination:" + dest);
      }
      try {
        this.addRoles(grants);
        emailClient.sendMail(dest.getEmail(), null,
            subject.replaceAll("XXX_INSTANCE_NAME_XXX", sb.getInstanceName()), this.emailInput);
      } catch (Exception e) {
        LOGGER.error("Error sending email", e);
        BeanHelper.error(sb.getMessage("error") + ": Email not sent");
      }
    }
  }
  
  /**
   * Send email to the person to share with
   * 
   * @param dest
   * @param subject
   * @param grants
   */
  private void sendEmailUnshare(User dest, String subject) {
    EmailClient emailClient = new EmailClient();
    SessionBean sb = (SessionBean) BeanHelper.getSessionBean(SessionBean.class);
    
    try {
      this.emailInput = new EmailMessages().getUnshareMessage(this.user.getPerson().getCompleteName(), dest.getPerson().getCompleteName(), title, getLinkToSharedObject());
      emailClient.sendMail(dest.getEmail(), null,
            subject.replaceAll("XXX_INSTANCE_NAME_XXX", sb.getInstanceName()), this.emailInput);
      } catch (Exception e) {
        BeanHelper.error(sb.getMessage("error") + ": Email not sent\n" + "User: " + user
            + "\nDestination:" + dest);
      }
  }

  private void addRoles(List<Grant> grants) {
    String grantsStr = "";
    List<String> roles = ShareController.transformGrantsToRoles(grants, getShareToUri());

    //Addition for the metadata Profile stuff
    List<String> metadataProfileRoles = getProfileUri() != null ? 
               ShareController.transformGrantsToRoles(grants, getProfileUri()):
                new ArrayList<String>();
    for (String profileRole:metadataProfileRoles){
          if (profileRole.equals("EDIT")) {
                roles.add("EDIT_PROFILE");
          }
    }
    
    SessionBean sb = (SessionBean) BeanHelper.getSessionBean(SessionBean.class);
    if (this.type.equals(SharedObjectType.ALBUM)) {
      for (int i = 0; i < roles.size(); i++) {
        String role = roles.get(i);
        switch (role) {
          case "READ":
            grantsStr += "- " + sb.getLabel("album_share_read") + "\n";
            break;
          case "CREATE":
            grantsStr += "- " + sb.getLabel("album_share_image_add") + "\n";
            break;
          case "EDIT":
            grantsStr += "- " + sb.getLabel("album_share_album_edit") + "\n";
            break;
          case "ADMIN":
            grantsStr += "- " + sb.getLabel("album_share_admin") + "\n";
            break;
        }
      }
    }
    if (this.type.equals(SharedObjectType.COLLECTION)) {
      for (int i = 0; i < roles.size(); i++) {
        String role = roles.get(i);
        switch (role) {
          case "READ":
            grantsStr += "- " + sb.getLabel("collection_share_read") + "\n";
            break;
          case "CREATE":
            grantsStr += "- " + sb.getLabel("collection_share_image_upload") + "\n";
            break;
          case "EDIT_ITEM":
            grantsStr += "- " + sb.getLabel("collection_share_image_edit") + "\n";
            break;
          case "DELETE_ITEM":
            grantsStr += "- " + sb.getLabel("collection_share_image_delete") + "\n";
            break;
          case "EDIT":
            grantsStr += "- " + sb.getLabel("collection_share_collection_edit") + "\n";
            break;
          case "EDIT_PROFILE":
            grantsStr += "- " + sb.getLabel("collection_share_profile_edit")+":  "+getProfileUri() + "\n";
            break;
          case "ADMIN":
            grantsStr += "- " + sb.getLabel("collection_share_admin") + "\n";
            break;
        }
      }
    }
    
    if (this.type.equals(SharedObjectType.ITEM)) {
      for (int i = 0; i < roles.size(); i++) {
        String role = roles.get(i);
        switch (role) {
          case "READ":
            grantsStr += "- " + sb.getLabel("collection_share_read");
            break;
        }
      }
    }
    this.emailInput = this.emailInput.replaceAll("XXX_RIGHTS_XXX", grantsStr.trim());
  }

  /**
   * Send an Email to all {@link User} of a {@link UserGroup}
   * 
   * @param group
   * @param subject
   */
  private void sendEmailToGroup(UserGroup group, String subject) {
    UserController c = new UserController(Imeji.adminUser);
    SessionBean sb = (SessionBean) BeanHelper.getSessionBean(SessionBean.class);
    for (URI uri : group.getUsers()) {
      try {
        List<Grant> grants =
            ShareController.transformRolesToGrants(selectedRoles, getLinkToSharedObject());
        // User u = ObjectLoader.loadUser(uri, Imeji.adminUser);
        // GrantController gc = new GrantController();
        // gc.addGrants(u, grants, u);
        sendEmail(c.retrieve(uri), subject, grants);
      } catch (Exception e) {
        LOGGER.error("Error sending email", e);
        BeanHelper.error(sb.getMessage("error") + ": Email not sent");
      }
    }
  }
  
  
  /**
   * Share the {@link Container} with the {@link List} of {@link User} and {@link UserGroup}
   * 
   * @param toList
   */
  public void shareTo(List<String> toList) {
    List<String> notShared = new ArrayList<String>();
    for (String to : toList) {
      List<Grant> grants = ShareController.transformRolesToGrants(selectedRoles, getShareToUri());
      try {
        User toUser = null;
        UserGroup toGroup = null;

        if (isValidEmail(to)) {
          toUser = ObjectLoader.loadUser(to, Imeji.adminUser);
        } else {
          toGroup = retrieveGroup(to);
        }

          share(sb.getUser(), toUser, toGroup, uri.toString(), profileUri, selectedRoles);

            if (sendEmail) {
              if (toUser != null) {
                sendEmail(toUser, title, grants);
              } else if (toGroup != null) {
                sendEmailToGroup(toGroup, title);
              }
            }
      } catch (Exception e) {
        LOGGER.error("Error by sharing ", e);
      }
    }
    clearError();
  }

  /**
   * Share...
   * 
   * @param fromUser
   * @param toUser
   * @param toGroup
   * @param uri
   * @param profileUri
   * @param rolesMenu
   * @throws Exception
   */
  public static void share(User fromUser, User toUser, UserGroup toGroup, String uri,
      String profileUri, List<String> rolesMenu) throws Exception {
    ShareController shareController = new ShareController();
    if (toUser != null) {
      shareController.shareToUser(fromUser, toUser, uri, rolesMenu);
    } else if (toGroup != null) {
      shareController.shareToGroup(fromUser, toGroup, uri, rolesMenu);
    }
  }

  /**
   * Initialize the ShareMenu
   * 
   * @param user
   * @param group
   * @param uri
   * @param profileUri
   * @return
   */
  public static List<String> initShareMenu(User user, UserGroup group, String uri,
      String profileUri) {
    Collection<Grant> grants = user != null ? user.getGrants() : group.getGrants();
    List<String> menu = ShareController.transformGrantsToRoles((List<Grant>) grants, uri);
    if (profileUri != null) {
      List<String> profileMenu =
          ShareController.transformGrantsToRoles((List<Grant>) grants, profileUri);
      if (profileMenu.contains(ShareRoles.EDIT.toString())) {
          menu.add(ShareRoles.EDIT_PROFILE.toString());
      }
    }

    return menu;
  }

  private void getEmailMessage(String from, String to, String name, String link) {
    EmailMessages emailMessages = new EmailMessages();
    this.emailInput = "";
    if (this.type.equals(SharedObjectType.COLLECTION)) {
      this.emailInput = emailMessages.getSharedCollectionMessage(from, to, name, link);
    }
    if (this.type.equals(SharedObjectType.ALBUM)) {
      this.emailInput = emailMessages.getSharedAlbumMessage(from, to, name, link);
    }
    if (this.type.equals(SharedObjectType.ITEM)) {
      this.emailInput = emailMessages.getSharedItemMessage(from, to, name, link);
    }
  }
  
  /**
   * Search a {@link UserGroup} by name
   * 
   * @param uri
   * @return
   */
  private UserGroup retrieveGroup(String uri) {
    UserGroupController c = new UserGroupController();
    try {
      return c.read(uri, Imeji.adminUser);
    } catch (Exception e) {
      return null;
    }
  }

  /**
   * REturn true if the {@link Container} has item
   * 
   * @param c
   * @return
   */
  private boolean hasContent(Container c) {
    ItemController ic = new ItemController();
    return ic.searchAndSetContainerItems(c, user, 1, 0).getImages().size() > 0;
  }

  public void clearError() {
    errorList.clear();
  }

  public User getUser() {
    return user;
  }

  public void setUser(User user) {
    this.user = user;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getEmailInput() {
    return emailInput;
  }

  public void setEmailInput(String emailInput) {
    this.emailInput = emailInput.toLowerCase();
  }

  public List<String> getEmailList() {
    return emailList;
  }

  public void setEmailList(List<String> emailList) {
    this.emailList = emailList;
  }

  public List<String> getErrorList() {
    return errorList;
    
  }

  public void setErrorList(List<String> errorList) {
    this.errorList = errorList;
  }

  public boolean isAdmin() {
    return isAdmin;
  }

  public void setAdmin(boolean isAdmin) {
    this.isAdmin = isAdmin;
  }

  public void checkGrants(List<String> selectedGrants) {
    this.selectedRoles = checkGrants(type, selectedGrants);
  }

  /**
   * Check that Grants are consistent
   */
  public static List<String> checkGrants(SharedObjectType type, List<String> selectedGrants) {
    switch (type) {
      case COLLECTION:
        if (selectedGrants.contains("ADMIN")) {
          selectedGrants.clear();
          selectedGrants.add(ShareRoles.READ.toString());
          selectedGrants.add(ShareRoles.CREATE.toString());
          selectedGrants.add(ShareRoles.EDIT_ITEM.toString());
          selectedGrants.add(ShareRoles.DELETE_ITEM.toString());
          selectedGrants.add(ShareRoles.EDIT.toString());
          selectedGrants.add(ShareRoles.EDIT_PROFILE.toString());
          selectedGrants.add(ShareRoles.ADMIN.toString());
        } else {
          if (!selectedGrants.contains("READ"))
            selectedGrants.add(ShareRoles.READ.toString());
        }
        break;
      case ALBUM:
        if (selectedGrants.contains("ADMIN")) {
          selectedGrants.clear();
          selectedGrants.add(ShareRoles.READ.toString());
          selectedGrants.add(ShareRoles.CREATE.toString());
          selectedGrants.add(ShareRoles.EDIT.toString());
          selectedGrants.add(ShareRoles.ADMIN.toString());
        } else {
          if (!selectedGrants.contains("READ"))
            selectedGrants.add(ShareRoles.READ.toString());
        }
        break;
      case ITEM:
        selectedGrants.clear();
        selectedGrants.add(ShareRoles.READ.toString());
        break;
    }
    return selectedGrants;
  }

  public List<String> getSelectedGrants() {
    return selectedRoles;
  }

  public void setSelectedGrants(List<String> selectedGrants) {
    this.selectedRoles = selectedGrants;
  }

  public String getShareToUri() {
    if (shareTo instanceof Properties) {
      return ((Properties) shareTo).getId().toString();
    }
    return null;
  }

  private String getLinkToSharedObject() {
    Navigation navigation = (Navigation) BeanHelper.getApplicationBean(Navigation.class);
    switch (type) {
      case COLLECTION:
        return navigation.getCollectionUrl() + ((Properties) shareTo).getIdString();
      case ALBUM:
        return navigation.getAlbumUrl() + ((Properties) shareTo).getIdString();
      case ITEM:
        return navigation.getItemUrl() + ((Properties) shareTo).getIdString();
    }
    return null;
  }

  public Object getShareTo() {
    return shareTo;
  }

  public void setShareToUri(Object shareTo) {
    this.shareTo = shareTo;
  }

  public String getProfileUri() {
    return profileUri;
  }

  public void setProfileUri(String profileUri) {
    this.profileUri = profileUri;
  }

  public List<SharedHistory> getSharedWith() {
    return sharedWith;
  }

  public void setSharedWith(List<SharedHistory> sharedWith) {
    this.sharedWith = sharedWith;
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

  /**
   * @return the hasContent
   */
  public boolean isHasContent() {
    return hasContent;
  }

  /**
   * @param hasContent the hasContent to set
   */
  public void setHasContent(boolean hasContent) {
    this.hasContent = hasContent;
  }

  public SessionBean getSb() {
    return sb;
  }

  public void setSb(SessionBean sb) {
    this.sb = sb;
  }

  public int getSharedWithNum() {
    return getSharedWith().size();
  }

  /**
   * Menu for sharing collection
   * 
   * @return
   */
  public List<SelectItem> getShareCollectionGrantItems() {
    List<SelectItem> itemList = new ArrayList<SelectItem>();
    itemList.add(new SelectItem(ShareRoles.READ, sb.getLabel("collection_share_read")));
    itemList.add(new SelectItem(ShareRoles.CREATE, sb.getLabel("collection_share_image_upload")));
    itemList.add(new SelectItem(ShareRoles.EDIT_ITEM, sb.getLabel("collection_share_image_edit")));
    itemList
        .add(new SelectItem(ShareRoles.DELETE_ITEM, sb.getLabel("collection_share_image_delete")));
    itemList.add(new SelectItem(ShareRoles.EDIT, sb.getLabel("collection_share_collection_edit")));
    if (AuthUtil.staticAuth().administrate(sb.getUser(), profileUri)) {
      itemList.add(
          new SelectItem(ShareRoles.EDIT_PROFILE, sb.getLabel("collection_share_profile_edit")));
    }
    itemList.add(new SelectItem(ShareRoles.ADMIN, sb.getLabel("collection_share_admin")));
    return itemList;
  }

  /**
   * Menu for sharing items
   * 
   * @return
   */
  public List<SelectItem> getShareItemGrantItems() {
    List<SelectItem> itemList = new ArrayList<SelectItem>();
    itemList.add(new SelectItem(ShareRoles.READ, sb.getLabel("collection_share_read")));
    return itemList;
  }

  /**
   * Menu for sharing Album
   * 
   * @return
   */
  public List<SelectItem> getShareAlbumGrantItems() {
    List<SelectItem> itemList = new ArrayList<SelectItem>();
    itemList.add(new SelectItem(ShareRoles.READ, sb.getLabel("album_share_read")));
    itemList.add(new SelectItem(ShareRoles.CREATE, sb.getLabel("album_share_image_add")));
    itemList.add(new SelectItem(ShareRoles.EDIT, sb.getLabel("album_share_album_edit")));
    itemList.add(new SelectItem(ShareRoles.ADMIN, sb.getLabel("album_share_admin")));
    return itemList;
  }


  public UserGroupsBean getUserGroupsBean() {
    return userGroupsBean;
  }

  public void setUserGroupsBean(UserGroupsBean ugroupsBean) {
    this.userGroupsBean = ugroupsBean;
  }
  
  public boolean isEmptyErrorList(){
      return ( this.errorList.size() == 0 );
  }
}
