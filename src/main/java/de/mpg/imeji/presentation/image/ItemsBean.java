/**
 * License: src/main/resources/license/escidoc.license
 */
package de.mpg.imeji.presentation.image;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.faces.application.FacesMessage;
import javax.faces.event.ValueChangeEvent;
import javax.faces.model.SelectItem;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.exceptions.WorkflowException;
import de.mpg.imeji.logic.controller.ItemController;
import de.mpg.imeji.logic.search.Search;
import de.mpg.imeji.logic.search.SearchQueryParser;
import de.mpg.imeji.logic.search.SearchResult;
import de.mpg.imeji.logic.search.jenasearch.JenaSearch;
import de.mpg.imeji.logic.search.model.SearchIndex;
import de.mpg.imeji.logic.search.model.SearchQuery;
import de.mpg.imeji.logic.search.model.SortCriterion;
import de.mpg.imeji.logic.search.model.SortCriterion.SortOrder;
import de.mpg.imeji.logic.util.UrlHelper;
import de.mpg.imeji.logic.vo.Album;
import de.mpg.imeji.logic.vo.Item;
import de.mpg.imeji.presentation.beans.BasePaginatorListSessionBean;
import de.mpg.imeji.presentation.beans.Navigation;
import de.mpg.imeji.presentation.facet.Facet.FacetType;
import de.mpg.imeji.presentation.facet.FacetsBean;
import de.mpg.imeji.presentation.filter.Filter;
import de.mpg.imeji.presentation.filter.FiltersBean;
import de.mpg.imeji.presentation.lang.MetadataLabels;
import de.mpg.imeji.presentation.session.SessionBean;
import de.mpg.imeji.presentation.session.SessionObjectsController;
import de.mpg.imeji.presentation.util.BeanHelper;
import de.mpg.imeji.presentation.util.ImejiFactory;
import de.mpg.imeji.presentation.util.PropertyReader;

