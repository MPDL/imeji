package de.mpg.imeji.presentation.servlet;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.io.FilenameUtils;
import org.apache.http.client.HttpResponseException;
import org.apache.log4j.Logger;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.media.multipart.file.FileDataBodyPart;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.Imeji;
import de.mpg.imeji.logic.controller.business.ItemBusinessController;
import de.mpg.imeji.logic.storage.impl.InternalStorage;
import de.mpg.imeji.logic.util.ObjectHelper;
import de.mpg.imeji.logic.vo.Item;
import de.mpg.imeji.logic.vo.Properties.Status;
import de.mpg.imeji.presentation.session.SessionBean;

/**
 * SErvlet to call Data viewer service
 *
 * @author saquet
 *
 */
public class DataViewerServlet extends HttpServlet {

  private static final long serialVersionUID = -4602021617386831403L;
  private static final Logger LOGGER = Logger.getLogger(DataViewerServlet.class);

  @Override
  public void init() throws ServletException {
    super.init();
    LOGGER.info("Data Viewer Servlet initialized");
  }

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    try {
      SessionBean sb =
          (SessionBean) req.getSession(false).getAttribute(SessionBean.class.getSimpleName());
      Item item = new ItemBusinessController()
          .retrieveLazy(ObjectHelper.getURI(Item.class, req.getParameter("id")), sb.getUser());
      boolean isPublicItem = Status.RELEASED.equals(item.getStatus());

      String fileExtensionName = FilenameUtils.getExtension(item.getFilename());
      String dataViewerUrl = "api/view";

      if (Imeji.CONFIG.getDataViewerUrl().endsWith("/")) {
        dataViewerUrl = Imeji.CONFIG.getDataViewerUrl() + dataViewerUrl;
      } else {
        dataViewerUrl = Imeji.CONFIG.getDataViewerUrl() + "/" + dataViewerUrl;
      }

      if (isPublicItem) {
        // if item is public, simply send the URL to the Data Viewer,
        // along with the fileExtensionName
        resp.sendRedirect(
            viewGenericUrl(item.getFullImageUrl().toString(), fileExtensionName, dataViewerUrl));
      } else

      {
        // Assume always Data Viewer will return an HTML (as is in the
        // Data Viewer Default definition)
        resp.setContentType(MediaType.TEXT_HTML);
        resp.getWriter().append(viewGenericFile(item, fileExtensionName, dataViewerUrl));
      }

      // resp.getWriter().append("id" + id);
    } catch (HttpResponseException he) {
      resp.sendError(he.getStatusCode(), he.getMessage());
    } catch (Exception e) {
      LOGGER.error(e.getMessage(), e);
      resp.sendError(HttpServletResponse.SC_BAD_REQUEST,
          "Requested resource could not be visualized!");
    }
  }

  private String viewGenericFile(Item item, String fileType, String dataViewerServiceTargetURL)
      throws FileNotFoundException, IOException, URISyntaxException, ImejiException {

    // in any other case, download the temporary file and send it to the
    // data viewer
    InternalStorage ist = new InternalStorage();
    File file = ist.read(item.getFullImageUrl().toString());

    // Data Viewer File Parameter is always named "file1" not filename
    FileDataBodyPart filePart = new FileDataBodyPart("file1", file);

    FormDataMultiPart multiPart = new FormDataMultiPart();
    multiPart.bodyPart(filePart);
    multiPart.field("mimetype", fileType);

    Client client = ClientBuilder.newClient();
    WebTarget target = client.target(dataViewerServiceTargetURL);

    Response response = target.register(MultiPartFeature.class)
        .request(MediaType.MULTIPART_FORM_DATA_TYPE, MediaType.TEXT_HTML_TYPE)
        .post(Entity.entity(multiPart, multiPart.getMediaType()));

    String theHTML = "";
    if (response.bufferEntity()) {
      theHTML = response.readEntity(String.class);
    }

    response.close();
    client.close();

    return theHTML;
  }

  private String viewGenericUrl(String originalUrl, String fileType,
      String dataViewerServiceTargetURL)
          throws FileNotFoundException, IOException, URISyntaxException {
    return dataViewerServiceTargetURL + "?" + "mimetype=" + fileType + "&url=" + originalUrl;
  }

}
