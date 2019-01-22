package de.mpg.imeji.logic.search.facet.model;

import java.io.Serializable;
import java.text.DecimalFormat;

/**
 * The value of one {@link FacetResult}
 * 
 * @author saquet
 *
 */
public class FacetResultValue implements Serializable {
  private static final long serialVersionUID = -1248482448497426184L;
  private static final DecimalFormat NUMBER_FORMAT = new DecimalFormat("#.##");;
  private final String label;
  private final long count;
  private String max;
  private String min;

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

  /**
   * @return the min
   */
  public String getMin() {
    return min;
  }

  /**
   * @param min the min to set
   */
  public void setMin(String min) {
    this.min = min;
  }

  /**
   * @return the max
   */
  public String getMax() {
    return max;
  }

  /**
   * @param max the max to set
   */
  public void setMax(String max) {
    this.max = max;
  }

}
