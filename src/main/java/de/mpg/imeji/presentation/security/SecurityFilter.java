package de.mpg.imeji.presentation.security;

import static de.mpg.imeji.logic.model.Properties.Status.PENDING;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.ws.rs.core.Response.Status;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import com.ocpsoft.pretty.PrettyContext;

import de.mpg.imeji.exceptions.AuthenticationError;
import de.mpg.imeji.exceptions.BadRequestException;
import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.exceptions.NotAllowedError;
import de.mpg.imeji.exceptions.NotFoundException;
import de.mpg.imeji.logic.config.Imeji;
import de.mpg.imeji.logic.model.CollectionImeji;
import de.mpg.imeji.logic.model.Item;
import de.mpg.imeji.logic.model.User;
import de.mpg.imeji.logic.search.jenasearch.ImejiSPARQL;
import de.mpg.imeji.logic.search.jenasearch.JenaCustomQueries;
import de.mpg.imeji.logic.search.jenasearch.JenaSearch;
import de.mpg.imeji.logic.search.model.SearchResult;
import de.mpg.imeji.logic.security.authentication.impl.HttpAuthentication;
import de.mpg.imeji.logic.security.authorization.util.SecurityUtil;
import de.mpg.imeji.logic.security.user.UserService;
import de.mpg.imeji.logic.util.ObjectHelper;
import de.mpg.imeji.logic.workflow.status.StatusUtil;
import de.mpg.imeji.presentation.navigation.Navigation;
import de.mpg.imeji.presentation.navigation.history.HistoryUtil;
import de.mpg.imeji.presentation.session.SessionBean;
import de.mpg.imeji.presentation.util.ServletUtil;

