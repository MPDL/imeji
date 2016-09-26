/**
 * License: src/main/resources/license/escidoc.license
 */
package de.mpg.imeji.presentation.album;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.ViewScoped;

import de.mpg.imeji.logic.Imeji;
import de.mpg.imeji.logic.controller.resource.AlbumController;
import de.mpg.imeji.logic.search.model.SearchQuery;
import de.mpg.imeji.logic.search.model.SearchResult;
import de.mpg.imeji.logic.search.model.SortCriterion;
import de.mpg.imeji.logic.vo.Album;
import de.mpg.imeji.presentation.beans.SuperContainerBean;
import de.mpg.imeji.presentation.session.SessionBean;
import de.mpg.imeji.presentation.util.ListUtils;

/**
 * Bean for the Albums page
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
@ManagedBean(name = "AlbumsBean")
@ViewScoped
public class AlbumsBean extends SuperContainerBean<AlbumBean> {
  private static final long serialVersionUID = -6633102421428463672L;
  private boolean addSelected = false;
  @ManagedProperty(value = "#{SessionBean.activeAlbum}")
  private Album activeAlbum;

  /**
   * Bean for the Albums page
   */
  public AlbumsBean() {
    super();
  }

  @PostConstruct
  @Override
  public void init() {
    super.init();
  }

  @Override
  public String getNavigationString() {
    return SessionBean.getPrettySpacePage("pretty:albums", getSpaceId());
  }

  @Override
  public List<AlbumBean> retrieveList(int offset, int limit) throws Exception {
    AlbumController controller = new AlbumController();
    Collection<Album> albums = new ArrayList<Album>();
    search(offset, limit);
    setTotalNumberOfRecords(searchResult.getNumberOfRecords());
    albums = controller.retrieveBatchLazy(searchResult.getResults(), getSessionUser(), -1, offset);
    return ListUtils.albumListToAlbumBeanList(albums, getSessionUser(), getActiveAlbum());
  }

  @Override
  public String selectAll() {
    // Not implemented
    return "";
  }

  @Override
  public String selectNone() {
    // Not implemented
    return "";
  }

  @Override
  public String getType() {
    return PAGINATOR_TYPE.ALBUMS.name();
  }

  @Override
  public SearchResult search(SearchQuery searchQuery, SortCriterion sortCriterion, int offset,
      int limit) {
    AlbumController controller = new AlbumController();
    return controller.search(searchQuery, getSessionUser(), sortCriterion, limit, offset,
        getSelectedSpaceString());
  }

  public String getTypeLabel() {
    return Imeji.RESOURCE_BUNDLE.getLabel("type_" + getType().toLowerCase(), getLocale());
  }

  public boolean isAddSelected() {
    return addSelected;
  }

  public void setAddSelected(boolean addSelected) {
    this.addSelected = addSelected;
  }

  public Album getActiveAlbum() {
    return activeAlbum;
  }

  public void setActiveAlbum(Album activeAlbum) {
    this.activeAlbum = activeAlbum;
  }
}
