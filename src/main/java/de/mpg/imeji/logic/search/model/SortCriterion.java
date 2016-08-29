/**
 * License: src/main/resources/license/escidoc.license
 */
package de.mpg.imeji.logic.search.model;

import de.mpg.imeji.logic.search.jenasearch.JenaSearch;
import de.mpg.imeji.logic.search.model.SearchIndex.SearchFields;

/**
 * A sort criterion for a {@link JenaSearch}
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public class SortCriterion {
  public enum SortOrder {
    ASCENDING, DESCENDING;
  }

  private SearchIndex index;
  private SortOrder sortOrder;

  public SortCriterion(SearchIndex index, SortOrder so) {
    this.index = index;
    this.sortOrder = so;
  }

  public SortCriterion(SearchFields field, SortOrder order) {
    this.index = new SearchIndex(field);
    this.sortOrder = order;
  }

  public SortCriterion() {
    this.sortOrder = SortOrder.ASCENDING;
  }

  /**
   * Toggle the order the the {@link SortCriterion}
   */
  public void toggle() {
    sortOrder =
        (SortOrder.ASCENDING.equals(sortOrder) ? SortOrder.DESCENDING : SortOrder.ASCENDING);
  }

  public void setSortOrder(SortOrder sortOrder) {
    this.sortOrder = sortOrder;
  }

  public SortOrder getSortOrder() {
    return sortOrder;
  }

  public void setIndex(SearchIndex index) {
    this.index = index;
  }

  public SearchIndex getIndex() {
    return index;
  }
}
