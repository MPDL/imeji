/**
 * License: src/main/resources/license/escidoc.license
 */
package de.mpg.imeji.presentation.album;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import de.mpg.imeji.logic.Imeji;
import de.mpg.imeji.logic.controller.resource.AlbumController;
import de.mpg.imeji.logic.search.model.SearchQuery;
import de.mpg.imeji.logic.search.model.SearchResult;
import de.mpg.imeji.logic.search.model.SortCriterion;
import de.mpg.imeji.logic.util.UrlHelper;
import de.mpg.imeji.logic.vo.Album;
import de.mpg.imeji.presentation.beans.SuperContainerBean;
import de.mpg.imeji.presentation.session.SessionBean;
import de.mpg.imeji.presentation.util.BeanHelper;
import de.mpg.imeji.presentation.util.ListUtils;

/**
 * Bean for the Albums page
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public class AlbumsBean extends SuperContainerBean<AlbumBean> {
  private boolean addSelected = false;

  /**
   * Bean for the Albums page
   */
  public AlbumsBean() {
    super();
    this.sb = (SessionBean) BeanHelper.getSessionBean(SessionBean.class);
  }

  @Override
  public String getNavigationString() {
    return sb.getPrettySpacePage("pretty:albums");
  }

  @Override
  public List<AlbumBean> retrieveList(int offset, int limit) throws Exception {
    addSelected = UrlHelper.getParameterBoolean("add_selected");
    AlbumController controller = new AlbumController();
    Collection<Album> albums = new ArrayList<Album>();
    search(offset, limit);
    setTotalNumberOfRecords(searchResult.getNumberOfRecords());
    albums = controller.retrieveBatchLazy(searchResult.getResults(), sb.getUser(), -1, offset);
    return ListUtils.albumListToAlbumBeanList(albums, sb.getUser());
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

  /*
   * Perform the {@link SPARQLSearch}
   *
   * @param searchQuery
   *
   * @param sortCriterion
   *
   * @return
   *
   * @see de.mpg.imeji.presentation.beans.SuperContainerBean#search(de.mpg.imeji.logic.search.vo.
   * SearchQuery , de.mpg.imeji.logic.search.vo.SortCriterion)
   */
  @Override
  public SearchResult search(SearchQuery searchQuery, SortCriterion sortCriterion, int offset,
      int limit) {
    AlbumController controller = new AlbumController();
    return controller.search(searchQuery, sb.getUser(), sortCriterion, limit, offset,
        sb.getSelectedSpaceString());
  }

  public String getTypeLabel() {
    return Imeji.RESOURCE_BUNDLE.getLabel("type_" + getType().toLowerCase(), sb.getLocale());
  }

  public boolean isAddSelected() {
    return addSelected;
  }

  public void setAddSelected(boolean addSelected) {
    this.addSelected = addSelected;
  }
}
