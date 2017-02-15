package de.mpg.imeji.logic.search.model;

import java.util.ArrayList;
import java.util.List;

import de.mpg.imeji.logic.util.StringHelper;

/**
 * Define a pair of a {@link SearchIndex} with a {@link String} value, related by a
 * {@link SearchOperators}<br/>
 * {@link SearchPair} are {@link SearchElement} of a {@link SearchQuery}
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public class SearchPair extends SearchElement {
  private static final long serialVersionUID = -1522952540004708017L;
  private final SearchOperators operator;
  private final String value;
  private final SearchFields field;

  /**
   * Construct an empty Pair
   */
  public SearchPair() {
    this(null, null, null, false);
  }

  /**
   * Constructor with default value for the operator (regex) and for not (false)
   * 
   * @param field
   * @param value
   */
  public SearchPair(SearchFields field, String value) {
    this(field, SearchOperators.EQUALS, value, false);
  }

  /**
   * Constructor
   * 
   * @param field
   * @param operator
   * @param value
   * @param not
   */
  public SearchPair(SearchFields field, SearchOperators operator, String value, boolean not) {
    this.operator = operator;
    this.value = value;
    this.field = field;
    setNot(not);
  }

  public SearchOperators getOperator() {
    return operator;
  }

  public String getValue() {
    return value;
  }

  @Override
  public SEARCH_ELEMENTS getType() {
    return SEARCH_ELEMENTS.PAIR;
  }

  @Override
  public List<SearchElement> getElements() {
    return new ArrayList<SearchElement>();
  }


  @Override
  public boolean isEmpty() {
    return StringHelper.isNullOrEmptyTrim(value);
  }

  /**
   * @return the field
   */
  public SearchFields getField() {
    return field;
  }

  @Override
  public boolean isSame(SearchElement element) {
    final SearchPair pair = toPair(element);
    if (pair != null) {
      return pair.field.equals(field) && pair.operator.equals(operator) && pair.value.equals(value);
    }
    return false;
  }

  /**
   * If the element can be reduced to one Pair, return a pair. Otherwise return null
   *
   * @param element
   * @return
   */
  private SearchPair toPair(SearchElement element) {
    if (element.getType() == SEARCH_ELEMENTS.PAIR) {
      return (SearchPair) element;
    } else if (element.getType() == SEARCH_ELEMENTS.QUERY
        && ((SearchQuery) element).getElements().size() == 1) {
      return toPair(((SearchQuery) element).getElements().get(0));
    } else if (element.getType() == SEARCH_ELEMENTS.GROUP
        && ((SearchGroup) element).getElements().size() == 1) {
      return toPair(((SearchGroup) element).getElements().get(0));
    }
    return null;
  }
}
