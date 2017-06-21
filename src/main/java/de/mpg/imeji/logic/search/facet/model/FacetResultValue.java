package de.mpg.imeji.logic.search.facet.model;

import java.io.Serializable;

import de.mpg.imeji.exceptions.UnprocessableError;
import de.mpg.imeji.logic.search.SearchQueryParser;
import de.mpg.imeji.logic.search.factory.SearchFactory;
import de.mpg.imeji.logic.search.model.SearchLogicalRelation.LOGICAL_RELATIONS;
import de.mpg.imeji.logic.search.model.SearchPair;
import de.mpg.imeji.logic.search.model.SearchQuery;

/**
 * The value of one {@link FacetResult}
 * 
 * @author saquet
 *
 */
public class FacetResultValue implements Serializable {
  private static final long serialVersionUID = -1248482448497426184L;
  private final String value;
  private final long count;
  private final SearchPair pair;

  public FacetResultValue(String value, long count, SearchPair query) {
    this.value = value;
    this.count = count;
    this.pair = query;
  }

  public String getValue() {
    return value;
  }

  public long getCount() {
    return count;
  }

  public String addQuery(SearchQuery facetQuery) throws UnprocessableError {
    return SearchQueryParser.transform2UTF8URL(
        new SearchFactory(facetQuery).addElement(pair, LOGICAL_RELATIONS.AND).build());
  }

}
