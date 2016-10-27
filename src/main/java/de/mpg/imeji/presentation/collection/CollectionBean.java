/**
 * License: src/main/resources/license/escidoc.license
 */
package de.mpg.imeji.presentation.collection;

import static com.google.common.base.Strings.isNullOrEmpty;

import java.net.URI;
import java.util.List;

import org.apache.log4j.Logger;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.exceptions.NotFoundException;
import de.mpg.imeji.logic.Imeji;
import de.mpg.imeji.logic.controller.resource.ProfileController;
import de.mpg.imeji.logic.util.UrlHelper;
import de.mpg.imeji.logic.vo.CollectionImeji;
import de.mpg.imeji.logic.vo.Container;
import de.mpg.imeji.logic.vo.MetadataProfile;
import de.mpg.imeji.presentation.beans.ContainerBean;
import de.mpg.imeji.presentation.session.BeanHelper;

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

  private CollectionImeji collection;
  private MetadataProfile profile = null;
  private MetadataProfile profileTemplate;

  private String id;
  private String profileId;
  private boolean selected;

  private boolean sendEmailNotification = false;
  private boolean collectionCreateMode = true;
  private boolean profileSelectMode = false;
  private CollectionActionMenu actionMenu;

  /**
   * New default {@link CollectionBean}
   */
  public CollectionBean() {
    collection = new CollectionImeji();
  }

  /**
   * Read the profile of the current collection
   *
   * @param user
   * @throws ImejiException
   */
  protected void initCollectionProfile() throws ImejiException {
    this.profile = new ProfileController().retrieve(collection.getProfile(), getSessionUser());
    this.profileId = profile != null ? profile.getIdString() : null;
  }

  @Override
  protected String getErrorMessageNoAuthor() {
    return "error_collection_need_one_author";
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
    if (getSelectedCollections().contains(collection.getId())) {
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
      if (!(getSelectedCollections().contains(collection.getId()))) {
        getSelectedCollections().add(collection.getId());
      }
    } else {
      getSelectedCollections().remove(collection.getId());
    }
    this.selected = selected;
  }

  protected abstract List<URI> getSelectedCollections();

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

  @Override
  public String getPageUrl() {
    return getNavigation().getCollectionUrl() + id;
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

  public boolean isSendEmailNotification() {
    return sendEmailNotification;
  }

  public void setSendEmailNotification(boolean sendEmailNotification) {
    this.sendEmailNotification = sendEmailNotification;
    // check if id already set
    if (!isNullOrEmpty(id)) {
      if (sendEmailNotification) {
        getSessionUser().addObservedCollection(id);
      } else {
        getSessionUser().removeObservedCollection(id);
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
        collectionProfile = pc.retrieve(collection.getProfile(), getSessionUser());
      } catch (NotFoundException e) {
        return true;
      } catch (ImejiException e) {
        BeanHelper.error(
            Imeji.RESOURCE_BUNDLE.getMessage("error_retrieving_metadata_profile", getLocale()));
      }
      return collectionProfile.getStatements().isEmpty();
    }
  }

  /**
   * @return the actionMenu
   */
  public CollectionActionMenu getActionMenu() {
    return actionMenu;
  }

  /**
   * @param actionMenu the actionMenu to set
   */
  public void setActionMenu(CollectionActionMenu actionMenu) {
    this.actionMenu = actionMenu;
  }
}
