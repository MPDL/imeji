/**
 * License: src/main/resources/license/escidoc.license
 */
package de.mpg.imeji.presentation.mdProfile;

import org.apache.log4j.Logger;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.controller.ProfileController;
import de.mpg.imeji.presentation.session.SessionBean;
import de.mpg.imeji.presentation.util.BeanHelper;

/**
 * Java Bean for profile view page
 * 
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public class ViewMdProfileBean extends MdProfileBean {
  private SessionBean session;
  private static final Logger LOGGER = Logger.getLogger(ViewMdProfileBean.class);

  /**
   * Bean constructor
   */
  public ViewMdProfileBean() {
    super();
    session = (SessionBean) BeanHelper.getSessionBean(SessionBean.class);
  }

  /**
   * Initialize the page
   * 
   * @throws ImejiException
   * @throws Exception
   */
  @Override
  public String getInit()  {
    try {
      if (this.getId() != null) {

        ProfileController profileController = new ProfileController();
        this.setProfile(profileController.retrieve(this.getId(), session.getUser()));
        super.getInit();
      
    } else {
      BeanHelper.error(session.getLabel("error") + "  No profile Id found in URL");
    }
    return "";

    } catch (Exception e) {
      return "";
   }
  }

  /**
   * Method for save button. Save the profile according to the form values TODO check if such a
   * method is used on view profile page...
   * 
   * @return
   */
  public String save() {
    try {
      ProfileController profileController = new ProfileController();
      profileController.update(this.getProfile(), session.getUser());
      session.getProfileCached().clear();
      BeanHelper.info(session.getMessage("success_profile_save"));
    } catch (Exception e) {
      BeanHelper.error(session.getMessage("error_profile_save"));
      LOGGER.error(session.getMessage("error_profile_save"), e);
    }
    return "pretty:";
  }

  @Override
  protected String getNavigationString() {
    return session.getPrettySpacePage("pretty:viewProfile");
  }
}
