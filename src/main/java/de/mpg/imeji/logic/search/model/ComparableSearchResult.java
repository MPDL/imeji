package de.mpg.imeji.logic.search.model;

import java.util.List;

import de.mpg.imeji.logic.search.model.SortCriterion.SortOrder;
import de.mpg.imeji.logic.search.util.SortHelper;

/**
 * Search result (as {@link String} entry of a {@link List} of uris) which can be sorted
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public final class ComparableSearchResult implements Comparable<ComparableSearchResult> {
  private final String value;
  private final String sortValue;
  private final SortOrder order;

  public ComparableSearchResult(String s, SortOrder order) {
    this.value = SortHelper.removeSortValue(s);
    this.sortValue = SortHelper.parseSortValue(s);
    this.order = order;
  }

  public String getValue() {
    return value;
  }

  public String getSortValue() {
    return sortValue;
  }

  @Override
  public int compareTo(ComparableSearchResult o) {
    return o.getSortValue().compareToIgnoreCase(sortValue) * orderAsInteger();
  }

  private int orderAsInteger() {
    if (SortOrder.DESCENDING.equals(order)) {
      return 1;
    }
    return -1;
  }

  public SortOrder getOrder() {
    return order;
  }
}