/**
 * {@link Filter} for imeji authentification
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public class SecurityFilter implements Filter {
  private FilterConfig filterConfig = null;
  private final Pattern jsfPattern = Pattern.compile(".*\\/jsf\\/.*\\.xhtml");
  private static final Logger LOGGER = LogManager.getLogger(SecurityFilter.class);
  private static final Navigation NAVIGATION = new Navigation();
  private static final JenaSearch JENA_SEARCH = new JenaSearch(null, null);

  /*
   * (non-Javadoc)
   *
   * @see javax.servlet.Filter#destroy()
   */
  @Override
  public void destroy() {
    setFilterConfig(null);
  }

  /*
   * (non-Javadoc)
   *
   * @see javax.servlet.Filter#doFilter(javax.servlet.ServletRequest,
   * javax.servlet.ServletResponse, javax.servlet.FilterChain)
   */
  @Override
  public void doFilter(ServletRequest serv, ServletResponse resp, FilterChain chain) throws IOException, ServletException {

    try {
      final HttpServletRequest request = (HttpServletRequest) serv;
      if (ServletUtil.isGetRequest(serv)) {
        User user = login(request);
        checkReadAuthorization(request, user);
      }
    } catch (NotFoundException e) {
      ((HttpServletResponse) resp).sendError(Status.NOT_FOUND.getStatusCode(), "RESOURCE_NOT_FOUND");
    } catch (AuthenticationError e) {
      redirectToLoginPage(serv, resp);
    } catch (NotAllowedError e) {
      ((HttpServletResponse) resp).sendError(Status.FORBIDDEN.getStatusCode(), "FORBIDDEN");
    } catch (Exception e) {
      LOGGER.error("Error in security Filter", e);
      ((HttpServletResponse) resp).sendError(Status.INTERNAL_SERVER_ERROR.getStatusCode(), "INTERNAL_SERVER_ERROR");
    } finally {
      chain.doFilter(serv, resp);
    }
  }

  /**
   * Check the authorization for this request with this user
   * 
   * @param request
   * @param user
   * @throws NotFoundException
   * @throws AuthenticationError
   * @throws NotAllowedError
   * @throws BadRequestException
   */
  private void checkReadAuthorization(HttpServletRequest request, User user) throws ImejiException {
    URI uri = getCollectionURI(request);
    if (uri != null) {
      // uri found in the request -> check if user can see it
      if (SecurityUtil.authorization().isSysAdmin(user)) {
        // Check if public in order to check if the collection really exists
        isPublic(uri);
      } else if (SecurityUtil.authorization().read(user, uri)) {
        // user can read -> OK
        return;
      } else if (isPublic(uri)) {
        // Object is public -> OK
        return;
      } else if (user == null) {
        // User must log in
        throw new AuthenticationError(uri.getPath() + " is not public");
      } else {
        // Use not allowed
        throw new NotAllowedError(user.getEmail() + " not allowed to view " + uri.getPath());
      }
    }
  }

  /**
   * True if public
   * 
   * @param uri
   * @return
   * @throws NotFoundException
   */
  private boolean isPublic(URI uri) throws NotFoundException {
    List<String> result = ImejiSPARQL.exec(JenaCustomQueries.selectStatus(uri.toString()), Imeji.collectionModel);
    if (result.size() < 1) {
      throw new NotFoundException(uri.getPath() + " not found");
    } else {
      return StatusUtil.parseStatus(result.get(0)) != PENDING;
    }
  }

  /**
   * Create the uri out of the request parameters
   * 
   * @param request
   * @return
   * @throws BadRequestException
   */
  private URI getCollectionURI(HttpServletRequest request) throws NotFoundException {
    final String collectionId = request.getParameter("collectionId");
    final String itemId = request.getParameter("id");
    if (collectionId != null) {
      return ObjectHelper.getURI(CollectionImeji.class, collectionId);
    } else if (itemId != null) {
      return getCollectionOfItem(ObjectHelper.getURI(Item.class, itemId));
    }
    return null;
  }

  /**
   * Return the collection URI for the item URI
   * 
   * @param uri
   * @return
   * @throws BadRequestException
   */
  private URI getCollectionOfItem(URI uri) throws NotFoundException {
    SearchResult result = JENA_SEARCH.searchString(JenaCustomQueries.selectCollectionIdOfItem(uri.toString()), null, null, 0, 1);
    if (result.getNumberOfRecords() < 1) {
      throw new NotFoundException(uri + " hasn't a collection");
    } else {
      return URI.create(result.getResults().get(0));
    }
  }

  /**
   * Login the user according to the request and what is in the session
   * 
   * @param request
   * @return
   * @throws Exception
   */
  private User login(HttpServletRequest request) throws Exception {
    final SessionBean session = getSession(request);

    if (session != null && session.getUser() == null) {
      final HttpAuthentication httpAuthentification = new HttpAuthentication(request);
      if (httpAuthentification.hasLoginInfos()) {
        session.setUser(httpAuthentification.doLogin());
      }
    } else if (session != null && session.getUser() != null) {
      if (isReloadUser(request, session.getUser())) {
        session.reloadUser();
      }
    }
    return session != null ? session.getUser() : null;
  }

  /**
   * Redirect the request to the login page
   *
   * @param serv
   * @param resp
   * @throws IOException
   * @throws UnsupportedEncodingException
   */
  private void redirectToLoginPage(ServletRequest serv, ServletResponse resp) throws UnsupportedEncodingException, IOException {
    HttpServletRequest request = (HttpServletRequest) serv;
    String url = NAVIGATION.getApplicationUri() + PrettyContext.getCurrentInstance(request).getRequestURL().toURL();
    Map<String, String[]> params = PrettyContext.getCurrentInstance(request).getRequestQueryString().getParameterMap();
    ((HttpServletResponse) resp).sendRedirect(serv.getServletContext().getContextPath() + "/login?redirect="
        + URLEncoder.encode(url + HistoryUtil.paramsMapToString(params), "UTF-8"));

  }

  /**
   * Redirect the request to the login page
   *
   * @param serv
   * @param resp
   * @throws IOException
   * @throws UnsupportedEncodingException
   */
  private void redirectToHome(ServletResponse resp) throws IOException {
    ((HttpServletResponse) resp).sendRedirect(NAVIGATION.getHomeUrl());

  }

  /**
   * True if it is necessary to reload the User. This method tried to reduce as much as possible
   * reload of the user, to avoid too much database queries.
   *
   * @param req
   * @return
   */
  private boolean isReloadUser(HttpServletRequest req, User user) {
    return isXHTMLRequest(req) && !isAjaxRequest(req) && isModifiedUser(user);
  }

  /**
   * True if the {@link User} has been modified in the database (for instance, a user has share
   * something with him)
   *
   * @param user
   * @return
   */
  private boolean isModifiedUser(User user) {
    try {
      return new UserService().isModified(user);
    } catch (final Exception e) {
      return true;
    }
  }

  /**
   * True if the request is done from an xhtml page
   *
   * @param req
   * @return
   */
  private boolean isXHTMLRequest(HttpServletRequest req) {
    final Matcher m = jsfPattern.matcher(req.getRequestURI());
    return m.matches();
  }

  /**
   * True of the request is an Ajax Request
   *
   * @param req
   * @return
   */
  private boolean isAjaxRequest(HttpServletRequest req) {
    return "partial/ajax".equals(req.getHeader("Faces-Request"));
  }

  /*
   * (non-Javadoc)
   *
   * @see javax.servlet.Filter#init(javax.servlet.FilterConfig)
   */
  @Override
  public void init(FilterConfig arg0) throws ServletException {
    this.setFilterConfig(arg0);
  }

  public FilterConfig getFilterConfig() {
    return filterConfig;
  }

  public void setFilterConfig(FilterConfig filterConfig) {
    this.filterConfig = filterConfig;
  }

  /**
   * Return the {@link SessionBean} form the {@link HttpSession}
   *
   * @param req
   * @return
   */
  private SessionBean getSession(HttpServletRequest req) {
    return (SessionBean) req.getSession(true).getAttribute(SessionBean.class.getSimpleName());
  }

}
