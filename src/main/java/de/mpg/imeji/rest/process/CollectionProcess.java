package de.mpg.imeji.rest.process;

import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.NO_CONTENT;
import static javax.ws.rs.core.Response.Status.OK;

import javax.servlet.http.HttpServletRequest;

import de.mpg.imeji.exceptions.BadRequestException;
import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.vo.CollectionImeji;
import de.mpg.imeji.logic.vo.User;
import de.mpg.imeji.rest.api.CollectionService;
import de.mpg.imeji.rest.to.CollectionTO;
import de.mpg.imeji.rest.to.JSONResponse;

public class CollectionProcess {

  public static JSONResponse readCollection(HttpServletRequest req, String id) {
    JSONResponse resp;

    User u = null;

    CollectionService ccrud = new CollectionService();
    try {
      u = BasicAuthentication.auth(req);
      resp = RestProcessUtils.buildResponse(OK.getStatusCode(), ccrud.read(id, u));
    } catch (Exception e) {
      resp = RestProcessUtils.localExceptionHandler(e, e.getLocalizedMessage());
    }
    return resp;

  }

  /**
   * Read the items of a {@link CollectionImeji} according to the search query
   *
   * @param req
   * @param id
   * @param q
   * @return
   */
  public static JSONResponse readCollectionItems(HttpServletRequest req, String id, String q,
      int offset, int size) {
    JSONResponse resp = null;
    User u = null;
    CollectionService ccrud = new CollectionService();
    try {
      u = BasicAuthentication.auth(req);
      resp = RestProcessUtils.buildResponse(OK.getStatusCode(),
          ccrud.readItems(id, u, q, offset, size));
    } catch (Exception e) {
      resp = RestProcessUtils.localExceptionHandler(e, e.getLocalizedMessage());
    }
    return resp;
  }

  public static JSONResponse createCollection(HttpServletRequest req) {
    JSONResponse resp;

    CollectionService service = new CollectionService();
    try {
      User u = BasicAuthentication.auth(req);
      CollectionTO to = (CollectionTO) RestProcessUtils.buildTOFromJSON(req, CollectionTO.class);
      resp = RestProcessUtils.buildResponse(CREATED.getStatusCode(), service.create(to, u));
    } catch (ImejiException e) {
      resp = RestProcessUtils.localExceptionHandler(e, e.getLocalizedMessage());
    }
    return resp;
  }

  public static JSONResponse updateCollection(HttpServletRequest req, String id) {
    JSONResponse resp;

    CollectionService service = new CollectionService();
    try {
      User u = BasicAuthentication.auth(req);
      CollectionTO to = (CollectionTO) RestProcessUtils.buildTOFromJSON(req, CollectionTO.class);
      if (!id.equals(to.getId())) {
        throw new BadRequestException("Collection id is not equal in request URL and in json");
      }
      resp = RestProcessUtils.buildResponse(OK.getStatusCode(), service.update(to, u));
    } catch (ImejiException e) {
      resp = RestProcessUtils.localExceptionHandler(e, e.getLocalizedMessage());
    }

    return resp;
  }

  public static JSONResponse releaseCollection(HttpServletRequest req, String id) {
    JSONResponse resp;

    CollectionService service = new CollectionService();

    try {
      User u = BasicAuthentication.auth(req);
      resp = RestProcessUtils.buildResponse(OK.getStatusCode(), service.release(id, u));
    } catch (Exception e) {
      resp = RestProcessUtils.localExceptionHandler(e, e.getLocalizedMessage());
    }
    return resp;
  }


  public static JSONResponse withdrawCollection(HttpServletRequest req, String id,
      String discardComment) throws Exception {
    JSONResponse resp;

    CollectionService service = new CollectionService();

    try {
      User u = BasicAuthentication.auth(req);
      resp = RestProcessUtils.buildResponse(OK.getStatusCode(),
          service.withdraw(id, u, discardComment));
    } catch (Exception e) {
      resp = RestProcessUtils.localExceptionHandler(e, e.getLocalizedMessage());
    }
    return resp;
  }

  public static JSONResponse deleteCollection(HttpServletRequest req, String id) {
    JSONResponse resp;
    CollectionService service = new CollectionService();
    try {
      User u = BasicAuthentication.auth(req);
      resp = RestProcessUtils.buildResponse(NO_CONTENT.getStatusCode(), service.delete(id, u));
    } catch (Exception e) {
      resp = RestProcessUtils.localExceptionHandler(e, e.getLocalizedMessage());
    }
    return resp;
  }

  public static JSONResponse readAllCollections(HttpServletRequest req, String q, int offset,
      int size) {
    JSONResponse resp;
    CollectionService ccrud = new CollectionService();
    try {
      User u = BasicAuthentication.auth(req);
      resp = RestProcessUtils.buildResponse(OK.getStatusCode(), ccrud.search(q, offset, size, u));
    } catch (Exception e) {
      resp = RestProcessUtils.localExceptionHandler(e, e.getLocalizedMessage());
    }
    return resp;
  }
}
