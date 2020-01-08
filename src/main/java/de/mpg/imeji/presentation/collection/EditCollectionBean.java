package de.mpg.imeji.presentation.collection;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.ViewScoped;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.exceptions.ImejiExceptionWithUserMessage;
import de.mpg.imeji.exceptions.ReloadBeforeSaveException;
import de.mpg.imeji.exceptions.UnprocessableError;
import de.mpg.imeji.logic.config.Imeji;
import de.mpg.imeji.logic.core.collection.CollectionService;
import de.mpg.imeji.logic.model.CollectionImeji;
import de.mpg.imeji.logic.model.ContainerAdditionalInfo;
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
  private static final Logger LOGGER = LogManager.getLogger(EditCollectionBean.class);
  @ManagedProperty(value = "#{ContainerEditorSession}")
  private ContainerEditorSession containerEditorSession;

  @PostConstruct
  public void init() {
    setId(UrlHelper.getParameterValue("collectionId"));
    if (getId() != null) {
      try {
        setCollection(new CollectionService().retrieve(ObjectHelper.getURI(CollectionImeji.class, getId()), getSessionUser()));
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


        //Sort additional Info: preselected metadata first        
        List<String> preselectedMetadata = Imeji.CONFIG.getCollectionMetadataSuggestionsPreselectAsList();
        if (preselectedMetadata != null && !preselectedMetadata.isEmpty()) {

          List<ContainerAdditionalInfo> additionalList = new ArrayList<ContainerAdditionalInfo>();
          List<String> currentLabels =
              getCollection().getAdditionalInformations().stream().map(i -> i.getLabel()).collect(Collectors.toList());

          for (String mdLabel : preselectedMetadata) {
            if (currentLabels.contains(mdLabel)) {
              additionalList.add(getCollection().getAdditionalInformations().remove(currentLabels.indexOf(mdLabel)));
              currentLabels.remove(mdLabel);
            } else {
              additionalList.add(new ContainerAdditionalInfo(mdLabel, "", ""));

            }
          }
          additionalList.addAll(getCollection().getAdditionalInformations());
          getCollection().setAdditionalInformations(additionalList);

        }
        //Always add empty additional info at the end
        getCollection().getAdditionalInformations().add(new ContainerAdditionalInfo());
        // init linked collections
        this.initLinkedCollections();

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
      final CollectionService collectionService = new CollectionService();
      final User user = getSessionUser();
      this.saveLinkedCollections();
      // in case a logo file was added or changed, save collection and logo
      if (containerEditorSession.getUploadedLogoPath() != null) {
        collectionService.updateLogo(getCollection(), new File(containerEditorSession.getUploadedLogoPath()), getSessionUser());
      }
      // save collection
      else {
        collectionService.update(getCollection(), user);
      }
      // new UserService().update(user, user);
      if (containerEditorSession.getErrorMessage() != "") {
        String msg = containerEditorSession.getErrorMessage();
        containerEditorSession.setErrorMessage("");
        throw new UnprocessableError(msg);
      }
      BeanHelper.info(Imeji.RESOURCE_BUNDLE.getMessage("success_collection_save", getLocale()));
      return true;
    } 
    catch (final ImejiExceptionWithUserMessage exceptionWithMessage) {
        String userMessage = Imeji.RESOURCE_BUNDLE.getMessage("error_collection_save", getLocale()) + " " +
        		exceptionWithMessage.getUserMessage(getLocale());
        BeanHelper.error(userMessage);
        if (exceptionWithMessage.getMessage() != null) {
          LOGGER.error("Error saving collection: " + exceptionWithMessage.getMessage(), exceptionWithMessage);
        } else {
          LOGGER.error(userMessage, exceptionWithMessage);
        }
      return false;
      }
    catch (final UnprocessableError e) {
      BeanHelper.error(e, getLocale());
      LOGGER.error("Error saving collection", e);
      return false;
    } catch (final IOException e) {
      BeanHelper.error(Imeji.RESOURCE_BUNDLE.getMessage("error_collection_logo_save", getLocale()));
      LOGGER.error("Error saving collection", e);
      return false;
    } catch (final URISyntaxException e) {
      BeanHelper.error(Imeji.RESOURCE_BUNDLE.getMessage("error_collection_logo_uri_save", getLocale()));
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
    return getNavigation().getCollectionUrl() + ObjectHelper.getId(getCollection().getId()) + "/" + getNavigation().getInfosPath()
        + "?init=1";
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
