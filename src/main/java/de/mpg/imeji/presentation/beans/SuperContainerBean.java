/**
 * License: src/main/resources/license/escidoc.license
 */
package de.mpg.imeji.presentation.beans;

import java.util.ArrayList;

import javax.faces.model.SelectItem;

import org.apache.log4j.Logger;

import de.mpg.imeji.logic.Imeji;
import de.mpg.imeji.logic.search.SearchIndexes;
import de.mpg.imeji.logic.search.SearchQueryParser;
import de.mpg.imeji.logic.search.model.SearchIndex;
import de.mpg.imeji.logic.search.model.SearchQuery;
import de.mpg.imeji.logic.search.model.SearchResult;
import de.mpg.imeji.logic.search.model.SortCriterion;
import de.mpg.imeji.logic.search.model.SortCriterion.SortOrder;
import de.mpg.imeji.logic.util.UrlHelper;
import de.mpg.imeji.logic.vo.Container;
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
  private static final Logger LOGGER = Logger.getLogger(SuperContainerBean.class);
  protected String query = "";
  protected String selectedMenu;
  protected SearchQuery searchQuery = new SearchQuery();
  protected SearchResult searchResult;
  private int totalNumberOfRecords;
  private static final String CONTAINER_SORT_ORDER_COOKIE = "CONTAINER_SORT_ORDER_COOKIE";
  private static final String CONTAINER_SORT_COOKIE = "CONTAINER_SORT_COOKIE";
  private static final int DEFAULT_ELEMENTS_PER_PAGE = 10;

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
    setSearchQuery(null);
    if (UrlHelper.hasParameter("tab") && !UrlHelper.getParameterValue("tab").isEmpty()) {
      selectedMenu = UrlHelper.getParameterValue("tab");
    }
    if (UrlHelper.hasParameter("q")) {
      query = UrlHelper.getParameterValue("q");
    }
    if (selectedMenu == null) {
      selectedMenu = "SORTING";
    }
    update();
  }

  /*
   * (non-Javadoc)
   *
   * @see de.mpg.imeji.presentation.beans.BasePaginatorListSessionBean# setCookieElementPerPage()
   */
  @Override
  public void setCookieElementPerPage() {
    CookieUtils.updateCookieValue(SuperPaginatorBean.numberOfContainersPerPageCookieName,
        Integer.toString(getElementsPerPage()));
  }

  @Override
  public void initSortMenu() {
    setSelectedSortCriterion(SearchIndex.SearchFields.valueOf(
        CookieUtils.readNonNull(CONTAINER_SORT_COOKIE, SearchIndex.SearchFields.modified.name()))
        .name());
    setSelectedSortOrder(SortOrder
        .valueOf(CookieUtils.readNonNull(CONTAINER_SORT_ORDER_COOKIE, SortOrder.DESCENDING.name()))
        .name());
    setSortMenu(new ArrayList<SelectItem>());
    getSortMenu().add(new SelectItem(SearchIndex.SearchFields.title.name(),
        Imeji.RESOURCE_BUNDLE.getLabel("sort_title", getLocale())));
    getSortMenu().add(new SelectItem(SearchIndex.SearchFields.modified.name(),
        Imeji.RESOURCE_BUNDLE.getLabel("sort_date_mod", getLocale())));
    getSortMenu().add(new SelectItem(SearchIndex.SearchFields.creator_id.name(),
        Imeji.RESOURCE_BUNDLE.getLabel("sort_author", getLocale())));
  }

  @Override
  public void initElementsPerPageMenu() {
    setElementsPerPage(Integer
        .parseInt(CookieUtils.readNonNull(SuperPaginatorBean.numberOfContainersPerPageCookieName,
            Integer.toString(DEFAULT_ELEMENTS_PER_PAGE))));
    try {
      String options = Imeji.PROPERTIES.getProperty("imeji.container.list.size.options");
      for (String option : options.split(",")) {
        getElementsPerPageSelectItems().add(new SelectItem(option));
      }
    } catch (Exception e) {
      LOGGER.error("Error reading property imeji.image.list.size.options", e);
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
    SearchQuery searchQuery = new SearchQuery();
    int myOffset = offset;
    if (!"".equals(getQuery())) {
      searchQuery = SearchQueryParser.parseStringQuery(getQuery());
    }
    if (getSearchQuery() == null) {
      setCurrentPageNumber(1);
      setGoToPage("1");
      myOffset = 0;
    }

    SortCriterion sortCriterion = new SortCriterion();
    sortCriterion.setIndex(SearchIndexes.getIndex(getSelectedSortCriterion()));
    sortCriterion.setSortOrder(SortOrder.valueOf(getSelectedSortOrder()));

    searchResult = search(searchQuery, sortCriterion, myOffset, limit);
    setSearchQuery(searchQuery);
    searchResult.setQuery(getQuery());
    // setQuery(getQuery());
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
  public abstract SearchResult search(SearchQuery searchQuery, SortCriterion sortCriterion,
      int offset, int limit);

  /*
   * (non-Javadoc)
   *
   * @see de.mpg.imeji.presentation.beans.BasePaginatorListSessionBean#getTotalNumberOfRecords()
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
}
