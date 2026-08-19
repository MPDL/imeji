package de.mpg.imeji.presentation.admin.mode;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.List;
import java.util.Map;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import de.mpg.imeji.presentation.rewrite.RequestHelper;
import de.mpg.imeji.logic.config.Imeji;
import de.mpg.imeji.presentation.navigation.Navigation;
import de.mpg.imeji.presentation.navigation.history.HistoryUtil;
import de.mpg.imeji.presentation.session.SessionBean;
import de.mpg.imeji.presentation.util.ServletUtil;

/**
 * Filter for imeji mode (private/normal)
 *
 * @author bastiens
 *
 */
public class ModeFilter implements Filter {
  private static final Navigation navigation = new Navigation();
  private static final String REDIRECT_AFTER_LOGIN_PARAM = "redirect";

  @Override
  public void init(FilterConfig filterConfig) throws ServletException {

  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse resp, FilterChain chain) throws IOException, ServletException {
    if (ServletUtil.isGetRequest(request)) {
      if (isPrivate((HttpServletRequest) request)) {
        redirectToLogin(request, resp);
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
    final String path = RequestHelper.getCurrentInstance(request).getPrettyRequestURL().toString();
    return Navigation.HELP.hasSamePath(path) || Navigation.HOME.hasSamePath(path) || Navigation.REGISTRATION.hasSamePath(path)
        || Navigation.IMPRINT.hasSamePath(path) || Navigation.PRIVACY_POLICY.hasSamePath(path) || Navigation.TERMS_OF_USE.hasSamePath(path)
        || Navigation.LOGIN.hasSamePath(path) || Navigation.PASSWORD_RESET.hasSamePath(path);
  }

  /**
   * True if the current user is logged in
   *
   * @param request
   * @return
   */
  private boolean isLoggedIn(HttpServletRequest request) {
    return SessionBean.getSessionBean(request) != null && SessionBean.getSessionBean(request).getUser() != null;
  }

  /**
   * True if the current imeji instance is in private modus
   *
   * @param request
   * @return
   */
  private boolean isPrivateModus() {
    return Imeji.CONFIG.getPrivateModus();
  }

  private void redirectToLogin(ServletRequest serv, ServletResponse resp) throws UnsupportedEncodingException, IOException {
    final String url =
        navigation.getApplicationUri() + RequestHelper.getCurrentInstance((HttpServletRequest) serv).getPrettyRequestURL().toString();
    final Map<String, List<String>> params = RequestHelper.getCurrentInstance((HttpServletRequest) serv).getRequestQueryParameters();
    ((HttpServletResponse) resp).sendRedirect(navigation.getApplicationUrl() + "login?" + REDIRECT_AFTER_LOGIN_PARAM + "="
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
    final String url = URLDecoder.decode(serv.getParameter(REDIRECT_AFTER_LOGIN_PARAM), "UTF-8");
    ((HttpServletResponse) resp).sendRedirect(url);
  }
}
