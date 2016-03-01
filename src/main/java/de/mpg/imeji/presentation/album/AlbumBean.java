/**
 * License: src/main/resources/license/escidoc.license
 */
package de.mpg.imeji.presentation.album;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;
import javax.faces.event.ValueChangeEvent;
import javax.faces.model.SelectItem;

import org.apache.commons.fileupload.FileUploadException;
import org.apache.log4j.Logger;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.exceptions.UnprocessableError;
import de.mpg.imeji.logic.controller.AlbumController;
import de.mpg.imeji.logic.controller.ItemController;
import de.mpg.imeji.logic.controller.UserController;
import de.mpg.imeji.logic.controller.exceptions.TypeNotAllowedException;
import de.mpg.imeji.logic.util.ObjectHelper;
import de.mpg.imeji.logic.util.UrlHelper;
import de.mpg.imeji.logic.vo.Album;
import de.mpg.imeji.logic.vo.Container;
import de.mpg.imeji.logic.vo.Item;
import de.mpg.imeji.logic.vo.Person;
import de.mpg.imeji.logic.vo.Properties.Status;
import de.mpg.imeji.logic.vo.User;
import de.mpg.imeji.presentation.beans.ContainerBean;
import de.mpg.imeji.presentation.beans.Navigation;
import de.mpg.imeji.presentation.history.HistorySession;
import de.mpg.imeji.presentation.image.ThumbnailBean;
import de.mpg.imeji.presentation.session.SessionBean;
import de.mpg.imeji.presentation.util.BeanHelper;
import de.mpg.imeji.presentation.util.CommonUtils;
import de.mpg.imeji.presentation.util.ObjectLoader;

