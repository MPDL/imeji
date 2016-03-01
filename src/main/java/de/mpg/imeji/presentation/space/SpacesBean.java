package de.mpg.imeji.presentation.space;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;

import org.apache.log4j.Logger;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.controller.SpaceController;
import de.mpg.imeji.logic.vo.Space;
import de.mpg.imeji.presentation.beans.Navigation;
import de.mpg.imeji.presentation.session.SessionBean;
import de.mpg.imeji.presentation.util.BeanHelper;

/**
 * Java Bean for the view spaces page
 * 
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
@ManagedBean(name = "SpacesBean")
@ViewScoped
public class SpacesBean implements Serializable {
  private static final long serialVersionUID = 909531319532057427L;

  private List<Space> spaces;
  private static final Logger LOGGER = Logger.getLogger(SpacesBean.class);
  private SessionBean sessionBean;
  private Navigation navigation;

  public SpacesBean() {
    spaces = new ArrayList<Space>();
    sessionBean = (SessionBean) BeanHelper.getSessionBean(SessionBean.class);
    navigation = (Navigation) BeanHelper.getApplicationBean(Navigation.class);

    init();
  }

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
      sc.delete(delSpace, sessionBean.getUser());
    } catch (Exception e) {
      BeanHelper.error(sessionBean.getMessage("error_delete_space"));
    }

    FacesContext.getCurrentInstance().getExternalContext().redirect(navigation.getSpacesUrl());

  }


}
