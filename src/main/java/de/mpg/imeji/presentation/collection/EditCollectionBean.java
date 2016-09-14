/**
 * License: src/main/resources/license/escidoc.license
 */
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
import javax.faces.context.FacesContext;

import org.apache.log4j.Logger;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.exceptions.UnprocessableError;
import de.mpg.imeji.logic.Imeji;
import de.mpg.imeji.logic.controller.resource.CollectionController;
import de.mpg.imeji.logic.controller.resource.ProfileController;
import de.mpg.imeji.logic.user.controller.UserBusinessController;
import de.mpg.imeji.logic.util.ObjectHelper;
import de.mpg.imeji.logic.util.UrlHelper;
import de.mpg.imeji.logic.vo.CollectionImeji;
import de.mpg.imeji.logic.vo.MetadataProfile;
import de.mpg.imeji.logic.vo.Organization;
import de.mpg.imeji.logic.vo.Person;
import de.mpg.imeji.logic.vo.User;
import de.mpg.imeji.presentation.beans.ContainerEditorSession;
import de.mpg.imeji.presentation.history.HistorySession;
import de.mpg.imeji.presentation.session.BeanHelper;
import de.mpg.imeji.presentation.session.SessionBean;

@ManagedBean(name = "EditCollectionBean")
@ViewScoped
public class EditCollectionBean extends CollectionBean {
  private static final long serialVersionUID = 568267990816647451L;
  private static final Logger LOGGER = Logger.getLogger(EditCollectionBean.class);
  @ManagedProperty(value = "#{ContainerEditorSession}")
  private ContainerEditorSession containerEditorSession;

  @PostConstruct
  public void init() {
    setId(UrlHelper.getParameterValue("id"));
    setTab(TabType.COLLECTION);
    setCollectionCreateMode(false);
    getProfileSelect();
    if (getId() != null) {
      try {
        setProfile(
            new ProfileController().retrieve(getCollection().getProfile(), getSessionUser()));
        setCollection(new CollectionController()
            .retrieveLazy(ObjectHelper.getURI(CollectionImeji.class, getId()), getSessionUser()));
        setSendEmailNotification(getSessionUser().getObservedCollections().contains(getId()));
        LinkedList<Person> persons = new LinkedList<Person>();
        if (getCollection().getMetadata().getPersons().size() == 0) {
          getCollection().getMetadata().getPersons().add(new Person());
        }
        for (Person p : getCollection().getMetadata().getPersons()) {
          LinkedList<Organization> orgs = new LinkedList<Organization>();
          for (Organization o : p.getOrganizations()) {
            orgs.add(o);
          }
          p.setOrganizations(orgs);
          persons.add(p);
        }
        getCollection().getMetadata().setPersons(persons);
      } catch (ImejiException e) {
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
      FacesContext.getCurrentInstance().getExternalContext()
          .redirect(((HistorySession) BeanHelper.getSessionBean(HistorySession.class))
              .getPreviousPage().getCompleteUrl());
    }
  }

  /**
   * Save Collection
   *
   * @return
   */
  public boolean saveEditedCollection() {
    try {
      CollectionController collectionController = new CollectionController();
      User user = getSessionUser();
      collectionController.update(getCollection(), user);
      UserBusinessController uc = new UserBusinessController();
      uc.update(user, user);
      if (containerEditorSession.getUploadedLogoPath() != null) {
        collectionController.updateLogo(getCollection(),
            new File(containerEditorSession.getUploadedLogoPath()), getSessionUser());
      }
      BeanHelper.info(Imeji.RESOURCE_BUNDLE.getMessage("success_collection_save", getLocale()));
      return true;
    } catch (UnprocessableError e) {
      BeanHelper.error(e, getLocale());
      LOGGER.error("Error saving collection", e);
      return false;
    } catch (IOException e) {
      BeanHelper.error(Imeji.RESOURCE_BUNDLE.getMessage("error_collection_logo_save", getLocale()));
      LOGGER.error("Error saving collection", e);
      return false;
    } catch (URISyntaxException e) {
      BeanHelper
          .error(Imeji.RESOURCE_BUNDLE.getMessage("error_collection_logo_uri_save", getLocale()));
      LOGGER.error("Error saving collection", e);
      return false;
    } catch (ImejiException e) {
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

  @Override
  protected String getNavigationString() {
    return SessionBean.getPrettySpacePage("pretty:editCollection", getSpace());
  }

  /**
   * Method called on the html page to trigger the initialization of the bean
   *
   * @return
   */
  public String getProfileSelect() {
    if (UrlHelper.getParameterBoolean("profileSelect")) {
      setProfileSelectMode(true);
    } else {
      setProfileSelectMode(false);
    }
    return "";
  }

  /**
   * Method for save&editProfile button. Create the {@link MetadataProfile} according to the form
   *
   * @return
   * @throws Exception
   */
  public String saveAndEditProfile() throws Exception {
    if (saveEditedCollection()) {
      FacesContext.getCurrentInstance().getExternalContext()
          .redirect(getNavigation().getProfileUrl() + getProfileId() + "/edit?init=1&col="
              + getCollection().getIdString());
    }
    return "";
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
