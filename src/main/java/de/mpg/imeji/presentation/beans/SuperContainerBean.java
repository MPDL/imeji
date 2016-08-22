/**
 * License: src/main/resources/license/escidoc.license
 */
package de.mpg.imeji.presentation.beans;

import java.util.ArrayList;
import java.util.List;

import javax.faces.model.SelectItem;

import org.apache.log4j.Logger;

import de.mpg.imeji.logic.Imeji;
import de.mpg.imeji.logic.search.SearchIndexes;
import de.mpg.imeji.logic.search.SearchQueryParser;
import de.mpg.imeji.logic.search.model.SearchIndex;
import de.mpg.imeji.logic.search.model.SearchIndex.SearchFields;
import de.mpg.imeji.logic.search.model.SearchLogicalRelation.LOGICAL_RELATIONS;
import de.mpg.imeji.logic.search.model.SearchOperators;
import de.mpg.imeji.logic.search.model.SearchPair;
import de.mpg.imeji.logic.search.model.SearchQuery;
import de.mpg.imeji.logic.search.model.SearchResult;
import de.mpg.imeji.logic.search.model.SortCriterion;
import de.mpg.imeji.logic.search.model.SortCriterion.SortOrder;
import de.mpg.imeji.logic.util.PropertyReader;
import de.mpg.imeji.logic.util.UrlHelper;
import de.mpg.imeji.logic.vo.Container;
import de.mpg.imeji.logic.vo.Grant.GrantType;
import de.mpg.imeji.logic.vo.Properties.Status;
import de.mpg.imeji.presentation.facet.FacetFilter;
import de.mpg.imeji.presentation.session.SessionBean;
import de.mpg.imeji.presentation.util.BeanHelper;
import de.mpg.imeji.presentation.util.CookieUtils;

/**
 * Java Bean for {@link Container} browse pages (collections and albums)
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 * @param <T>
 */
public abstract class SuperContainerBean<T> extends BasePaginatorListSessionBean<T> {
  protected String query = "";
  protected String selectedMenu;
  private String selectedSortCriterion;
  private String selectedSortOrder;
  private String selectedFilter;
  protected SessionBean sb;
  private List<SelectItem> sortMenu = new ArrayList<SelectItem>();
  protected List<SelectItem> filterMenu = new ArrayList<SelectItem>();
  private static final Logger LOGGER = Logger.getLogger(SuperContainerBean.class);
  // private SearchQuery searchQuery ;
  protected SearchPair selectedFilterSearch;
  protected SearchQuery searchQuery = new SearchQuery();
  protected SearchResult searchResult;
  private int totalNumberOfRecords;

  /**
   * Constructor
   */
  public SuperContainerBean() {
    sb = (SessionBean) BeanHelper.getSessionBean(SessionBean.class);
    initMenus();
    selectedSortCriterion = SearchIndex.SearchFields.modified.name();
    selectedSortOrder = SortOrder.DESCENDING.name();
    setElementsPerPage(sb.getNumberOfContainersPerPage());
    try {
      String options = PropertyReader.getProperty("imeji.container.list.size.options");
      for (String option : options.split(",")) {
        getElementsPerPageSelectItems().add(new SelectItem(option));
      }
    } catch (Exception e) {
      LOGGER.error("Error reading property imeji.container.list.size.options", e);
    }
  }

  /*
   * (non-Javadoc)
   *
   * @see de.mpg.imeji.presentation.beans.BasePaginatorListSessionBean# setCookieElementPerPage()
   */
  @Override
  public void setCookieElementPerPage() {
    CookieUtils.updateCookieValue(SessionBean.numberOfContainersPerPageCookieName,
        Integer.toString(getElementsPerPage()));
  }

  /**
   * Initialize the page
   *
   * @return
   */
  public String getInit() {
    setSelectedFilterSearch(null);
    setSearchQuery(null);
    if (UrlHelper.hasParameter("f") && !UrlHelper.getParameterValue("f").isEmpty()) {
      selectedFilter = UrlHelper.getParameterValue("f");
    }
    if (UrlHelper.hasParameter("tab") && !UrlHelper.getParameterValue("tab").isEmpty()) {
      selectedMenu = UrlHelper.getParameterValue("tab");
    }
    if (UrlHelper.hasParameter("q")) {
      query = UrlHelper.getParameterValue("q");
    }
    if (selectedFilter == null) {
      selectedFilter = "all";
    }
    if (selectedMenu == null) {
      selectedMenu = "SORTING";
    }
    initMenus();
    return "";
  }

  /**
   * Initialize the menus of the page
   */
  protected void initMenus() {
    sortMenu = new ArrayList<SelectItem>();
    sortMenu.add(new SelectItem(SearchIndex.SearchFields.title.name(),
        Imeji.RESOURCE_BUNDLE.getLabel("sort_title", sb.getLocale())));
    sortMenu.add(new SelectItem(SearchIndex.SearchFields.modified.name(),
        Imeji.RESOURCE_BUNDLE.getLabel("sort_date_mod", sb.getLocale())));
    sortMenu.add(new SelectItem(SearchIndex.SearchFields.creator_id.name(),
        Imeji.RESOURCE_BUNDLE.getLabel("sort_author", sb.getLocale())));
    if (sb.getUser() != null) {
      filterMenu = new ArrayList<SelectItem>();
      filterMenu.add(new SelectItem("all",
          Imeji.RESOURCE_BUNDLE.getLabel("all_except_withdrawn", sb.getLocale())));
      filterMenu.add(new SelectItem("my",
          Imeji.RESOURCE_BUNDLE.getLabel("my_except_withdrawn", sb.getLocale())));
      filterMenu.add(new SelectItem("private",
          Imeji.RESOURCE_BUNDLE.getLabel("only_private", sb.getLocale())));
      filterMenu.add(
          new SelectItem("public", Imeji.RESOURCE_BUNDLE.getLabel("only_public", sb.getLocale())));
      filterMenu.add(new SelectItem("withdrawn",
          Imeji.RESOURCE_BUNDLE.getLabel("only_withdrawn", sb.getLocale())));
    }

  }

