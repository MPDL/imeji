package de.mpg.imeji.presentation.collection;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.ViewScoped;

import org.apache.log4j.Logger;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.exceptions.UnprocessableError;
import de.mpg.imeji.logic.config.Imeji;
import de.mpg.imeji.logic.core.collection.CollectionService;
import de.mpg.imeji.logic.model.CollectionImeji;
import de.mpg.imeji.logic.model.Organization;
import de.mpg.imeji.logic.model.Person;
import de.mpg.imeji.logic.model.User;
import de.mpg.imeji.logic.security.user.UserService;
import de.mpg.imeji.logic.util.ObjectHelper;
import de.mpg.imeji.logic.util.UrlHelper;
import de.mpg.imeji.presentation.beans.ContainerEditorSession;
import de.mpg.imeji.presentation.session.BeanHelper;

@ManagedBean(name = "EditCollectionBean")
@ViewScoped
public class EditCollectionBean extends CollectionBean {
  private static final long serialVersionUID = 568267990816647451L;
  private static final Logger LOGGER = Logger.getLogger(EditCollectionBean.class);
  @ManagedProperty(value = "#{ContainerEditorSession}")
  private ContainerEditorSession containerEditorSession;

  @PostConstruct
  public void init() {
    setId(UrlHelper.getParameterValue("collectionId"));
    if (getId() != null) {
      try {
        setCollection(new CollectionService()
            .retrieve(ObjectHelper.getURI(CollectionImeji.class, getId()), getSessionUser()));
        setSendEmailNotification(getSessionUser().getObservedCollections().contains(getId()));
        final LinkedList<Person> persons = new LinkedList<Person>();
        if (getCollection().getPersons().size() == 0) {
          getCollection().getPersons().add(new Person());
        }
        for (final Person p : getCollection().getPersons()) {
          final LinkedList<Organization> orgs = new LinkedList<Organization>();
          for (final Organization o : p.getOrganizations()) {
            orgs.add(o);
          }
          p.setOrganizations(orgs);
          persons.add(p);
        }
        getCollection().setPersons(persons);
      } catch (final ImejiException e) {
        BeanHelper.error("Error initiatilzing page: " + e.getMessage());
        LOGGER.error("Error init edit collection page", e);
      }
    } else {
      BeanHelper.error(Imeji.RESOURCE_BUNDLE.getLabel("error", getLocale()) + " : no ID in URL");
    }
    containerEditorSession.setUploadedLogoPath(null);
  }

  public void save() throws Exception {
    if (saveEditedCollection()) {
      redirect(getPreviousPage().getCompleteUrl());
    }
  }

  /**
   * Save Collection
   *
   * @return
   */
  public boolean saveEditedCollection() {
    try {
      final CollectionService collectionController = new CollectionService();
      final User user = getSessionUser();
      collectionController.update(getCollection(), user);
      new UserService().update(user, user);
      if (containerEditorSession.getErrorMessage() != "") {
        String msg = containerEditorSession.getErrorMessage();
        containerEditorSession.setErrorMessage("");
        throw new UnprocessableError(msg);
      }
      if (containerEditorSession.getUploadedLogoPath() != null) {
        collectionController.updateLogo(getCollection(),
            new File(containerEditorSession.getUploadedLogoPath()), getSessionUser());
      }
      BeanHelper.info(Imeji.RESOURCE_BUNDLE.getMessage("success_collection_save", getLocale()));
      return true;
    } catch (final UnprocessableError e) {
      BeanHelper.error(e, getLocale());
      LOGGER.error("Error saving collection", e);
      return false;
    } catch (final IOException e) {
      BeanHelper.error(Imeji.RESOURCE_BUNDLE.getMessage("error_collection_logo_save", getLocale()));
      LOGGER.error("Error saving collection", e);
      return false;
    } catch (final URISyntaxException e) {
      BeanHelper
          .error(Imeji.RESOURCE_BUNDLE.getMessage("error_collection_logo_uri_save", getLocale()));
      LOGGER.error("Error saving collection", e);
      return false;
    } catch (final ImejiException e) {
      BeanHelper.error(Imeji.RESOURCE_BUNDLE.getMessage("error_collection_save", getLocale()));
      LOGGER.error("Error saving collection", e);
      return false;
    }
  }

  /**
   * Return the link for the Cancel button
   *
   * @return
   */
  public String getCancel() {
    return getNavigation().getCollectionUrl() + ObjectHelper.getId(getCollection().getId()) + "/"
        + getNavigation().getInfosPath() + "?init=1";
  }


  protected String getNavigationString() {
    return "pretty:editCollection";
  }



  @Override
  protected List<URI> getSelectedCollections() {
    return new ArrayList<>();
  }

  /**
   * @return the containerEditorSession
   */
  public ContainerEditorSession getContainerEditorSession() {
    return containerEditorSession;
  }

  /**
   * @param containerEditorSession the containerEditorSession to set
   */
  public void setContainerEditorSession(ContainerEditorSession containerEditorSession) {
    this.containerEditorSession = containerEditorSession;
  }
}
