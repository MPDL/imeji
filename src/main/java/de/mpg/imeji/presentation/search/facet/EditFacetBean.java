package de.mpg.imeji.presentation.search.facet;

import java.io.IOException;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.exceptions.ImejiExceptionWithUserMessage;
import de.mpg.imeji.exceptions.NotFoundException;
import de.mpg.imeji.logic.config.Imeji;
import de.mpg.imeji.logic.search.facet.FacetService;
import de.mpg.imeji.logic.search.facet.model.Facet;
import de.mpg.imeji.logic.util.UrlHelper;
import de.mpg.imeji.presentation.session.BeanHelper;

@ManagedBean(name = "EditFacetBean")
@ViewScoped
public class EditFacetBean extends CreateFacetBean {
  private static final long serialVersionUID = 740835048734163748L;
  private static final Logger LOGGER = LogManager.getLogger(EditFacetBean.class);
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
      setObjectType(facet.getObjectType());
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
    catch (NotFoundException e) {
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
    facet.setObjectType(getObjecttype());
    try {
      new FacetService().update(facet, getSessionUser());
      redirect(getNavigation().getApplicationUrl() + "facets");
    } 
    catch (final ImejiExceptionWithUserMessage exceptionWithMessage) {
        String userMessage = "Error saving facet: " + Imeji.RESOURCE_BUNDLE.getMessage(exceptionWithMessage.getMessageLabel(), getLocale());
        BeanHelper.error(userMessage);
        if (exceptionWithMessage.getMessage() != null) {
          LOGGER.error("Error saving facet: " + exceptionWithMessage.getMessage(), exceptionWithMessage);
        } else {
          LOGGER.error(userMessage, exceptionWithMessage);
        }
      }
    catch (ImejiException | IOException e) {
      LOGGER.error("Error updating Facet ", e);
      BeanHelper.error("Error saving Facet", e.getMessage());
    }
  }

}
