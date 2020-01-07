package de.mpg.imeji.presentation.search.facet;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.exceptions.ImejiExceptionWithUserMessage;
import de.mpg.imeji.logic.config.Imeji;
import de.mpg.imeji.logic.search.facet.FacetService;
import de.mpg.imeji.logic.search.facet.model.Facet;
import de.mpg.imeji.presentation.beans.SuperBean;
import de.mpg.imeji.presentation.session.BeanHelper;

@ManagedBean(name = "FacetsBean")
@ViewScoped
public class FacetsBean extends SuperBean {
  private static final long serialVersionUID = -2474393161093378625L;
  private static final Logger LOGGER = LogManager.getLogger(FacetsBean.class);
  private List<Facet> itemFacets = new ArrayList<>();
  private List<Facet> collectionFacets = new ArrayList<>();
  private FacetService facetService = new FacetService();

  public FacetsBean() {

  }

  @PostConstruct
  public void init() {
    try {
      List<Facet> facets = facetService.retrieveAll();

      for (Facet f : facets) {
        if (Facet.OBJECTTYPE_ITEM.equals(f.getObjectType())) {
          itemFacets.add(f);
        } else if (Facet.OBJECTTYPE_COLLECTION.equals(f.getObjectType())) {
          collectionFacets.add(f);
        }
      }
      setPosition();
    } 
    catch (final ImejiExceptionWithUserMessage exceptionWithMessage) {
        String userMessage = Imeji.RESOURCE_BUNDLE.getMessage(exceptionWithMessage.getMessageLabel(), getLocale());
        BeanHelper.error(userMessage);
        if (exceptionWithMessage.getMessage() != null) {
          LOGGER.error(exceptionWithMessage.getMessage(), exceptionWithMessage);
        } else {
          LOGGER.error(userMessage, exceptionWithMessage);
        }
      }
    catch (ImejiException e) {
      BeanHelper.error("Error retrieving facets: " + e.getMessage());
      LOGGER.error("Error retrieving facets ", e);
    }
  }

  /**
   * Set the position of the item of the list
   */
  private void setPosition() {
    int i = 0;
    for (Facet f : itemFacets) {
      f.setPosition(i);
      i++;
    }
    i = 0;
    for (Facet f : collectionFacets) {
      f.setPosition(i);
      i++;
    }

  }

  public void delete() {
    final String index = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap().get("index");
    try {
      Facet f = facetService.retrieveByIndexFromCache(index);
      facetService.delete(f, getSessionUser());
      BeanHelper.info("Facet " + f.getName() + " successfully deleted");
      reload();
    } 
    catch (final ImejiExceptionWithUserMessage exceptionWithMessage) {
        String userMessage = Imeji.RESOURCE_BUNDLE.getMessage(exceptionWithMessage.getMessageLabel(), getLocale());
        BeanHelper.error(userMessage);
        if (exceptionWithMessage.getMessage() != null) {
          LOGGER.error(exceptionWithMessage.getMessage(), exceptionWithMessage);
        } else {
          LOGGER.error(userMessage, exceptionWithMessage);
        }
      }
    catch (ImejiException | IOException e) {
      BeanHelper.error("Error deleting facet: " + e.getMessage());
      LOGGER.error("Error deleting facet: ", e);
    }
  }

  /**
   * Move the facet in the position to the top. i.e., get a lower position to appear one position
   * higher in the list
   * 
   * @param facet
   * @throws ImejiException
   */
  public void moveUp(Facet facet) throws ImejiException {

    List<Facet> facetCollection = Facet.OBJECTTYPE_ITEM.equals(facet.getObjectType()) ? itemFacets : collectionFacets;
    Collections.swap(facetCollection, facet.getPosition(), facet.getPosition() - 1);
    setPosition();
    facetService.update(facetCollection, getSessionUser());
  }

  /**
   * Move the facet to the bottom of the list, i.e. get a higher position to appear one position
   * lower in the list
   * 
   * @param facet
   * @throws ImejiException
   */
  public void moveDown(Facet facet) throws ImejiException {
    List<Facet> facetCollection = Facet.OBJECTTYPE_ITEM.equals(facet.getObjectType()) ? itemFacets : collectionFacets;
    Collections.swap(facetCollection, facet.getPosition(), facet.getPosition() + 1);
    setPosition();
    facetService.update(facetCollection, getSessionUser());
  }

  public List<Facet> getItemFacets() {
    return itemFacets;
  }

  public void setItemFacets(List<Facet> itemFacets) {
    this.itemFacets = itemFacets;
  }

  public List<Facet> getCollectionFacets() {
    return collectionFacets;
  }

  public void setCollectionFacets(List<Facet> collectionFacets) {
    this.collectionFacets = collectionFacets;
  }


}
