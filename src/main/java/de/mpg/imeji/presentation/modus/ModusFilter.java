package de.mpg.imeji.presentation.modus;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Map;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ocpsoft.pretty.PrettyContext;

import de.mpg.imeji.presentation.beans.ConfigurationBean;
import de.mpg.imeji.presentation.beans.Navigation;
import de.mpg.imeji.presentation.history.HistoryUtil;
import de.mpg.imeji.presentation.util.ServletUtil;

/**
 * Filter for imeji Modus
 * 
 * @author bastiens
 *
 */
public class ModusFilter implements Filter {
  private static final Navigation navigation = new Navigation();
  private static final String REDIRECT_AFTER_LOGIN_PARAM = "redirectAfterLogin";

  @Override
  public void init(FilterConfig filterConfig) throws ServletException {

  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse resp, FilterChain chain)
      throws IOException, ServletException {
    if (ServletUtil.isGetRequest(request)) {
      if (isPrivate((HttpServletRequest) request)) {
        redirectToStartPage(request, resp);
        return;
      } else if (isRedirected((HttpServletRequest) request)) {
        redirect(request, resp);
        return;
      }
    }
    chain.doFilter(request, resp);
  }


  @Override
  public void destroy() {

  }



  /**
   * True if a redirect parameter is defined in the url
   * 
   * @param request
   * @return
   */
  private boolean isRedirected(HttpServletRequest request) {
    return request.getParameter(REDIRECT_AFTER_LOGIN_PARAM) != null && isLoggedIn(request);
  }


  /**
   * True if the current requested URL is private (i.e., imeji is in private modus, page is not the
   * start page and user is not logged in)
   * 
   * @param request
   * @return
   */
  private boolean isPrivate(HttpServletRequest request) {
    return isPrivateModus() && !isPublicPage(request) && !isLoggedIn(request);
  }

  /**
   * True if the page which is public even in private mode. For instance the Help Page
   * 
   * @param request
   * @return
   */
  private boolean isPublicPage(HttpServletRequest request) {
    String path = PrettyContext.getCurrentInstance(request).getRequestURL().toURL();
    return Navigation.HELP.hasSamePath(path) || Navigation.HOME.hasSamePath(path)
        || Navigation.REGISTRATION.hasSamePath(path) || Navigation.IMPRINT.hasSamePath(path);
  }

  /**
   * True if the current user is logged in
   * 
   * @param request
   * @return
   */
  private boolean isLoggedIn(HttpServletRequest request) {
    return ServletUtil.getSessionBean(request) != null
        && ServletUtil.getSessionBean(request).getUser() != null;
  }


  /**
   * True if the current imeji instance is in private modus
   * 
   * @param request
   * @return
   */
  private boolean isPrivateModus() {
    return ConfigurationBean.getPrivateModusStatic();
  }

  /**
   * Redirect to the start Page
   * 
   * @param serv
   * @param resp
   * @throws UnsupportedEncodingException
   * @throws IOException
   */
  private void redirectToStartPage(ServletRequest serv, ServletResponse resp)
      throws UnsupportedEncodingException, IOException {
    String url = navigation.getApplicationUri()
        + PrettyContext.getCurrentInstance((HttpServletRequest) serv).getRequestURL().toURL();
    Map<String, String[]> params = PrettyContext.getCurrentInstance((HttpServletRequest) serv)
        .getRequestQueryString().getParameterMap();
    ((HttpServletResponse) resp)
        .sendRedirect(navigation.getApplicationUri() + "?" + REDIRECT_AFTER_LOGIN_PARAM + "="
            + URLEncoder.encode(url + HistoryUtil.paramsMapToString(params), "UTF-8"));
  }

  /**
   * Redirect to the page defined by the parameter
   * 
   * @param serv
   * @param resp
   * @throws IOException
   */
  private void redirect(ServletRequest serv, ServletResponse resp) throws IOException {
    String url = URLDecoder.decode(serv.getParameter(REDIRECT_AFTER_LOGIN_PARAM), "UTF-8");
    ((HttpServletResponse) resp).sendRedirect(url);
  }
}
