package de.mpg.imeji.presentation.search.breadcrumb;

import java.io.Serializable;

import de.mpg.imeji.logic.search.facet.model.Facet;

/**
 * An entry of the {@link SearchBreadcrumbBean}
 * 
 * @author saquet
 *
 */
public class SearchBreadcrumbEntry implements Serializable {
  private static final long serialVersionUID = 2936538556616876085L;
  private final String index;
  private final String label;
  private final String value;
  private final String removeQuery;

  public SearchBreadcrumbEntry(Facet facet, String value, String removeQuery) {
    this.index = facet.getIndex();
    this.value = value;
    this.label = facet.getName();
    this.removeQuery = removeQuery;
  }

  /**
   * @return the index
   */
  public String getIndex() {
    return index;
  }

  /**
   * @return the value
   */
  public String getValue() {
    return value;
  }

  /**
   * @return the removeQuery
   */
  public String getRemoveQuery() {
    return removeQuery;
  }

  /**
   * @return the label
   */
  public String getLabel() {
    return label;
  }

}
