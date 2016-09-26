/**
 * License: src/main/resources/license/escidoc.license
 */
package de.mpg.imeji.presentation.mdProfile;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.controller.resource.CollectionController;
import de.mpg.imeji.logic.controller.resource.CollectionController.MetadataProfileCreationMethod;
import de.mpg.imeji.logic.controller.resource.ProfileController;
import de.mpg.imeji.logic.security.util.SecurityUtil;
import de.mpg.imeji.logic.util.ObjectHelper;
import de.mpg.imeji.logic.util.UrlHelper;
import de.mpg.imeji.logic.vo.CollectionImeji;
import de.mpg.imeji.logic.vo.MetadataProfile;
import de.mpg.imeji.logic.vo.util.ImejiFactory;
import de.mpg.imeji.presentation.beans.SuperBean;

/**
 * Java Bean for {@link MetadataProfile} create page
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
@ManagedBean(name = "CreateMdProfileBean")
@ViewScoped
public class CreateMdProfileBean extends SuperBean {
  private static final long serialVersionUID = 3639532454098819901L;
  private ProfileSelector profileSelector;
  private boolean showWarning = false;
  private boolean showSelector = false;
  private String collectionId;
  private String redirect;


  public String getInit() {
    showWarning = UrlHelper.getParameterBoolean("warn");
    collectionId = UrlHelper.getParameterValue("col");
    redirect = UrlHelper.getParameterValue("redirect");
    profileSelector = new ProfileSelector(null, getSessionUser(), getSelectedSpaceString(), getLocale());
    return "";
  }

  /**
   * Show the profile selector
   */
  public void startSelectProfile() {
    showSelector = true;
  }

  /**
   * Create a new profile, assign it to the current collection and redirect to the previous page
   *
   * @throws Exception
   */
  public void startNewProfile() throws Exception {
    MetadataProfile profile = ImejiFactory.newProfile();
    profile.setTitle("New Profile of " + getSessionUser().getPerson().getCompleteName());
    ProfileController controller = new ProfileController();
    profile = controller.create(profile, getSessionUser());
    changeProfile(profile, MetadataProfileCreationMethod.REFERENCE);
    redirect = "edit";
    redirect(profile);
  }


  /**
   * Change the profile of the current collection with the selected method
   *
   * @throws Exception
   */
  public void selectProfile() throws Exception {
    MetadataProfile profile =
        changeProfile(profileSelector.getProfile(), profileSelector.getSelectorMode());
    redirect(profile);
  }

  /**
   * Change the
   *
   * @param profile
   * @param method
   * @return
   * @throws ImejiException
   */
  private MetadataProfile changeProfile(MetadataProfile profile,
      MetadataProfileCreationMethod method) throws ImejiException {
    CollectionController controller = new CollectionController();
    CollectionImeji collection = controller
        .retrieve(ObjectHelper.getURI(CollectionImeji.class, collectionId), getSessionUser());
    CollectionImeji col =
        controller.updateCollectionProfile(collection, profile, getSessionUser(), method);
    ProfileController profileController = new ProfileController();
    return profileController.retrieve(col.getProfile(), getSessionUser());
  }

  /**
   * Go to the previous page in the History
   *
   * @throws Exception
   */
  public void cancel() throws Exception {
    redirect(profileSelector.getProfile());
  }

  private void redirect(MetadataProfile profile) throws Exception {
    if ("view".equals(redirect) && collectionId != null) {
      redirect(getNavigation().getCollectionUrl() + collectionId + "/infos");
    } else if ("edit".equals(redirect)
        && SecurityUtil.staticAuth().update(getSessionUser(), profile)) {
      redirect(getNavigation().getProfileUrl() + profile.getIdString() + "/edit?init=1&col="
          + collectionId);
    } else if ("edit".equals(redirect)
        && !SecurityUtil.staticAuth().update(getSessionUser(), profile)) {
      redirect(getNavigation().getCollectionUrl() + collectionId + "/infos");
    } else {
      redirect(getHistory().getPreviousPage().getCompleteUrlWithHistory());
    }
  }


  public ProfileSelector getProfileSelector() {
    return profileSelector;
  }

  public void setProfileSelector(ProfileSelector profileSelector) {
    this.profileSelector = profileSelector;
  }

  public boolean isShowWarning() {
    return showWarning;
  }

  public void setShowWarning(boolean showWarning) {
    this.showWarning = showWarning;
  }

  public boolean isShowSelector() {
    return showSelector;
  }

  public void setShowSelector(boolean showSelector) {
    this.showSelector = showSelector;
  }

  public String getCollectionId() {
    return collectionId;
  }

  public void setCollectionId(String collectionId) {
    this.collectionId = collectionId;
  }
}
