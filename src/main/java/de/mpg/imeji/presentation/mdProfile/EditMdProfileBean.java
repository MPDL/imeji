package de.mpg.imeji.presentation.mdProfile;

import java.io.IOException;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;
import javax.faces.event.ValueChangeEvent;

import org.apache.log4j.Logger;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.exceptions.UnprocessableError;
import de.mpg.imeji.logic.Imeji;
import de.mpg.imeji.logic.controller.resource.CollectionController;
import de.mpg.imeji.logic.controller.resource.ProfileController;
import de.mpg.imeji.logic.util.ObjectHelper;
import de.mpg.imeji.logic.util.UrlHelper;
import de.mpg.imeji.logic.vo.CollectionImeji;
import de.mpg.imeji.logic.vo.MetadataProfile;
import de.mpg.imeji.logic.vo.Statement;
import de.mpg.imeji.logic.vo.util.ImejiFactory;
import de.mpg.imeji.presentation.session.BeanHelper;
import de.mpg.imeji.presentation.session.SessionBean;
import de.mpg.imeji.presentation.util.VocabularyHelper;

/**
 * Java Bean for the edit metadata Profile page
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
@ManagedBean(name = "EditMdProfileBean")
@ViewScoped
public class EditMdProfileBean extends MdProfileBean {
  private static final long serialVersionUID = 7411697130976477046L;
  private String colId = null;
  private static final Logger LOGGER = Logger.getLogger(EditMdProfileBean.class);
  private VocabularyHelper vocabularyHelper;
  private CollectionImeji collection;

  @Override
  public void specificSetup() {
    colId = UrlHelper.getParameterValue("col");
    vocabularyHelper = new VocabularyHelper(getLocale());
    if (getId() != null) {
      retrieveProfile();
      try {
        if (colId != null) {
          // load the collection if provided in the url
          CollectionController cc = new CollectionController();
          setCollection(
              cc.retrieve(ObjectHelper.getURI(CollectionImeji.class, colId), getSessionUser()));
        }
      } catch (ImejiException e) {
        BeanHelper.error(e.getMessage());
        LOGGER.error("Error initialising edit profile page", e);
      }
    }
    super.specificSetup();
  }

  public void test() {
    System.out.println("test");
  }

  /**
   * @throws ImejiException
   *
   */
  public void changeProfile() {
    setProfile(null);
  }

  /**
   * Start a new emtpy profile
   *
   * @throws ImejiException
   */
  public void startNewProfile() throws ImejiException {
    ProfileController profileController = new ProfileController();
    MetadataProfile profile = ImejiFactory.newProfile();
    profile.setTitle("Profile for " + getCollection().getMetadata().getTitle());
    profile = profileController.create(profile, getSessionUser());
    setProfile(profile);
    initStatementWrappers(getProfile());
    if (getProfile().getStatements().isEmpty()) {
      addFirstStatement();
    }
  }

  /**
   * Method when cancel button is clicked
   *
   * @return
   * @throws IOException
   */
  public String cancel() throws IOException {
    if (colId != null) {
      FacesContext.getCurrentInstance().getExternalContext()
          .redirect(getNavigation().getCollectionUrl() + colId + "/"
              + getNavigation().getInfosPath() + "?init=1");
    } else {
      redirect(getHistory().getPreviousPage().getCompleteUrlWithHistory());
    }
    return "";
  }

  /**
   * Method when save button is clicked
   *
   * @return
   * @throws IOException
   */
  public String save() throws IOException {
    getProfile().setStatements(getUnwrappedStatements());
    int pos = 0;
    // Set the position of the statement (used for the sorting later)
    for (Statement st : getProfile().getStatements()) {
      st.setPos(pos);
      pos++;
    }

    try {
      ProfileController profileController = new ProfileController();
      profileController.update(getProfile(), getSessionUser());
      // session.getProfileCached().clear();
      BeanHelper.info(Imeji.RESOURCE_BUNDLE.getMessage("success_profile_save", getLocale()));
      cancel();
    } catch (UnprocessableError e) {
      BeanHelper.error(e.getMessage());
    } catch (Exception e) {
      BeanHelper.error(Imeji.RESOURCE_BUNDLE.getMessage("error_profile_save", getLocale()),
          e.getMessage());
      LOGGER.error("Error saving profile", e);
    }
    return "";
  }

  /**
   * Listener for the title input
   *
   * @param event
   */
  public void titleListener(ValueChangeEvent event) {
    if (event.getNewValue() != null && event.getNewValue() != event.getOldValue()) {
      this.getProfile().setTitle(event.getNewValue().toString());
    }
  }

  /**
   * Listener for the description input
   *
   * @param event
   */
  public void descriptionListener(ValueChangeEvent event) {
    if (event.getNewValue() != null && event.getNewValue() != event.getOldValue()) {
      this.getProfile().setTitle(event.getNewValue().toString());
    }
  }

  @Override
  protected String getNavigationString() {
    return SessionBean.getPrettySpacePage("pretty:editProfile", getSelectedSpaceString());
  }

  /**
   * getter
   *
   * @return
   */
  public String getColId() {
    return colId;
  }

  /**
   * setter
   *
   * @param colId
   */
  public void setColId(String colId) {
    this.colId = colId;
  }

  /**
   * @return the vocabularyHelper
   */
  public VocabularyHelper getVocabularyHelper() {
    return vocabularyHelper;
  }

  /**
   * @param vocabularyHelper the vocabularyHelper to set
   */
  public void setVocabularyHelper(VocabularyHelper vocabularyHelper) {
    this.vocabularyHelper = vocabularyHelper;
  }

  public CollectionImeji getCollection() {
    return collection;
  }

  public void setCollection(CollectionImeji collection) {
    this.collection = collection;
  }
}