/**
 * The javabean for the {@link Album}
 * 
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
@ManagedBean(name = "AlbumBean")
@ViewScoped
public class AlbumBean extends ContainerBean {
  private static final long serialVersionUID = -8161410292667767348L;
  protected SessionBean sessionBean = null;
  private Album album = null;
  private String id = null;
  private List<SelectItem> profilesMenu = new ArrayList<SelectItem>();
  private boolean active;
  private String tab;
  /**
   * True if the {@link AlbumBean} is used for the create page, else false
   */
  // protected boolean create;
  private boolean selected;
  private static final Logger LOGGER = Logger.getLogger(AlbumBean.class);
  /**
   * Maximum number of character displayed in the list for the description
   */
  private static final int DESCRIPTION_MAX_SIZE = 300;

  /**
   * Maximum number of items displayed on album start page
   */
  private static final int MAX_ITEM_NUM_VIEW = 13;
  /**
   * A small description when the description of the {@link Album} is too large for the list view
   */
  // private String smallDescription = null;
  private String description = "";
  private String descriptionFull = null;
  private ThumbnailBean thumbnail;
  // number of items which the current user is allowed to see
  private int allowedItemsSize;

  /**
   * Construct an {@link AlbumBean} from an {@link Album}
   * 
   * @param album
   * @throws Exception
   */
  public AlbumBean(Album album) throws Exception {
    this.album = album;
    if (album != null) {
      this.id = ObjectHelper.getId(album.getId());
      sessionBean = (SessionBean) BeanHelper.getSessionBean(SessionBean.class);
      if (sessionBean.getActiveAlbum() != null
          && sessionBean.getActiveAlbum().getId().equals(album.getId())) {
        active = true;
      }
      if (album.getId() != null) {
        findItems(sessionBean.getUser(), 1);
        loadItems(sessionBean.getUser(), 1);
        countItems();
        description = album.getMetadata().getDescription();
        descriptionFull = description;
        description = CommonUtils.removeTags(description);
        if (description != null && description.length() > DESCRIPTION_MAX_SIZE) {
          description = description.substring(0, DESCRIPTION_MAX_SIZE) + "...";
        }
        /*
         * Set Logo: if a logo is defined, use it, else take first picture of the album
         */
        if (album.getLogoUrl() != null) {
          thumbnail = new ThumbnailBean();
          thumbnail.setLink(album.getLogoUrl().toString());
        } else if (!getItems().isEmpty()) {
          thumbnail = new ThumbnailBean(getItems().get(0), false);
        }
      }
    }
  }

  /**
   * Construct an emtpy {@link AlbumBean}
   */
  public AlbumBean() {
    sessionBean = (SessionBean) BeanHelper.getSessionBean(SessionBean.class);
  }

  /**
   * Load the {@link Album} and its {@link Item} when the {@link AlbumBean} page is called, and
   * initialize it.
   * 
   * @throws Exception
   */
  public void initView() throws Exception {
    try {
      if (id != null) {
        album =
            ObjectLoader.loadAlbumLazy(ObjectHelper.getURI(Album.class, id), sessionBean.getUser());
        if (album != null) {
          findItems(sessionBean.getUser(), MAX_ITEM_NUM_VIEW);
          loadItems(sessionBean.getUser(), MAX_ITEM_NUM_VIEW);
          countItems();
          countAllowedItems();
          countDiscardedItems(sessionBean.getUser());
          if (sessionBean.getActiveAlbum() != null
              && sessionBean.getActiveAlbum().getId().equals(album.getId())) {
            active = true;
            // sessionBean.setActiveAlbum(album);
          }

          int myPrivateCount = getPrivateCount();
          if (myPrivateCount != 0) {
            BeanHelper.info(sessionBean.getMessage("album_Private_Content").replace("XXX_COUNT_XXX",
                myPrivateCount + ""));
          }
        }

      }
    } catch (Exception e) {
      LOGGER.error(e.getMessage());
      // Has to be in try/catch block, otherwise redirct from
      // HistoryFilter will not work.
      // Here simply do nothing
    }
  }

  /**
   * Initialize the album form to edit the metadata of the album
   */
  public void initEdit() {
    AlbumController ac = new AlbumController();
    try {
      setAlbum(ac.retrieveLazy(ObjectHelper.getURI(Album.class, id), sessionBean.getUser()));
      sessionBean.setSpaceLogoIngestImage(null);
      setIngestImage(null);
    } catch (Exception e) {
      BeanHelper.error(e.getMessage());
      LOGGER.error("Error init album edit", e);
    }
    if (UrlHelper.getParameterBoolean("start")) {
      try {
        upload();
      } catch (FileUploadException e) {
        BeanHelper.error("Could not upload the image " + e.getMessage());
      } catch (TypeNotAllowedException e) {
        BeanHelper.error("Could not upload the image " + e.getMessage());
      }
    }
  }

  /**
   * Return the link for the Cancel button
   * 
   * @return
   */
  public String getCancel() {
    Navigation nav = (Navigation) BeanHelper.getApplicationBean(Navigation.class);
    return nav.getAlbumUrl() + id + "/" + nav.getInfosPath();
  }

  @Override
  protected String getErrorMessageNoAuthor() {
    return "error_album_need_one_author";
  }

  /**
   * Listener for the discard comment
   * 
   * @param event
   */
  public void discardCommentListener(ValueChangeEvent event) {
    if (event.getNewValue() != null && event.getNewValue().toString().trim().length() > 0) {
      album.setDiscardComment(event.getNewValue().toString().trim());
    }
  }

  /**
   * getter
   * 
   * @return
   */
  @Override
  protected String getNavigationString() {
    return "pretty:";
  }

  /**
   * @return the id
   */
  public String getId() {
    return id;
  }

  /**
   * @param id the id to set
   */
  public void setId(String id) {
    this.id = id;
  }

  /**
   * getter
   * 
   * @return
   */
  public List<SelectItem> getProfilesMenu() {
    return profilesMenu;
  }

  /**
   * setter
   * 
   * @param profilesMenu
   */
  public void setProfilesMenu(List<SelectItem> profilesMenu) {
    this.profilesMenu = profilesMenu;
  }

  /**
   * Save (create or update) the {@link Album} in the database
   * 
   * @return
   * @throws Exception
   */
  public String save() throws Exception {
    if (update()) {
      Navigation navigation = (Navigation) BeanHelper.getApplicationBean(Navigation.class);
      FacesContext.getCurrentInstance().getExternalContext().redirect(navigation.getAlbumUrl()
          + ObjectHelper.getId(getAlbum().getId()) + "/" + navigation.getInfosPath() + "?init=1");

    }
    return "";
  }

  /**
   * Update the {@link Album} in the dabatase with the values defined in this {@link AlbumBean}
   * 
   * @return
   * @throws Exception
   */
  public boolean update() throws Exception {
    AlbumController ac = new AlbumController();
    try {

      Album icPre = ac.retrieveLazy(album.getId(), sessionBean.getUser());
      if (icPre.getLogoUrl() != null && album.getLogoUrl() == null) {
        ac.updateLogo(icPre, null, sessionBean.getUser());
      }
      ac.update(getAlbum(), sessionBean.getUser());
      // here separate update for the Logo only, as it will only be
      // allowed by edited collection through the web application
      // not yet for REST
      // getIngestImage is inherited from Container!

      if (sessionBean.getSpaceLogoIngestImage() != null) {
        ac.updateLogo(getAlbum(), sessionBean.getSpaceLogoIngestImage().getFile(),
            sessionBean.getUser());
        setIngestImage(null);
        sessionBean.setSpaceLogoIngestImage(null);
      }
      BeanHelper.info(sessionBean.getMessage("success_album_update"));
      return true;
    } catch (UnprocessableError e) {
      BeanHelper.cleanMessages();
      BeanHelper.error(sessionBean.getMessage("error_album_update"));
      List<String> listOfErrors = Arrays.asList(e.getMessage().split(";"));
      for (String errorM : listOfErrors) {
        BeanHelper.error(sessionBean.getMessage(errorM));
      }
      return false;
    }

  }

  /**
   * setter
   * 
   * @param album
   */
  public void setAlbum(Album album) {
    this.album = album;
  }

  /**
   * getter
   * 
   * @return
   */
  public Album getAlbum() {
    return album;
  }

  /**
   * Return the all author of this album as a single {@link String}
   * 
   * @return
   */
  @Override
  public String getPersonString() {
    String personString = "";
    for (Person p : album.getMetadata().getPersons()) {
      if (!"".equals(personString)) {
        personString += "; ";
      }
      personString += p.getFamilyName() + ", " + p.getGivenName();
    }
    return personString;
  }

  /**
   * setter
   * 
   * @param active
   */
  public void setActive(boolean active) {
    this.active = active;
  }

  /**
   * getter
   * 
   * @return
   */
  public boolean getActive() {
    return active;
  }

  /**
   * Make the current {@link Album} active
   * 
   * @return
   * @throws ImejiException
   * @throws IOException
   */
  public String makeActive(boolean addSelected) throws ImejiException, IOException {
    findItems(sessionBean.getUser(), getSize());
    sessionBean.setActiveAlbum(this.album);
    this.setActive(true);
    if (addSelected) {
      FacesContext.getCurrentInstance().getExternalContext()
          .redirect(((HistorySession) BeanHelper.getSessionBean(HistorySession.class))
              .getPreviousPage().getCompleteUrlWithHistory() + "&add_selected=1");
    }
    return "";
  }

  /**
   * Make the current {@link Album} inactive
   * 
   * @return
   */
  public String makeInactive() {
    sessionBean.setActiveAlbum(null);
    this.setActive(false);
    return "";
  }

  /**
   * Release the current {@link Album}
   * 
   * @return
   */
  public String release() {
    AlbumController ac = new AlbumController();
    try {
      ac.release(album, sessionBean.getUser());
      makeInactive();
      BeanHelper.info(sessionBean.getMessage("success_album_release"));
    } catch (Exception e) {
      BeanHelper.error(sessionBean.getMessage("error_album_release"));
      BeanHelper.error(e.getMessage());
      LOGGER.error("Issue during release", e);
    }
    return "pretty:";
  }

  /**
   * delete an {@link Album}
   * 
   * @return
   */
  public String delete() {
    AlbumController c = new AlbumController();
    try {
      makeInactive();
      c.delete(album, sessionBean.getUser());
      BeanHelper.info(sessionBean.getMessage("success_album_delete").replace("XXX_albumName_XXX",
          this.album.getMetadata().getTitle()));
    } catch (Exception e) {
      BeanHelper.error(sessionBean.getMessage("error_album_delete"));
      BeanHelper.error(e.getMessage());
      LOGGER.error("Error during delete album", e);
    }
    return sessionBean.getPrettySpacePage("pretty:albums");
  }

  /**
   * Discard the {@link AlbumImeji} of this {@link Album}
   * 
   * @return
   * @throws Exception
   */
  public String withdraw() throws Exception {
    AlbumController c = new AlbumController();
    try {
      c.withdraw(album, sessionBean.getUser());
      BeanHelper.info(sessionBean.getMessage("success_album_withdraw"));
    } catch (Exception e) {
      BeanHelper.error(sessionBean.getMessage("error_album_withdraw"));
      BeanHelper.error(e.getMessage());
      LOGGER.error("Error during withdraw album", e);
    }
    return "pretty:";
  }

  /**
   * True if the {@link Album} is selected in the album list
   * 
   * @return
   */
  public boolean getSelected() {
    if (sessionBean.getSelectedAlbums().contains(album.getId())) {
      selected = true;
    } else {
      selected = false;
    }
    return selected;
  }

  /**
   * setter: called when the user click on the select box to select the {@link Album}. Set the
   * status "selected" in the session
   * 
   * @param selected
   */
  public void setSelected(boolean selected) {
    if (selected) {
      if (!(sessionBean.getSelectedAlbums().contains(album.getId()))) {
        sessionBean.getSelectedAlbums().add(album.getId());
      }
    } else {
      sessionBean.getSelectedAlbums().remove(album.getId());
    }
    this.selected = selected;
  }

  /**
   * getter
   * 
   * @return the thumbnail
   */
  public ThumbnailBean getThumbnail() {
    return thumbnail;
  }

  /**
   * setter
   * 
   * @param thumbnail the thumbnail to set
   */
  public void setThumbnail(ThumbnailBean thumbnail) {
    this.thumbnail = thumbnail;
  }

  public String getFormattedDescription() {
    if (this.getAlbum() == null || this.getAlbum().getMetadata().getDescription() == null) {
      return "";
    }
    return this.getAlbum().getMetadata().getDescription().replaceAll("\n", "<br/>");
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getDescription() {
    return description;
  }

  public String getDescriptionFull() {
    return descriptionFull;
  }

  public void setDescriptionFull(String descriptionFull) {
    this.descriptionFull = descriptionFull;
  }

  public String getTab() {
    if (UrlHelper.getParameterValue("tab") != null) {
      tab = UrlHelper.getParameterValue("tab").toUpperCase();
    }
    return tab;
  }

  public void setTab(String tab) {
    this.tab = tab.toUpperCase();
  }

  public String getPageUrl() {
    return ((Navigation) BeanHelper.getApplicationBean(Navigation.class)).getAlbumUrl() + id;
  }

  public User getAlbumCreator() throws Exception {
    UserController uc = new UserController(sessionBean.getUser());
    User user = uc.retrieve(album.getCreatedBy());
    return user;
  }

  public String getCitation() {
    String title = album.getMetadata().getTitle();
    String author = this.getPersonString();
    String url = this.getPageUrl();
    String citation =
        title + " " + sessionBean.getLabel("from") + " <i>" + author + "</i></br>" + url;
    return citation;
  }

  /*
   * (non-Javadoc)
   * 
   * @see de.mpg.imeji.presentation.beans.ContainerBean#getType()
   */
  @Override
  public String getType() {
    return CONTAINER_TYPE.ALBUM.name();
  }

  /*
   * (non-Javadoc)
   * 
   * @see de.mpg.imeji.presentation.beans.ContainerBean#getContainer()
   */
  @Override
  public Container getContainer() {
    return getAlbum();
  }

  /*
   * (non-Javadoc) following getter functions are for standardization and simplification the output
   * of album data in a general template system
   */
  public String getTitle() {
    if (getContainer() != null) {
      return getContainer().getMetadata().getTitle();
    }
    return null;
  }

  public String getAuthors() {
    return this.getPersonString();
  }

  public Date getCreationDate() {
    return this.getContainer().getCreated().getTime();
  }

  public Date getLastModificationDate() {
    return this.getContainer().getModified().getTime();
  }

  public Date getVersionDate() {
    return this.getContainer().getVersionDate().getTime();
  }

  public Status getStatus() {
    return this.getContainer().getStatus();
  }

  public String getDiscardComment() {
    return this.getContainer().getDiscardComment();
  }

  public void setDiscardComment(String comment) {
    this.getContainer().setDiscardComment(comment);
  }

  /**
   * Compute the amount of private items within an album
   * 
   * @return
   */
  public int getPrivateCount() {
    int count = 0;
    if (this.getSize() > allowedItemsSize) {
      count = this.getSize() - allowedItemsSize;
    }
    return count;
  }

  /**
   * Count the amount of items a user is allow to see
   */
  private void countAllowedItems() {
    ItemController ic = new ItemController();
    this.allowedItemsSize = ic.search(getContainer().getId(), null, null, sessionBean.getUser(),
        sessionBean.getSpaceId(), -1, 0).getNumberOfRecords();
  }
}
