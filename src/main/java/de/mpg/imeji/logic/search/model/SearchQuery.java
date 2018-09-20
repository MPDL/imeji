package de.mpg.imeji.logic.search.model;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.Logger; 
import org.apache.logging.log4j.LogManager;

/**
 * A search query composed of {@link SearchElement}
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public class SearchQuery extends SearchElement {
  private static final long serialVersionUID = 409836256942379675L;
  private static final Logger LOGGER = LogManager.getLogger(SearchQuery.class);
  /**
   * The elements of the {@link SearchQuery}
   */
  private List<SearchElement> elements = new ArrayList<>();

  private List<SearchElement> filterElements = new ArrayList<>();

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
   * Initialize the factory with an exiecting query
   * 
   * @param elements
   * @param filterElements
   */
  public SearchQuery(List<SearchElement> elements, List<SearchElement> filterElements) {
    this.elements = new ArrayList<SearchElement>(elements);
    this.filterElements = new ArrayList<SearchElement>(filterElements);
  }

  /**
   * Clone a {@link SearchQuery}
   */
  public SearchQuery copy() {
    final SearchQuery q = new SearchQuery();
    q.setElements(new ArrayList<>(elements));
    return q;
  }

  /**
   * True if the Search element is found in the search query.
   * <li>Note: Only first level elements are found, i.e. a searchPair will not be found if included
   * into a searchgroup
   * 
   * @param query
   * @param element
   * @return
   */
  boolean contains(SearchElement element) {
    return elements.stream().anyMatch(e -> element.isSame(e));
  }

  @Override
  public boolean isEmpty() {
    return super.isEmpty() && filterElements.isEmpty();
  }

  /**
   * Clear the {@link SearchElement} of the {@link SearchQuery}
   */
  public void clear() {
    elements.clear();
    filterElements.clear();
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
      final SearchGroup g1 = new SearchGroup();
      g1.setGroup(((SearchQuery) element).elements);
      final SearchGroup g2 = new SearchGroup();
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
    final SearchQuery query = new SearchQuery();
    try {
      if (element.getType() == SEARCH_ELEMENTS.PAIR) {
        query.addPair((SearchPair) element);
      } else if (element.getType() == SEARCH_ELEMENTS.GROUP) {
        query.addGroup((SearchGroup) element);
      }
    } catch (final Exception e) {
      LOGGER.error("Error Transforming SearchElement to SearchQuery");
    }

    return query;
  }

  /**
   * @return the filterElements
   */
  public List<SearchElement> getFilterElements() {
    return filterElements;
  }

  /**
   * @param filterElements the filterElements to set
   */
  public void setFilterElements(List<SearchElement> filterElements) {
    this.filterElements = filterElements;
  }
}
