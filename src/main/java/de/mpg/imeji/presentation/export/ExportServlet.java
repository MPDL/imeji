/**
 * License: src/main/resources/license/escidoc.license
 */
package de.mpg.imeji.presentation.export;

import java.io.IOException;
import java.util.Date;

import javax.faces.FactoryFinder;
import javax.faces.component.UIViewRoot;
import javax.faces.context.FacesContext;
import javax.faces.context.FacesContextFactory;
import javax.faces.lifecycle.Lifecycle;
import javax.faces.lifecycle.LifecycleFactory;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.http.client.HttpResponseException;

import de.mpg.imeji.logic.export.ExportService;
import de.mpg.imeji.logic.search.model.SearchResult;
import de.mpg.imeji.logic.vo.User;
import de.mpg.imeji.presentation.notification.NotificationUtils;
import de.mpg.imeji.presentation.session.SessionBean;

@WebServlet("/exportServlet")
public class ExportServlet extends HttpServlet {
  private static final long serialVersionUID = -777947169051357999L;


  /**
   * {@inheritDoc}
   */
  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    final SessionBean session = getSessionBean(req, resp);
    final String instanceName = session.getInstanceName();
    final User user = session.getUser();

    try {
      final ExportService exportService = new ExportService(resp.getOutputStream(), user,
          req.getParameterMap(), session.getSelected());
      String exportName = instanceName + "_";
      exportName += new Date().toString().replace(" ", "_").replace(":", "-");
      if (exportService.getContentType().equalsIgnoreCase("application/xml")) {
        exportName += ".xml";
      }
      if (exportService.getContentType().equalsIgnoreCase("application/zip")) {
        exportName += ".zip";
      }
      resp.setHeader("Connection", "close");
      resp.setHeader("Content-Type", exportService.getContentType() + ";charset=UTF-8");
      resp.setHeader("Content-disposition", "filename=" + exportName);
      resp.setStatus(HttpServletResponse.SC_OK);
      final SearchResult result = exportService.search();
      exportService.export(result, user);
      NotificationUtils.notifyByExport(user, exportService.getExport(), session);
      resp.getOutputStream().flush();
    } catch (final HttpResponseException he) {
      resp.sendError(he.getStatusCode(), he.getMessage());
    } catch (final Exception e) {
      resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
    }
  }


  /**
   * Get the {@link SessionBean} from the {@link HttpSession}
   *
   * @param req
   * @param resp
   * @return
   */
  private SessionBean getSessionBean(HttpServletRequest req, HttpServletResponse resp) {
    final FacesContext fc = getFacesContext(req, resp);
    final Object session = fc.getExternalContext().getSessionMap().get("SessionBean");
    if (session == null) {
      try {
        final SessionBean newSession = SessionBean.class.newInstance();
        fc.getExternalContext().getSessionMap().put("SessionBean", newSession);
        return newSession;
      } catch (final Exception e) {
        throw new RuntimeException("Error creating Session", e);
      }
    }
    return (SessionBean) session;
  }

  /**
   * Get Faces Context from Filter
   *
   * @param request
   * @param response
   * @return
   */
  private FacesContext getFacesContext(ServletRequest request, ServletResponse response) {
    // Try to get it first
    FacesContext facesContext = FacesContext.getCurrentInstance();
    // if (facesContext != null) return facesContext;
    final FacesContextFactory contextFactory =
        (FacesContextFactory) FactoryFinder.getFactory(FactoryFinder.FACES_CONTEXT_FACTORY);
    final LifecycleFactory lifecycleFactory =
        (LifecycleFactory) FactoryFinder.getFactory(FactoryFinder.LIFECYCLE_FACTORY);
    final Lifecycle lifecycle = lifecycleFactory.getLifecycle(LifecycleFactory.DEFAULT_LIFECYCLE);
    facesContext =
        contextFactory.getFacesContext(getServletContext(), request, response, lifecycle);
    // Set using our inner class
    InnerFacesContext.setFacesContextAsCurrentInstance(facesContext);
    // set a new viewRoot, otherwise context.getViewRoot returns null
    final UIViewRoot view =
        facesContext.getApplication().getViewHandler().createView(facesContext, "imeji");
    facesContext.setViewRoot(view);
    return facesContext;
  }

  public abstract static class InnerFacesContext extends FacesContext {
    protected static void setFacesContextAsCurrentInstance(FacesContext facesContext) {
      FacesContext.setCurrentInstance(facesContext);
    }
  }
}
