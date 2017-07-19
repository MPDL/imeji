package de.mpg.imeji.presentation.security;

import static de.mpg.imeji.logic.util.StringHelper.isNullOrEmptyTrim;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.ViewScoped;

import org.apache.log4j.Logger;

import com.ocpsoft.pretty.PrettyContext;

import de.mpg.imeji.exceptions.AuthenticationError;
import de.mpg.imeji.exceptions.InactiveAuthenticationError;
import de.mpg.imeji.logic.authentication.Authentication;
import de.mpg.imeji.logic.authentication.factory.AuthenticationFactory;
import de.mpg.imeji.logic.config.Imeji;
import de.mpg.imeji.logic.util.StringHelper;
import de.mpg.imeji.logic.util.UrlHelper;
import de.mpg.imeji.logic.vo.User;
import de.mpg.imeji.presentation.beans.SuperBean;
import de.mpg.imeji.presentation.session.BeanHelper;
import de.mpg.imeji.presentation.session.SessionBean;

/**
 * Bean for login features
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
@ManagedBean(name = "LoginBean")
@ViewScoped
public class LoginBean extends SuperBean {
  private static final long serialVersionUID = 3597358452256592426L;
  private String login;
  private String passwd;
  @ManagedProperty(value = "#{SessionBean}")
  private SessionBean sessionBean;
  private String redirect = null;
  private static final Logger LOGGER = Logger.getLogger(LoginBean.class);
  private String requestUrl;

  /**
   * Constructor
   */
  public LoginBean() {

  }

  @PostConstruct
  public void init() {
    initRequestUrl();
    try {
      final String login = UrlHelper.getParameterValue("login");
      if (!isNullOrEmptyTrim(login)) {
        setLogin(login);
      }
      if (UrlHelper.getParameterValue("redirect") != null) {
        this.redirect = URLDecoder.decode(UrlHelper.getParameterValue("redirect"), "UTF-8");
      }
    } catch (final Exception e) {
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

  public void doLogin() {
    final String instanceName = Imeji.CONFIG.getInstanceName();
    if (StringHelper.isNullOrEmptyTrim(getLogin())) {
      return;
    }
    final Authentication auth = AuthenticationFactory.factory(getLogin(), getPasswd());
    try {
      final User user = auth.doLogin();
      sessionBean.setUser(user);
      BeanHelper.cleanMessages();
      BeanHelper.info(Imeji.RESOURCE_BUNDLE.getMessage("success_log_in", getLocale()));
    } catch (final InactiveAuthenticationError e) {
      BeanHelper.error(Imeji.RESOURCE_BUNDLE.getMessage("error_log_in_inactive", getLocale()));
    } catch (final AuthenticationError e) {
      BeanHelper.error(Imeji.RESOURCE_BUNDLE.getMessage("error_log_in", getLocale())
          .replace("XXX_INSTANCE_NAME_XXX", instanceName));
    }
    if (isNullOrEmptyTrim(redirect)) {
      // HistoryPage current = getHistory().getCurrentPage();
      if (!requestUrl.equals(getNavigation().getRegistrationUrl())
          && !requestUrl.equals(getNavigation().getLoginUrl())) {
        redirect = requestUrl;
      } else {
        redirect = getNavigation().getHomeUrl();
      }
    }

    try {
      redirect(redirect);
    } catch (IOException e) {
      LOGGER.error("Error redirect after login", e);
    }
  }

  private void initRequestUrl() {
    this.requestUrl = getNavigation().getApplicationUri()
        + PrettyContext.getCurrentInstance().getRequestURL().toURL()
        + PrettyContext.getCurrentInstance().getRequestQueryString().toQueryString();
  }

  public SessionBean getSessionBean() {
    return sessionBean;
  }

  public void setSessionBean(SessionBean sessionBean) {
    this.sessionBean = sessionBean;
  }

  public String getRedirect() {
    return redirect;
  }

  public void setRedirect(String redirect) {
    this.redirect = redirect;
  }

  public String getEncodedRedirect() throws UnsupportedEncodingException {
    return URLEncoder.encode(UrlHelper.getParameterValue("redirect"), "UTF-8");
  }



}
