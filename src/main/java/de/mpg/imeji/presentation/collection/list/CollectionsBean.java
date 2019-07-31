package de.mpg.imeji.presentation.collection.list;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import org.apache.jena.sparql.pfunction.library.container;

import de.mpg.imeji.logic.config.Imeji;
import de.mpg.imeji.logic.core.collection.CollectionService;
import de.mpg.imeji.logic.model.CollectionImeji;
import de.mpg.imeji.logic.search.model.SearchQuery;
import de.mpg.imeji.logic.search.model.SearchResult;
import de.mpg.imeji.logic.search.model.SortCriterion;
import de.mpg.imeji.presentation.beans.SuperContainerBean;

/**
 * Bean for the collections page
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
@ManagedBean(name = "CollectionsBean")
@ViewScoped
public class CollectionsBean extends SuperContainerBean<CollectionListItem> {
  private static final long serialVersionUID = -3417058608949508441L;
  /**
   * The comment required to discard a {@link container}
   */
  private String discardComment = "";

  /**
   * Bean for the collections page
   */
  public CollectionsBean() {
    super();
  }

  @PostConstruct
  @Override
  public void init() {
    super.init();
  }

  @Override
  public String getNavigationString() {
    return "pretty:collections";
  }

  @Override
  public List<CollectionListItem> retrieveList(int offset, int limit) throws Exception {
    final CollectionService controller = new CollectionService();
    Collection<CollectionImeji> collections = new ArrayList<CollectionImeji>();
    search(offset, limit);
    setTotalNumberOfRecords(searchResult.getNumberOfRecords());
    collections = controller.retrieve(searchResult.getResults(), getSessionUser());
    return collections.stream().parallel().map(c -> new CollectionListItem(c, getSessionUser(), getNavigation().getFileUrl()))
        .collect(Collectors.toList());
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

  /**
   * getter
   *
   * @return
   */
  public String getDiscardComment() {
    return discardComment;
  }

  /**
   * setter
   *
   * @param discardComment
   */
  public void setDiscardComment(String discardComment) {
    this.discardComment = discardComment;
  }

  @Override
  public String getType() {
    return PAGINATOR_TYPE.COLLECTION_ITEMS.name();
  }

  @Override
  public SearchResult search(SearchQuery searchQuery, SortCriterion sortCriterion, int offset, int limit) {
    final CollectionService controller = new CollectionService();
    List<SortCriterion> scList = new ArrayList<SortCriterion>();
    scList.add(sortCriterion);
    return controller.searchWithFacetsAndMultiLevelSorting(searchQuery, scList, getSessionUser(), limit, offset);
  }

  public String getTypeLabel() {
    return Imeji.RESOURCE_BUNDLE.getLabel("type_" + getType().toLowerCase(), getLocale());
  }
}
