package de.mpg.imeji.presentation.security;

import java.io.IOException;
import java.io.Serializable;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;

import de.mpg.imeji.presentation.navigation.Navigation;

/**
 * JSF Bean for the logout page
 * 
 * @author saquet
 *
 */
@ViewScoped
@ManagedBean(name = "LogoutBean")
public class LogoutBean implements Serializable {
  private static final long serialVersionUID = -2722355711874392048L;

  public void logout() throws IOException {
    ExternalContext ec = FacesContext.getCurrentInstance().getExternalContext();
    ec.invalidateSession();
    ec.redirect(new Navigation().getHomeUrl() + "?logout=1");
  }
}
