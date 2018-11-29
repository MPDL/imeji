package de.mpg.imeji.logic.search.model;

import java.io.Serializable;

import de.mpg.imeji.logic.model.SearchFields;
import de.mpg.imeji.logic.search.jenasearch.JenaSearch;

/**
 * A sort criterion for a {@link JenaSearch}
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public class SortCriterion implements Serializable {
  private static final long serialVersionUID = -8328050571389634798L;

  public enum SortOrder {
    ASCENDING,
    DESCENDING;
  }

  private final SortOrder sortOrder;
  private final SearchFields field;

  public SortCriterion(SearchFields field, SortOrder order) {
    this.field = field;
    this.sortOrder = order;
  }

  public SortOrder getSortOrder() {
    return sortOrder;
  }

  public SearchFields getField() {
    return field;
  }
}