  /**
   * Reset the page, called when url paramenter "init" is set to 1
   */
  @Override
  public void reset() {
    super.reset();
    initMenus();
  }

  /**
   * Return the current {@link FacetFilter} for the {@link List} of {@link Container}
   *
   * @return
   */
  public SearchPair getFilter() {
    SearchPair pair = null;
    if ("my".equals(selectedFilter)) {
      pair =
          new SearchPair(SearchFields.grant, SearchOperators.EQUALS, GrantType.READ.name(), false);
    } else if ("private".equals(selectedFilter)) {
      pair = new SearchPair(SearchFields.status, SearchOperators.EQUALS,
          Status.PENDING.getUriString(), false);
    } else if ("public".equals(selectedFilter)) {
      pair = new SearchPair(SearchFields.status, SearchOperators.EQUALS,
          Status.RELEASED.getUriString(), false);
    } else if ("withdrawn".equals(selectedFilter)) {
      pair = new SearchPair(SearchIndex.SearchFields.status, SearchOperators.EQUALS,
          Status.WITHDRAWN.getUriString(), false);
    }
    return pair;
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
   * @param sortMenu
   */
  public void setSortMenu(List<SelectItem> sortMenu) {
    this.sortMenu = sortMenu;
  }

  /**
   * getter
   *
   * @return
   */
  public List<SelectItem> getSortMenu() {
    return sortMenu;
  }

  public void setSelectedSortOrder(String selectedSortOrder) {
    this.selectedSortOrder = selectedSortOrder;
  }

  /**
   * getter
   *
   * @return
   */
  public String getSelectedSortOrder() {
    return selectedSortOrder;
  }

  /**
   * setter
   *
   * @param selectedSortCriterion
   */
  public void setSelectedSortCriterion(String selectedSortCriterion) {
    this.selectedSortCriterion = selectedSortCriterion;
  }

  /**
   * getter
   *
   * @return
   */
  public String getSelectedSortCriterion() {
    return selectedSortCriterion;
  }

  /**
   * Change the sort sort order
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

  /**
   * return the {@link FacetFilter} name defined in the url as a {@link String}
   *
   * @return
   */
  public String getSelectedFilter() {
    return selectedFilter;
  }

  /**
   * setter
   *
   * @param selectedFilter
   */
  public void setSelectedFilter(String selectedFilter) {
    this.selectedFilter = selectedFilter;
  }

  /**
   * getter
   *
   * @return
   */
  public List<SelectItem> getFilterMenu() {
    return filterMenu;
  }

  /**
   * setter
   *
   * @param filterMenu
   */
  public void setFilterMenu(List<SelectItem> filterMenu) {
    this.filterMenu = filterMenu;
  }

  /**
   * return the internationalized labels of the {@link FacetFilter}
   *
   * @return
   */
  public String getFilterLabel() {
    for (SelectItem si : filterMenu) {
      if (selectedFilter.equals(si.getValue())) {
        return si.getLabel();
      }
    }
    return Imeji.RESOURCE_BUNDLE.getLabel("all_except_withdrawn", sb.getLocale());
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
   * @return the selectedFilterSearch
   */
  public SearchPair getSelectedFilterSearch() {
    return selectedFilterSearch;
  }

  /**
   * @param selectedFilterSearch the selectedFilterSearch to set
   */
  public void setSelectedFilterSearch(SearchPair selectedFilterSearch) {
    this.selectedFilterSearch = selectedFilterSearch;
  }


  /**
   * Checks if filters between two different searches have been changed in order to trigger new
   * query execution
   *
   * @param p1
   * @param p2
   * @return
   */
  protected boolean changedFilters(SearchPair p1, SearchPair p2) {
    if ((p1 == null && p2 != null) || (p1 != null && p2 == null)) {
      return true;
    }

    if (p1 != null && p2 != null) {
      if (!p1.getValue().equals(p2.getValue())) {
        return true;
      }
    }

    return false;
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
    // get Filters for Collections
    SearchPair sp = getFilter();
    if (sp != null) {
      searchQuery.addLogicalRelation(LOGICAL_RELATIONS.AND);
      searchQuery.addPair(sp);
    }
    if (getSearchQuery() == null || changedFilters(sp, getSelectedFilterSearch())) {
      setCurrentPageNumber(1);
      setGoToPage("1");
      myOffset = 0;
    }

    SortCriterion sortCriterion = new SortCriterion();
    sortCriterion.setIndex(SearchIndexes.getIndex(getSelectedSortCriterion()));
    sortCriterion.setSortOrder(SortOrder.valueOf(getSelectedSortOrder()));

    searchResult = search(searchQuery, sortCriterion, myOffset, limit);
    setSearchQuery(searchQuery);
    setSelectedFilterSearch(sp);
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

}
