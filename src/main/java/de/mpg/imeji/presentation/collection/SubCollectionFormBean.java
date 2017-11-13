package de.mpg.imeji.presentation.collection;

import java.io.IOException;
import java.io.Serializable;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import org.apache.log4j.Logger;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.core.collection.CollectionService;
import de.mpg.imeji.logic.model.CollectionImeji;
import de.mpg.imeji.logic.model.factory.ImejiFactory;
import de.mpg.imeji.presentation.beans.SuperBean;
import de.mpg.imeji.presentation.session.BeanHelper;

/**
 * JSF Bean for the subcollection form
 * 
 * @author saquet
 *
 */
@ManagedBean(name = "SubCollectionFormBean")
@ViewScoped
public class SubCollectionFormBean extends SuperBean implements Serializable {
  private static final long serialVersionUID = 3542202393184509349L;
  private static final Logger LOGGER = Logger.getLogger(SubCollectionFormBean.class);
  private final CollectionService collectionService = new CollectionService();
  private String name;

  /**
   * Create a subcollection with the name set in the bean into the parent collection
   * 
   * @param parent
   * @throws IOException
   */
  public void create(CollectionImeji parent) throws IOException {
    try {
      collectionService.create(ImejiFactory.newCollection().setTitle(name)
          .setPerson(getSessionUser().getPerson()).setCollection(parent.getId().toString()).build(),
          getSessionUser());
      BeanHelper.info("Subcollection created");
      reload();
    } catch (ImejiException e) {
      BeanHelper.error("Error creating Subcollection: " + e.getMessage());
      LOGGER.error("Error creating Subcollection", e);
    }
  }

  /**
   * Create a Subcollection and then redirect to the upload link
   * 
   * @param parent
   * @throws IOException
   */
  public void createAndUpload(CollectionImeji parent) throws IOException {
    try {
      CollectionImeji subcollection = collectionService.create(
          ImejiFactory.newCollection().setTitle(name).setPerson(getSessionUser().getPerson())
              .setCollection(parent.getId().toString()).build(),
          getSessionUser());
      BeanHelper.info("Subcollection created");
      redirect(getNavigation().getCollectionUrl() + subcollection.getIdString() + "?showUpload=1");
    } catch (ImejiException e) {
      BeanHelper.error("Error creating Subcollection: " + e.getMessage());
      LOGGER.error("Error creating Subcollection", e);
    }
  }

  /**
   * Change the name of the subcollection
   * 
   * @param subCollection
   */
  public void edit(CollectionImeji subCollection) {
    try {
      subCollection.setTitle(name);
      collectionService.update(subCollection, getSessionUser());
      BeanHelper.info("Subcollection name changed");
    } catch (ImejiException e) {
      BeanHelper.info("Error editing Subcollection: " + e.getMessage());
      LOGGER.error("Error editing Subcollection", e);
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
}
