/**
 * License: src/main/resources/license/escidoc.license
 */
package de.mpg.imeji.presentation.collection;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.ViewScoped;
import javax.faces.model.SelectItem;

import org.apache.log4j.Logger;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.exceptions.UnprocessableError;
import de.mpg.imeji.logic.Imeji;
import de.mpg.imeji.logic.controller.resource.CollectionController;
import de.mpg.imeji.logic.user.controller.UserBusinessController;
import de.mpg.imeji.logic.vo.CollectionImeji;
import de.mpg.imeji.logic.vo.Organization;
import de.mpg.imeji.logic.vo.Person;
import de.mpg.imeji.logic.vo.util.ImejiFactory;
import de.mpg.imeji.presentation.beans.ContainerEditorSession;
import de.mpg.imeji.presentation.mdProfile.ProfileSelector;
import de.mpg.imeji.presentation.session.BeanHelper;
import de.mpg.imeji.presentation.session.SessionBean;
import de.mpg.imeji.presentation.util.VocabularyHelper;

/**
 * Java Bean for the create Collection Page
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
@ManagedBean(name = "CreateCollectionBean")
@ViewScoped
public class CreateCollectionBean extends CollectionBean {
  private static final Logger LOGGER = Logger.getLogger(CreateCollectionBean.class);
  private static final long serialVersionUID = 1257698224590957642L;
  private VocabularyHelper vocabularyHelper;
  private ProfileSelector profileSelector;
  private boolean createProfile = false;
  @ManagedProperty(value = "#{ContainerEditorSession}")
  private ContainerEditorSession containerEditorSession;

  /**
   * Method called when paged is loaded
   */
  @PostConstruct
  public void init() {
    profileSelector = new ProfileSelector(null, getSessionUser(), getSelectedSpaceString(), getLocale());
    vocabularyHelper = new VocabularyHelper(getLocale());
    setCollectionCreateMode(true);
    setCollection(ImejiFactory.newCollection());
    ((List<Person>) getCollection().getMetadata().getPersons()).set(0,
        getSessionUser().getPerson().clone());
    containerEditorSession.setUploadedLogoPath(null);
  }

  /**
   * Method for save button. Create the {@link CollectionImeji} according to the form
   *
   * @return
   * @throws Exception
   */
  public String save() {
    if (createCollection()) {
      try {
        redirect(getNavigation().getCollectionUrl() + getCollection().getIdString());
      } catch (IOException e) {
        LOGGER.error("Error redirecting after saving collection", e);
      }
    }
    return "";
  }

  /**
   * Method for save&editProfile button. Create the {@link CollectionImeji} according to the form
   *
   * @return
   * @throws Exception
   */
  public String saveAndEditProfile() {
    if (createCollection()) {
      try {
        redirect(getNavigation().getProfileUrl() + extractIDFromURI(getCollection().getProfile())
            + "/edit?init=1&col=" + getCollection().getIdString());
      } catch (IOException e) {
        LOGGER.error("Error redirect after create collection", e);
      }
    }
    return "";
  }

  /**
   * Create the collection and its profile
   *
   * @return
   * @throws Exception
   */
  public boolean createCollection() {
    try {
      CollectionController collectionController = new CollectionController();
      int pos = 0;
      // Set the position of the persons and organizations (used for the sorting later)
      for (Person p : getCollection().getMetadata().getPersons()) {
        p.setPos(pos);
        pos++;
        int pos2 = 0;
        for (Organization o : p.getOrganizations()) {
          o.setPos(pos2);
          pos2++;
        }
      }
      if (!createProfile) {
        profileSelector.setProfile(null);
      }
      setCollection(collectionController.create(getCollection(), profileSelector.getProfile(),
          getSessionUser(), profileSelector.getSelectorMode(), getSelectedSpaceString()));
      if (containerEditorSession.getUploadedLogoPath() != null) {
        collectionController.updateLogo(getCollection(),
            new File(containerEditorSession.getUploadedLogoPath()), getSessionUser());
      }
      setSendEmailNotification(isSendEmailNotification());
      UserBusinessController uc = new UserBusinessController();
      uc.update(getSessionUser(), getSessionUser());
      BeanHelper.info(Imeji.RESOURCE_BUNDLE.getMessage("success_collection_create", getLocale()));
      return true;
    } catch (UnprocessableError e) {
      BeanHelper.error(e, getLocale());
      LOGGER.error("Error create collection", e);
    } catch (ImejiException e) {
      BeanHelper.cleanMessages();
      BeanHelper.error(Imeji.RESOURCE_BUNDLE.getMessage(e.getLocalizedMessage(), getLocale()));
      LOGGER.error("Error create collection", e);
    } catch (Exception e) {
      LOGGER.error("Error create collection", e);
    }
    return false;
  }

  public static String extractIDFromURI(URI uri) {
    return uri.getPath().substring(uri.getPath().lastIndexOf("/") + 1);
  }

  /**
   * Return the link for the Cancel button
   *
   * @return
   */
  public String getCancel() {
    return getNavigation().getCollectionsUrl() + "?q=";
  }

  @Override
  protected String getNavigationString() {
    return SessionBean.getPrettySpacePage("pretty:createCollection", getSelectedSpaceString());
  }

  public String getVocabularyLabel(URI id) {
    for (SelectItem item : vocabularyHelper.getVocabularies()) {
      if (id.toString().equals(item.getValue().toString())) {
        return item.getLabel();
      }
    }
    return "";
  }

  public ProfileSelector getProfileSelector() {
    return profileSelector;
  }

  public void setProfileSelector(ProfileSelector profileSelector) {
    this.profileSelector = profileSelector;
  }

  public boolean isCreateProfile() {
    return createProfile;
  }

  public void setCreateProfile(boolean createProfile) {
    this.createProfile = createProfile;
  }

  public ContainerEditorSession getContainerEditorEditorSession() {
    return containerEditorSession;
  }

  public void setContainerEditorSession(ContainerEditorSession collectionEditorSession) {
    this.containerEditorSession = collectionEditorSession;
  }

  @Override
  protected List<URI> getSelectedCollections() {
    return new ArrayList<>();
  }

}
