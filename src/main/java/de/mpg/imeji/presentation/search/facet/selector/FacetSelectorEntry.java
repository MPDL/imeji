package de.mpg.imeji.presentation.search.facet.selector;

import java.io.Serializable;
import java.util.List;
import java.util.stream.Collectors;

import de.mpg.imeji.logic.search.facet.FacetService;
import de.mpg.imeji.logic.search.facet.model.Facet;
import de.mpg.imeji.logic.search.facet.model.FacetResult;
import de.mpg.imeji.logic.search.model.SearchQuery;

/**
 * Entry of the {@link FacetSelectorBean}
 * 
 * @author saquet
 *
 */
public class FacetSelectorEntry implements Serializable {
  private static final long serialVersionUID = -982329261816788783L;
  private final Facet facet;
  private final List<FacetSelectorEntryValue> values;

  public FacetSelectorEntry(FacetResult facetResult, SearchQuery facetsQuery) {
    FacetService facetService = new FacetService();
    this.facet = facetService.retrieveByIndexFromCache(facetResult.getIndex());
    this.values = facetResult.getValues().stream()
        .map(v -> new FacetSelectorEntryValue(v, facet, facetsQuery)).collect(Collectors.toList());
  }

  /**
   * @return the facet
   */
  public Facet getFacet() {
    return facet;
  }

  /**
   * @return the values
   */
  public List<FacetSelectorEntryValue> getValues() {
    return values;
  }

}
