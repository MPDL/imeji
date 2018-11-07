package de.mpg.imeji.presentation.storage;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import de.mpg.imeji.exceptions.AuthenticationError;
import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.exceptions.NotAllowedError;
import de.mpg.imeji.exceptions.NotFoundException;
import de.mpg.imeji.logic.core.content.ContentService;
import de.mpg.imeji.logic.model.ContentVO;
import de.mpg.imeji.logic.model.Item;
import de.mpg.imeji.logic.model.User;
import de.mpg.imeji.logic.security.authentication.factory.AuthenticationFactory;
import de.mpg.imeji.logic.storage.Storage;
import de.mpg.imeji.logic.storage.StorageController;
import de.mpg.imeji.logic.storage.impl.ExternalStorage;
import de.mpg.imeji.logic.util.ObjectHelper;
import de.mpg.imeji.logic.util.StorageUtils;
import de.mpg.imeji.logic.util.StringHelper;
import de.mpg.imeji.presentation.navigation.Navigation;
import de.mpg.imeji.presentation.session.SessionBean;

/**
 * The Servlet to Read files from imeji {@link Storage}
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
@WebServlet(urlPatterns = {"/file", "/file/*"}, asyncSupported = true)
public class FileServlet extends HttpServlet {
	private static final long serialVersionUID = 5502546330318540997L;
	private static final Logger LOGGER = LogManager.getLogger(FileServlet.class);
	private final StorageController storageController = new StorageController();
	private final ContentService contentController = new ContentService();
	private final ExternalStorage externalStorage = new ExternalStorage();
	private final Navigation navivation = new Navigation();
	private String domain;
	private final static int MAX_RANGE_LENGTH = 1000000;

	private static final String RESOURCE_EMTPY_ICON_URL = "http://localhost:8080/imeji/resources/icon/empty.png";

	@Override
	public void init() {
		try {
			domain = StringHelper.normalizeURI(navivation.getDomain());
			domain = domain.substring(0, domain.length() - 1);
			LOGGER.info("File Servlet initialized");
		} catch (final Exception e) {
			LOGGER.info("Error intializing File Servlet", e);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		String url = req.getParameter("id");
		final String contentId = req.getParameter("content");
		final String filename = req.getParameter("filename");
		final String range = req.getHeader("range");
		final String itemUri = req.getParameter("itemId") == null
				? req.getParameter("item")
				: ObjectHelper.getURI(Item.class, req.getParameter("itemId")).toString();
		User user;

		try {
			final SessionBean session = getSession(req);
			user = getUser(req, session);
			if (url == null && contentId != null) {
				url = retrieveUrlOfContent(contentId, req.getParameter("resolution"));
			} else if (url == null && itemUri != null) {
				url = retrieveUrlOfContent(new ContentService().findContentId(itemUri), req.getParameter("resolution"));
			} else if (url == null) {
				url = domain + req.getRequestURI();
			}
			long contentLength = (long) storageController.getStorage().getContentLenght(url);
			resp.setContentType(StorageUtils.getMimeType(StringHelper.getFileExtension(url)));
			resp.setHeader("Accept-Ranges", isFirefox(req) ? "none" : "bytes");
			resp.setContentLengthLong(contentLength);
			if (!StringHelper.isNullOrEmptyTrim(filename)) {
				resp.setHeader("content-disposition", "attachment; filename=\"" + filename + "\"");
			}
			if ("NO_THUMBNAIL_URL".equals(url)) {
				externalStorage.read(RESOURCE_EMTPY_ICON_URL, resp.getOutputStream(), true);
			} else if (range != null && !isFirefox(req)) {
				doRangeRequest(req, resp, user, range, url, contentLength);
			} else {
				readFile(url, resp, false, user);
			}
		} catch (final Exception e) {
			if (e instanceof NotAllowedError) {
				// resp.sendError(HttpServletResponse.SC_FORBIDDEN,
				// "imeji security: You are not allowed to view this file");
			} else if (e instanceof AuthenticationError) {
				// resp.sendError(HttpServletResponse.SC_UNAUTHORIZED,
				// "imeji security: You need to be signed-in to view this file.");
			} else if (e instanceof NotFoundException) {
				// resp.sendError(HttpServletResponse.SC_NOT_FOUND,
				// "The resource you are trying to retrieve does not exist!");
			} else {
				LOGGER.error(e.getMessage(), e);
				sendEmptyThumbnail(resp);
				if (!resp.isCommitted()) {
					// resp.sendError(422, "Unprocessable entity!");
				}
			}
		}
	}

	private boolean isFirefox(HttpServletRequest req) {
		return req.getHeader("User-Agent").contains("Firefox");
	}

	private String retrieveUrlOfContent(String contentId, String resolution) throws ImejiException {
		final ContentVO content = contentController.retrieveLazy(contentId);
		switch (resolution) {
			case "thumbnail" :
				return content.getThumbnail();
			case "preview" :
				return content.getPreview();
			case "full" :
				return content.getFull();
			default :
				return content.getOriginal();
		}
	}

	/**
	 * Send an empty thumbnail
	 *
	 * @param resp
	 */
	private void sendEmptyThumbnail(HttpServletResponse resp) {
		try {
			externalStorage.read(RESOURCE_EMTPY_ICON_URL, resp.getOutputStream(), true);
		} catch (ImejiException | IOException e) {
			LOGGER.error("Error reading default thumbnail", e);
		}
	}

	/**
	 * Read a File and write it back in the response
	 *
	 * @param url
	 * @param resp
	 * @param isExternalStorage
	 * @throws ImejiException
	 * @throws IOException
	 */
	private void readFile(String url, HttpServletResponse resp, boolean isExternalStorage, User user)
			throws ImejiException, IOException {
		if (isExternalStorage) {
			readExternalFile(url, resp);
		} else {
			readStorageFile(url, resp, user);
		}
	}

	/**
	 * Do a Range Request: Force the response to to smaller than MAX_RANGE_LENGTH
	 * 
	 * @param resp
	 * @param user
	 * @param range
	 * @param url
	 * @param contentLength
	 * @throws ImejiException
	 * @throws IOException
	 */
	private void doRangeRequest(HttpServletRequest req, HttpServletResponse resp, User user, String range, String url,
			long contentLength) throws ImejiException, IOException {
		// First parse request
		int rangeStart = parseRangeStart(range);
		int rangeEnd = parseRangeEnd(range);
		if (isFirefox(req)) {
			// Firefox doensn't support smaller part than requested part (for now, see
			// https://bugzilla.mozilla.org/show_bug.cgi?id=570755)
			resp.setContentLengthLong(contentLength - rangeStart);
			resp.setHeader("Content-Range",
					"bytes " + rangeStart + "-" + (rangeEnd > 0 ? rangeEnd : "" + "/" + contentLength));
		} else {
			// For all other browsers, set the rangeEnd according the MAX_RANGE_LENGTH, in
			// order to limit
			// the response size
			rangeEnd = rangeStart + MAX_RANGE_LENGTH;
			// If the rangeEnd is bigger than the content length, initialize the rangeEnd
			// with the
			// contentLength
			rangeEnd = (int) (rangeStart + MAX_RANGE_LENGTH > contentLength ? contentLength : rangeEnd) - 1;
			resp.setContentLengthLong(rangeEnd - rangeStart + 1);
			resp.setHeader("Content-Range",
					"bytes " + rangeStart + "-" + (rangeEnd > 0 ? rangeEnd : "") + "/" + contentLength);
		}
		resp.setStatus(206);
		readPartFile(url, resp, false, user, rangeStart, rangeEnd + 1 - rangeStart);
	}

	/**
	 * Read Firefox
	 * 
	 * @param url
	 * @param resp
	 * @param isExternalStorage
	 * @param user
	 * @param offset
	 * @param length
	 * @throws ImejiException
	 * @throws IOException
	 */
	private void readPartFile(String url, HttpServletResponse resp, boolean isExternalStorage, User user, int offset,
			int length) throws ImejiException, IOException {
		if (isExternalStorage) {
			readExternalFile(url, resp);
		} else {
			readStoragePartFile(url, resp, user, offset, length);
		}
	}

	private int parseRangeStart(String range) {
		return Integer.parseInt(range.replace("bytes=", "").trim().split("-")[0]);
	}

	private int parseRangeEnd(String range) {
		String[] ranges = range.replace("bytes=", "").trim().split("-");
		return ranges.length < 2 ? -1 : Integer.parseInt(ranges[1]);
	}

	/**
	 * Read a File from the current storage
	 *
	 * @param url
	 * @param resp
	 * @throws ImejiException
	 * @throws IOException
	 */
	private void readStorageFile(String url, HttpServletResponse resp, User user) throws ImejiException, IOException {
		checkSecurity(url, user);
		storageController.read(url, resp.getOutputStream(), false);
	}

	private void readStoragePartFile(String url, HttpServletResponse resp, User user, long offset, long length)
			throws ImejiException, IOException {
		checkSecurity(url, user);
		storageController.readPart(url, resp.getOutputStream(), false, offset, length);
	}

	/**
	 * Exeption if the user is not allowed to read the file
	 *
	 * @param url
	 * @param user
	 * @return
	 * @throws Exception
	 */
	private void checkSecurity(String url, User user) throws NotAllowedError {
		if (!StorageUtil.isAllowedToViewFile(url, user)) {
			throw new NotAllowedError("You are not allowed to read this file");
		}
	}

	/**
	 * Read an external (i.e not in the current storage) file
	 *
	 * @param url
	 * @param resp
	 * @throws ImejiException
	 * @throws IOException
	 */
	private void readExternalFile(String url, HttpServletResponse resp) throws ImejiException, IOException {
		externalStorage.read(url, resp.getOutputStream(), true);
	}

	/**
	 * Return the {@link User} of the request. Check first is a user is send with
	 * the request. If not, check in the the session.
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
	 * {@inheritDoc}
	 */
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		return;
	}
}
