package de.mpg.imeji.rest.process;

import static com.google.common.base.Strings.isNullOrEmpty;
import static javax.ws.rs.core.Response.Status.OK;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.mpg.imeji.exceptions.BadRequestException;
import de.mpg.imeji.exceptions.UnprocessableError;
import de.mpg.imeji.logic.storage.StorageController;
import de.mpg.imeji.logic.storage.util.StorageUtils;
import de.mpg.imeji.logic.util.StringHelper;
import de.mpg.imeji.logic.util.TempFileUtil;
import de.mpg.imeji.logic.vo.User;
import de.mpg.imeji.rest.api.ItemAPIService;
import de.mpg.imeji.rest.to.JSONResponse;
import de.mpg.imeji.rest.to.defaultItemTO.DefaultItemTO;
import de.mpg.imeji.rest.to.defaultItemTO.DefaultItemWithFileTO;

/**
 * Process the REST requests for Items
 *
 * @author bastiens
 *
 */
public class ItemProcess {
  private static final Logger LOGGER = LoggerFactory.getLogger(ItemProcess.class);
  public static final String METADATA_KEY = "metadata";

  /**
   * Create an Item with its file
   *
   * @param req
   * @param file
   * @param json
   * @param origName
   * @return
   */
  public static JSONResponse createItem(HttpServletRequest req, InputStream file, String json,
      String origName) {
    try {
      final User u = BasicAuthentication.auth(req);
      DefaultItemWithFileTO defaultItemWithFileTO = (DefaultItemWithFileTO) RestProcessUtils
          .buildTOFromJSON(json, DefaultItemWithFileTO.class);
      defaultItemWithFileTO = uploadAndValidateFile(file, defaultItemWithFileTO, origName);
      final DefaultItemTO defaultItemTO = new ItemAPIService().create(defaultItemWithFileTO, u);
      return RestProcessUtils.buildResponse(Status.CREATED.getStatusCode(), defaultItemTO);
    } catch (final Exception e) {
      LOGGER.error("Error creating item", e);
      return RestProcessUtils.localExceptionHandler(e, e.getLocalizedMessage());
    }
  }

  /**
   * Read an Item according to its Id
   *
   * @param req
   * @param id
   * @return
   */
  public static JSONResponse readItem(HttpServletRequest req, String id) {
    final ItemAPIService service = new ItemAPIService();
    try {
      final User u = BasicAuthentication.auth(req);
      return RestProcessUtils.buildResponse(Status.OK.getStatusCode(), service.read(id, u));
    } catch (final Exception e) {
      return RestProcessUtils.localExceptionHandler(e, e.getLocalizedMessage());
    }
  }

  /**
   * Delete an Item according to its id
   *
   * @param req
   * @param id
   * @return
   */
  public static JSONResponse deleteItem(HttpServletRequest req, String id) {
    final ItemAPIService service = new ItemAPIService();
    try {
      final User u = BasicAuthentication.auth(req);
      service.delete(id, u);
      return RestProcessUtils.buildResponse(Status.NO_CONTENT.getStatusCode(), null);
    } catch (final Exception e) {
      return RestProcessUtils.localExceptionHandler(e, e.getLocalizedMessage());
    }
  }

  /**
   * Read Items according to the query
   *
   * @param req
   * @param q
   * @return
   */
  public static JSONResponse readItems(HttpServletRequest req, String q, int offset, int size) {
    final ItemAPIService service = new ItemAPIService();
    try {
      final User u = BasicAuthentication.auth(req);
      return RestProcessUtils.buildResponse(OK.getStatusCode(), service.search(q, offset, size, u));
    } catch (final Exception e) {
      return RestProcessUtils.localExceptionHandler(e, e.getLocalizedMessage());
    }
  }



  /**
   * Update an Item, update file if provided
   *
   * @param req
   * @param id
   * @param fileInputStream
   * @param json
   * @param filename
   * @return
   * @throws BadRequestException
   */
  public static JSONResponse updateItem(HttpServletRequest req, String id,
      InputStream fileInputStream, String json, String filename) {
    try {
      final ItemAPIService service = new ItemAPIService();
      DefaultItemWithFileTO to = (DefaultItemWithFileTO) RestProcessUtils.buildTOFromJSON(json,
          DefaultItemWithFileTO.class);
      validateId(id, to);
      to.setId(id);
      final boolean fileUpdate = !isNullOrEmpty(json) && (fileInputStream != null
          || json.indexOf("fetchUrl") > 0 || json.indexOf("referenceUrl") > 0);
      final User u = BasicAuthentication.auth(req);
      if (fileUpdate) {
        to = uploadAndValidateFile(fileInputStream, to, filename);
      }
      return RestProcessUtils.buildResponse(Status.OK.getStatusCode(), service.update(to, u));
    } catch (final Exception e) {
      LOGGER.error("Error updating item ", e);
      return RestProcessUtils.localExceptionHandler(e, e.getMessage());
    }
  }

  private static void validateId(String id, DefaultItemTO to) throws BadRequestException {
    if (!isNullOrEmpty(id) && !isNullOrEmpty(to.getId()) && !id.equals(to.getId())) {
      throw new BadRequestException(
          "Ambiguous item id: <" + id + "> in path; <" + to.getId() + "> in JSON");
    }
  }

  /**
   * Upload the File to imeji, and return a valid {@link DefaultItemWithFileTO}
   *
   * @param file
   * @param to
   * @param origName
   * @return
   * @throws IOException
   * @throws UnprocessableError
   * @throws BadRequestException
   */
  private static DefaultItemWithFileTO uploadAndValidateFile(InputStream file,
      DefaultItemWithFileTO to, String origName)
      throws IOException, UnprocessableError, BadRequestException {
    if (file != null) {
      final String calculatedFilename =
          StringHelper.isNullOrEmptyTrim(to.getFilename()) ? origName : to.getFilename();
      String calculatedExtension = FilenameUtils.getExtension(calculatedFilename);
      calculatedExtension = !isNullOrEmpty(calculatedExtension) ? "." + calculatedExtension : null;
      // Note: createTempFile suffix must be provided in order not to rename the file to .tmp
      final File tmp = TempFileUtil.createTempFile("imejiAPI", calculatedExtension);
      IOUtils.copy(file, new FileOutputStream(tmp));
      final StorageController c = new StorageController();
      // convenience call to stop the processing here asap. Item controller checks it anyway
      final String guessNotAllowedFormatUploaded = c.guessNotAllowedFormat(tmp);
      if (StorageUtils.BAD_FORMAT.equals(guessNotAllowedFormatUploaded)) {
        throw new UnprocessableError(
            "upload_format_not_allowed: " + " (" + calculatedExtension + ")");
      }
      to.setFile(tmp);
      to.setFilename(calculatedFilename);

      if (to.getFile() == null && isNullOrEmpty(to.getFetchUrl())
          && isNullOrEmpty(to.getReferenceUrl())) {
        throw new BadRequestException(
            "A file must be uploaded, referenced or fetched from external location.");
      }
    }
    return to;
  }


}
