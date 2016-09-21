package de.mpg.imeji.logic.search.model;

import de.mpg.imeji.logic.search.model.SearchIndex.SearchFields;

/**
 * Search element for technical metadata
 * 
 * @author saquet
 *
 */
public class SearchTechnicalMetadata extends SearchPair {
  private final String label;

  public SearchTechnicalMetadata() {
    this.label = null;
  }

  public SearchTechnicalMetadata(SearchFields field, SearchOperators operator, String value,
      String label, boolean not) {
    super(field, operator, value, not);
    this.label = label;
  }

  public String getLabel() {
    return label;
  }

  @Override
  public SEARCH_ELEMENTS getType() {
    return SEARCH_ELEMENTS.TECHNICAL_METADATA;
  }
}
