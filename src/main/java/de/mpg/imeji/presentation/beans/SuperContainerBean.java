package de.mpg.imeji.presentation.beans;

import java.util.ArrayList;

import javax.faces.model.SelectItem;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import de.mpg.imeji.logic.config.Imeji;
import de.mpg.imeji.logic.model.SearchFields;
import de.mpg.imeji.logic.search.SearchQueryParser;
import de.mpg.imeji.logic.search.factory.SearchFactory;
import de.mpg.imeji.logic.search.model.SearchGroup;
import de.mpg.imeji.logic.search.model.SearchQuery;
import de.mpg.imeji.logic.search.model.SearchResult;
import de.mpg.imeji.logic.search.model.SortCriterion;
import de.mpg.imeji.logic.search.model.SortCriterion.SortOrder;
import de.mpg.imeji.logic.util.UrlHelper;
import de.mpg.imeji.presentation.session.BeanHelper;
import de.mpg.imeji.presentation.util.CookieUtils;

/**
 * Java Bean for {@link Container} browse pages (collections and albums)
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 * @param <T>
 */
public abstract class SuperContainerBean<T> extends SuperPaginatorBean<T> {
  private static final long serialVersionUID = -7823020782502007646L;
  private static final Logger LOGGER = LogManager.getLogger(SuperContainerBean.class);
  protected String query = "";
  protected String filter = "";
  protected String facetQueryString;
  protected String selectedMenu;
  protected SearchQuery searchQuery = new SearchQuery();
  private SearchQuery facetQuery = new SearchQuery();
  protected SearchResult searchResult;
  private int totalNumberOfRecords;
  private static final String CONTAINER_SORT_ORDER_COOKIE = "CONTAINER_SORT_ORDER_COOKIE";
  private static final String CONTAINER_SORT_COOKIE = "CONTAINER_SORT_COOKIE";
  private static final int DEFAULT_ELEMENTS_PER_PAGE = 10;
  private static final String ELEMENTS_PER_PAGE_MENU = "5,10,20,50,100";

  /**
   * Constructor
   */
  public SuperContainerBean() {
    super();
  }

