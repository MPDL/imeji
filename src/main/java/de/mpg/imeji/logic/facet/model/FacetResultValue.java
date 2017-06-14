package de.mpg.imeji.logic.facet.model;

/**
 * The value of one {@link FacetResult}
 * 
 * @author saquet
 *
 */
public class FacetResultValue {
  private final String value;
  private final long count;
  private final String query;

  public FacetResultValue(String value, long count, String query) {
    this.value = value;
    this.count = count;
    this.query = query;
  }

  public String getValue() {
    return value;
  }

  public long getCount() {
    return count;
  }
  
  public String getQuery() {
    return query;
  }
}