/**
 * The bean for all list of images
 * 
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public class ItemsBean extends BasePaginatorListSessionBean<ThumbnailBean> {
  private final SessionBean session;
  private final Navigation navigation;
  private int totalNumberOfRecords;
  private List<SelectItem> sortMenu;
  private String selectedSortCriterion;
  private String selectedSortOrder = SortOrder.DESCENDING.name();
  private FacetsBean facets;
  protected FiltersBean filters;
  private String query;
  private Filter searchFilter;
  private boolean isSimpleSearch;
  private SearchQuery searchQuery = new SearchQuery();
  private String discardComment;
  private String selectedImagesContext;
  private SearchResult searchResult;

  /**
   * The context of the browse page (browse, collection browse, album browse)
   */
  protected String browseContext;

  /**
   * The bean for all list of images
   */
  public ItemsBean() {
    super();
    navigation = (Navigation) BeanHelper.getApplicationBean(Navigation.class);
    session = (SessionBean) BeanHelper.getSessionBean(SessionBean.class);
    filters = new FiltersBean();
    selectedSortCriterion = null;
    setElementsPerPage(session.getNumberOfItemsPerPage());
    try {
      String options = PropertyReader.getProperty("imeji.image.list.size.options");
      for (String option : options.split(",")) {
        getElementsPerPageSelectItems().add(new SelectItem(option));
      }
    } catch (Exception e) {
      LOGGER.error("Error reading property imeji.image.list.size.options", e);
    }
  }

  /**
   * Init the page when it is called
   * 
   * @return
   * @throws Exception
   */
  public String getInitPage() throws Exception {
    browseContext = getNavigationString();
    browseInit();
    isSimpleSearch = SearchQueryParser.isSimpleSearch(searchQuery);
    if (UrlHelper.getParameterBoolean("add_selected")) {
      addSelectedToActiveAlbum();
    }
    return "";
  }

  /**
   * Initialization for all browse pages for get queries (non ajax queries)
   */
  public void browseInit() {
    parseSearchQuery();
    initMenus();
    cleanSelectItems();
    initFilters();
    cleanFacets();
    initFacets();
    setCurrentPageNumber(1);
  }

  /**
   * Init all menus of the page
   */
  public void initMenus() {
    sortMenu = new ArrayList<SelectItem>();
    if (selectedSortCriterion == null) {
      this.selectedSortCriterion = SearchIndex.SearchFields.modified.name();
    }
    sortMenu
        .add(new SelectItem(SearchIndex.SearchFields.modified, session.getLabel("sort_date_mod")));
    sortMenu.add(new SelectItem(SearchIndex.SearchFields.filename, session.getLabel("filename")));
    sortMenu.add(new SelectItem(SearchIndex.SearchFields.filesize, session.getLabel("file_size")));
    sortMenu.add(new SelectItem(SearchIndex.SearchFields.filetype, session.getLabel("file_type")));
  }

  @Override
  public List<ThumbnailBean> retrieveList(int offset, int size) {
    try {
      // Search the items of the page
      searchResult = search(searchQuery, getSortCriterion(), offset, size);
      totalNumberOfRecords = searchResult.getNumberOfRecords();
      // load the item
      Collection<Item> items = loadImages(searchResult.getResults());
      // Init the labels for the item
      if (!items.isEmpty()) {
        ((MetadataLabels) BeanHelper.getSessionBean(MetadataLabels.class)).init((List<Item>) items);
      }
      // Return the item as thumbnailBean
      return ImejiFactory.imageListToThumbList(items);
    } catch (ImejiException e) {
      BeanHelper.error(e.getMessage());
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
    ItemController controller = new ItemController();
    return controller.search(null, searchQuery, sortCriterion, session.getUser(),
        session.getSelectedSpaceString(), size, offset);
  }

  /**
   * load all items (defined by their uri)
   * 
   * @param uris
   * @return
   * @throws ImejiException
   */
  public Collection<Item> loadImages(List<String> uris) throws ImejiException {
    ItemController controller = new ItemController();
    return controller.retrieveBatchLazy(uris, -1, 0, session.getUser());
  }

  /**
   * Clean the list of select {@link Item} in the session if the selected images context is not
   * "pretty:browse"
   */
  public void cleanSelectItems() {
    if (session.getSelectedImagesContext() != null
        && !(session.getSelectedImagesContext().equals(browseContext))) {
      session.getSelected().clear();
    }
    session.setSelectedImagesContext(browseContext);
  }

  @Override
  public String getNavigationString() {
    return session.getPrettySpacePage("pretty:browse");
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
      String q = UrlHelper.getParameterValue("q");
      if (q != null) {
        setQuery(URLEncoder.encode(q, "UTF-8"));
        setSearchQuery(SearchQueryParser.parseStringQuery(query));
      }
    } catch (Exception e) {
      BeanHelper.error("Error parsing query: " + e.getMessage());
      LOGGER.error("Error parsing query", e);
    }
  }


  public SortCriterion getSortCriterion() {
    return new SortCriterion(JenaSearch.getIndex(getSelectedSortCriterion()),
        SortOrder.valueOf(getSelectedSortOrder()));
  }

  /**
   * return the current {@link SearchQuery} in a user friendly style.
   * 
   * @return
   */
  public String getSimpleQuery() {
    if (searchFilter != null && searchFilter.getSearchQuery() != null) {
      return SearchQueryParser.searchQuery2PrettyQuery(searchFilter.getSearchQuery());
    }
    return "";
  }

  /**
   * Init the filters with the new search query
   */
  public void initFilters() {
    filters = new FiltersBean(searchQuery, totalNumberOfRecords);
    searchFilter = null;
    for (Filter f : filters.getSession().getFilters()) {
      if (FacetType.SEARCH.equals(f.getType())) {
        searchFilter = f;
      }
    }
  }

  /**
   * Methods called at the end of the page loading, which initialize the facets
   * 
   * @return
   * @throws Exception
   */
  public void initFacets() {
    try {
      SearchResult searchRes = search(getSearchQuery(), null, 0, this.getTotalNumberOfElements());
      this.setFacets(new FacetsBean(SearchQueryParser.parseStringQuery(query), searchRes));
      ExecutorService executor = Executors.newSingleThreadScheduledExecutor();
      executor.submit(facets);
      executor.shutdown();
    } catch (Exception e) {
      LOGGER.error("Error Initializing the facets", e);
    }
  }

  /**
   * When the page starts to load, clean all facets to avoid displaying wrong facets
   */
  public void cleanFacets() {
    if (facets != null) {
      facets.getFacets().clear();
    }
  }

  /**
   * Add all select {@link Item} to the active {@link Album}, and unselect all {@link Item} from
   * session
   * 
   * @return
   * @throws Exception
   */
  public String addSelectedToActiveAlbum() throws Exception {
    addToActiveAlbum(session.getSelected());
    session.getSelected().clear();
    return "pretty:";
  }

  /**
   * Add all {@link Item} of the current {@link ItemsBean} (i.e. browse page) to the active album
   * 
   * @return
   * @throws Exception
   */
  public String addAllToActiveAlbum() throws Exception {
    addToActiveAlbum(search(searchQuery, null, 0, -1).getResults());
    return "pretty:";
  }

  /**
   * Delete selected {@link Item}
   * 
   * @return
   * @throws Exception
   */
  public String deleteSelected() throws Exception {
    delete(session.getSelected());
    return "pretty:";
  }

  /**
   * Delete all {@link Item} currently browsed
   * 
   * @return
   * @throws Exception
   */
  public String deleteAll() {
    delete(search(searchQuery, null, 0, -1).getResults());
    return "pretty:";
  }

  /**
   * Withdraw all {@link Item} currently browsed
   * 
   * @return
   * @throws Exception
   */
  public String withdrawAll() throws Exception {
    withdraw(search(searchQuery, null, 0, -1).getResults());
    return "pretty:";
  }

  /**
   * Withdraw all selected {@link Item}
   * 
   * @return
   * @throws Exception
   */
  public String withdrawSelected() throws Exception {
    withdraw(session.getSelected());
    return "pretty:";
  }

  /**
   * withdraw a list of {@link Item} (defined by their uri)
   * 
   * @param uris
   * @throws Exception
   */
  private void withdraw(List<String> uris) throws Exception {
    Collection<Item> items = loadImages(uris);
    int count = items.size();
    if ("".equals(discardComment.trim())) {
      BeanHelper.error(session.getMessage("error_image_withdraw_discardComment"));
    } else {
      ItemController c = new ItemController();
      c.withdraw((List<Item>) items, discardComment, session.getUser());
      discardComment = null;
      unselect(uris);
      BeanHelper.info(count + " " + session.getLabel("images_withdraw"));
    }
  }

  /**
   * Delete a {@link List} of {@link Item} (defined by their uris).
   * 
   * @param uris
   * @throws Exception
   */
  private void delete(List<String> uris) {
    try {
      Collection<Item> items = loadImages(uris);
      ItemController ic = new ItemController();
      ic.delete((List<Item>) items, session.getUser());
      BeanHelper.info(uris.size() + " " + session.getLabel("images_deleted"));
      unselect(uris);
    } catch (WorkflowException e) {
      BeanHelper.error(session.getMessage("error_delete_items_public"));
      LOGGER.error("Error deleting items", e);
    } catch (ImejiException e) {
      LOGGER.error("Error deleting items", e);
      BeanHelper.error(e.getMessage());
    }
  }

  /**
   * Unselect a list of {@link Item}
   * 
   * @param uris
   */
  private void unselect(List<String> l) {
    SessionObjectsController soc = new SessionObjectsController();
    List<String> uris = new ArrayList<String>(l);
    for (String uri : uris) {
      soc.unselectItem(uri);
    }
  }

  /**
   * Add a {@link List} of uris to the active album, and write an info message in the
   * {@link FacesMessage}
   * 
   * @param uris
   * @throws Exception
   */
  private void addToActiveAlbum(List<String> uris) throws Exception {
    int sizeToAdd = uris.size();
    int sizeBefore = session.getActiveAlbumSize();
    SessionObjectsController soc = new SessionObjectsController();
    soc.addToActiveAlbum(uris);
    int sizeAfter = session.getActiveAlbumSize();
    int added = sizeAfter - sizeBefore;
    int notAdded = sizeToAdd - added;
    String message = "";
    String error = "";
    if (added > 0) {
      message = " " + added + " " + session.getMessage("images_added_to_active_album");
    }
    if (notAdded > 0) {
      error += " " + notAdded + " " + session.getMessage("already_in_active_album");
    }
    if (!"".equals(message))
      BeanHelper.info(message);
    if (!"".equals(error))
      BeanHelper.error(error);
  }

  public String getInitComment() {
    setDiscardComment("");
    return "";
  }

  public String getSelectedImagesContext() {
    return selectedImagesContext;
  }

  public void setSelectedImagesContext(String selectedImagesContext) {
    if (selectedImagesContext.equals(session.getSelectedImagesContext())) {
      this.selectedImagesContext = selectedImagesContext;
    } else {
      session.getSelected().clear();
      this.selectedImagesContext = selectedImagesContext;
      session.setSelectedImagesContext(selectedImagesContext);
    }
  }

  /**
   * The based url used to link to the detail page
   * 
   * @return
   */
  public String getImageBaseUrl() {
    return navigation.getApplicationSpaceUrl();
  }

  public String getBackUrl() {
    return navigation.getBrowseUrl();
  }

  public List<SelectItem> getSortMenu() {
    return sortMenu;
  }

  public void setSortMenu(List<SelectItem> sortMenu) {
    this.sortMenu = sortMenu;
  }

  public String getSelectedSortCriterion() {
    return selectedSortCriterion;
  }

  public String changeSortCriterion(String selectedSortCriterion) {
    if (selectedSortCriterion.equals(this.selectedSortCriterion)) {
      toggleSortOrder();
    }
    this.selectedSortCriterion = selectedSortCriterion;
    return "";
  }

  public void setSelectedSortCriterion(String selectedSortCriterion) {
    this.selectedSortCriterion = selectedSortCriterion;
  }

  public String getSelectedSortOrder() {
    return selectedSortOrder;
  }

  public void setSelectedSortOrder(String selectedSortOrder) {
    this.selectedSortOrder = selectedSortOrder;
  }

  /**
   * Method called when user toggle the sort order
   * 
   * @return
   */
  public String toggleSortOrder() {
    if (selectedSortOrder.equals("DESCENDING")) {
      selectedSortOrder = "ASCENDING";
    } else {
      selectedSortOrder = "DESCENDING";
    }
    return getNavigationString();
  }

  public FacetsBean getFacets() {
    return facets;
  }

  public void setFacets(FacetsBean facets) {
    this.facets = facets;
  }

  public void setQuery(String query) {
    this.query = query;
  }

  public String getQuery() {
    return query;
  }

  public FiltersBean getFilters() {
    return filters;
  }

  public void setFilters(FiltersBean filters) {
    this.filters = filters;
  }

  /**
   * Select all item on the current page
   * 
   * @return
   */
  public String selectAll() {
    for (ThumbnailBean bean : getCurrentPartList()) {
      if (!(session.getSelected().contains(bean.getUri().toString()))) {
        session.getSelected().add(bean.getUri().toString());
      }
    }
    return getNavigationString();
  }

  public String selectNone() {
    session.getSelected().clear();
    return getNavigationString();
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

  public void discardCommentListener(ValueChangeEvent event) throws Exception {
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

  public Filter getSearchFilter() {
    return searchFilter;
  }

  public void setSearchFilter(Filter searchFilter) {
    this.searchFilter = searchFilter;
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
    return session.getLabel("type_" + getType().toLowerCase());
  }

  public void changeAllSelected(ValueChangeEvent event) {
    if (isAllSelected()) {
      selectNone();
    } else {
      selectAll();
    }
  }

  public boolean isAllSelected() {
    boolean result = true;
    for (ThumbnailBean bean : getCurrentPartList()) {
      if (bean.isSelected() == false) {
        result = false;
        break;
      }
    }
    return result;
  }

  public void setAllSelected(boolean allSelected) {

  }

}
