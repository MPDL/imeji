package de.mpg.imeji.presentation.storage;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
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
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.media.multipart.file.FileDataBodyPart;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.config.Imeji;
import de.mpg.imeji.logic.core.content.ContentService;
import de.mpg.imeji.logic.core.item.ItemService;
import de.mpg.imeji.logic.model.ContentVO;
import de.mpg.imeji.logic.model.Item;
import de.mpg.imeji.logic.model.Properties.Status;
import de.mpg.imeji.logic.storage.impl.InternalStorage;
import de.mpg.imeji.logic.util.ObjectHelper;
import de.mpg.imeji.presentation.session.SessionBean;

/**
 * Servlet to call Data viewer service
 *
 * @author saquet
 *
 */
@WebServlet("/dataviewer")
public class DataViewerServlet extends HttpServlet {
	private static final long serialVersionUID = -4602021617386831403L;
	private static final Logger LOGGER = LogManager.getLogger(DataViewerServlet.class);

	@Override
	public void init() throws ServletException {
		super.init();
		LOGGER.info("Data Viewer Servlet initialized");
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		try {
			final SessionBean sb = (SessionBean) req.getSession(false).getAttribute(SessionBean.class.getSimpleName());
			final Item item = new ItemService().retrieveLazy(ObjectHelper.getURI(Item.class, req.getParameter("id")),
					sb.getUser());
			final boolean isPublicItem = Status.RELEASED.equals(item.getStatus());

			final String fileExtensionName = FilenameUtils.getExtension(item.getFilename());
			String dataViewerUrl = "api/view";

			if (Imeji.CONFIG.getDataViewerUrl().endsWith("/")) {
				dataViewerUrl = Imeji.CONFIG.getDataViewerUrl() + dataViewerUrl;
			} else {
				dataViewerUrl = Imeji.CONFIG.getDataViewerUrl() + "/" + dataViewerUrl;
			}

			if (isPublicItem) {
				// if item is public, simply send the URL to the Data Viewer,
				// along with the fileExtensionName
				final ContentService contentService = new ContentService();
				final ContentVO content = contentService
						.retrieveLazy(contentService.findContentId(item.getId().toString()));
				resp.sendRedirect(viewGenericUrl(content.getOriginal(), fileExtensionName, dataViewerUrl));
			} else

			{
				// Assume always Data Viewer will return an HTML (as is in the
				// Data Viewer Default definition)
				resp.setContentType(MediaType.TEXT_HTML);
				resp.getWriter().append(viewGenericFile(item, fileExtensionName, dataViewerUrl));
			}

			// resp.getWriter().append("id" + id);
		} catch (final HttpResponseException he) {
			resp.sendError(he.getStatusCode(), he.getMessage());
		} catch (final Exception e) {
			LOGGER.error(e.getMessage(), e);
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Requested resource could not be visualized!");
		}
	}

	private String viewGenericFile(Item item, String fileType, String dataViewerServiceTargetURL)
			throws FileNotFoundException, IOException, URISyntaxException, ImejiException {

		// in any other case, download the temporary file and send it to the
		// data viewer
		final InternalStorage ist = new InternalStorage();
		final ContentService contentService = new ContentService();
		final ContentVO content = contentService.retrieveLazy(contentService.findContentId(item.getId().toString()));
		final File file = ist.read(content.getOriginal());

		// Data Viewer File Parameter is always named "file1" not filename
		final FileDataBodyPart filePart = new FileDataBodyPart("file1", file);

		final FormDataMultiPart multiPart = new FormDataMultiPart();
		multiPart.bodyPart(filePart);
		multiPart.field("mimetype", fileType);

		final Client client = ClientBuilder.newClient();
		final WebTarget target = client.target(dataViewerServiceTargetURL);

		final Response response = target.register(MultiPartFeature.class)
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

	private String viewGenericUrl(String originalUrl, String fileType, String dataViewerServiceTargetURL)
			throws FileNotFoundException, IOException, URISyntaxException {
		return dataViewerServiceTargetURL + "?" + "mimetype=" + fileType + "&url=" + originalUrl;
	}

}
