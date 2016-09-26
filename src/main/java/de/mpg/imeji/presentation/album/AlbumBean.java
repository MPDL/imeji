/**
 * License: src/main/resources/license/escidoc.license
 */
package de.mpg.imeji.presentation.album;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.ViewScoped;
import javax.faces.event.ValueChangeEvent;
import javax.faces.model.SelectItem;

import org.apache.log4j.Logger;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.Imeji;
import de.mpg.imeji.logic.controller.business.ItemBusinessController;
import de.mpg.imeji.logic.controller.resource.AlbumController;
import de.mpg.imeji.logic.user.controller.UserBusinessController;
import de.mpg.imeji.logic.util.ObjectHelper;
import de.mpg.imeji.logic.util.UrlHelper;
import de.mpg.imeji.logic.vo.Album;
import de.mpg.imeji.logic.vo.Container;
import de.mpg.imeji.logic.vo.Item;
import de.mpg.imeji.logic.vo.Person;
import de.mpg.imeji.logic.vo.Properties.Status;
import de.mpg.imeji.logic.vo.User;
import de.mpg.imeji.presentation.beans.ContainerBean;
import de.mpg.imeji.presentation.image.ThumbnailBean;
import de.mpg.imeji.presentation.session.BeanHelper;
import de.mpg.imeji.presentation.session.SessionBean;
import de.mpg.imeji.presentation.util.CommonUtils;

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
  private static final Logger LOGGER = Logger.getLogger(AlbumBean.class);
  private Album album = null;
  private String id = null;
  private List<SelectItem> profilesMenu = new ArrayList<SelectItem>();
  private boolean active;
  private String tab;
  @ManagedProperty(value = "#{SessionBean.activeAlbum}")
  private Album activeAlbum;
  @ManagedProperty(value = "#{SessionBean}")
  private SessionBean sessionBean;
  /**
   * Maximum number of character displayed in the list for the description
   */
  private static final int DESCRIPTION_MAX_SIZE = 300;
  private String description = "";
  private String descriptionFull = null;
  private ThumbnailBean thumbnail;
  // number of items which the current user is allowed to see
  private int allowedItemsSize;

  public AlbumBean() {
    // must be defined for class extending albumBean
  }

  /**
   * Construct an {@link AlbumBean} from an {@link Album}
   *
   * @param album
   * @throws Exception
   */
  public AlbumBean(Album album, User user, Album activeAlbum) throws Exception {
    this.album = album;
    setSessionUser(user);
    if (album != null) {
      this.id = ObjectHelper.getId(album.getId());
      if (activeAlbum != null && activeAlbum.getId().equals(album.getId())) {
        active = true;
      }
      if (album.getId() != null) {
        findItems(user, 1);
        loadItems(user, 1);
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
   * Load the {@link Album} and its {@link Item} when the {@link AlbumBean} page is called, and
   * initialize it.
   *
   * @throws Exception
   */
  @PostConstruct
  public void init() {
    setId(UrlHelper.getParameterValue("id"));
    try {
      if (id != null) {
        album =
            new AlbumController().retrieve(ObjectHelper.getURI(Album.class, id), getSessionUser());
        if (album != null) {
          countItems();
          countAllowedItems();
          countDiscardedItems(getSessionUser());
          if (getActiveAlbum() != null && getActiveAlbum().getId().equals(album.getId())) {
            active = true;
          }
          int myPrivateCount = getPrivateCount();
          if (myPrivateCount != 0) {
            BeanHelper.info(Imeji.RESOURCE_BUNDLE.getMessage("album_Private_Content", getLocale())
                .replace("XXX_COUNT_XXX", myPrivateCount + ""));
          }
        }
      }
    } catch (Exception e) {
      LOGGER.error("Error initializing album page", e);
      // Has to be in try/catch block, otherwise redirect from
      // HistoryFilter will not work.
      // Here simply do nothing
    }
  }

  /**
   * Return the link for the Cancel button
   *
   * @return
   */
  public String getCancel() {
    return getPageUrl();
  }

  @Override
  protected String getErrorMessageNoAuthor() {
    return "error_album_need_one_author";
  }

  @Override
  public String getPageUrl() {
    return getNavigation().getAlbumUrl() + id;
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
   * Release the current {@link Album}
   *
   * @return
   */
  public String release() {
    AlbumController ac = new AlbumController();
    try {
      ac.release(album, getSessionUser());
      if (active) {
        sessionBean.deactivateAlbum();
      }
      BeanHelper.info(Imeji.RESOURCE_BUNDLE.getMessage("success_album_release", getLocale()));
    } catch (Exception e) {
      BeanHelper.error(Imeji.RESOURCE_BUNDLE.getMessage("error_album_release", getLocale()));
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
      c.delete(album, getSessionUser());
      if (active) {
        sessionBean.deactivateAlbum();
      }
      BeanHelper.info(Imeji.RESOURCE_BUNDLE.getMessage("success_album_delete", getLocale())
          .replace("XXX_albumName_XXX", this.album.getMetadata().getTitle()));
    } catch (Exception e) {
      BeanHelper.error(Imeji.RESOURCE_BUNDLE.getMessage("error_album_delete", getLocale()));
      BeanHelper.error(e.getMessage());
      LOGGER.error("Error during delete album", e);
    }
    return SessionBean.getPrettySpacePage("pretty:albums", getSelectedSpaceString());
  }

  /**
   * Discard the {@link AlbumImeji} of this {@link Album}
   *
   * @return
   * @throws Exception
   */
  public String withdraw() throws ImejiException {
    AlbumController c = new AlbumController();
    try {
      c.withdraw(album, getSessionUser());
      if (active) {
        sessionBean.deactivateAlbum();
      }
      BeanHelper.info(Imeji.RESOURCE_BUNDLE.getMessage("success_album_withdraw", getLocale()));
    } catch (Exception e) {
      BeanHelper.error(Imeji.RESOURCE_BUNDLE.getMessage("error_album_withdraw", getLocale()));
      BeanHelper.error(e.getMessage());
      LOGGER.error("Error during withdraw album", e);
    }
    return "pretty:";
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

  public User getAlbumCreator() throws Exception {
    UserBusinessController uc = new UserBusinessController();
    User user = uc.retrieve(album.getCreatedBy(), Imeji.adminUser);
    return user;
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
    ItemBusinessController ic = new ItemBusinessController();
    allowedItemsSize =
        ic.search(getContainer().getId(), null, null, getSessionUser(), getSpaceId(), -1, 0)
            .getNumberOfRecords();
  }

  public Album getActiveAlbum() {
    return activeAlbum;
  }

  public void setActiveAlbum(Album activeAlbum) {
    this.activeAlbum = activeAlbum;
  }

  public SessionBean getSessionBean() {
    return sessionBean;
  }

  public void setSessionBean(SessionBean sessionBean) {
    this.sessionBean = sessionBean;
  }
}
