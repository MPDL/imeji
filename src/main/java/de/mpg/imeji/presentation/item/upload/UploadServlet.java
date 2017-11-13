package de.mpg.imeji.presentation.item.upload;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

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
import de.mpg.imeji.logic.core.collection.CollectionService;
import de.mpg.imeji.logic.core.item.ItemService;
import de.mpg.imeji.logic.model.CollectionImeji;
import de.mpg.imeji.logic.model.Item;
import de.mpg.imeji.logic.model.License;
import de.mpg.imeji.logic.model.User;
import de.mpg.imeji.logic.model.factory.ImejiFactory;
import de.mpg.imeji.logic.security.authentication.factory.AuthenticationFactory;
import de.mpg.imeji.logic.storage.Storage;
import de.mpg.imeji.logic.util.StorageUtils;
import de.mpg.imeji.logic.util.StringHelper;
import de.mpg.imeji.logic.util.TempFileUtil;
import de.mpg.imeji.presentation.session.SessionBean;

/**
 * The Servlet to Read files from imeji {@link Storage}
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
@WebServlet(urlPatterns = "/uploadServlet", asyncSupported = true, loadOnStartup = 5)
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
    private File file;
    private String filename;
    private Map<String, String> params = new HashMap<String, String>();

    /**
     * @param file the file to set
     */
    public void setFile(File file) {
      this.file = file;
    }

    /**
     * @param filename the filename to set
     */
    public void setFilename(String filename) {
      this.filename = filename;
    }

    public File getFile() {
      return file;
    }

    public String getFilename() {
      return filename;
    }

    /**
     * @return the params
     */
    public Map<String, String> getParams() {
      return params;
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
    final String uploadId = req.getParameter("uploadId");
    final License license = getLicense(upload);
    try {
      final User user = getUser(req, session);
      final CollectionImeji col = retrieveCollection(req, user);
      if (!StringHelper.isNullOrEmptyTrim(uploadId)) {
        itemService.uploadToStaging(uploadId, upload.getFile(), upload.getFilename(), col, user);
      } else {
        Item item = ImejiFactory.newItem(col);
        item.setLicenses(Arrays.asList(license));
        itemService.createWithFile(item, upload.getFile(), upload.getFilename(), col, user);
      }
      writeResponse(resp, "");
    } catch (final AuthenticationError e) {
      writeResponse(resp, e.getMessage());
      resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
    } catch (final ImejiException e) {
      LOGGER.error("Error uploading File", e);
      writeResponse(resp, e.getMessage());
      resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }
  }

  /**
   * Create a Licence from the request
   * 
   * @param req
   * @return
   * @throws ServletException
   * @throws IOException
   */
  private License getLicense(UploadItem uploadItem) throws IOException, ServletException {
    final License license = new License();
    license.setLabel(uploadItem.getParams().get("licenseLabel"));
    license.setName(uploadItem.getParams().get("licenseName"));
    license.setUrl(uploadItem.getParams().get("licenseUrl"));
    license.setStart(System.currentTimeMillis());
    return license;
  }

  private void writeResponse(HttpServletResponse resp, String errorMessage) throws IOException {
    resp.getOutputStream()
        .write(("<error>" + errorMessage + "</error>").getBytes(Charset.forName("UTF-8")));
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
      UploadItem uploadItem = new UploadItem();
      while (iter.hasNext()) {
        final FileItemStream fis = iter.next();
        if (!fis.isFormField()) {
          uploadItem.setFilename(fis.getName());
          final File tmp = TempFileUtil.createTempFile("upload", null);
          StorageUtils.writeInOut(fis.openStream(), new FileOutputStream(tmp), true);
          uploadItem.setFile(tmp);
        } else {
          ByteArrayOutputStream out = new ByteArrayOutputStream();
          StorageUtils.writeInOut(fis.openStream(), out, true);
          uploadItem.getParams().put(fis.getFieldName(), out.toString("UTF-8"));
        }
      }
      return uploadItem;
    } catch (final Exception e) {
      LOGGER.error("Error file upload", e);
    }
    return new UploadItem();
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
