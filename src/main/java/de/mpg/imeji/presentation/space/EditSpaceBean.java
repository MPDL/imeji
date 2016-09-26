package de.mpg.imeji.presentation.space;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.exceptions.UnprocessableError;
import de.mpg.imeji.logic.Imeji;
import de.mpg.imeji.logic.controller.resource.SpaceController;
import de.mpg.imeji.presentation.beans.Navigation;
import de.mpg.imeji.presentation.session.BeanHelper;

@ManagedBean(name = "EditSpaceBean")
@ViewScoped
public class EditSpaceBean extends SpaceBean {
  private static final long serialVersionUID = -5469506610392005312L;

  public EditSpaceBean() {
    setSpaceCreateMode(false);
    init();
  }

  public String save() throws Exception {
    if (updatedSpace()) {
      getSessionBean().setSpaceId(getSpace().getSlug());
      // Go to the home URL of the Space
      if (!isBackToAdminNoSpace()) {
        FacesContext.getCurrentInstance().getExternalContext()
            .redirect(getNavigation().getHomeUrl());
      } else {
        redirect(getNavigation().getApplicationUrl() + Navigation.spacesAllSlug);
      }
    }

    return "";
  }

  public String cancel() throws Exception {
    getSessionBean().setSpaceId(getSpace().getSlug());
    // Go to the home URL of the Space
    FacesContext.getCurrentInstance().getExternalContext().redirect(getNavigation().getAdminUrl());
    return "";
  }

  public boolean updatedSpace() throws ImejiException, IOException {
    try {
      SpaceController spaceController = new SpaceController();
      File spaceLogoFile = (getSessionBean().getSpaceLogoIngestImage() != null)
          ? getSessionBean().getSpaceLogoIngestImage().getFile() : null;
      setSpace(spaceController.update(getSpace(), getSelectedCollections(), spaceLogoFile,
          getSessionBean().getUser()));
      // reset the Session bean and this local, as anyway it will navigate
      // back to the home page
      // Note: check how it will work with eDit! Edit bean should be
      // implemented
      setIngestImage(null);
      BeanHelper.info(Imeji.RESOURCE_BUNDLE.getMessage("success_space_update", getLocale()));
      return true;
    } catch (UnprocessableError e) {
      BeanHelper.cleanMessages();
      BeanHelper.error(Imeji.RESOURCE_BUNDLE.getMessage("error_space_update", getLocale()));
      List<String> listOfErrors = Arrays.asList(e.getMessage().split(";"));
      for (String errorM : listOfErrors) {
        BeanHelper.error(Imeji.RESOURCE_BUNDLE.getMessage(errorM, getLocale()));
      }
      return false;
    }
  }

}
