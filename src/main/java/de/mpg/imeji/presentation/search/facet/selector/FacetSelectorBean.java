package de.mpg.imeji.presentation.search.facet.selector;

import java.util.List;
import java.util.TreeMap;
import java.util.stream.Collectors;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import de.mpg.imeji.logic.search.facet.FacetService;
import de.mpg.imeji.logic.search.facet.model.Facet;
import de.mpg.imeji.logic.search.facet.model.FacetResult;
import de.mpg.imeji.logic.search.model.SearchResult;
import de.mpg.imeji.presentation.beans.SuperBean;

/**
 * Bean for the FacetSelector
 * 
 * @author saquet
 *
 */
@ManagedBean(name = "FacetSelectorBean")
@ViewScoped
public class FacetSelectorBean extends SuperBean {
  private static final long serialVersionUID = -1071527421059585519L;
  private final TreeMap<String, List<FacetSelectorEntry>> entries = new TreeMap<>();


  public FacetSelectorBean() {

  }

  public String init(SearchResult result) {
    FacetService facetService = new FacetService();
    for (FacetResult fr : result.getFacets()) {
      Facet facet = facetService.retrieveByIndexFromCache(fr.getIndex());
      entries.put(facet.getName(), fr.getValues().stream()
          .map(v -> new FacetSelectorEntry(v, facet)).collect(Collectors.toList()));
    }
    return "";
  }

  /**
   * @return the entries
   */
  public TreeMap<String, List<FacetSelectorEntry>> getEntries() {
    return entries;
  }



}
