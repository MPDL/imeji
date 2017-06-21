package de.mpg.imeji.presentation.search.facet;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import org.apache.log4j.Logger;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.search.facet.FacetService;
import de.mpg.imeji.logic.search.facet.model.Facet;
import de.mpg.imeji.presentation.beans.SuperBean;
import de.mpg.imeji.presentation.session.BeanHelper;

@ManagedBean(name = "CreateFacetBean")
@ViewScoped
public class CreateFacetBean extends SuperBean {
  private static final long serialVersionUID = 4885254101366390248L;
  private String name;
  private String index;
  private String type;
  private static final Logger LOGGER = Logger.getLogger(CreateFacetBean.class);


  /**
   * Create the facet
   */
  public void save() {
    Facet facet = new Facet();
    facet.setIndex(index);
    facet.setName(name);
    facet.setType(type);
    FacetService service = new FacetService();
    try {
      service.create(facet, getSessionUser());
    } catch (ImejiException e) {
      LOGGER.error("Error creating facet", e);
      BeanHelper.error("Error creating facet: " + e.getMessage());
    }
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
   * @return the type
   */
  public String getType() {
    return type;
  }

  /**
   * @param type the type to set
   */
  public void setType(String type) {
    this.type = type;
  }



}
