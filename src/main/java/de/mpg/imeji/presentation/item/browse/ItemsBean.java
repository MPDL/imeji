package de.mpg.imeji.presentation.item.browse;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
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
import de.mpg.imeji.exceptions.ImejiExceptionWithUserMessage;
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
import de.mpg.imeji.logic.search.SearchQueryParser;
import de.mpg.imeji.logic.search.factory.SearchFactory;
import de.mpg.imeji.logic.search.model.SearchGroup;
import de.mpg.imeji.logic.search.model.SearchQuery;
import de.mpg.imeji.logic.search.model.SearchResult;
import de.mpg.imeji.logic.search.model.SortCriterion;
import de.mpg.imeji.logic.search.model.SortCriterion.SortOrder;
import de.mpg.imeji.logic.security.authorization.Authorization;
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
    try {
      setSelectedSortCriterion(SearchFields.valueOf(CookieUtils.readNonNull(ITEM_SORT_COOKIE, SearchFields.modified.name())).name());
      setSelectedSortOrder(SortOrder.valueOf(CookieUtils.readNonNull(ITEM_SORT_ORDER_COOKIE, SortOrder.DESCENDING.name())).name());
      setSortMenu(new ArrayList<SelectItem>());
      if (getSelectedSortCriterion() == null) {
        setSelectedSortCriterion(SearchFields.modified.name());
      }
      getSortMenu().add(new SelectItem(SearchFields.modified, Imeji.RESOURCE_BUNDLE.getLabel("sort_date_mod", getLocale())));
      getSortMenu().add(new SelectItem(SearchFields.filename, Imeji.RESOURCE_BUNDLE.getLabel("filename", getLocale())));
      getSortMenu().add(new SelectItem(SearchFields.filesize, Imeji.RESOURCE_BUNDLE.getLabel("file_size", getLocale())));
      getSortMenu().add(new SelectItem(SearchFields.fileextension, Imeji.RESOURCE_BUNDLE.getLabel("file_type", getLocale())));
    } catch (Exception e) {
      LOGGER.error("Error initializing sort menu", e);
    }

  }

  @Override
  public void initElementsPerPageMenu() {
    setElementsPerPage(Integer
        .parseInt(CookieUtils.readNonNull(SuperPaginatorBean.numberOfItemsPerPageCookieName, Integer.toString(DEFAULT_ELEMENTS_PER_PAGE))));
    setElementsPerPageSelectItems(Stream.of("12,24,48,96".split(",")).map(s -> new SelectItem(s)).collect(Collectors.toList()));
  }

  public List<String> getEmptyList() {
    return Collections.nCopies(6 - (getTotalNumberOfRecords() % 6), "");
  }

  @Override
  public List<ThumbnailBean> retrieveList(int offset, int size) {
    try {

      // (a) Search items in ElasticSearch
      // Access ElasticSearch, read UIDs of all Items that can bee seen by logged-on
      // user
      // SearchResult: contains a list of Item UIDs
      searchResult = search(getSearchQuery(), getSortCriteriaForItems(), offset, size);
      totalNumberOfRecords = searchResult.getNumberOfRecords();

      // (b) Build Item objects and read Item content from Jena
      // Access Jena, create empty Item objects, stock them with their UIDs,
      // fill objects with content stored in Jena
      // result: Collection of Item objects
      final Collection<Item> items = loadItems(searchResult.getResults());

      // (c) create "Thumbnails" for Items and return them
      HierarchyService hierarchyService = new HierarchyService();

      List<ThumbnailBean> thumbnailBeans = items.stream().parallel().map(item -> new ThumbnailBean(item, getSessionBean(), getNavigation()))
          .peek(t -> t.initPath(hierarchyService)).collect(Collectors.toList());

      return thumbnailBeans;

    } catch (final ImejiExceptionWithUserMessage exceptionWithMessage) {
      String userMessage = exceptionWithMessage.getUserMessage(getLocale());
      BeanHelper.error(userMessage);
      if (exceptionWithMessage.getMessage() != null) {
        LOGGER.error(exceptionWithMessage.getMessage(), exceptionWithMessage);
      } else {
        LOGGER.error(userMessage, exceptionWithMessage);
      }
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
  public SearchResult search(SearchQuery searchQuery, List<SortCriterion> sortCriteria, int offset, int size) {
    return new ItemService().searchWithFacetsAndMultiLevelSorting(null, searchQuery, sortCriteria, getSessionUser(), size, offset);
  }

  /**
   * Trigger the update method after a search commit, to ma
   */
  public void refresh() {
    /*
     * ((ElasticIndexer) SearchFactory.create(SearchObjectTypes.ITEM,
     * SEARCH_IMPLEMENTATIONS.ELASTIC).getIndexer()) .commit();
     */
    update();
  }

  /**
   * load all items (defined by their uri)
   *
   * @param uris
   * @return
   * @throws ImejiException
   */
  public Collection<Item> loadItems(List<String> uris) throws ImejiException {
    final ItemService itemService = new ItemService();
    return itemService.retrieveBatch(uris, Search.GET_ALL_RESULTS, Search.SEARCH_FROM_START_INDEX, getSessionUser());
  }

  /**
   * Clean the list of select {@link Item} in the session if the selected images context is not
   * "pretty:browse"
   */
  public void cleanSelectItems() {
    if (sessionBean.getSelectedImagesContext() != null && !sessionBean.getSelectedImagesContext().equals(browseContext)) {
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
          .and(new SearchGroup(SearchQueryParser.parseStringQuery(facetQueryString).getElements())).initFilter(filterQueryString);
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

  /**
   * Get sort criteria for Items page Two-level sorting: (First level) Sort all items by chosen sort
   * criterion (date modified, filename, file size, file type) (Second level) sort all items that
   * fall into the same category by filename (alphabetically, ascending)
   * 
   * @return
   */
  private List<SortCriterion> getSortCriteriaForItems() {

    List<SortCriterion> itemsSortCriteria = new LinkedList<SortCriterion>();
    itemsSortCriteria.add(getUserSetSortCriterion());
    itemsSortCriteria.add(getSortByFilenameAscendingSortCriterion());
    return itemsSortCriteria;
  }

  /**
   * Get the sort criterion for ElasticSearch
   * 
   * @return
   */
  private SortCriterion getUserSetSortCriterion() {
    return new SortCriterion(SearchFields.valueOfIndex(getSelectedSortCriterion()), SortOrder.valueOf(getSelectedSortOrder()));
  }

  public static SortCriterion getSortByFilenameAscendingSortCriterion() {
    SortCriterion sortByFilenameAscending = new SortCriterion(SearchFields.filename, SortOrder.ASCENDING);
    return sortByFilenameAscending;
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
   * Search for all items of the current page (no subcollection are retrieved)
   * 
   * @return
   */
  public List<String> searchAllItems() {
    return new ItemService().search(searchQuery, null, getSessionUser(), Search.GET_ALL_RESULTS, Search.SEARCH_FROM_START_INDEX)
        .getResults();
  }

  /**
   * Returns whether the currently selected items have licenses - true if all selected items have
   * licenses - false if at least one does not have a license
   * 
   * @return
   */
  public boolean allSelectedHaveALicense() {

    List<String> selectedItemUris = sessionBean.getSelected();
    try {
      List<Item> selectedItems = new ItemService().retrieveBatch(selectedItemUris, getSessionUser());
      for (Item item : selectedItems) {
        if (item.getLicenses() == null || item.getLicenses().isEmpty()) {
          return false;
        }
      }
    } catch (ImejiException e) {
      LOGGER.error("Error reading selected items from Jena", e);
      return false;
    }
    return true;
  }

  /**
   * Delete all {@link Item} currently browsed
   *
   * @return @
   * @throws IOException
   */
  public void deleteAll() throws IOException {
    delete(searchAllItems());
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
    withdraw(searchAllItems());
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

    final Collection<Item> items =
        new ItemService().retrieveBatch(uris, Search.GET_ALL_RESULTS, Search.SEARCH_FROM_START_INDEX, getSessionUser());
    final int count = items.size();

    try {
      final ItemService c = new ItemService();
      c.withdraw((List<Item>) items, discardComment, getSessionUser());
      discardComment = null;
      unselect(uris);
      BeanHelper.info(count + " " + Imeji.RESOURCE_BUNDLE.getLabel("images_withdraw", getLocale()));
    } catch (final ImejiExceptionWithUserMessage exceptionWithMessage) {
      String userMessage = Imeji.RESOURCE_BUNDLE.getMessage("error_withdraw_selected_items", getLocale()) + " "
          + exceptionWithMessage.getUserMessage(getLocale());
      BeanHelper.error(userMessage);
      if (exceptionWithMessage.getMessage() != null) {
        LOGGER.error(exceptionWithMessage.getMessage(), exceptionWithMessage);
      } else {
        LOGGER.error(userMessage, exceptionWithMessage);
      }
    } catch (final ImejiException e) {
      BeanHelper.error(Imeji.RESOURCE_BUNDLE.getMessage("error_withdraw_selected_items", getLocale()));
      BeanHelper.error(e.getMessage());
      LOGGER.error("Error discarding items:", e);
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
      final Collection<Item> items =
          controller.retrieveBatch(uris, Search.GET_ALL_RESULTS, Search.SEARCH_FROM_START_INDEX, getSessionUser());
      final ItemService ic = new ItemService();
      ic.delete((List<Item>) items, getSessionUser());
      BeanHelper.info(uris.size() + " " + Imeji.RESOURCE_BUNDLE.getLabel("images_deleted", getLocale()));
      unselect(uris);
    } catch (final ImejiExceptionWithUserMessage exceptionWithMessage) {
      String userMessage = exceptionWithMessage.getUserMessage(getLocale());
      BeanHelper.error(userMessage);
      if (exceptionWithMessage.getMessage() != null) {
        LOGGER.error(exceptionWithMessage.getMessage(), exceptionWithMessage);
      } else {
        LOGGER.error(userMessage, exceptionWithMessage);
      }
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
      if (!(sessionBean.getSelected().contains(bean.getUri().toString())) && !bean.getStatus().equals(Status.WITHDRAWN.toString())
          && !bean.isCollection()) {
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

  /**
   * Check if the discard comment is empty or contains only spaces
   * 
   * @return
   */
  public boolean discardCommentEmpty() {
    if (this.discardComment == null || "".equals(this.discardComment) || "".equals(this.discardComment.trim())) {
      return true;
    } else {
      return false;
    }
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
      if (!bean.isCollection() && !bean.isSelected()) {
        return false;
      }
    }
    return getNumberOfSubCollections() < getElementsPerPage() * getCurrentPageNumber() && searchResult != null
        && searchResult.getNumberOfItemsOfCollection() > 0;
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

  public int getNumberOfRecords() {
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

  public String getFacetQueryString() {
    return facetQueryString;
  }

  public String getFilterQueryStringEncoded() throws UnsupportedEncodingException {
    return filterQueryString != null ? URLEncoder.encode(filterQueryString, "UTF-8") : "";
  }

  public String getFacetQueryStringEncoded() throws UnsupportedEncodingException {
    return facetQueryString != null ? URLEncoder.encode(facetQueryString, "UTF-8") : "";
  }

  public String getQueryEncoded() throws UnsupportedEncodingException {
    return query != null ? URLEncoder.encode(query, "UTF-8") : "";
  }

  /**
   * Checks whether at least one of the selected items is deletable.
   * 
   * @return true if at least one selected item is deletable.
   */
  public boolean isOneSelectedItemDeletable() {
    int numberofDeletableItems = this.getNumberOfDeletableSelectedItems();

    return numberofDeletableItems >= 1;
  }

  private int getNumberOfSelectedItems() {
    return sessionBean.getSelected().size();
  }

  private int getNumberOfDeletableSelectedItems() {
    return this.findSelectedDeletableItems().size();
  }

  private int getNumberOfNonDeletableSelectedItems() {
    return sessionBean.getSelected().size() - this.findSelectedDeletableItems().size();
  }

  private Collection<Item> findSelectedDeletableItems() {
    List<String> selectedItemsUris = sessionBean.getSelected();
    Authorization authorization = new Authorization();
    final ItemService itemService = new ItemService();

    try {
      Collection<Item> selectedItems =
          itemService.retrieveBatch(selectedItemsUris, Search.GET_ALL_RESULTS, Search.SEARCH_FROM_START_INDEX, getSessionUser());
      Collection<Item> deletableItems = selectedItems.stream()
          // Items can be deleted if the user has the permission to delete them
          // and if the items have the status pending.
          .filter(item -> authorization.delete(getSessionUser(), item)).filter(item -> (item.getStatus() == Status.PENDING))
          .collect(Collectors.toList());
      return deletableItems;
    } catch (final ImejiExceptionWithUserMessage exceptionWithMessage) {
      String userMessage = Imeji.RESOURCE_BUNDLE.getMessage("error_retrieve_selected_items", getLocale()) + " "
          + exceptionWithMessage.getUserMessage(getLocale());
      BeanHelper.error(userMessage);
      if (exceptionWithMessage.getMessage() != null) {
        LOGGER.error(exceptionWithMessage.getMessage(), exceptionWithMessage);
      } else {
        LOGGER.error(userMessage, exceptionWithMessage);
      }
      return new ArrayList<Item>();
    } catch (final ImejiException e) {
      String errorMessage = Imeji.RESOURCE_BUNDLE.getMessage("error_retrieve_selected_items", getLocale());
      LOGGER.error(errorMessage, e);
      BeanHelper.error(errorMessage);
      return new ArrayList<Item>();
    }
  }

  /**
   * Delete all selected {@link Item}s that can be deleted.
   */
  public void deleteSelectedDeletableItems() {
    Collection<Item> deletableItems = this.findSelectedDeletableItems();
    List<String> deletableItemsUris = deletableItems.stream().map(item -> item.getUri()).collect(Collectors.toList());
    delete(deletableItemsUris);

    try {
      reload();
    } catch (IOException e) {
      String errorMessage = Imeji.RESOURCE_BUNDLE.getMessage("error_reload_page", getLocale());
      LOGGER.error(errorMessage, e);
      BeanHelper.error(errorMessage);
    }
  }

  public String getDeleteItemsNotAllowedNotice() {
    int numberOfNonDeletableSelectedItems = this.getNumberOfNonDeletableSelectedItems();
    int numberOfSelectedItems = this.getNumberOfSelectedItems();

    if (numberOfNonDeletableSelectedItems >= 1 && numberOfSelectedItems > 1) {
      return Imeji.RESOURCE_BUNDLE.getMessage("not_allowed_to_delete_items_notice", getLocale())
          .replaceAll("XXX_NUMBER_OF_NON_DELETABLE_XXX", Integer.toString(numberOfNonDeletableSelectedItems))
          .replaceAll("XXX_NUMBER_OF_SELECTED_XXX", Integer.toString(numberOfSelectedItems));
    } else if (numberOfNonDeletableSelectedItems == 1 && numberOfSelectedItems == 1) {
      return Imeji.RESOURCE_BUNDLE.getMessage("not_allowed_to_delete_item_notice", getLocale());
    } else {
      return "";
    }
  }

  public String getDeleteItemsConfirmationText() {
    int numberOfDeletableSelectedItems = this.getNumberOfDeletableSelectedItems();

    if (numberOfDeletableSelectedItems > 1) {
      return Imeji.RESOURCE_BUNDLE.getMessage("confirmation_delete_number_of_items", getLocale()).replaceAll("XXX_NUMBER_OF_DELETABLE_XXX",
          Integer.toString(numberOfDeletableSelectedItems));
    } else if (numberOfDeletableSelectedItems == 1) {
      return Imeji.RESOURCE_BUNDLE.getMessage("confirmation_delete_one_item", getLocale());
    } else {
      return "";
    }
  }

  public String getDeleteItemsSubmitLabel() {
    if (this.isOneSelectedItemDeletable()) {
      return Imeji.RESOURCE_BUNDLE.getLabel("delete_selectedImages", getLocale());
    } else {
      return "";
    }
  }

}