  /**
   * Initialize the page
   *
   * @return
   */
  @Override
  public void init() {
    super.init();

    parseSearchQuery();

    if (selectedMenu == null) {
      selectedMenu = "SORTING";
    }
    update();
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
      filter = UrlHelper.getParameterValue("filter");
      SearchFactory factory = new SearchFactory().initQuery(query)
          .and(new SearchGroup(SearchQueryParser.parseStringQuery(facetQueryString).getElements())).initFilter(filter);
      setSearchQuery(factory.build());
    } catch (final Exception e) {
      BeanHelper.error("Error parsing query: " + e.getMessage());
      LOGGER.error("Error parsing query", e);
    }
  }

  /*
   * (non-Javadoc)
   *
   * @see de.mpg.imeji.presentation.beans.BasePaginatorListSessionBean#
   * setCookieElementPerPage()
   */
  @Override
  public void setCookieElementPerPage() {
    CookieUtils.updateCookieValue(SuperPaginatorBean.numberOfContainersPerPageCookieName, Integer.toString(getElementsPerPage()));
  }

  @Override
  public void initSortMenu() {
    try {
      setSelectedSortCriterion(SearchFields.valueOf(CookieUtils.readNonNull(CONTAINER_SORT_COOKIE, SearchFields.created.name())).name());
      setSelectedSortOrder(SortOrder.valueOf(CookieUtils.readNonNull(CONTAINER_SORT_ORDER_COOKIE, SortOrder.DESCENDING.name())).name());
      setSortMenu(new ArrayList<SelectItem>());
      getSortMenu().add(new SelectItem(SearchFields.title.name(), Imeji.RESOURCE_BUNDLE.getLabel("sort_title", getLocale())));
      getSortMenu().add(new SelectItem(SearchFields.created.name(), Imeji.RESOURCE_BUNDLE.getLabel("sort_date_created", getLocale())));
      getSortMenu().add(new SelectItem(SearchFields.modified.name(), Imeji.RESOURCE_BUNDLE.getLabel("sort_date_mod", getLocale())));
      getSortMenu().add(new SelectItem(SearchFields.creatorid.name(), Imeji.RESOURCE_BUNDLE.getLabel("sort_author", getLocale())));
    } catch (Exception e) {
      LOGGER.error("Error initializing sort menu", e);
    }

  }

  @Override
  public void initElementsPerPageMenu() {
    setElementsPerPage(Integer.parseInt(
        CookieUtils.readNonNull(SuperPaginatorBean.numberOfContainersPerPageCookieName, Integer.toString(DEFAULT_ELEMENTS_PER_PAGE))));
    for (final String option : ELEMENTS_PER_PAGE_MENU.split(",")) {
      getElementsPerPageSelectItems().add(new SelectItem(option));
    }
  }

  /**
   * setter
   *
   * @param selectedMenu
   */
  public void setSelectedMenu(String selectedMenu) {
    this.selectedMenu = selectedMenu;
  }

  /**
   * getter: Return the current tab name in the actions div on the xhtml page
   *
   * @return
   */
  public String getSelectedMenu() {
    return selectedMenu;
  }

  /**
   * select all {@link Container} on the page
   *
   * @return
   */
  public String selectAll() {
    return getNavigationString();
  }

  /**
   * Unselect all {@link Container} on the page
   *
   * @return
   */
  public String selectNone() {
    return getNavigationString();
  }

  /**
   * setter
   *
   * @param query
   */
  public void setQuery(String query) {
    this.query = query;
  }

  /**
   * getter
   *
   * @return
   */
  public String getQuery() {
    return query;
  }

  @Override
  public String getType() {
    return "supercontainer";
  }

  /**
   * @return the searchQuery
   */
  public SearchQuery getSearchQuery() {
    return searchQuery;
  }

  /**
   * @param searchQuery the searchQuery to set
   */
  public void setSearchQuery(SearchQuery searchQuery) {
    this.searchQuery = searchQuery;
  }

  /**
   * Search for containers, according to the current queries
   *
   * @param offset
   * @param limit
   * @return
   * @throws Exception
   */
  public int search(int offset, int limit) throws Exception {

    int myOffset = offset;
    final SortCriterion sortCriterion =
        new SortCriterion(SearchFields.valueOfIndex(getSelectedSortCriterion()), SortOrder.valueOf(getSelectedSortOrder()));
    searchResult = search(searchQuery, sortCriterion, myOffset, limit);
    setSearchQuery(searchQuery);
    searchResult.setQuery(getQuery());
    searchResult.setSort(sortCriterion);
    setTotalNumberOfRecords(searchResult.getNumberOfRecords());
    return myOffset;
  }

  /**
   * Search for the container
   *
   * @param searchQuery
   * @param sortCriterion
   * @return
   */
  public abstract SearchResult search(SearchQuery searchQuery, SortCriterion sortCriterion, int offset, int limit);

  /*
   * (non-Javadoc)
   *
   * @see de.mpg.imeji.presentation.beans.BasePaginatorListSessionBean#
   * getTotalNumberOfRecords()
   */
  @Override
  public int getTotalNumberOfRecords() {
    return totalNumberOfRecords;
  }

  /**
   * @param totalNumberOfRecords the totalNumberOfRecords to set
   */
  public void setTotalNumberOfRecords(int totalNumberOfRecords) {
    this.totalNumberOfRecords = totalNumberOfRecords;
  }

  /**
   * needed for searchQueryDisplayArea.xhtml component
   *
   * @return
   */
  public String getSimpleQuery() {
    if (query != null) {
      return query;
    }
    return "";
  }

  /**
   * search is always a simple search (needed for searchQueryDisplayArea.xhtml component)
   *
   * @return
   */
  public boolean isSimpleSearch() {
    return true;
  }

  @Override
  protected void setCookieSortValue(String value) {
    CookieUtils.updateCookieValue(CONTAINER_SORT_COOKIE, value);
  }

  @Override
  protected void setCookieSortOrder(String order) {
    CookieUtils.updateCookieValue(CONTAINER_SORT_ORDER_COOKIE, order);
  }

  public SearchResult getSearchResult() {
    return searchResult;
  }

  public void setSearchResult(SearchResult searchResult) {
    this.searchResult = searchResult;
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
}
