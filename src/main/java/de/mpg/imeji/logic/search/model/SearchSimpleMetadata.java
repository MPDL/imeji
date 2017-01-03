package de.mpg.imeji.logic.search.model;

import de.mpg.imeji.logic.search.model.SearchIndex.SearchFields;

/**
 * Similar to {@link SearchMetadata} but simpler to use. Simply user the label of the Metadata to
 * search (statement is not used). Beware, if 2 metadata are defined with the same label, there is
 * now way to differentiate them
 *
 * @author bastiens
 *
 */
public class SearchSimpleMetadata extends SearchPair {
  /**
   *
   */
  private static final long serialVersionUID = -4825456417808018282L;
  private String label;

  public SearchSimpleMetadata(String label, SearchOperators operator, String value, boolean not) {
    super(SearchFields.label, operator, value, not);
    this.label = label;
  }

  @Override
  public SEARCH_ELEMENTS getType() {
    return SEARCH_ELEMENTS.SIMPLE_METADATA;
  }

  /**
   * @return the label
   */
  public String getLabel() {
    return label;
  }

  /**
   * @param label the label to set
   */
  public void setLabel(String label) {
    this.label = label;
  }

}
