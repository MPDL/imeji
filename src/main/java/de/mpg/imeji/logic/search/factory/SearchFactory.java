package de.mpg.imeji.logic.search.factory;


import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import de.mpg.imeji.exceptions.UnprocessableError;
import de.mpg.imeji.logic.search.Search;
import de.mpg.imeji.logic.search.Search.SearchObjectTypes;
import de.mpg.imeji.logic.search.SearchQueryParser;
import de.mpg.imeji.logic.search.elasticsearch.ElasticSearch;
import de.mpg.imeji.logic.search.jenasearch.JenaSearch;
import de.mpg.imeji.logic.search.model.SearchElement;
import de.mpg.imeji.logic.search.model.SearchElement.SEARCH_ELEMENTS;
import de.mpg.imeji.logic.search.model.SearchGroup;
import de.mpg.imeji.logic.search.model.SearchLogicalRelation.LOGICAL_RELATIONS;
import de.mpg.imeji.logic.search.model.SearchMetadata;
import de.mpg.imeji.logic.search.model.SearchPair;
import de.mpg.imeji.logic.search.model.SearchQuery;
import de.mpg.imeji.logic.search.model.SearchTechnicalMetadata;

/**
 * Factory for {@link Search}
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public class SearchFactory {
  private static SEARCH_IMPLEMENTATIONS defaultSearch = SEARCH_IMPLEMENTATIONS.JENA;
  private SearchQuery query = new SearchQuery();

  public enum SEARCH_IMPLEMENTATIONS {
    JENA, ELASTIC;
  }

  public SearchFactory() {
    query = new SearchQuery();
  }

  public SearchFactory(SearchQuery query) {
    this.query = new SearchQuery(query != null ? query.getElements() : new ArrayList<>());
  }

  public SearchFactory clone() {
    return new SearchFactory(query);
  }

  public SearchFactory initQuery(SearchQuery query) {
    this.query = new SearchQuery(query != null ? query.getElements() : new ArrayList<>());
    return this;
  }

  public SearchFactory initQuery(String query) throws UnprocessableError {
    this.query = new SearchQuery(query != null
        ? SearchQueryParser.parseStringQuery(query).getElements() : new ArrayList<>());
    return this;
  }

  public SearchFactory initFilter(SearchQuery query) {
    query.setFilterElements(cleanElements(query.getElements()));
    return this;
  }

  public SearchFactory initFilter(String filter) throws UnprocessableError {
    if (filter != null) {
      query.setFilterElements(SearchQueryParser.parseStringQuery(filter).getElements());
    }
    return this;
  }

  /**
   * Build a {@link SearchQuery} of the Factory
   * 
   * @return
   */
  public SearchQuery build() {
    return query;
  }

  /**
   * Return a clean Search Query
   * 
   * @param query
   * @return
   */
  private SearchQuery cleanQuery(SearchQuery query) {
    return new SearchQuery(cleanElements(query.getElements()), query.getFilterElements());
  }

  /**
   * Clean the elements
   * 
   * @param elements
   * @return
   */
  private List<SearchElement> cleanElements(List<SearchElement> elements) {
    List<SearchElement> cleaned = new ArrayList<>();
    for (SearchElement element : elements) {
      if (SEARCH_ELEMENTS.GROUP.equals(element.getType()) && elements.size() == 1) {
        cleaned.addAll(cleanElements(element.getElements()));
      } else {
        cleaned.add(cleanElement(element));
      }
    }
    return cleaned;
  }

  /**
   * Remove an element from the query.
   * <li>Note: only the first level elements can be removed. (for example, a pair of a group can't
   * be removed)
   * 
   * @param element
   */
  public SearchFactory remove(SearchElement element) {
    List<SearchElement> elements = new ArrayList<>();
    for (int i = 0; i < query.getElements().size(); i++) {
      if (!query.getElements().get(i).isSame(element)) {
        elements.add(query.getElements().get(i));
      } else if (i > 0) {
        elements.remove(elements.size() - 1);// remove Logical relation
      }
    }
    if (elements.size() > 0 && elements.get(0).getType() == SEARCH_ELEMENTS.LOGICAL_RELATIONS) {
      elements.remove(0);
    }
    query.setElements(elements);
    return this;
  }

  /**
   * Remove the query from the search factory
   * 
   * @param q
   */
  public SearchFactory remove(SearchQuery q) {
    remove(toElement(q));
    return this;
  }

  /**
   * True if the query contains this element
   * <li>Note: only the first level can be found. (for example, a pair of a group won't be found)
   * 
   * @param element
   * @return
   */
  public boolean contains(SearchElement element) {
    return query.getElements().stream().anyMatch(e -> e.isSame(element));
  }

  /**
   * True if the query contains this query
   * <li>Note: only the first level can be found. (for example, a pair of a group won't be found)
   * 
   * @param element
   * @return
   */
  public boolean contains(SearchQuery q) {
    return contains(toElement(q));
  }

  /**
   * Return the Metadata Elements having this index
   * 
   * @param index
   * @return
   */
  public List<SearchMetadata> getElementsWithIndex(String index) {
    String mdIndex = index.replace("md.", "").split("\\.")[0];
    return query.getElements().stream()
        .filter(e -> e instanceof SearchMetadata && ((SearchMetadata) e).getIndex().equals(mdIndex))
        .map(e -> (SearchMetadata) e).collect(Collectors.toList());
  }

  /**
   * Return a searchquery as a searchpair or a searchgroup
   * 
   * @param q
   * @return
   */
  private SearchElement toElement(SearchQuery q) {
    if (q != null && q.getElements() != null && !q.getElements().isEmpty()) {
      if (q.getElements().size() > 1) {
        return new SearchFactory(q).buildAsGroup();
      } else {
        return q.getElements().get(0);
      }
    }
    return q;
  }

  /**
   * Clean a single element
   * 
   * @param element
   * @return
   */
  private SearchElement cleanElement(SearchElement element) {
    if (SEARCH_ELEMENTS.GROUP.equals(element.getType()) && element.getElements().size() == 1) {
      return cleanElement(element.getElements().get(0));
    }
    return element;
  }

  public static void main(String[] args) throws UnprocessableError {
    String q1 =
        "(creator%3Dadmin%40imeji.org+OR+collaborator%3Dadmin%40imeji.org AND (md.album.text%3D1+Album+au+hasard) AND () OR (creator%3Dadmin%40imeji.org+OR+collaborator%3Dadmin%40imeji.org AND (md.album.text%3D1+Album+au+hasard AND ())))";
    String q2 = "(creator%3Dadmin%40imeji.org+OR+collaborator%3Dadmin%40imeji.org)";
    String q = "((md.title.text%3DMes+premières+photos)+AND+md.album.text%3D1+Album+au+hasard)";
    SearchQuery sq = SearchQueryParser.parseStringQuery(q1);
    sq = new SearchFactory(sq).build();
    System.out.println(SearchQueryParser.transform2URL(sq));
  }

  /**
   * Build a Search Group with the current factory
   * 
   * @return
   */
  public SearchGroup buildAsGroup() {
    if (query.getElements().size() == 1
        && query.getElements().get(0).getType().equals(SEARCH_ELEMENTS.GROUP)) {
      return (SearchGroup) query.getElements().get(0);
    } else {
      SearchGroup group = new SearchGroup();
      group.setGroup(query.getElements());
      return group;
    }
  }


  /**
   * Add the elements in the query with an AND relation
   * 
   * @param elements
   * @return
   * @throws UnprocessableError
   */
  public SearchFactory and(List<SearchElement> elements) throws UnprocessableError {
    for (SearchElement element : elements) {
      addElement(element, LOGICAL_RELATIONS.AND);
    }
    return this;
  }

  /**
   * Add the element with a end relation
   * 
   * @param element
   * @return
   * @throws UnprocessableError
   */
  public SearchFactory and(SearchElement element) throws UnprocessableError {
    addElement(element, LOGICAL_RELATIONS.AND);
    return this;
  }

  /**
   * Add a search query to the current search query
   * 
   * @param q
   * @return
   * @throws UnprocessableError
   */
  public SearchFactory and(SearchQuery q) throws UnprocessableError {
    addElement(toElement(q), LOGICAL_RELATIONS.AND);
    return this;
  }

  /**
   * Set the Not for the whole Query
   * 
   * @param not
   * @return
   */
  public SearchFactory setNot(boolean not) {
    SearchGroup g = buildAsGroup();
    g.setNot(not);
    query = new SearchQuery(g.getElements());
    return this;
  }

  /**
   * Add the elements in the query with an OR relation
   * 
   * @param elements
   * @return
   * @throws UnprocessableError
   */
  public SearchFactory or(List<SearchElement> elements) throws UnprocessableError {
    for (SearchElement element : elements) {
      addElement(element, LOGICAL_RELATIONS.OR);
    }
    return this;
  }

  /**
   * Add the elements to the query as following: query REL (Element1 OR Element2 ...)
   * 
   * @param elements
   * @param rel
   * @return
   * @throws UnprocessableError
   */
  public SearchFactory addOrGroup(List<SearchElement> elements, LOGICAL_RELATIONS rel)
      throws UnprocessableError {
    query.addLogicalRelation(rel);
    query.addGroup(new SearchFactory().or(elements).buildAsGroup());
    return this;
  }

  /**
   * Add the elements to the query as following: query REL (Element1 AND Element2 ...)
   * 
   * @param elements
   * @param rel
   * @return
   * @throws UnprocessableError
   */
  public SearchFactory addAndGroup(List<SearchElement> elements, LOGICAL_RELATIONS rel)
      throws UnprocessableError {
    query.addLogicalRelation(rel);
    query.addGroup(new SearchFactory().and(elements).buildAsGroup());
    return this;
  }


  /**
   * Add a SearchElement to the query. Valid elements: {@link SearchPair}, {@link SearchGroup},
   * {@link SearchMetadata}, {@link SearchTechnicalMetadata}
   * 
   * @param element
   * @param rel
   * @return
   * @throws UnprocessableError
   */
  public SearchFactory addElement(SearchElement element, LOGICAL_RELATIONS rel)
      throws UnprocessableError {
    if (element != null && !element.isEmpty()) {
      query.addLogicalRelation(rel);
      if (element instanceof SearchPair) {
        query.addPair((SearchPair) element);
      } else if (element instanceof SearchGroup) {
        query.addGroup((SearchGroup) element);
      } else {
        throw new UnprocessableError("Invalid SearchElement type " + element.getClass());
      }
    }
    return this;
  }

  /**
   * Create a new {@link Search}
   *
   * @return
   */
  public static Search create() {
    return create(defaultSearch);
  }

  /**
   * Create A new {@link Search}
   *
   * @param impl
   * @return
   */
  public static Search create(SEARCH_IMPLEMENTATIONS impl) {
    return create(SearchObjectTypes.ALL, impl);
  }

  /**
   * Create a new {@link Search}
   *
   * @param type
   * @param impl TODO
   * @return
   */
  public static Search create(SearchObjectTypes type, SEARCH_IMPLEMENTATIONS impl) {
    switch (impl) {
      case JENA:
        return new JenaSearch(type, null);
      case ELASTIC:
        return new ElasticSearch(type);
    }
    return null;
  }

  /**
   * Create a new {@link Search} !!! Only for JENA Search !!!
   *
   * @param type
   * @param containerUri
   * @return
   */
  public static Search create(SearchObjectTypes type, String containerUri) {
    return new JenaSearch(type, containerUri);
  }
}
