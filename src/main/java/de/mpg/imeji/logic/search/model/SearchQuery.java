package de.mpg.imeji.logic.search.model;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

/**
 * A search query composed of {@link SearchElement}
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public class SearchQuery extends SearchElement {
  private static final Logger LOGGER = Logger.getLogger(SearchQuery.class);
  /**
   * The elements of the {@link SearchQuery}
   */
  private List<SearchElement> elements = null;

  /**
   * Construct an empty {@link SearchQuery}
   */
  public SearchQuery() {
    elements = new ArrayList<SearchElement>();
  }

  /**
   * Construct a {@link SearchQuery} with a {@link List} of {@link SearchElement}
   *
   * @param elements
   */
  public SearchQuery(List<SearchElement> elements) {
    this.elements = new ArrayList<SearchElement>(elements);
  }

  /**
   * Clone a {@link SearchQuery}
   */
  public SearchQuery copy() {
    SearchQuery q = new SearchQuery();
    q.setElements(new ArrayList<>(elements));
    return q;
  }

  /**
   * Clear the {@link SearchElement} of the {@link SearchQuery}
   */
  public void clear() {
    elements.clear();
  }

  public void setElements(List<SearchElement> elements) {
    this.elements = elements;
  }

  @Override
  public List<SearchElement> getElements() {
    return elements;
  }

  @Override
  public SEARCH_ELEMENTS getType() {
    return SEARCH_ELEMENTS.QUERY;
  }

  @Override
  public boolean isSame(SearchElement element) {
    element = toSearchQuery(element);
    if (element.getType() == SEARCH_ELEMENTS.QUERY) {
      SearchGroup g1 = new SearchGroup();
      g1.setGroup(((SearchQuery) element).elements);
      SearchGroup g2 = new SearchGroup();
      g2.setGroup(elements);
      return g1.isSame(g2);
    }
    return false;
  }

  /**
   * Transform a SearchElent to a SearchQuery when possible (i.e for SearchPair and SearchGroup)
   * 
   * @param element
   * @return
   */
  public static SearchQuery toSearchQuery(SearchElement element) {
    SearchQuery query = new SearchQuery();
    try {
      if (element.getType() == SEARCH_ELEMENTS.PAIR) {
        query.addPair((SearchPair) element);
      } else if (element.getType() == SEARCH_ELEMENTS.GROUP) {
        query.addGroup((SearchGroup) element);
      }
    } catch (Exception e) {
      LOGGER.error("Error Transforming SearchElement to SearchQuery");
    }

    return query;
  }
}
