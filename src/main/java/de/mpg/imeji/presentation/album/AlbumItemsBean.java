/**
 * License: src/main/resources/license/escidoc.license
 */
package de.mpg.imeji.presentation.album;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import javax.faces.context.FacesContext;
import javax.faces.event.ValueChangeEvent;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.controller.AlbumController;
import de.mpg.imeji.logic.controller.ItemController;
import de.mpg.imeji.logic.search.SearchResult;
import de.mpg.imeji.logic.search.model.SearchQuery;
import de.mpg.imeji.logic.search.model.SortCriterion;
import de.mpg.imeji.logic.util.ObjectHelper;
import de.mpg.imeji.logic.vo.Album;
import de.mpg.imeji.logic.vo.CollectionImeji;
import de.mpg.imeji.logic.vo.Item;
import de.mpg.imeji.presentation.beans.Navigation;
import de.mpg.imeji.presentation.image.ItemsBean;
import de.mpg.imeji.presentation.session.SessionBean;
import de.mpg.imeji.presentation.session.SessionObjectsController;
import de.mpg.imeji.presentation.util.BeanHelper;
import de.mpg.imeji.presentation.util.ObjectLoader;

/**
 * {@link ItemsBean} within an {@link Album}: Used to browse {@link Item} of an {@link Album}
 * 
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public class AlbumItemsBean extends ItemsBean {
  private String id;
  private Album album;
  private URI uri;
  private SessionBean sb;
  private CollectionImeji collection;
  private Navigation navigation;

  /**
   * Constructor
   */
  public AlbumItemsBean() {
    super();
    sb = (SessionBean) BeanHelper.getSessionBean(SessionBean.class);
    this.navigation = (Navigation) BeanHelper.getApplicationBean(Navigation.class);

  }

  @Override
  public String getInitPage() throws Exception {
    uri = ObjectHelper.getURI(Album.class, id);
    loadAlbum();
    browseContext = getNavigationString() + id;
    browseInit();
    if (sb.getActiveAlbum() != null && sb.getActiveAlbumSize() != getTotalNumberOfRecords()) {
      AlbumController ac = new AlbumController();
      Album activeA = ac.retrieve(sb.getActiveAlbum().getId(), sb.getUser());
      sb.setActiveAlbum(activeA);
      setAlbum(activeA);
    }
    return "";
  }

  @Override
  public String getNavigationString() {
    return sb.getPrettySpacePage("pretty:albumBrowse");
  }

  @Override
  public SearchResult search(SearchQuery searchQuery, SortCriterion sortCriterion, int offset,
      int limit) {
    ItemController controller = new ItemController();
    return controller.search(uri, searchQuery, sortCriterion, sb.getUser(), null, limit, offset);
  }

  /**
   * Load the current album
   * 
   * @throws Exception
   */
  public void loadAlbum() throws Exception {
    album = ObjectLoader.loadAlbumLazy(uri, sb.getUser());
  }

  @Override
  public void initFacets() {
    // NO FACETs FOR ALBUMS
  }

  /**
   * Remove the selected {@link Item} from the current {@link Album}
   * 
   * @return
   * @throws Exception
   */
  public String removeFromAlbum() {
    removeFromAlbum(sb.getSelected());
    sb.getSelected().clear();
    return "pretty:";
  }

  /**
   * Remove selected {@link Item} from active {@link Album}
   * 
   * @return
   * @throws Exception
   */
  public String removeFromActiveAlbum() throws Exception {
    removeFromActive(sb.getSelected());
    sb.getSelected().clear();
    return "pretty:";
  }

  /**
   * Remove all current {@link Item} from {@link Album}
   * 
   * @return
   * @throws Exception
   */
  public String removeAllFromAlbum() {
    try {
      removeAllFromAlbum(album);
    } catch (ImejiException e) {
      BeanHelper.error(sb.getMessage(e.getMessage()));
    }
    return "pretty:";
  }

  /**
   * Remove all current {@link Item} from active {@link Album}
   * 
   * @return
   * @throws Exception
   */
  public String removeAllFromActiveAlbum() throws Exception {
    removeAllFromAlbum(sb.getActiveAlbum());
    return "pretty:";
  }

  /**
   * Remove all {@link Item} from an {@link Album}
   * 
   * @param album
   * @throws Exception
   */
  private void removeAllFromAlbum(Album album) throws ImejiException {
    if (sb.getActiveAlbum() != null
        && album.getId().toString().equals(sb.getActiveAlbum().getId().toString())) {
      // if the current album is the active album as well
      List<String> uris = new ArrayList<>();
      for (URI uri : sb.getActiveAlbum().getImages()) {
        uris.add(uri.toString());
      }
      removeFromActive(uris);
    } else {
      AlbumController ac = new AlbumController();
      ItemController ic = new ItemController();
      ac.removeFromAlbum(album,
          ic.search(album.getId(), null, null, sb.getUser(), sb.getSpaceId(), -1, 0).getResults(),
          sb.getUser());
    }
  }

  /**
   * Remove a list of {@link Item} from the current {@link Album}
   * 
   * @param uris
   * @throws Exception
   */
  private void removeFromAlbum(List<String> uris) {
    try {
      if (sb.getActiveAlbum() != null
          && album.getId().toString().equals(sb.getActiveAlbum().getId().toString())) {
        // if the current album is the active album as well
        removeFromActive(uris);
      } else {
        ItemController ic = new ItemController();
        album = (Album) ic.searchAndSetContainerItems(album, sb.getUser(), -1, 0);
        AlbumController ac = new AlbumController();
        int deletedCount = ac.removeFromAlbum(album, uris, sb.getUser());
        BeanHelper.info(deletedCount + " " + sb.getMessage("success_album_remove_images"));
      }
    } catch (Exception e) {
      BeanHelper.error(e.getMessage());
    }
  }

  /**
   * Remove a list of {@link Item} from the active {@link Album}
   * 
   * @param uris
   * @throws Exception
   */
  private void removeFromActive(List<String> uris) throws ImejiException {
    SessionObjectsController soc = new SessionObjectsController();
    int deleted = soc.removeFromActiveAlbum(uris);
    sb.getSelected().clear();
    BeanHelper.info(deleted + " " + sb.getMessage("success_album_remove_images"));
  }

  @Override
  public String getImageBaseUrl() {
    if (album == null || album.getId() == null)
      return "";
    return navigation.getApplicationSpaceUrl() + "album/" + this.id + "/";
  }

  @Override
  public String getBackUrl() {
    return navigation.getBrowseUrl() + "/album" + "/" + this.id;
  }

  /**
   * Release current {@link Album}
   * 
   * @return
   * @throws Exception
   */
  public String release() throws Exception {
    ((AlbumBean) BeanHelper.getSessionBean(AlbumBean.class)).setId(id);
    try {
      ((AlbumBean) BeanHelper.getSessionBean(AlbumBean.class)).initView();
    } catch (IOException e) {
      LOGGER.error("Error during release album items", e);
    }
    ((AlbumBean) BeanHelper.getSessionBean(AlbumBean.class)).release();
    return "pretty:";
  }

  /**
   * Delete current {@link Album}
   * 
   * @return
   * @throws Exception
   */
  public String delete() throws Exception {
    ((AlbumBean) BeanHelper.getSessionBean(AlbumBean.class)).setId(id);
    try {
      ((AlbumBean) BeanHelper.getSessionBean(AlbumBean.class)).initView();
    } catch (IOException e) {
      // TODO Auto-generated catch block
      LOGGER.error("Error during delete album items ", e);
    }
    ((AlbumBean) BeanHelper.getSessionBean(AlbumBean.class)).delete();
    return sb.getPrettySpacePage("pretty:albums");
  }

  /**
   * Withdraw current {@link Album}
   * 
   * @return
   * @throws Exception
   */
  public String withdraw() throws Exception {
    ((AlbumBean) BeanHelper.getSessionBean(AlbumBean.class)).setId(id);
    ((AlbumBean) BeanHelper.getSessionBean(AlbumBean.class)).initView();
    String dc = album.getDiscardComment();
    ((AlbumBean) BeanHelper.getSessionBean(AlbumBean.class)).getAlbum().setDiscardComment(dc);
    ((AlbumBean) BeanHelper.getSessionBean(AlbumBean.class)).withdraw();
    return "pretty:";
  }

  /**
   * Listener for the discard comment
   * 
   * @param event
   */
  @Override
  public void discardCommentListener(ValueChangeEvent event) {
    album.setDiscardComment(event.getNewValue().toString());
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
    // @Ye set session value to share with AlbumItemsBean, another way is
    // via injection
    FacesContext.getCurrentInstance().getExternalContext().getSessionMap()
        .put("AlbumItemsBean.id", id);
  }

  public void setCollection(CollectionImeji collection) {
    this.collection = collection;
  }

  public CollectionImeji getCollection() {
    return collection;
  }

  public void setAlbum(Album album) {
    this.album = album;
  }

  public Album getAlbum() {
    return album;
  }

  @Override
  public String getType() {
    return PAGINATOR_TYPE.ALBUM_ITEMS.name();

  }
}
