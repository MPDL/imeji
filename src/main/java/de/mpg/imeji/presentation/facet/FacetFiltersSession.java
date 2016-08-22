/**
 * License: src/main/resources/license/escidoc.license
 */
package de.mpg.imeji.presentation.facet;

import java.util.ArrayList;
import java.util.List;

import de.mpg.imeji.presentation.facet.Facet.FacetType;

/**
 * Session where the {@link FacetFilter} are stored
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public class FacetFiltersSession {
  private List<FacetFilter> filters = new ArrayList<FacetFilter>();
  private String wholeQuery = "";
  private List<FacetFilter> noResultsFilters = new ArrayList<FacetFilter>();

  /**
   * Check if the name correspond to an existing filter name
   *
   * @param name
   * @return
   */
  public boolean isFilter(String name) {
    for (FacetFilter f : filters) {
      if (f.getLabel().equalsIgnoreCase(name)) {
        return true;
      }
      if (f.getLabel().equalsIgnoreCase("No " + name)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Check if the filter has no results
   *
   * @param name
   * @return
   */
  public boolean isNoResultFilter(String name) {
    for (FacetFilter f : noResultsFilters) {
      if (f.getLabel().equalsIgnoreCase(name)) {
        return true;
      }
    }
    return false;
  }

  /**
   * @return filter with the search query
   */
  public FacetFilter getSearchFilter() {
    for (FacetFilter f : filters) {
      if (f.getType() == FacetType.SEARCH) {
        return f;
      }
    }
    return null;
  }

  public List<FacetFilter> getFilters() {
    return filters;
  }

  public void setFilters(List<FacetFilter> filters) {
    this.filters = filters;
  }

  public String getWholeQuery() {
    return wholeQuery;
  }

  public void setWholeQuery(String wholeQuery) {
    this.wholeQuery = wholeQuery;
  }

  public List<FacetFilter> getNoResultsFilters() {
    return noResultsFilters;
  }

  public void setNoResultsFilters(List<FacetFilter> noResultsFilters) {
    this.noResultsFilters = noResultsFilters;
  }
}
