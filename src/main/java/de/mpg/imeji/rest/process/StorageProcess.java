package de.mpg.imeji.rest.process;

import static com.google.common.base.Strings.isNullOrEmpty;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response.Status;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.exceptions.NotAllowedError;
import de.mpg.imeji.logic.events.MessageService;
import de.mpg.imeji.logic.model.User;
import de.mpg.imeji.logic.security.authorization.util.SecurityUtil;
import de.mpg.imeji.logic.storage.StorageController;
import de.mpg.imeji.rest.to.JSONResponse;
import de.mpg.imeji.rest.to.StorageTO;

/**
 * Created by vlad on 14.01.15.
 */
public class StorageProcess {

  public static JSONResponse getStorageProperties() {
    JSONResponse resp;
    final StorageTO sto = new StorageTO();

    try {
      final StorageController c = new StorageController();
      final String black = c.getFormatBlackList();
      if (!isNullOrEmpty(black)) {
        sto.setUploadBlackList(black);
      }
      final String white = c.getFormatWhiteList();
      if (!isNullOrEmpty(white)) {
        sto.setUploadWhiteList(white);
      }
      resp = RestProcessUtils.buildResponse(Status.OK.getStatusCode(), sto);
    } catch (final Exception e) {
      resp = RestProcessUtils.localExceptionHandler(e, e.getLocalizedMessage());
    }
    return resp;
  }

  /**
   * Return all messages as JSON
   * 
   * @return
   */
  public static JSONResponse getMessages(HttpServletRequest req) {
    try {
      final User u = BasicAuthentication.auth(req);
      if (!SecurityUtil.authorization().isSysAdmin(u)) {
        throw new NotAllowedError("Only for system adminsitrator");
      }
      return RestProcessUtils.buildResponse(Status.OK.getStatusCode(), new MessageService().readAll());
    } catch (ImejiException e) {
      return RestProcessUtils.localExceptionHandler(e, e.getLocalizedMessage());
    }
  }
}
