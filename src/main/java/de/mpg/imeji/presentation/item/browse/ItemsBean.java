package de.mpg.imeji.presentation.item.browse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.ViewScoped;
import javax.faces.event.ValueChangeEvent;
import javax.faces.model.SelectItem;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.exceptions.UnprocessableError;
import de.mpg.imeji.exceptions.WorkflowException;
import de.mpg.imeji.logic.config.Imeji;
import de.mpg.imeji.logic.core.item.ItemService;
import de.mpg.imeji.logic.hierarchy.HierarchyService;
import de.mpg.imeji.logic.model.CollectionImeji;
import de.mpg.imeji.logic.model.Item;
import de.mpg.imeji.logic.model.Properties.Status;
import de.mpg.imeji.logic.model.SearchFields;
import de.mpg.imeji.logic.search.Search;
import de.mpg.imeji.logic.search.Search.SearchObjectTypes;
import de.mpg.imeji.logic.search.SearchQueryParser;
import de.mpg.imeji.logic.search.elasticsearch.ElasticIndexer;
import de.mpg.imeji.logic.search.factory.SearchFactory;
import de.mpg.imeji.logic.search.factory.SearchFactory.SEARCH_IMPLEMENTATIONS;
import de.mpg.imeji.logic.search.model.SearchGroup;
import de.mpg.imeji.logic.search.model.SearchQuery;
import de.mpg.imeji.logic.search.model.SearchResult;
import de.mpg.imeji.logic.search.model.SortCriterion;
import de.mpg.imeji.logic.search.model.SortCriterion.SortOrder;
import de.mpg.imeji.logic.util.StringHelper;
import de.mpg.imeji.logic.util.UrlHelper;
import de.mpg.imeji.presentation.beans.SuperPaginatorBean;
import de.mpg.imeji.presentation.session.BeanHelper;
import de.mpg.imeji.presentation.session.SessionBean;
import de.mpg.imeji.presentation.util.CookieUtils;

