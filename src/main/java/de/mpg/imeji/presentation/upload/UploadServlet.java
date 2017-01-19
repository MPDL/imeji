/**
 * License: src/main/resources/license/escidoc.license
 */
package de.mpg.imeji.presentation.upload;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.Charset;

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
import org.apache.log4j.Logger;

import de.mpg.imeji.exceptions.AuthenticationError;
import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.authentication.factory.AuthenticationFactory;
import de.mpg.imeji.logic.collection.CollectionService;
import de.mpg.imeji.logic.item.ItemService;
import de.mpg.imeji.logic.storage.Storage;
import de.mpg.imeji.logic.storage.util.StorageUtils;
import de.mpg.imeji.logic.util.TempFileUtil;
import de.mpg.imeji.logic.vo.CollectionImeji;
import de.mpg.imeji.logic.vo.User;
import de.mpg.imeji.presentation.session.SessionBean;

/**
 * The Servlet to Read files from imeji {@link Storage}
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
@WebServlet(urlPatterns = "/uploadServlet", asyncSupported = true)
public class UploadServlet extends HttpServlet {
  private static final long serialVersionUID = -4879871986174193049L;
  private static final Logger LOGGER = Logger.getLogger(UploadServlet.class);
  private static final ItemService itemService = new ItemService();
  private static final CollectionService collectionController = new CollectionService();

  /**
   * The result of an upload
   *
   * @author saquet
   *
   */
  private class UploadItem {
    private final File file;
    private final String filename;

    public UploadItem(File file, String filename) {
      this.file = file;
      this.filename = filename;
    }

    public File getFile() {
      return file;
    }

    public String getFilename() {
      return filename;
    }
  }

  @Override
  public void init() {

  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    final UploadItem upload = doUpload(req);
    final SessionBean session = getSession(req);
    try {
      final User user = getUser(req, session);
      final CollectionImeji col = retrieveCollection(req, user);
      itemService.createWithFile(null, upload.getFile(), upload.getFilename(), col, user);
    } catch (final AuthenticationError e) {
      writeResponse(resp, e.getMessage());
      resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
    } catch (final ImejiException e) {
      LOGGER.error("Error uploading File", e);
      writeResponse(resp, e.getMessage());
      resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }
  }

  private void writeResponse(HttpServletResponse resp, String errorMessage) throws IOException {
    resp.getOutputStream().write(errorMessage.getBytes(Charset.forName("UTF-8")));
  }

  /**
   * Download the file on the disk in a tmp file
   *
   * @param req
   * @return
   * @throws FileUploadException
   * @throws IOException
   */
  private UploadItem doUpload(HttpServletRequest req) {
    try {
      final ServletFileUpload upload = new ServletFileUpload();
      final FileItemIterator iter = upload.getItemIterator(req);
      while (iter.hasNext()) {
        final FileItemStream fis = iter.next();
        final InputStream stream = fis.openStream();
        if (!fis.isFormField()) {
          final String name = fis.getName();
          final File tmp = TempFileUtil.createTempFile("upload", ".jpg");
          StorageUtils.writeInOut(stream, new FileOutputStream(tmp), true);
          return new UploadItem(tmp, name);
        }
      }
    } catch (final Exception e) {
      LOGGER.error("Error file upload", e);
    }
    return null;
  }

  private CollectionImeji retrieveCollection(HttpServletRequest req, User user) {
    if (req.getParameter("col") != null) {
      try {
        return collectionController.retrieve(URI.create(req.getParameter("col")), user);
      } catch (final ImejiException e) {
        LOGGER.error("Error retrieving collection " + req.getParameter("col"), e);
      }
    }
    return null;
  }

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {}

  /**
   * Return the {@link SessionBean} form the {@link HttpSession}
   *
   * @param req
   * @return
   */
  private SessionBean getSession(HttpServletRequest req) {
    return (SessionBean) req.getSession(true).getAttribute(SessionBean.class.getSimpleName());
  }

  /**
   * Return the {@link User} of the request. Check first is a user is send with the request. If not,
   * check in the the session.
   *
   * @param req
   * @return
   * @throws AuthenticationError
   */
  private User getUser(HttpServletRequest req, SessionBean session) throws AuthenticationError {
    if (session != null) {
      return session.getUser();
    }
    final User user = AuthenticationFactory.factory(req).doLogin();
    if (user != null) {
      return user;
    }
    return null;
  }
}
