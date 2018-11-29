package de.mpg.imeji.presentation.startpage;

import java.io.IOException;
import java.io.Serializable;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import de.mpg.imeji.logic.config.Imeji;
import de.mpg.imeji.logic.util.UrlHelper;
import de.mpg.imeji.presentation.beans.SuperBean;
import de.mpg.imeji.presentation.session.BeanHelper;

/**
 * the Java Bean for the Start Page
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
@ManagedBean(name = "StartPageBean")
@RequestScoped
public class StartPageBean extends SuperBean implements Serializable {
  private static final Logger LOGGER = LogManager.getLogger(StartPageBean.class);
  private static final long serialVersionUID = 5267521759370584976L;

  @PostConstruct
  public void init() {
    resetSelectedItems();
  }

  /**
   * Method called before the message rendering. Postconstruct method happens too late to display
   * the messages
   *
   * @throws IOException
   */
  public void onload() {
    try {
      if (UrlHelper.hasParameter("redirectAfterLogin")) {
        BeanHelper.info(Imeji.RESOURCE_BUNDLE.getLabel("view_page_disallowed", getLocale()));
      }
      if (UrlHelper.getParameterBoolean("uploadForbidden")) {
        BeanHelper.info(Imeji.RESOURCE_BUNDLE.getLabel("upload_forbidden", getLocale()));
      }
      if (UrlHelper.getParameterBoolean("logout")) {
        BeanHelper.info(Imeji.RESOURCE_BUNDLE.getMessage("success_log_out", getLocale()));
        redirect(getNavigation().getHomeUrl());
      }
    } catch (Exception e) {
      LOGGER.error("Error on startpage", e);
    }
  }

}