/**
 * The bean for all list of images
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
@ManagedBean(name = "ItemsBean")
@ViewScoped
public class ItemsBean extends SuperPaginatorBean<ThumbnailBean> {
  private static final long serialVersionUID = -5564640316578205957L;
  private int totalNumberOfRecords;
  private String query;
  private String facetQueryString;
  private String filterQueryString;
  private boolean isSimpleSearch;
  private SearchQuery searchQuery = new SearchQuery();
  private SearchQuery facetQuery = new SearchQuery();
  private String discardComment;
  private SearchResult searchResult;
  public static final String ITEM_SORT_ORDER_COOKIE = "CONTAINER_SORT_ORDER_COOKIE";
  public static final String ITEM_SORT_COOKIE = "ITEM_SORT_COOKIE";
  private static final int DEFAULT_ELEMENTS_PER_PAGE = 24;
  // From session
  @ManagedProperty(value = "#{SessionBean}")
  private SessionBean sessionBean;

  /**
   * The context of the browse page (browse, collection browse, album browse)
   */
  protected String browseContext;

  /**
   * The bean for all list of images
   */
  public ItemsBean() {
    super();
  }

  @Override
  @PostConstruct
  public void init() {
    super.init();
    parseSearchQuery();
    initSpecific();
    cleanSelectItems();
  }

  /**
   * Initialization which are specific for this bean. Can be overriden by other beans
   */
  public void initSpecific() {
    browseContext = getNavigationString();
    isSimpleSearch = SearchQueryParser.isSimpleSearch(searchQuery);
    update();
  }

  @Override
  public void initSortMenu() {
    setSelectedSortCriterion(SearchFields
        .valueOf(CookieUtils.readNonNull(ITEM_SORT_COOKIE, SearchFields.modified.name())).name());
    setSelectedSortOrder(SortOrder
        .valueOf(CookieUtils.readNonNull(ITEM_SORT_ORDER_COOKIE, SortOrder.DESCENDING.name()))
        .name());
    setSortMenu(new ArrayList<SelectItem>());
    if (getSelectedSortCriterion() == null) {
      setSelectedSortCriterion(SearchFields.modified.name());
    }
    getSortMenu().add(new SelectItem(SearchFields.modified,
        Imeji.RESOURCE_BUNDLE.getLabel("sort_date_mod", getLocale())));
    getSortMenu().add(new SelectItem(SearchFields.filename,
        Imeji.RESOURCE_BUNDLE.getLabel("filename", getLocale())));
    getSortMenu().add(new SelectItem(SearchFields.filesize,
        Imeji.RESOURCE_BUNDLE.getLabel("file_size", getLocale())));
    getSortMenu().add(new SelectItem(SearchFields.filetype,
        Imeji.RESOURCE_BUNDLE.getLabel("file_type", getLocale())));
  }

  @Override
  public void initElementsPerPageMenu() {
    setElementsPerPage(
        Integer.parseInt(CookieUtils.readNonNull(SuperPaginatorBean.numberOfItemsPerPageCookieName,
            Integer.toString(DEFAULT_ELEMENTS_PER_PAGE))));
    setElementsPerPageSelectItems(Stream.of("12,24,48,96".split(",")).map(s -> new SelectItem(s))
        .collect(Collectors.toList()));
  }

  public List<String> getEmptyList() {
    return Collections.nCopies(6 - (getTotalNumberOfRecords() % 6), "");
  }

  @Override
  public List<ThumbnailBean> retrieveList(int offset, int size) {
    try {
      // Search the items of the page
      searchResult = search(getSearchQuery(), getSortCriterion(), offset, size);
      totalNumberOfRecords = searchResult.getNumberOfRecords();
      // load the item
      final Collection<Item> items = loadImages(searchResult.getResults());
      HierarchyService hierarchyService = new HierarchyService();
      items.stream().map(item -> new ThumbnailBean(item, getSessionBean(), getNavigation()))
          .peek(t -> t.initPath(hierarchyService)).collect(Collectors.toList());
      return items.stream().map(item -> new ThumbnailBean(item, getSessionBean(), getNavigation()))
          .peek(t -> t.initPath(hierarchyService)).collect(Collectors.toList());
    } catch (final ImejiException e) {
      BeanHelper.error(e.getMessage());
      LOGGER.error("Error retrieving items", e);
    }
    return new ArrayList<>();
  }

  /**
   * Perform the {@link Search}
   *
   * @param searchQuery
   * @param sortCriterion
   * @return
   */
  public SearchResult search(SearchQuery searchQuery, SortCriterion sortCriterion, int offset,
      int size) {
    return new ItemService().searchWithFacets(null, searchQuery, sortCriterion, getSessionUser(),
        size, offset);
  }

  /**
   * Trigger the update method after a search commit, to ma
   */
  public void refresh() {
    ((ElasticIndexer) SearchFactory.create(SearchObjectTypes.ITEM, SEARCH_IMPLEMENTATIONS.ELASTIC)
        .getIndexer()).commit();
    update();
  }

  /**
   * load all items (defined by their uri)
   *
   * @param uris
   * @return
   * @throws ImejiException
   */
  public Collection<Item> loadImages(List<String> uris) throws ImejiException {
    final ItemService controller = new ItemService();
    return controller.retrieveBatch(uris, -1, 0, getSessionUser());
  }

  /**
   * Clean the list of select {@link Item} in the session if the selected images context is not
   * "pretty:browse"
   */
  public void cleanSelectItems() {
    if (sessionBean.getSelectedImagesContext() != null
        && !sessionBean.getSelectedImagesContext().equals(browseContext)) {
      selectNone();
    }
    sessionBean.setSelectedImagesContext(browseContext);
  }

  @Override
  public String getNavigationString() {
    return "pretty:browse";
  }

  @Override
  public int getTotalNumberOfRecords() {
    return totalNumberOfRecords;
  }


  /**
   * Parse the search query in the url, as defined by the parameter q
   *
   * @return
   */
  private void parseSearchQuery() {
    try {
      query = UrlHelper.getParameterValue("q");
      facetQueryString = UrlHelper.getParameterValue("fq");
      filterQueryString = UrlHelper.getParameterValue("filter");
      SearchFactory factory = new SearchFactory().initQuery(query)
          .and(new SearchGroup(SearchQueryParser.parseStringQuery(facetQueryString).getElements()))
          .initFilter(filterQueryString);
      setSearchQuery(factory.build());
    } catch (final Exception e) {
      BeanHelper.error("Error parsing query: " + e.getMessage());
      LOGGER.error("Error parsing query", e);
    }
  }

  /**
   * @return the facetQuery
   */
  public SearchQuery getFacetQuery() {
    return facetQuery;
  }

  /**
   * @param facetQuery the facetQuery to set
   */
  public void setFacetQuery(SearchQuery facetQuery) {
    this.facetQuery = facetQuery;
  }

  public SortCriterion getSortCriterion() {
    return new SortCriterion(SearchFields.valueOfIndex(getSelectedSortCriterion()),
        SortOrder.valueOf(getSelectedSortOrder()));
  }

  /**
   * return the current {@link SearchQuery} in a user friendly style.
   *
   * @return
   * @throws UnprocessableError
   */
  public String getSimpleQuery() throws UnprocessableError {
    final String q = UrlHelper.getParameterValue("q");
    if (!StringHelper.isNullOrEmptyTrim(q)) {
      final SearchQuery query = SearchQueryParser.parseStringQuery(q);
      return SearchQueryParser.searchQuery2PrettyQuery(query);
    }
    return "";
  }



  /**
   * Delete selected {@link Item}
   *
   * @return @
   * @throws IOException
   */
  public void deleteSelected() throws IOException {
    delete(sessionBean.getSelected());
    selectNone();
    reload();
  }

  /**
   * Delete all {@link Item} currently browsed
   *
   * @return @
   * @throws IOException
   */
  public void deleteAll() throws IOException {
    delete(search(searchQuery, null, 0, -1).getResults());
    selectNone();
    reload();
  }

  /**
   * Withdraw all {@link Item} currently browsed
   *
   * @return @
   * @throws ImejiException
   * @throws IOException
   */
  public void withdrawAll() throws ImejiException, IOException {
    withdraw(search(searchQuery, null, 0, -1).getResults());
    selectNone();
    reload();
  }

  /**
   * Withdraw all selected {@link Item}
   *
   * @return @
   * @throws ImejiException
   * @throws IOException
   */
  public void withdrawSelected() throws ImejiException, IOException {
    withdraw(sessionBean.getSelected());
    selectNone();
    reload();
  }

  /**
   * withdraw a list of {@link Item} (defined by their uri)
   *
   * @param uris
   * @throws ImejiException @
   */
  private void withdraw(List<String> uris) throws ImejiException {
    final Collection<Item> items = new ItemService().retrieveBatch(uris, -1, 0, getSessionUser());
    final int count = items.size();
    if ("".equals(discardComment.trim())) {
      BeanHelper.error(
          Imeji.RESOURCE_BUNDLE.getMessage("error_image_withdraw_discardComment", getLocale()));
    } else {
      final ItemService c = new ItemService();
      c.withdraw((List<Item>) items, discardComment, getSessionUser());
      discardComment = null;
      unselect(uris);
      BeanHelper.info(count + " " + Imeji.RESOURCE_BUNDLE.getLabel("images_withdraw", getLocale()));
    }
  }

  /**
   * Delete a {@link List} of {@link Item} (defined by their uris).
   *
   * @param uris @
   */
  private void delete(List<String> uris) {
    try {
      final ItemService controller = new ItemService();
      final Collection<Item> items = controller.retrieveBatch(uris, -1, 0, getSessionUser());
      final ItemService ic = new ItemService();
      ic.delete((List<Item>) items, getSessionUser());
      BeanHelper
          .info(uris.size() + " " + Imeji.RESOURCE_BUNDLE.getLabel("images_deleted", getLocale()));
      unselect(uris);
    } catch (final WorkflowException e) {
      BeanHelper.error(Imeji.RESOURCE_BUNDLE.getMessage("error_delete_items_public", getLocale()));
      LOGGER.error("Error deleting items", e);
    } catch (final ImejiException e) {
      LOGGER.error("Error deleting items", e);
      BeanHelper.error(e.getMessage());
    }
  }



  /**
   * Unselect a list of {@link Item}
   *
   * @param uris
   */
  private void unselect(List<String> uris) {
    sessionBean.getSelected().removeAll(uris);
  }


  public String getInitComment() {
    setDiscardComment("");
    return "";
  }

  @Override
  protected void setCookieSortValue(String value) {
    CookieUtils.updateCookieValue(ITEM_SORT_COOKIE, value);
  }

  @Override
  protected void setCookieSortOrder(String order) {
    CookieUtils.updateCookieValue(ITEM_SORT_ORDER_COOKIE, order);
  }

  /**
   * The based url used to link to the detail page
   *
   * @return
   */
  public String getImageBaseUrl() {
    return getNavigation().getApplicationUrl();
  }

  public String getBackUrl() {
    return getNavigation().getBrowseUrl();
  }


  public void setQuery(String query) {
    this.query = query;
  }

  public String getQuery() {
    return query;
  }

  /**
   * Select all item on the current page
   *
   * @return
   */
  public void selectAll() {
    for (final ThumbnailBean bean : getCurrentPartList()) {
      if (!(sessionBean.getSelected().contains(bean.getUri().toString()))
          && !bean.getStatus().equals(Status.WITHDRAWN.toString()) && !bean.isCollection()) {
        sessionBean.getSelected().add(bean.getUri().toString());
        bean.setSelected(true);
      }
    }
  }

  public void selectNone() {
    sessionBean.setSelected(new ArrayList<>());
    for (final ThumbnailBean bean : getCurrentPartList()) {
      bean.setSelected(false);
    }
  }

  public boolean isEditable() {
    return false;
  }

  public boolean isVisible() {
    return false;
  }

  public boolean isDeletable() {
    return false;
  }

  public String getDiscardComment() {
    return discardComment;
  }

  public void setDiscardComment(String discardComment) {
    this.discardComment = discardComment;
  }

  public void discardCommentListener(ValueChangeEvent event) {
    discardComment = event.getNewValue().toString();
  }

  public void setSearchQuery(SearchQuery searchQuery) {
    this.searchQuery = searchQuery;
  }

  public SearchQuery getSearchQuery() {
    return searchQuery;
  }

  public boolean isSimpleSearch() {
    return isSimpleSearch;
  }

  public void setSimpleSearch(boolean isSimpleSearch) {
    this.isSimpleSearch = isSimpleSearch;
  }

  /**
   * @return the searchResult
   */
  public SearchResult getSearchResult() {
    return searchResult;
  }

  @Override
  public String getType() {
    return PAGINATOR_TYPE.ITEMS.name();
  }

  public String getTypeLabel() {
    return Imeji.RESOURCE_BUNDLE.getLabel("type_" + getType().toLowerCase(), getLocale());
  }

  public void changeAllSelected(ValueChangeEvent event) {
    if (isAllSelected()) {
      selectNone();
    } else {
      selectAll();
    }
  }

  public boolean isAllSelected() {
    for (final ThumbnailBean bean : getCurrentPartList()) {
      if (!bean.isSelected()) {
        return false;
      }
    }
    return true;
  }

  public void setAllSelected(boolean b) {
    // do nothing
  }

  public SessionBean getSessionBean() {
    return sessionBean;
  }

  public void setSessionBean(SessionBean sessionBean) {
    this.sessionBean = sessionBean;
  }

  public String getCollectionId() {
    return null;
  }

  public CollectionImeji getCollection() {
    return null;
  }

  public String getFacetQueryString() {
    return facetQueryString;
  }

  public int getNumberOfItems() {
    return getTotalNumberOfRecords();
  }

  public int getNumberOfSubCollections() {
    return 0;
  }

  public int getFistItemPosition() {
    return searchResult.getNumberOfRecords() - searchResult.getNumberOfItemsOfCollection();
  }

  public String getFilterQueryString() {
    return filterQueryString;
  }

}
