/**
 * License: src/main/resources/license/escidoc.license
 */
package de.mpg.imeji.presentation.history;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Map;

import javax.faces.FactoryFinder;
import javax.faces.component.UIViewRoot;
import javax.faces.context.FacesContext;
import javax.faces.context.FacesContextFactory;
import javax.faces.lifecycle.Lifecycle;
import javax.faces.lifecycle.LifecycleFactory;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotAllowedException;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.ocpsoft.pretty.PrettyContext;
import com.ocpsoft.pretty.faces.config.PrettyConfig;
import com.ocpsoft.pretty.faces.config.mapping.UrlMapping;
import com.ocpsoft.pretty.faces.url.URL;

import de.mpg.imeji.exceptions.AuthenticationError;
import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.exceptions.NotAllowedError;
import de.mpg.imeji.exceptions.NotFoundException;
import de.mpg.imeji.logic.controller.resource.SpaceController;
import de.mpg.imeji.presentation.beans.Navigation;
import de.mpg.imeji.presentation.session.SessionBean;
import de.mpg.imeji.presentation.util.ServletUtil;

/**
 * {@link Filter} for the imeji history
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public class HistoryFilter implements Filter {
  private FilterConfig filterConfig = null;
  private static final Logger LOGGER = Logger.getLogger(HistoryFilter.class);
  private static final Navigation navigation = new Navigation();

  @Override
  public void destroy() {
    setFilterConfig(null);
  }

  @Override
  public void doFilter(ServletRequest serv, ServletResponse resp, FilterChain chain)
      throws IOException, ServletException {
    try {
      // Limit the case to filter: dispachertype only forward, and only
      // HTTP GET method
      if (ServletUtil.isGetRequest(serv)) {
        dofilterImpl((HttpServletRequest) serv, resp);
      }
    } catch (NotFoundException | NullPointerException e) {
      LOGGER.error("Error history filter", e);
      if ("SPACE_NOT_FOUND".equals(e.getMessage())) {
        ((HttpServletResponse) resp).sendRedirect(navigation.getApplicationUrl());
      } else {
        ((HttpServletResponse) resp).sendError(Status.NOT_FOUND.getStatusCode(),
            "RESOURCE_NOT_FOUND");
      }
    } catch (AuthenticationError e) {
      LOGGER.error("Error history filter", e);
      redirectToLoginPage(serv, resp);
    } catch (NotAllowedException | NotAllowedError e) {
      LOGGER.error("Error history filter", e);
      ((HttpServletResponse) resp).sendError(Status.FORBIDDEN.getStatusCode(), "FORBIDDEN");
    } catch (BadRequestException e) {
      LOGGER.error("Error history filter", e);
      ((HttpServletResponse) resp).sendError(Status.BAD_REQUEST.getStatusCode(), "BAD_REQUEST");
    } catch (Exception e) {
      LOGGER.error("Error history filter", e);
      ((HttpServletResponse) resp).sendError(Status.INTERNAL_SERVER_ERROR.getStatusCode(),
          "INTERNAL_SERVER_ERROR");
    } finally {
      chain.doFilter(serv, resp);
    }
  }

  /**
   * Redicrect the request to the login page
   *
   * @param serv
   * @param resp
   * @throws IOException
   * @throws UnsupportedEncodingException
   */
  private void redirectToLoginPage(ServletRequest serv, ServletResponse resp)
      throws UnsupportedEncodingException, IOException {
    HttpServletRequest request = (HttpServletRequest) serv;
    String url = navigation.getApplicationUri()
        + PrettyContext.getCurrentInstance(request).getRequestURL().toURL();
    Map<String, String[]> params =
        PrettyContext.getCurrentInstance(request).getRequestQueryString().getParameterMap();
    ((HttpServletResponse) resp)
        .sendRedirect(serv.getServletContext().getContextPath() + "/login?redirect="
            + URLEncoder.encode(url + HistoryUtil.paramsMapToString(params), "UTF-8"));

  }

  /**
   * Implement the History filter
   *
   * @param request
   * @param resp
   * @throws Exception
   */
  private void dofilterImpl(HttpServletRequest request, ServletResponse resp) throws Exception {
    getFacesContext(request, resp);
    SessionBean session = getSessionBean(request, resp);
    HistorySession hs = getHistorySession(request, resp);
    if (session != null && hs != null) {
      checkSpaceMatching(request, session, hs);
      String url = navigation.getApplicationUri()
          + PrettyContext.getCurrentInstance(request).getRequestURL().toURL();
      Map<String, String[]> params =
          PrettyContext.getCurrentInstance(request).getRequestQueryString().getParameterMap();
      if (params.containsKey("h")) {
        params.remove("h");
      }
      HistoryPage p = new HistoryPage(url, params, session.getUser());
      hs.addPage(p);
    }

  }

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



  private void checkSpaceMatching(HttpServletRequest request, SessionBean session,
      HistorySession hs) throws NotFoundException, ImejiException {
    // TODO CHANGE ME
    String spaceHome = "space_home";
    String matchingUrl = PrettyContext.getCurrentInstance(request).getRequestURL().toURL();
    PrettyConfig pc =
        PrettyContext.getCurrentInstance(FacesContext.getCurrentInstance()).getConfig();

    if (pc.isURLMapped(new URL(matchingUrl))) {
      UrlMapping myMap =
          pc.getMappingForUrl(PrettyContext.getCurrentInstance(request).getRequestURL());
      if (myMap.getId().startsWith("space_")) {
        String mySpaceId = PrettyContext.getCurrentInstance(request).getRequestURL().toURL();
        mySpaceId = spaceHome.equals(myMap.getId())
            ? StringUtils.substringAfter(matchingUrl, "/space/")
            : StringUtils.substringBefore(StringUtils.substringAfter(matchingUrl, "/space/"), "/");
        if (!mySpaceId.equals(session.getSpaceId())) {
          hs.getPages().clear();
          SpaceController sc = new SpaceController();
          if (!sc.isSpaceByLabel(mySpaceId)) {
            session.setSpaceId("");
            throw new NotFoundException("SPACE_NOT_FOUND");
          }
        }
      } else {
        if (!("".equals(session.getSpaceId()))) {
          // Clean old history pages when switching to a new space
          hs.getPages().clear();
          session.setSpaceId("");
        }
      }
    }
  }

  /**
   * Return the {@link SessionBean}
   *
   * @param req
   * @return
   */
  private SessionBean getSessionBean(HttpServletRequest req, ServletResponse resp) {
    return (SessionBean) getBean(SessionBean.class, req, resp);

  }

  /**
   * Get the {@link HistorySession} from the {@link FacesContext}
   *
   * @param request
   * @param resp
   * @return
   */
  private HistorySession getHistorySession(HttpServletRequest req, ServletResponse resp) {
    return (HistorySession) getBean(HistorySession.class, req, resp);
  }


  private Object getBean(Class<?> c, ServletRequest request, ServletResponse resp) {
    String name = c.getSimpleName();
    FacesContext fc = getFacesContext(request, resp);
    Object result = fc.getExternalContext().getSessionMap().get(name);
    if (result == null) {
      try {
        Object b = c.newInstance();
        FacesContext.getCurrentInstance().getExternalContext().getSessionMap().put(name, b);
        return b;
      } catch (Exception e) {
        throw new RuntimeException("Error creating History Session", e);
      }
    } else {
      return result;
    }
  }

  /**
   * Used to initialized FacesContext
   *
   * @author bastiens
   *
   */
  public abstract static class InnerFacesContext extends FacesContext {
    protected static void setFacesContextAsCurrentInstance(FacesContext facesContext) {
      FacesContext.setCurrentInstance(facesContext);
    }
  }

  /**
   * Get {@link FacesContext} from Filter
   *
   * @param request
   * @param response
   * @return
   */
  private FacesContext getFacesContext(ServletRequest request, ServletResponse response) {
    ServletContext servletContext = ((HttpServletRequest) request).getSession().getServletContext();
    // Try to get it first
    FacesContext facesContext = FacesContext.getCurrentInstance();
    // if (facesContext != null) return facesContext;
    FacesContextFactory contextFactory =
        (FacesContextFactory) FactoryFinder.getFactory(FactoryFinder.FACES_CONTEXT_FACTORY);
    LifecycleFactory lifecycleFactory =
        (LifecycleFactory) FactoryFinder.getFactory(FactoryFinder.LIFECYCLE_FACTORY);
    Lifecycle lifecycle = lifecycleFactory.getLifecycle(LifecycleFactory.DEFAULT_LIFECYCLE);
    facesContext = contextFactory.getFacesContext(servletContext, request, response, lifecycle);
    // Set using our inner class
    InnerFacesContext.setFacesContextAsCurrentInstance(facesContext);
    // set a new viewRoot, otherwise context.getViewRoot returns null
    UIViewRoot view =
        facesContext.getApplication().getViewHandler().createView(facesContext, "imeji");
    facesContext.setViewRoot(view);
    return facesContext;
  }

}
