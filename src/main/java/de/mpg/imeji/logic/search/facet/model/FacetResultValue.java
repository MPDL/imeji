package de.mpg.imeji.logic.search.facet.model;

import java.io.Serializable;

/**
 * The value of one {@link FacetResult}
 * 
 * @author saquet
 *
 */
public class FacetResultValue implements Serializable {
  private static final long serialVersionUID = -1248482448497426184L;
  private final String label;
  private final long count;

  public FacetResultValue(String label, long count) {
    this.label = label;
    this.count = count;
  }

  public String getLabel() {
    return label;
  }

  public long getCount() {
    return count;
  }

}
