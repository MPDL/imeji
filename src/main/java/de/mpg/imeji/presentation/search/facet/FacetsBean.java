package de.mpg.imeji.presentation.search.facet;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;

import org.apache.log4j.Logger;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.search.facet.FacetService;
import de.mpg.imeji.logic.search.facet.model.Facet;
import de.mpg.imeji.presentation.beans.SuperBean;
import de.mpg.imeji.presentation.session.BeanHelper;

@ManagedBean(name = "FacetsBean")
@ViewScoped
public class FacetsBean extends SuperBean {
  private static final long serialVersionUID = -2474393161093378625L;
  private static final Logger LOGGER = Logger.getLogger(FacetsBean.class);
  private List<Facet> facets = new ArrayList<>();
  private FacetService facetService = new FacetService();

  public FacetsBean() {

  }

  @PostConstruct
  public void init() {
    try {
      facets = facetService.retrieveAll();
    } catch (ImejiException e) {
      BeanHelper.error("Error retrieving facets: " + e.getMessage());
      LOGGER.error("Error retrieving facets ", e);
    }
  }

  public void delete() {
    final String index = FacesContext.getCurrentInstance().getExternalContext()
        .getRequestParameterMap().get("index");
    try {
      Facet f = facetService.retrieveByIndexFromCache(index);
      facetService.delete(f, getSessionUser());
      BeanHelper.info("Facet " + f.getName() + " successfully deleted");
      facets = facetService.retrieveAll();
    } catch (ImejiException e) {
      BeanHelper.error("Error deleting facet: " + e.getMessage());
      LOGGER.error("Error deleting facet: ", e);
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
