package de.mpg.imeji.presentation.space;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import org.apache.log4j.Logger;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.Imeji;
import de.mpg.imeji.logic.controller.resource.SpaceController;
import de.mpg.imeji.logic.vo.Space;
import de.mpg.imeji.presentation.beans.SuperBean;
import de.mpg.imeji.presentation.session.BeanHelper;

/**
 * Java Bean for the view spaces page
 *
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
@ManagedBean(name = "SpacesBean")
@ViewScoped
public class SpacesBean extends SuperBean implements Serializable {
  private static final long serialVersionUID = 909531319532057427L;

  private List<Space> spaces;
  private static final Logger LOGGER = Logger.getLogger(SpacesBean.class);

  public SpacesBean() {
    spaces = new ArrayList<Space>();
  }

  @PostConstruct
  public void init() {
    SpaceController sc = new SpaceController();
    try {
      spaces = sc.retrieveAll();
    } catch (ImejiException e) {
      LOGGER.error("Error retrieving all spaces", e);
    }
  }

  /**
   * @return the spaces
   */
  public List<Space> getSpaces() {
    return spaces;
  }


  /**
   * @param spaces the spaces to set
   */
  public void setSpaces(List<Space> spaces) {
    this.spaces = spaces;
  }

  public void delete(Space delSpace) throws IOException {
    SpaceController sc = new SpaceController();
    try {
      sc.delete(delSpace, getSessionUser());
    } catch (Exception e) {
      BeanHelper.error(Imeji.RESOURCE_BUNDLE.getMessage("error_delete_space", getLocale()));
    }

    redirect(getNavigation().getSpacesUrl());

  }


}
