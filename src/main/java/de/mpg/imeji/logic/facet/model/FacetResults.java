package de.mpg.imeji.logic.facet.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import de.mpg.imeji.logic.search.model.SearchQuery;

/**
 * Results for the facets
 * 
 * @author saquet
 *
 */
public class FacetResults implements Serializable {
  private static final long serialVersionUID = -8243050151477865485L;
  private List<FacetResult> results = new ArrayList<>();
  private SearchQuery query;

  /**
   * @return the results
   */
  public List<FacetResult> getResults() {
    return results;
  }

  /**
   * @param results the results to set
   */
  public void setResults(List<FacetResult> results) {
    this.results = results;
  }

  /**
   * @return the query
   */
  public SearchQuery getQuery() {
    return query;
  }

  /**
   * @param query the query to set
   */
  public void setQuery(SearchQuery query) {
    this.query = query;
  }

}
