/**
 * License: src/main/resources/license/escidoc.license
 */
package de.mpg.imeji.presentation.mdProfile;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.presentation.session.SessionBean;

/**
 * Java Bean for profile view page
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
@ManagedBean(name = "ViewMdProfileBean")
@RequestScoped
public class ViewMdProfileBean extends MdProfileBean {
  private static final long serialVersionUID = 4353869579444298312L;

  /**
   * Initialize the page
   *
   * @throws ImejiException
   * @throws Exception
   */
  @Override
  public void specificSetup() {
    // nothing...
  }

  @Override
  protected String getNavigationString() {
    return SessionBean.getPrettySpacePage("pretty:viewProfile", getSelectedSpaceString());
  }
}
