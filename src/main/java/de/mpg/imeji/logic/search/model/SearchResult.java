package de.mpg.imeji.logic.search.model;

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
  private final int numberOfRecords;
  private final int numberOfItems;
  private final int numberOfItemsOfCollection;
  private final int numberOfSubcollections;
  private List<String> results;
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
    numberOfItems = numberOfRecords;
    numberOfItemsOfCollection = numberOfItems;
    numberOfSubcollections = 0;
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
    this(ids, null);
  }

  public SearchResult(List<String> ids, long numberOfRecords, long numberOfItems,
      long numberOfItemsOfCollection, long numberOfSubcollections, List<FacetResult> facetResults) {
    this.numberOfRecords = (int) numberOfRecords;
    this.numberOfItems = (int) numberOfItems;
    this.numberOfItemsOfCollection = (int) numberOfItemsOfCollection;
    this.numberOfSubcollections = (int) numberOfSubcollections;
    this.results = ids;
    this.facets = facetResults;
  }

  public int getNumberOfRecords() {
    return numberOfRecords;
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

  public int getNumberOfItems() {
    return numberOfItems;
  }

  public int getNumberOfSubcollections() {
    return numberOfSubcollections;
  }

  public int getNumberOfItemsOfCollection() {
    return numberOfItemsOfCollection;
  }
}
