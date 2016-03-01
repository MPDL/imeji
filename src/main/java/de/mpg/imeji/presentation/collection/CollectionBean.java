/**
 * License: src/main/resources/license/escidoc.license
 */
package de.mpg.imeji.presentation.collection;

import static com.google.common.base.Strings.isNullOrEmpty;
import static de.mpg.imeji.logic.notification.CommonMessages.getSuccessCollectionDeleteMessage;

import javax.faces.event.ValueChangeEvent;

import org.apache.log4j.Logger;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.exceptions.NotFoundException;
import de.mpg.imeji.logic.controller.CollectionController;
import de.mpg.imeji.logic.controller.ProfileController;
import de.mpg.imeji.logic.doi.DoiService;
import de.mpg.imeji.logic.util.UrlHelper;
import de.mpg.imeji.logic.vo.CollectionImeji;
import de.mpg.imeji.logic.vo.Container;
import de.mpg.imeji.logic.vo.MetadataProfile;
import de.mpg.imeji.logic.vo.User;
import de.mpg.imeji.presentation.beans.ContainerBean;
import de.mpg.imeji.presentation.beans.Navigation;
import de.mpg.imeji.presentation.session.SessionBean;
import de.mpg.imeji.presentation.util.BeanHelper;

/**
 * Abstract bean for all collection beans
 * 
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public abstract class CollectionBean extends ContainerBean {
  private static final long serialVersionUID = -3071769388574710503L;

  public enum TabType {
    COLLECTION, PROFILE, HOME, UTIL;
  }

  private static final Logger LOGGER = Logger.getLogger(CollectionBean.class);
  private TabType tab = TabType.HOME;

  protected SessionBean sessionBean;

  protected Navigation navigation;
  private CollectionImeji collection;
  private MetadataProfile profile = null;
  private MetadataProfile profileTemplate;

  private String id;
  private String profileId;
  private boolean selected;

  private boolean sendEmailNotification = false;

  private boolean collectionCreateMode = true;

  private boolean profileSelectMode = false;

  /**
   * New default {@link CollectionBean}
   */
  public CollectionBean() {
    collection = new CollectionImeji();
    sessionBean = (SessionBean) BeanHelper.getSessionBean(SessionBean.class);
    navigation = (Navigation) BeanHelper.getApplicationBean(Navigation.class);
  }

  /**
   * Read the profile of the current collection
   * 
   * @param user
   * @throws ImejiException
   */
  protected void initCollectionProfile() throws ImejiException {
    this.profile = new ProfileController().retrieve(collection.getProfile(), sessionBean.getUser());
    this.profileId = profile != null ? profile.getIdString() : null;
  }

  @Override
  protected String getErrorMessageNoAuthor() {
    return "error_collection_need_one_author";
  }

  /**
   * Listener for the discard comment
   * 
   * @param event
   */
  public void discardCommentListener(ValueChangeEvent event) {
    if (event.getNewValue() != null && event.getNewValue().toString().trim().length() > 0) {
      getContainer().setDiscardComment(event.getNewValue().toString().trim());
    }
  }

  /**
   * getter
   * 
   * @return the tab
   */
  public TabType getTab() {
    if (UrlHelper.getParameterValue("tab") != null) {
      tab = TabType.valueOf(UrlHelper.getParameterValue("tab").toUpperCase());
    }
    return tab;
  }

  /**
   * setter
   * 
   * @param the tab to set
   */
  public void setTab(TabType tab) {
    this.tab = tab;
  }

  /**
   * @return the collection
   */
  public CollectionImeji getCollection() {
    return collection;
  }

  /**
   * @param collection the collection to set
   */
  public void setCollection(CollectionImeji collection) {
    this.collection = collection;
  }

  /**
   * @return the id
   */
  public String getId() {
    return id;
  }

  /**
   * @param id the id to set
   */
  public void setId(String id) {
    this.id = id;
  }

  /**
   * @return the selected
   */
  public boolean getSelected() {
    if (sessionBean.getSelectedCollections().contains(collection.getId())) {
      selected = true;
    } else {
      selected = false;
    }
    return selected;
  }

  /**
   * @param selected the selected to set
   */
  public void setSelected(boolean selected) {
    if (selected) {
      if (!(sessionBean.getSelectedCollections().contains(collection.getId()))) {
        sessionBean.getSelectedCollections().add(collection.getId());
      }
    } else {
      sessionBean.getSelectedCollections().remove(collection.getId());
    }
    this.selected = selected;
  }

  /**
   * release the {@link CollectionImeji}
   * 
   * @return
   */
  public String release() {
    CollectionController cc = new CollectionController();
    try {
      cc.release(collection, sessionBean.getUser());
      BeanHelper.info(sessionBean.getMessage("success_collection_release"));
    } catch (Exception e) {
      BeanHelper.error(sessionBean.getMessage("error_collection_release: " + e.getMessage()));
      LOGGER.error("Error during collection release", e);
    }
    return "pretty:";
  }

  public String createDOI() {
    try {
      String doi = UrlHelper.getParameterValue("doi");
      DoiService doiService = new DoiService();
      if (doi != null) {
        doiService.addDoiToCollection(doi, collection, sessionBean.getUser());
      } else {
        doiService.addDoiToCollection(collection, sessionBean.getUser());
      }
      BeanHelper.info(sessionBean.getMessage("success_doi_creation"));
    } catch (ImejiException e) {
      BeanHelper.error(sessionBean.getMessage("error_doi_creation") + " " + e.getMessage());
      LOGGER.error("Error during doi creation", e);
    }
    return "pretty:";
  }

  /**
   * Delete the {@link CollectionImeji}
   * 
   * @return
   */
  public String delete() {
    CollectionController cc = new CollectionController();
    try {
      cc.delete(collection, sessionBean.getUser());
      BeanHelper.info(
          getSuccessCollectionDeleteMessage(this.collection.getMetadata().getTitle(), sessionBean));
    } catch (Exception e) {
      BeanHelper.error(sessionBean.getMessage(e.getLocalizedMessage()));
      LOGGER.error("Error delete collection", e);
    }
    return sessionBean.getPrettySpacePage("pretty:collections");
  }

  /**
   * Discard the {@link CollectionImeji} of this {@link CollectionBean}
   * 
   * @return
   * @throws Exception
   */
  public String withdraw() throws Exception {
    CollectionController cc = new CollectionController();
    try {
      cc.withdraw(collection, sessionBean.getUser());
      BeanHelper.info(sessionBean.getMessage("success_collection_withdraw"));
    } catch (Exception e) {
      BeanHelper.error(sessionBean.getMessage("error_collection_withdraw"));
      BeanHelper.error(e.getMessage());
      LOGGER.error("Error discarding collection:", e);
    }
    return "pretty:";
  }

  /**
   * getter
   * 
   * @return
   */
  public MetadataProfile getProfile() {
    return profile;
  }

  /**
   * setter
   * 
   * @param profile
   */
  public void setProfile(MetadataProfile profile) {
    this.profile = profile;
  }

  public MetadataProfile getProfileTemplate() {
    return profileTemplate;
  }

  public void setProfileTemplate(MetadataProfile profileTemplate) {
    this.profileTemplate = profileTemplate;
  }

  /**
   * getter
   * 
   * @return
   */
  public String getProfileId() {
    return profileId;
  }

  /**
   * setter
   * 
   * @param profileId
   */
  public void setProfileId(String profileId) {
    this.profileId = profileId;
  }

  public String getPageUrl() {
    return navigation.getCollectionUrl() + id;
  }

  /*
   * (non-Javadoc)
   * 
   * @see de.mpg.imeji.presentation.beans.ContainerBean#getType()
   */
  @Override
  public String getType() {
    return CONTAINER_TYPE.COLLECTION.name();
  }

  /*
   * (non-Javadoc)
   * 
   * @see de.mpg.imeji.presentation.beans.ContainerBean#getContainer()
   */
  @Override
  public Container getContainer() {
    return collection;
  }

  public String getDiscardComment() {
    return this.getContainer().getDiscardComment();
  }

  public void setDiscardComment(String comment) {
    this.getContainer().setDiscardComment(comment);
  }

  public boolean isSendEmailNotification() {
    return sendEmailNotification;
  }

  public void setSendEmailNotification(boolean sendEmailNotification) {
    this.sendEmailNotification = sendEmailNotification;
    // check if id already set
    if (!isNullOrEmpty(id)) {
      User user = sessionBean.getUser();
      if (sendEmailNotification) {
        user.addObservedCollection(id);
      } else {
        user.removeObservedCollection(id);
      }
    }
  }

  public boolean isCollectionCreateMode() {
    return collectionCreateMode;
  }

  public void setCollectionCreateMode(boolean collectionCreateMode) {
    this.collectionCreateMode = collectionCreateMode;
  }

  public boolean isProfileSelectMode() {
    return profileSelectMode;
  }

  public void setProfileSelectMode(boolean profileSelectMode) {
    this.profileSelectMode = profileSelectMode;
  }

  public boolean isShowCheckBoxUseTemplate() {
    if (collectionCreateMode) {
      return collectionCreateMode;
    } else {
      ProfileController pc = new ProfileController();
      MetadataProfile collectionProfile = null;
      try {
        collectionProfile = pc.retrieve(collection.getProfile(), sessionBean.getUser());
      } catch (NotFoundException e) {
        return true;
      } catch (ImejiException e) {
        BeanHelper.error(sessionBean.getMessage("error_retrieving_metadata_profile"));
      }
      return collectionProfile.getStatements().isEmpty();
    }
  }
}
