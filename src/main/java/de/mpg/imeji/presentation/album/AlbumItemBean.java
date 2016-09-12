/**
 * License: src/main/resources/license/escidoc.license
 */
package de.mpg.imeji.presentation.album;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import org.apache.log4j.Logger;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.Imeji;
import de.mpg.imeji.logic.controller.resource.AlbumController;
import de.mpg.imeji.logic.util.UrlHelper;
import de.mpg.imeji.logic.vo.Album;
import de.mpg.imeji.logic.vo.Item;
import de.mpg.imeji.logic.vo.util.ObjectHelper;
import de.mpg.imeji.presentation.beans.Navigation;
import de.mpg.imeji.presentation.image.ItemBean;
import de.mpg.imeji.presentation.image.SingleItemBrowse;
import de.mpg.imeji.presentation.session.SessionBean;
import de.mpg.imeji.presentation.util.BeanHelper;

/**
 * Bean for the detail {@link Item} page within an {@link Album}
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
@ManagedBean(name = "AlbumItemBean")
@ViewScoped
public class AlbumItemBean extends ItemBean {
  private static final long serialVersionUID = -3382499449142525367L;
  private static final Logger LOGGER = Logger.getLogger(AlbumItemBean.class);
  private String albumId;
  private Album album;

  public AlbumItemBean() {
    super();
    this.albumId = UrlHelper.getParameterValue("albumId");
    this.prettyLink = SessionBean.getPrettySpacePage("pretty:editImageOfAlbum", getSpace());
  }

  @Override
  public void initBrowsing() {
    try {
      // Should redirect to the Item if user can not see the Album, but
      // can see the Item (this is by default)
      setAlbum(loadAlbum());
      setBrowse(new SingleItemBrowse(getImage(), "album",
          ObjectHelper.getURI(Album.class, albumId).toString(), getSessionUser(), getSpaceId()));
    } catch (ImejiException e) {
      setBrowse(new SingleItemBrowse(getImage(), "item", null, getSessionUser(), getSpaceId()));
    }

  }

  private Album loadAlbum() throws ImejiException {
    return new AlbumController().retrieveLazy(ObjectHelper.getURI(Album.class, albumId),
        getSessionUser());
  }

  /**
   * Remove the current {@link Item} from the current {@link Album}
   *
   * @return
   * @throws IOException
   * @throws Exception
   */
  public void removeFromAlbum() {
    try {
      if (getIsActiveAlbum()) {
        super.removeFromActiveAlbum();
      } else {
        AlbumController ac = new AlbumController();
        List<String> l = new ArrayList<String>();
        l.add(getImage().getId().toString());
        Album album = ac.retrieve(getAlbum().getId(), getSessionUser());
        ac.removeFromAlbum(album, l, getSessionUser());
        BeanHelper.info(
            Imeji.RESOURCE_BUNDLE.getLabel("image", getLocale()) + " " + getImage().getFilename()
                + " " + Imeji.RESOURCE_BUNDLE.getMessage("success_album_remove_from", getLocale()));
      }
    } catch (Exception e) {
      BeanHelper.error(e.getMessage());
      LOGGER.error("Error remove item from album", e);
    }
    redirectToBrowsePage();
  }

  @Override
  public boolean getIsActiveAlbum() {
    return getActiveAlbum() != null && albumId.equals(getActiveAlbum().getIdString());
  }

  public String getAlbumId() {
    return albumId;
  }

  public void setAlbumId(String albumId) {
    this.albumId = albumId;
  }

  @Override
  public String getPageUrl() {
    return getNavigation().getAlbumUrl() + albumId + "/" + Navigation.ITEM.getPath() + "/"
        + getId();
  }

  @Override
  public String getNavigationString() {
    return SessionBean.getPrettySpacePage("pretty:albumItem", getSpace());
  }

  @Override
  public void redirectToBrowsePage() {
    try {
      redirect(getNavigation().getAlbumUrl() + albumId + "/" + getNavigation().getBrowsePath());
    } catch (IOException e) {
      LOGGER.error("Error redirecting to browse page", e);
    }
  }

  public Album getAlbum() {
    return album;
  }

  public void setAlbum(Album album) {
    this.album = album;
  }

}
