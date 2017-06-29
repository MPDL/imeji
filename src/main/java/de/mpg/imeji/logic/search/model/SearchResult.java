package de.mpg.imeji.logic.search.model;

import java.util.ArrayList;
import java.util.List;

import com.hp.hpl.jena.Jena;

import de.mpg.imeji.logic.search.facet.model.FacetResult;
import de.mpg.imeji.logic.search.jenasearch.JenaSearch;
import de.mpg.imeji.logic.search.util.SortHelper;

/**
 * Result {@link Object} for {@link JenaSearch}
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public class SearchResult {
  private int numberOfRecords = 0;
  private List<String> results = new ArrayList<String>();
  private String query = null;
  private SortCriterion sort;
  private List<FacetResult> facets;

  /**
   * Create a new {@link SearchResult} from a {@link List} of String, and sort it if a
   * {@link SortCriterion} has been defined <br/>
   * Sorting not made on {@link Jena} level, for performance purpose
   *
   * @param unsortedResults
   * @param sort
   */
  public SearchResult(List<String> unsortedResults, SortCriterion sort) {
    numberOfRecords = unsortedResults.size();
    if (sort != null) {
      this.sort = sort;
      results = SortHelper.sort(unsortedResults, this.sort.getSortOrder());
    } else {
      results = unsortedResults;
    }
  }

  /**
   * Default constructor
   */
  public SearchResult(List<String> ids) {
    numberOfRecords = ids.size();
    results = ids;
  }

  /**
   * Constructor when the number of records is known
   */
  public SearchResult(List<String> ids, long numberOfRecords) {
    this.numberOfRecords = (int) numberOfRecords;
    results = ids;
  }

  public SearchResult(List<String> ids, long numberOfRecords, List<FacetResult> facetResults) {
    this.numberOfRecords = (int) numberOfRecords;
    this.results = ids;
    this.facets = facetResults;
  }

  public int getNumberOfRecords() {
    return numberOfRecords;
  }

  public void setNumberOfRecords(int numberOfRecords) {
    this.numberOfRecords = numberOfRecords;
  }

  public List<String> getResults() {
    return results;
  }

  public void setResults(List<String> results) {
    this.results = results;
  }

  public String getQuery() {
    return query;
  }

  public void setQuery(String query) {
    this.query = query;
  }

  public SortCriterion getSort() {
    return sort;
  }

  public void setSort(SortCriterion sort) {
    this.sort = sort;
  }

  /**
   * @return the facets
   */
  public List<FacetResult> getFacets() {
    return facets;
  }

  /**
   * @param facets the facets to set
   */
  public void setFacets(List<FacetResult> facets) {
    this.facets = facets;
  }
}
