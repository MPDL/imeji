package de.mpg.imeji.presentation.search.facet;

import java.io.IOException;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import org.apache.log4j.Logger;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.exceptions.NotFoundException;
import de.mpg.imeji.logic.search.facet.FacetService;
import de.mpg.imeji.logic.search.facet.model.Facet;
import de.mpg.imeji.logic.util.UrlHelper;
import de.mpg.imeji.presentation.session.BeanHelper;

@ManagedBean(name = "EditFacetBean")
@ViewScoped
public class EditFacetBean extends CreateFacetBean {
  private static final long serialVersionUID = 740835048734163748L;
  private static final Logger LOGGER = Logger.getLogger(EditFacetBean.class);
  private Facet facet;

  @PostConstruct
  public void init() {
    String facetId = null;
    try {
      facetId = UrlHelper.getParameterValue("facetId");
      this.facet = new FacetService().read(facetId);
      setName(facet.getName());
      setType(facet.getType());
      setIndex(facet.getIndex());
    } catch (NotFoundException e) {
      LOGGER.error("Error initializing FacetBean", e);
      BeanHelper.error("Unknown facet: " + facetId);
    } catch (Exception e) {
      LOGGER.error("Error initializing FacetBean", e);
    }
  }

  @Override
  public void save() {
    facet.setIndex(getIndex());
    facet.setName(getName());
    facet.setType(getType());
    try {
      new FacetService().update(facet, getSessionUser());
      redirect(getNavigation().getApplicationUrl() + "facets");
    } catch (ImejiException | IOException e) {
      LOGGER.error("Error updateing Facet ", e);
      BeanHelper.error("Error saving Facet", e.getMessage());
    }
  }

}
