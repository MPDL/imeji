/**
 * License: src/main/resources/license/escidoc.license
 */
package de.mpg.imeji.presentation.user;

import static de.mpg.imeji.logic.util.StringHelper.isNullOrEmptyTrim;

import java.io.IOException;
import java.net.URLDecoder;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;

import de.mpg.imeji.exceptions.AuthenticationError;
import de.mpg.imeji.exceptions.InactiveAuthenticationError;
import de.mpg.imeji.logic.auth.authentication.Authentication;
import de.mpg.imeji.logic.auth.authentication.AuthenticationFactory;
import de.mpg.imeji.logic.concurrency.locks.Locks;
import de.mpg.imeji.logic.util.StringHelper;
import de.mpg.imeji.logic.util.UrlHelper;
import de.mpg.imeji.logic.vo.User;
import de.mpg.imeji.presentation.beans.ConfigurationBean;
import de.mpg.imeji.presentation.beans.Navigation;
import de.mpg.imeji.presentation.history.HistoryPage;
import de.mpg.imeji.presentation.history.HistorySession;
import de.mpg.imeji.presentation.session.SessionBean;
import de.mpg.imeji.presentation.util.BeanHelper;

/**
 * Bean for login features
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
@ManagedBean(name = "LoginBean")
@ViewScoped
public class LoginBean {
  private String login;
  private String passwd;
  private SessionBean sb;
  private String redirect;
  private static final Logger LOGGER = Logger.getLogger(LoginBean.class);

  /**
   * Constructor
   */
  public LoginBean() {

  }

  @PostConstruct
  public void init() {
    this.sb = (SessionBean) BeanHelper.getSessionBean(SessionBean.class);
    try {
      if (UrlHelper.getParameterBoolean("logout")) {
        logout();
      }
      String login = UrlHelper.getParameterValue("login");
      if (!isNullOrEmptyTrim(login)) {
        setLogin(login);
      }
      if (UrlHelper.getParameterValue("redirect") != null) {
        this.redirect = URLDecoder.decode(UrlHelper.getParameterValue("redirect"), "UTF-8");
      }
    } catch (Exception e) {
      LOGGER.error("Error initializing LoginBean", e);
    }
  }

  public void setLogin(String login) {
    this.login = login.trim();
  }

  public String getLogin() {
    return login;
  }

  public void setPasswd(String passwd) {
    this.passwd = passwd.trim();
  }

  public String getPasswd() {
    return passwd;
  }

  public void loginClick() {
    sb.setShowLogin(true);
  }

  public void doLogin() throws IOException {
    String instanceName =
        ((ConfigurationBean) BeanHelper.getApplicationBean(ConfigurationBean.class))
            .getInstanceName();
    if (StringHelper.isNullOrEmptyTrim(getLogin())) {
      return;
    }
    Authentication auth = AuthenticationFactory.factory(getLogin(), getPasswd());
    try {
      User user = auth.doLogin();
      sb.setUser(user);
      BeanHelper.cleanMessages();
      BeanHelper.info(sb.getMessage("success_log_in"));
    } catch (InactiveAuthenticationError e) {
      BeanHelper.error(sb.getMessage("error_log_in_inactive"));
    } catch (AuthenticationError e) {
      BeanHelper
          .error(sb.getMessage("error_log_in").replace("XXX_INSTANCE_NAME_XXX", instanceName));
    }
    if (isNullOrEmptyTrim(redirect)) {
      HistoryPage current =
          ((HistorySession) BeanHelper.getSessionBean(HistorySession.class)).getCurrentPage();
      Navigation navigation = (Navigation) BeanHelper.getApplicationBean(Navigation.class);
      if (current != null && !current.getUrl().equals(navigation.getRegistrationUrl())) {
        redirect = current.getCompleteUrl();
      } else {
        redirect = navigation.getHomeUrl();
      }
    }

    FacesContext.getCurrentInstance().getExternalContext().redirect(redirect);
  }

  /**
   * Logout and redirect to the home page
   *
   * @throws IOException
   */
  public void logout() throws IOException {
    FacesContext fc = FacesContext.getCurrentInstance();
    String spaceId = sb.getSpaceId();
    Locks.unlockAll(sb.getUser().getEmail());
    HttpSession session = (HttpSession) fc.getExternalContext().getSession(false);
    session.invalidate();
    sb.setUser(null);
    sb = (SessionBean) BeanHelper.getSessionBean(SessionBean.class);
    sb.setShowLogin(false);
    sb.setSpaceId(spaceId);
    BeanHelper.info(sb.getMessage("success_log_out"));
    fc = FacesContext.getCurrentInstance();
    Navigation nav = (Navigation) BeanHelper.getApplicationBean(Navigation.class);
    FacesContext.getCurrentInstance().getExternalContext().redirect(nav.getHomeUrl());
  }
}
