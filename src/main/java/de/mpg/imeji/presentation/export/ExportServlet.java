package de.mpg.imeji.presentation.export;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

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
import org.apache.log4j.Logger;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.exceptions.UnprocessableError;
import de.mpg.imeji.logic.core.item.ItemService;
import de.mpg.imeji.logic.export.ExportAbstract;
import de.mpg.imeji.logic.export.FileExport;
import de.mpg.imeji.logic.export.SitemapExport;
import de.mpg.imeji.logic.export.ZIPExport;
import de.mpg.imeji.logic.model.CollectionImeji;
import de.mpg.imeji.logic.model.User;
import de.mpg.imeji.logic.search.SearchQueryParser;
import de.mpg.imeji.logic.util.ObjectHelper;
import de.mpg.imeji.logic.util.StringHelper;
import de.mpg.imeji.presentation.notification.NotificationUtils;
import de.mpg.imeji.presentation.session.SessionBean;

@WebServlet(urlPatterns = "/exportServlet", asyncSupported = true)
public class ExportServlet extends HttpServlet {
  private static final long serialVersionUID = -777947169051357999L;
  private static final Logger LOGGER = Logger.getLogger(ExportServlet.class);

  /**
   * {@inheritDoc}
   */
  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    final SessionBean session = getSessionBean(req, resp);
    try {
      final ExportAbstract export = doExport(req, resp, session);
      resp.setHeader("Connection", "close");
      resp.setHeader("Content-Type", export.getContentType() + ";charset=UTF-8");
      resp.setHeader("Content-disposition", "attachment; filename=\"" + export.getName() + "\"");
      resp.setHeader("Content-length", export.getSize());
      resp.setStatus(HttpServletResponse.SC_OK);
      export.export(resp.getOutputStream());
      resp.getOutputStream().flush();

      String format = req.getParameter("format");
      if (format.equals("file")) {
        NotificationUtils.notifyByItemDownload(session.getUser(),
            new ItemService().retrieve(req.getParameter("id"), session.getUser()), Locale.ENGLISH);
      } else {
        NotificationUtils.notifyByExport(export, session, req.getParameter("q"),
            req.getParameter("col"));
      }
    } catch (final HttpResponseException he) {
      LOGGER.error("Error export", he);
      resp.sendError(he.getStatusCode(), he.getMessage());
    } catch (final Exception e) {
      LOGGER.error("Error export", e);
      resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
    }
  }


  /**
   * Perform the export from the request parameters
   * 
   * @param req
   * @param resp
   * @param session
   * @return
   * @throws ImejiException
   * @throws IOException
   */
  private ExportAbstract doExport(HttpServletRequest req, HttpServletResponse resp,
      SessionBean session) throws ImejiException, IOException {
    final User user = session.getUser();
    final String format = req.getParameter("format");
    ExportAbstract export = null;
    if (format != null) {
      switch (format) {
        case "zip":
          export = new ZIPExport(getItemIds(req, session), user);
          break;
        case "file":
          export = new FileExport(req.getParameter("id"), user);
          break;
        case "sitemap":
          export = new SitemapExport(req.getParameter("q"), req.getParameter("priority"), user);
          break;
      }
    } else {
      throw new UnprocessableError(
          "Unknown format parameter. Possible values: files,selected, sitemap, item");
    }
    return export;
  }

  /**
   * Return the item ids to be exported. Default, return the selected items in the session
   * 
   * @param req
   * @param session
   * @return
   * @throws UnprocessableError
   */
  private List<String> getItemIds(HttpServletRequest req, SessionBean session)
      throws UnprocessableError {
    final String query = req.getParameter("q");
    final String collectionId = req.getParameter("col");
    if (query != null || !StringHelper.isNullOrEmptyTrim(collectionId)) {
      return new ItemService()
          .search(
              !StringHelper.isNullOrEmptyTrim(collectionId)
                  ? ObjectHelper.getURI(CollectionImeji.class, collectionId) : null,
              SearchQueryParser.parseStringQuery(query), null, session.getUser(), -1, 0)
          .getResults();
    } else {
      return session.getSelected();
    }
  }


  /**
   * Return the extension of the exported file according to the mimeType
   * 
   * @param mimetype
   * @return
   */
  private String getExportExtension(String mimetype) {
    if (mimetype.equalsIgnoreCase("application/zip")) {
      return ".zip";
    }
    return ".zip";
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
