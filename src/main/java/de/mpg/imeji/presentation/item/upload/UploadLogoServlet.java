package de.mpg.imeji.presentation.item.upload;

import java.io.File;
import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import de.mpg.imeji.exceptions.TypeNotAllowedException;
import de.mpg.imeji.exceptions.UnprocessableError;
import de.mpg.imeji.logic.util.StorageUtils;
import de.mpg.imeji.presentation.beans.ContainerEditorSession;
import de.mpg.imeji.presentation.session.SessionBean;

/**
 * Servlet to upload logo to the ContainerEditorSession
 *
 * @author bastiens
 *
 */
@WebServlet("/uploadlogo/*")
public class UploadLogoServlet extends HttpServlet {
  private static final long serialVersionUID = 8271914066699208201L;
  private static final Logger LOGGER = LogManager.getLogger(UploadLogoServlet.class);

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    resp.sendError(HttpServletResponse.SC_NOT_IMPLEMENTED, "Only post supported");
  }

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    if (!isLoggedIn(req)) {
      resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Please login");
    } else {
      try {
        final File f = uploadLogo(req, resp);
        getContainerEditorSession(req).setUploadedLogoPath(f != null && f.exists() ? f.getAbsolutePath() : null);
      } catch (FileUploadException | TypeNotAllowedException e) {
        LOGGER.error("Error uploading logo", e);
        resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error uploading logo");
      } catch (UnprocessableError e) {
        getContainerEditorSession(req).setErrorMessage(e.getMessage());
      }
    }
  }

  private File uploadLogo(HttpServletRequest request, HttpServletResponse response)
      throws FileUploadException, TypeNotAllowedException, IOException, UnprocessableError {
    File tmp = null;
    final boolean isMultipart = ServletFileUpload.isMultipartContent(request);
    if (isMultipart) {
      final ServletFileUpload upload = new ServletFileUpload();
      final FileItemIterator iter = upload.getItemIterator(request);
      while (iter.hasNext()) {
        final FileItemStream fis = iter.next();
        tmp = StorageUtils.toFile(fis.openStream());
        if (StorageUtils.getMimeType(tmp).contains("image")) {
          return tmp;
        }
      }
      throw new UnprocessableError("This file cannot be used as logo. No image file.");
    }
    return null;
  }

  /**
   * Return the {@link SessionBean} form the {@link HttpSession}
   *
   * @param req
   * @return
   */
  private ContainerEditorSession getContainerEditorSession(HttpServletRequest req) {
    return (ContainerEditorSession) req.getSession(true).getAttribute(ContainerEditorSession.class.getSimpleName());
  }

  private boolean isLoggedIn(HttpServletRequest req) {
    return ((SessionBean) req.getSession(true).getAttribute(SessionBean.class.getSimpleName())).getUser() != null;
  }

}
