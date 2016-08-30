/**
 * License: src/main/resources/license/escidoc.license
 */
package de.mpg.imeji.presentation.image;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.ViewScoped;
import javax.faces.event.ValueChangeEvent;
import javax.faces.model.SelectItem;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.exceptions.UnprocessableError;
import de.mpg.imeji.exceptions.WorkflowException;
import de.mpg.imeji.logic.Imeji;
import de.mpg.imeji.logic.controller.resource.ItemController;
import de.mpg.imeji.logic.search.Search;
import de.mpg.imeji.logic.search.SearchIndexes;
import de.mpg.imeji.logic.search.SearchQueryParser;
import de.mpg.imeji.logic.search.model.SearchIndex;
import de.mpg.imeji.logic.search.model.SearchQuery;
import de.mpg.imeji.logic.search.model.SearchResult;
import de.mpg.imeji.logic.search.model.SortCriterion;
import de.mpg.imeji.logic.search.model.SortCriterion.SortOrder;
import de.mpg.imeji.logic.util.StringHelper;
import de.mpg.imeji.logic.util.UrlHelper;
import de.mpg.imeji.logic.vo.Album;
import de.mpg.imeji.logic.vo.Item;
import de.mpg.imeji.presentation.beans.BasePaginatorListSessionBean;
import de.mpg.imeji.presentation.beans.MetadataLabels;
import de.mpg.imeji.presentation.facet.FacetsJob;
import de.mpg.imeji.presentation.session.SessionBean;
import de.mpg.imeji.presentation.session.SessionObjectsController;
import de.mpg.imeji.presentation.util.BeanHelper;
import de.mpg.imeji.presentation.util.CookieUtils;
import de.mpg.imeji.presentation.util.ListUtils;

