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

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.exceptions.UnprocessableError;
import de.mpg.imeji.logic.config.Imeji;
import de.mpg.imeji.logic.core.collection.CollectionService;
import de.mpg.imeji.logic.model.CollectionImeji;
import de.mpg.imeji.logic.model.ContainerAdditionalInfo;
import de.mpg.imeji.logic.model.Organization;
import de.mpg.imeji.logic.model.Person;
import de.mpg.imeji.logic.model.factory.ImejiFactory;
import de.mpg.imeji.logic.security.user.UserService;
import de.mpg.imeji.logic.util.UrlHelper;
import de.mpg.imeji.presentation.beans.ContainerEditorSession;
import de.mpg.imeji.presentation.session.BeanHelper;

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
  private static final Logger LOGGER = LogManager.getLogger(CreateCollectionBean.class);
  private static final long serialVersionUID = 1257698224590957642L;
  @ManagedProperty(value = "#{ContainerEditorSession}")
  private ContainerEditorSession containerEditorSession;
  private boolean showUpload = false;

  /**
   * Method called when paged is loaded
   */
  @PostConstruct
  public void init() {
    showUpload = UrlHelper.getParameterBoolean("showUpload");
    setCollection(ImejiFactory.newCollection().setPerson(getSessionUser().getPerson().clone()).build());

    List<String> preselectedMetadataLabels = Imeji.CONFIG.getCollectionMetadataSuggestionsPreselectAsList();

    if (preselectedMetadataLabels != null && !preselectedMetadataLabels.isEmpty()) {
      for (String s : preselectedMetadataLabels) {
        getCollection().getAdditionalInformations().add(new ContainerAdditionalInfo(s, "", null));
      }
    }
    //Add one more
    getCollection().getAdditionalInformations().add(new ContainerAdditionalInfo());
    containerEditorSession.setUploadedLogoPath(null);
  }

  /**
   * Method for save button. Create the {@link CollectionImeji} according to the form
   *
   * @return
   * @throws Exception
   */
  public void save() {
    if (createCollection()) {
      try {
        redirect(getNavigation().getCollectionUrl() + getCollection().getIdString() + (showUpload ? "?showUpload=1" : ""));
      } catch (final IOException e) {
        LOGGER.error("Error redirecting after saving collection", e);
      }
    }
  }

  /**
   * Create the collection and its profile
   *
   * @return
   * @throws Exception
   */
  public boolean createCollection() {
    try {
      final CollectionService collectionController = new CollectionService();
      int pos = 0;
      // Set the position of the persons and organizations (used for the sorting
      // later)
      for (final Person p : getCollection().getPersons()) {
        p.setPos(pos);
        pos++;
        int pos2 = 0;
        for (final Organization o : p.getOrganizations()) {
          o.setPos(pos2);
          pos2++;
        }
      }
      // add additional information (i.e. created, modified) and write to database
      CollectionImeji createdCollection = collectionController.create(getCollection(), getSessionUser());
      setCollection(createdCollection);
      setId(getCollection().getIdString());
      if (containerEditorSession.getUploadedLogoPath() != null) {
        // reload session user  - who has new grants now (grants for the created collection)
        UserService userService = new UserService();
        this.setSessionUser(userService.retrieve(getSessionUser().getId(), getSessionUser()));
        collectionController.updateLogo(getCollection(), new File(containerEditorSession.getUploadedLogoPath()), getSessionUser());
      }
      BeanHelper.info(Imeji.RESOURCE_BUNDLE.getMessage("success_collection_create", getLocale()));
      return true;
    } catch (final UnprocessableError e) {
      BeanHelper.error(e, getLocale());
      LOGGER.error("Error create collection", e);
    } catch (final ImejiException e) {
      BeanHelper.error(Imeji.RESOURCE_BUNDLE.getMessage(e.getLocalizedMessage(), getLocale()));
      LOGGER.error("Error create collection", e);
    } catch (final Exception e) {
      LOGGER.error("Error create collection", e);
      BeanHelper.error(Imeji.RESOURCE_BUNDLE.getMessage("error_collection_create", getLocale()) + ": " + e.getMessage());
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

  protected String getNavigationString() {
    return "pretty:createCollection";
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
