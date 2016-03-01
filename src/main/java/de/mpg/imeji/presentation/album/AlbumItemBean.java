/**
 * License: src/main/resources/license/escidoc.license
 */
package de.mpg.imeji.presentation.album;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.faces.context.FacesContext;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.controller.AlbumController;
import de.mpg.imeji.logic.util.ObjectHelper;
import de.mpg.imeji.logic.vo.Album;
import de.mpg.imeji.logic.vo.Item;
import de.mpg.imeji.presentation.beans.Navigation;
import de.mpg.imeji.presentation.image.ItemBean;
import de.mpg.imeji.presentation.image.ItemsBean;
import de.mpg.imeji.presentation.image.SingleItemBrowse;
import de.mpg.imeji.presentation.session.SessionBean;
import de.mpg.imeji.presentation.util.BeanHelper;
import de.mpg.imeji.presentation.util.ObjectLoader;

/**
 * Bean for the detail {@link Item} page within an {@link Album}
 * 
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public class AlbumItemBean extends ItemBean {
  private String albumId;
  private Navigation navigation;
  private SessionBean session = (SessionBean) BeanHelper.getSessionBean(SessionBean.class);
  private Album album;

  public AlbumItemBean() throws Exception {
    super();
    this.prettyLink = session.getPrettySpacePage("pretty:editImageOfAlbum");
    navigation = (Navigation) BeanHelper.getApplicationBean(Navigation.class);
  }

  @Override
  public void initBrowsing() throws Exception {
    try {
      String tempId = (String) FacesContext.getCurrentInstance().getExternalContext()
          .getSessionMap().get("AlbumItemsBean.id");
      setBrowse(
          new SingleItemBrowse((AlbumItemsBean) BeanHelper.getSessionBean(AlbumItemsBean.class),
              getImage(), "album", tempId));
      // Should redirect to the Item if user can not see the Album, but
      // can see the Item (this is by default)
      Album alb = this.loadAlbum();
      this.setAlbum(alb);
    } catch (ImejiException e) {
      setBrowse(new SingleItemBrowse((ItemsBean) BeanHelper.getSessionBean(ItemsBean.class),
          getImage(), "item", ""));
    }

  }

  private Album loadAlbum() throws Exception {
    return ObjectLoader.loadAlbumLazy(ObjectHelper.getURI(Album.class, albumId), session.getUser());
  }

  /**
   * Remove the current {@link Item} from the current {@link Album}
   * 
   * @return
   * @throws Exception
   */
  public String removeFromAlbum() throws Exception {
    try {
      if (isActiveAlbum()) {
        super.removeFromActiveAlbum();
      } else {
        AlbumController ac = new AlbumController();
        List<String> l = new ArrayList<String>();
        l.add(getImage().getId().toString());
        Album album = ObjectLoader.loadAlbum(getAlbum().getId(), session.getUser());
        ac.removeFromAlbum(album, l, session.getUser());
        BeanHelper.info(session.getLabel("image") + " " + getImage().getFilename() + " "
            + session.getMessage("success_album_remove_from"));
      }
    } catch (Exception e) {
      BeanHelper.error(e.getMessage());
      return "";
    }
    return session.getPrettySpacePage("pretty:albumBrowse");
  }

  @Override
  public boolean isActiveAlbum() {
    return session.getActiveAlbum() != null && albumId.equals(session.getActiveAlbumId());
  }

  public String getAlbumId() {
    return albumId;
  }

  public void setAlbumId(String albumId) {
    this.albumId = albumId;
  }

  @Override
  public String getPageUrl() {
    return navigation.getAlbumUrl() + albumId + "/" + navigation.ITEM.getPath() + "/" + getId();
  }

  @Override
  public String getNavigationString() {
    return session.getPrettySpacePage("pretty:albumItem");
  }

  @Override
  public void redirectToBrowsePage() throws IOException {
    FacesContext.getCurrentInstance().getExternalContext()
        .redirect(navigation.getAlbumUrl() + albumId + "/" + navigation.getBrowsePath());
  }

  public Album getAlbum() {
    return album;
  }

  public void setAlbum(Album album) {
    this.album = album;
  }

}
