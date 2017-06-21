package de.mpg.imeji.presentation.search.facet;

import java.util.ArrayList;
import java.util.List;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.search.facet.FacetService;
import de.mpg.imeji.logic.search.facet.model.Facet;
import de.mpg.imeji.presentation.beans.SuperBean;

@ManagedBean(name = "FacetsBean")
@ViewScoped
public class FacetsBean extends SuperBean {
  private static final long serialVersionUID = -2474393161093378625L;
  private List<Facet> facets = new ArrayList<>();
  private FacetService facetService = new FacetService();

  public FacetsBean() {
    try {
      facets = facetService.retrieveAll();
    } catch (ImejiException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  /**
   * @return the facets
   */
  public List<Facet> getFacets() {
    return facets;
  }

  /**
   * @param facets the facets to set
   */
  public void setFacets(List<Facet> facets) {
    this.facets = facets;
  }
}
