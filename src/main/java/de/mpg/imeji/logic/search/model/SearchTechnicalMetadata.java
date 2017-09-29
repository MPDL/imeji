package de.mpg.imeji.logic.search.model;

import de.mpg.imeji.logic.model.SearchFields;

/**
 * Search element for technical metadata
 *
 * @author saquet
 *
 */
public class SearchTechnicalMetadata extends SearchPair {
  private static final long serialVersionUID = -2241342408739861614L;
  private final String label;

  public SearchTechnicalMetadata(SearchOperators operator, String value, String label,
      boolean not) {
    super(SearchFields.technical, operator, value, not);
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