/**
 * The bean for all list of images
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
@ManagedBean(name = "ItemsBean")
@ViewScoped
public class ItemsBean extends BasePaginatorListSessionBean<ThumbnailBean> {
  private static final long serialVersionUID = -5564640316578205957L;
  private int totalNumberOfRecords;
  private FacetsJob facets;
  private String query;
  private boolean isSimpleSearch;
  private SearchQuery searchQuery = new SearchQuery();
  private String discardComment;
  private SearchResult searchResult;
  protected MetadataLabels metadataLabels;
  public static final String ITEM_SORT_ORDER_COOKIE = "CONTAINER_SORT_ORDER_COOKIE";
  public static final String ITEM_SORT_COOKIE = "CONTAINER_SORT_COOKIE";
  private static final int DEFAULT_ELEMENTS_PER_PAGE = 18;
  // From session
  @ManagedProperty(value = "#{SessionBean.selected}")
  private List<String> selected;
  @ManagedProperty(value = "#{SessionBean.activeAlbum}")
  private Album activeAlbum;
  @ManagedProperty(value = "#{SessionBean.selectedImagesContext}")
  private String selectedImagesContext;

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
    initSpecific();
    cleanSelectItems();
  }

  /**
   * Initialization which are specific for this bean. Can be overriden by other beans
   */
  public void initSpecific() {
    parseSearchQuery();
    metadataLabels = new MetadataLabels(new ArrayList<Item>(), getLocale());
    isSimpleSearch = SearchQueryParser.isSimpleSearch(searchQuery);
    if (UrlHelper.getParameterBoolean("add_selected")) {
      try {
        addSelectedToActiveAlbum();
      } catch (ImejiException e) {
        LOGGER.error("Error initializing itemsbean", e);
      }
    }
    update();
  }

  @Override
  public void initSortMenu() {
    setSelectedSortCriterion(SearchIndex.SearchFields
        .valueOf(
            CookieUtils.readNonNull(ITEM_SORT_COOKIE, SearchIndex.SearchFields.modified.name()))
        .name());
    setSelectedSortOrder(SortOrder
        .valueOf(CookieUtils.readNonNull(ITEM_SORT_ORDER_COOKIE, SortOrder.DESCENDING.name()))
        .name());
    setSortMenu(new ArrayList<SelectItem>());
    if (getSelectedSortCriterion() == null) {
      setSelectedSortCriterion(SearchIndex.SearchFields.modified.name());
    }
    getSortMenu().add(new SelectItem(SearchIndex.SearchFields.modified,
        Imeji.RESOURCE_BUNDLE.getLabel("sort_date_mod", getLocale())));
    getSortMenu().add(new SelectItem(SearchIndex.SearchFields.filename,
        Imeji.RESOURCE_BUNDLE.getLabel("filename", getLocale())));
    getSortMenu().add(new SelectItem(SearchIndex.SearchFields.filesize,
        Imeji.RESOURCE_BUNDLE.getLabel("file_size", getLocale())));
    getSortMenu().add(new SelectItem(SearchIndex.SearchFields.filetype,
        Imeji.RESOURCE_BUNDLE.getLabel("file_type", getLocale())));
  }

  @Override
  public void initElementsPerPageMenu() {
    setElementsPerPage(Integer.parseInt(CookieUtils.readNonNull(
        SessionBean.numberOfItemsPerPageCookieName, Integer.toString(DEFAULT_ELEMENTS_PER_PAGE))));
    try {
      String options = Imeji.PROPERTIES.getProperty("imeji.image.list.size.options");
      for (String option : options.split(",")) {
        getElementsPerPageSelectItems().add(new SelectItem(option));
      }
    } catch (Exception e) {
      LOGGER.error("Error reading property imeji.image.list.size.options", e);
    }
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
        metadataLabels = new MetadataLabels((List<Item>) items, getLocale());
      }
      // Return the item as thumbnailBean
      return ListUtils.itemListToThumbList(items);
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
    return controller.search(null, searchQuery, sortCriterion, getSessionUser(), getSpace(), size,
        offset);
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
    return controller.retrieveBatchLazy(uris, -1, 0, getSessionUser());
  }

  /**
   * Clean the list of select {@link Item} in the session if the selected images context is not
   * "pretty:browse"
   */
  public void cleanSelectItems() {
    if (getSelectedImagesContext() != null && !(getSelectedImagesContext().equals(browseContext))) {
      getSelected().clear();
    }
    setSelectedImagesContext(browseContext);
  }

  @Override
  public String getNavigationString() {
    return SessionBean.getPrettySpacePage("pretty:browse", getSpaceId());
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
    return new SortCriterion(SearchIndexes.getIndex(getSelectedSortCriterion()),
        SortOrder.valueOf(getSelectedSortOrder()));
  }

  /**
   * return the current {@link SearchQuery} in a user friendly style.
   *
   * @return
   * @throws UnprocessableError
   */
  public String getSimpleQuery() throws UnprocessableError {
    String q = UrlHelper.getParameterValue("q");
    if (StringHelper.isNullOrEmptyTrim(q)) {
      SearchQuery query = SearchQueryParser.parseStringQuery(q);
      return SearchQueryParser.searchQuery2PrettyQuery(query, getLocale(),
          metadataLabels.getInternationalizedLabels());
    }
    return "";
  }


  /**
   * Methods called at the end of the page loading, which initialize the facets
   *
   * @return @
   */
  public void initFacets() {
    // No Facets for browse page
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
   * @return @
   * @throws ImejiException
   */
  public String addSelectedToActiveAlbum() throws ImejiException {
    addToActiveAlbum(selected);
    selected.clear();
    return "pretty:";
  }

  /**
   * Add all {@link Item} of the current {@link ItemsBean} (i.e. browse page) to the active album
   *
   * @return @
   * @throws ImejiException
   */
  public String addAllToActiveAlbum() throws ImejiException {
    addToActiveAlbum(search(searchQuery, null, 0, -1).getResults());
    return "pretty:";
  }

  /**
   * Delete selected {@link Item}
   *
   * @return @
   */
  public String deleteSelected() {
    delete(getSelected());
    return "pretty:";
  }

  /**
   * Delete all {@link Item} currently browsed
   *
   * @return @
   */
  public String deleteAll() {
    delete(search(searchQuery, null, 0, -1).getResults());
    return "pretty:";
  }

  /**
   * Withdraw all {@link Item} currently browsed
   *
   * @return @
   * @throws ImejiException
   */
  public String withdrawAll() throws ImejiException {
    withdraw(search(searchQuery, null, 0, -1).getResults());
    return "pretty:";
  }

  /**
   * Withdraw all selected {@link Item}
   *
   * @return @
   * @throws ImejiException
   */
  public String withdrawSelected() throws ImejiException {
    withdraw(getSelected());
    return "pretty:";
  }

  /**
   * withdraw a list of {@link Item} (defined by their uri)
   *
   * @param uris
   * @throws ImejiException @
   */
  private void withdraw(List<String> uris) throws ImejiException {
    Collection<Item> items = loadImages(uris);
    int count = items.size();
    if ("".equals(discardComment.trim())) {
      BeanHelper.error(
          Imeji.RESOURCE_BUNDLE.getMessage("error_image_withdraw_discardComment", getLocale()));
    } else {
      ItemController c = new ItemController();
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
      Collection<Item> items = loadImages(uris);
      ItemController ic = new ItemController();
      ic.delete((List<Item>) items, getSessionUser());
      BeanHelper
          .info(uris.size() + " " + Imeji.RESOURCE_BUNDLE.getLabel("images_deleted", getLocale()));
      unselect(uris);
    } catch (WorkflowException e) {
      BeanHelper.error(Imeji.RESOURCE_BUNDLE.getMessage("error_delete_items_public", getLocale()));
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
   * @param uris @
   * @throws ImejiException
   */
  private void addToActiveAlbum(List<String> uris) throws ImejiException {
    int sizeToAdd = uris.size();
    int sizeBefore = getActiveAlbum().getImages().size();
    SessionObjectsController soc = new SessionObjectsController();
    soc.addToActiveAlbum(uris);
    int sizeAfter = getActiveAlbum().getImages().size();
    int added = sizeAfter - sizeBefore;
    int notAdded = sizeToAdd - added;
    String message = "";
    String error = "";
    if (added > 0) {
      message = " " + added + " "
          + Imeji.RESOURCE_BUNDLE.getMessage("images_added_to_active_album", getLocale());
    }
    if (notAdded > 0) {
      error += " " + notAdded + " "
          + Imeji.RESOURCE_BUNDLE.getMessage("already_in_active_album", getLocale());
    }
    if (!"".equals(message)) {
      BeanHelper.info(message);
    }
    if (!"".equals(error)) {
      BeanHelper.error(error);
    }
  }

  public String getInitComment() {
    setDiscardComment("");
    return "";
  }

  public String getSelectedImagesContext() {
    return selectedImagesContext;
  }

  public void setSelectedImagesContext(String newContext) {
    // this.selected = this.selectedImagesContext.equals(newContext) ? selected : new ArrayList<>();
    this.selectedImagesContext = newContext;
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
    return getNavigation().getApplicationSpaceUrl();
  }

  public String getBackUrl() {
    return getNavigation().getBrowseUrl();
  }

  public FacetsJob getFacets() {
    return facets;
  }

  public void setFacets(FacetsJob facets) {
    this.facets = facets;
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
  public String selectAll() {
    for (ThumbnailBean bean : getCurrentPartList()) {
      if (!(getSelected().contains(bean.getUri().toString()))) {
        getSelected().add(bean.getUri().toString());
      }
    }
    return getNavigationString();
  }

  public String selectNone() {
    selected = new ArrayList<>();
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
    for (ThumbnailBean bean : getCurrentPartList()) {
      if (!bean.isSelected()) {
        return false;
      }
    }
    return true;
  }

  public void setAllSelected(boolean allSelected) {

  }

  public MetadataLabels getMetadataLabels() {
    return metadataLabels;
  }

  public List<String> getSelected() {
    return selected;
  }

  public void setSelected(List<String> selected) {
    this.selected = selected;
  }


  public Album getActiveAlbum() {
    return activeAlbum;
  }

  public void setActiveAlbum(Album activeAlbum) {
    this.activeAlbum = activeAlbum;
  }



}
