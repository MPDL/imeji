package de.mpg.imeji.logic.search.facet.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import de.mpg.imeji.logic.search.model.SearchQuery;

/**
 * An entry of a {@link FacetResult}
 * 
 * @author saquet
 *
 */
public class FacetResult implements Serializable {
  private static final long serialVersionUID = 7184359434011876177L;
  private String name;
  private String index;
  private final List<FacetResultValue> values = new ArrayList<>();

  public FacetResult(String name, String index) {
    this.name = name;
    this.index = index;
  }
  

  /**
   * @return the name
   */
  public String getName() {
    return name;
  }

  /**
   * @param name the name to set
   */
  public void setName(String name) {
    this.name = name;
  }

  /**
   * @return the index
   */
  public String getIndex() {
    return index;
  }

  /**
   * @param index the index to set
   */
  public void setIndex(String index) {
    this.index = index;
  }

  /**
   * @return the values
   */
  public List<FacetResultValue> getValues() {
    return values;
  }

  /**
   * True if this result has at least one value
   * 
   * @return
   */
  public boolean hasValue() {
    return values.size() > 0;
  }
}
