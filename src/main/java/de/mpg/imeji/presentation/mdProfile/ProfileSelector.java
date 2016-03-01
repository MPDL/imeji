package de.mpg.imeji.presentation.mdProfile;

import java.io.Serializable;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import javax.faces.event.ValueChangeEvent;
import javax.faces.model.SelectItem;

import org.apache.log4j.Logger;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.auth.util.AuthUtil;
import de.mpg.imeji.logic.controller.CollectionController;
import de.mpg.imeji.logic.controller.CollectionController.MetadataProfileCreationMethod;
import de.mpg.imeji.logic.controller.ProfileController;
import de.mpg.imeji.logic.vo.MetadataProfile;
import de.mpg.imeji.logic.vo.Properties.Status;
import de.mpg.imeji.logic.vo.Statement;
import de.mpg.imeji.logic.vo.User;
import de.mpg.imeji.presentation.mdProfile.wrapper.StatementWrapper;
import de.mpg.imeji.presentation.util.BeanHelper;

/**
 * 
 * Bean for the ProfileSelector component
 * 
 * @author bastiens
 */
public class ProfileSelector implements Serializable {
  private static final long serialVersionUID = -6341938350166184007L;
  private static final Logger LOGGER = Logger.getLogger(ProfileSelector.class);
  private MetadataProfile profile;
  private User user;
  private String space;
  // Select profile menu
  private List<SelectItem> referenceProfiles = new ArrayList<SelectItem>();
  private List<SelectItem> copyProfiles = new ArrayList<SelectItem>();
  private String selectedProfileItem;
  private List<StatementWrapper> statementWrappers = new ArrayList<StatementWrapper>();
  private boolean copyProfile;

  /**
   * Constructor
   * 
   * @param profile
   * @param user
   * @param space
   */
  public ProfileSelector(MetadataProfile profile, User user, String space) {
    // this.profile = profile;
    this.user = user;
    this.space = space;
  }

  /**
   * Initialize the selector component. This methods is triggered from the xhtml page
   */
  public void init() {
    try {
      initSelectProfileMenu();
      initStatementWrapperList();
    } catch (ImejiException e) {
      BeanHelper.error(e.getMessage());
      LOGGER.error("Error Initialization", e);
    }
  }

  /**
   * Initialize the select profile menu
   * 
   * @throws ImejiException
   */
  private void initSelectProfileMenu() throws ImejiException {
    ProfileController profileController = new ProfileController();
    List<MetadataProfile> profiles = profileController.search(user, space);
    referenceProfiles = new ArrayList<>();
    referenceProfiles.add(new SelectItem(null, "Select a profile"));
    for (MetadataProfile mdp : profiles) {
      if (!mdp.getStatements().isEmpty() && (AuthUtil.staticAuth().administrate(user, mdp)
          || mdp.getStatus() == Status.RELEASED)) {
        referenceProfiles.add(new SelectItem(mdp.getId().toString(), mdp.getTitle()));
      }
      if (!mdp.getStatements().isEmpty() && AuthUtil.staticAuth().read(user, mdp)) {
        copyProfiles.add(new SelectItem(mdp.getId().toString(), mdp.getTitle()));
      }
    }
  }

  /**
   * Initialize the list of {@link StatementWrapper} with the statement of the selected profile
   */
  private void initStatementWrapperList() {
    statementWrappers = new ArrayList<>();
    if (profile != null) {
      for (Statement st : profile.getStatements()) {
        statementWrappers.add(new StatementWrapper(st, profile.getId(), 0));
      }
    }
  }

  /**
   * Listener for the select profile menu
   * 
   * @param event
   * @throws ImejiException
   */
  public void selectProfileListener(ValueChangeEvent event) throws ImejiException {
    this.selectedProfileItem = (String) event.getNewValue();
    if (selectedProfileItem != null) {
      this.profile = new ProfileController().retrieve(URI.create(selectedProfileItem), user);
    }
    initStatementWrapperList();
  }

  public MetadataProfile getProfile() {
    return profile;
  }

  public void setProfile(MetadataProfile profile) {
    this.profile = profile;
  }

  public String getSelectedProfileItem() {
    return selectedProfileItem;
  }

  public void setSelectedProfileItem(String selectedProfileItem) {
    this.selectedProfileItem = selectedProfileItem;
  }

  public List<SelectItem> getProfileItems() {
    return isCopyProfile() ? copyProfiles : referenceProfiles;
  }

  public void setProfileItems(List<SelectItem> profileItems) {
    this.referenceProfiles = profileItems;
  }

  public List<StatementWrapper> getStatementWrappers() {
    return statementWrappers;
  }

  public void setStatementWrappers(List<StatementWrapper> statementWrappers) {
    this.statementWrappers = statementWrappers;
  }

  public CollectionController.MetadataProfileCreationMethod getSelectorMode() {
    return copyProfile ? MetadataProfileCreationMethod.COPY
        : MetadataProfileCreationMethod.REFERENCE;
  }

  public boolean isCopyProfile() {
    return copyProfile;
  }

  public void setCopyProfile(boolean copyProfile) {
    this.copyProfile = copyProfile;
  }


}
