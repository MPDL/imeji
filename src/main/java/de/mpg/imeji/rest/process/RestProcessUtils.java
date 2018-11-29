package de.mpg.imeji.rest.process;

import static com.google.common.base.Strings.isNullOrEmpty;

import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.common.io.ByteStreams;

import de.mpg.imeji.exceptions.AuthenticationError;
import de.mpg.imeji.exceptions.BadRequestException;
import de.mpg.imeji.exceptions.NotAllowedError;
import de.mpg.imeji.exceptions.NotFoundException;
import de.mpg.imeji.exceptions.NotSupportedMethodException;
import de.mpg.imeji.exceptions.UnprocessableError;
import de.mpg.imeji.exceptions.WorkflowException;
import de.mpg.imeji.logic.util.StringHelper;
import de.mpg.imeji.rest.to.HTTPError;
import de.mpg.imeji.rest.to.JSONException;
import de.mpg.imeji.rest.to.JSONResponse;
import de.mpg.imeji.rest.to.SearchResultTO;
import de.mpg.imeji.rest.to.defaultItemTO.DefaultItemTO;

public class RestProcessUtils {

  private static final Logger LOGGER = LogManager.getLogger(RestProcessUtils.class);

  /**
   * Parse a json file and construct a new Object of type T
   *
   * @param json
   * @param type
   * @return
   */
  public static <T> Object buildTOFromJSON(String json, Class<T> type) throws BadRequestException {
    try {
      final ObjectReader reader = new ObjectMapper().reader().withType(type);
      return reader.readValue(json);
    } catch (final Exception e) {
      throw new BadRequestException("Cannot parse json: " + e.getLocalizedMessage(), e);
    }
  }

  /**
   * Parse a JSON to a parameterized type object. Usage:
   *
   * <pre>
   * buildTOFromJSON(json,new TypeReference{@literal<T>}(){})
   * </pre>
   *
   * For instance, to parse a {@link SearchResultTO} of {@link DefaultItemTO}, do:
   *
   * <pre>
   * buildTOFromJSON(json,new TypeReference{@literal<SearchResultTO<DefaultItemTO>>}(){})
   * </pre>
   *
   *
   * @param json
   * @param type
   * @return
   * @throws BadRequestException
   */
  public static <T> T buildTOFromJSON(final String json, final TypeReference<T> type) throws BadRequestException {
    T data = null;
    try {
      data = new ObjectMapper().readValue(json, type);
    } catch (final Exception e) {
      throw new BadRequestException("Cannot parse json: " + e.getLocalizedMessage(), e);
    }
    return data;
  }

  public static JsonNode buildJsonNode(Object obj) {
    final ObjectMapper mapper = new ObjectMapper();
    return mapper.convertValue(obj, JsonNode.class);
  }

  public static <T> Object buildTOFromJSON(HttpServletRequest req, Class<T> type) throws BadRequestException {
    final ObjectReader reader = new ObjectMapper().reader().withType(type);
    try {
      return reader.readValue(req.getInputStream());
    } catch (final Exception e) {
      throw new BadRequestException("Cannot parse json: ", e);
    }
  }

  public static String buildJSONFromObject(Object obj) throws BadRequestException {
    final ObjectWriter ow = new ObjectMapper().writer().with(SerializationFeature.INDENT_OUTPUT);
    try {
      return ow.writeValueAsString(obj);
    } catch (final Exception e) {
      throw new BadRequestException("Cannot parse json: ", e);
    }
  }

  public static Response buildJSONResponse(JSONResponse resp) {
    final ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
    String json = "";
    try {
      json = ow.writeValueAsString(resp.getObject());
    } catch (final JsonProcessingException e) {
      LOGGER.error("Have a JSON Processing Exception during building JSON Response", e);
    }
    return Response.status(resp.getStatus()).entity(json).type(MediaType.APPLICATION_JSON).build();
  }

  public static Object buildExceptionResponse(int errorCode, String e) {
    final JSONException ex = new JSONException();
    final HTTPError error = new HTTPError();
    final String errorCodeLocal = "1" + errorCode;
    error.setCode(errorCodeLocal);
    String errorTitleLocal = "";
    final Status localStatus = Status.fromStatusCode(errorCode);
    if (localStatus != null) {
      errorTitleLocal = localStatus.getReasonPhrase();
    } else {
      if (errorCode == HttpStatus.SC_UNPROCESSABLE_ENTITY) {
        errorTitleLocal = "Unprocessable entity";
      } else {
        errorTitleLocal = Status.INTERNAL_SERVER_ERROR.getReasonPhrase();
      }
    }
    if (!StringHelper.isNullOrEmptyTrim(e) && e.contains("id_error:")) {
      error.setId(e.split("id_error:")[1]);
      e = e.split("id_error:")[0];
    }

    error.setExceptionReport(e);
    error.setCode(errorCodeLocal);
    error.setTitle(errorTitleLocal);
    error.setMessage(errorCodeLocal + "-message");
    ex.setError(error);
    return ex;
  }

  /**
   * This method builds exception response. Based on the error Code, local title and message (which
   * can be localized through the Language Bundles) are built
   *
   * @param errorCode
   * @param e
   * @return
   */
  public static JSONResponse buildJSONAndExceptionResponse(int errorCode, String e) {
    final JSONResponse resp = new JSONResponse();
    resp.setStatus(errorCode);
    resp.setObject(buildExceptionResponse(errorCode, e));
    return resp;
  }

  /**
   * This method builds the response for successfully created, updated, deleted, released etc.
   * object. It is a convenience method to save 3 lines of code every time the HTTP Response needs
   * to be built after success
   *
   * @param statusCode
   * @param responseObject
   * @return
   */
  public static JSONResponse buildResponse(int statusCode, Object responseObject) {
    final JSONResponse resp = new JSONResponse();
    resp.setStatus(statusCode);
    resp.setObject(responseObject);
    return resp;
  }

  /**
   * This method checks the exception type and returns appropriate JSON Response with properly
   * set-up HTTP Code.
   *
   *
   * @param eX
   * @param message
   * @return
   */
  public static JSONResponse localExceptionHandler(Exception eX, String message) {
    if (isNullOrEmpty(message)) {
      message = eX.getLocalizedMessage();
    }
    final String localMessage = message;
    JSONResponse resp;

    if (eX instanceof AuthenticationError) {
      resp = RestProcessUtils.buildJSONAndExceptionResponse(Status.UNAUTHORIZED.getStatusCode(), localMessage);
    } else if (eX instanceof NotAllowedError) {
      resp = RestProcessUtils.buildJSONAndExceptionResponse(Status.FORBIDDEN.getStatusCode(), localMessage);
    } else if (eX instanceof NotFoundException) {
      resp = RestProcessUtils.buildJSONAndExceptionResponse(Status.NOT_FOUND.getStatusCode(), localMessage);
    } else if (eX instanceof UnprocessableError) {
      resp = RestProcessUtils.buildJSONAndExceptionResponse(HttpStatus.SC_UNPROCESSABLE_ENTITY, localMessage);
    } else if (eX instanceof WorkflowException) {
      resp = RestProcessUtils.buildJSONAndExceptionResponse(HttpStatus.SC_UNPROCESSABLE_ENTITY, localMessage);
    } else if (eX instanceof InternalServerErrorException) {
      resp = RestProcessUtils.buildJSONAndExceptionResponse(Status.INTERNAL_SERVER_ERROR.getStatusCode(), localMessage);
    } else if (eX instanceof BadRequestException) {
      resp = RestProcessUtils.buildJSONAndExceptionResponse(Status.BAD_REQUEST.getStatusCode(), localMessage);
    } else if (eX instanceof ClassCastException) {
      resp = RestProcessUtils.buildJSONAndExceptionResponse(Status.BAD_REQUEST.getStatusCode(), localMessage);
    } else if (eX instanceof NotSupportedMethodException) {
      resp = RestProcessUtils.buildJSONAndExceptionResponse(Status.METHOD_NOT_ALLOWED.getStatusCode(), localMessage);
    } else {
      resp = RestProcessUtils.buildJSONAndExceptionResponse(Status.INTERNAL_SERVER_ERROR.getStatusCode(), localMessage);
    }
    LOGGER.error("Error API:", eX);
    return resp;

  }

  public static String formatDate(Date d) {
    SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd");
    String output = f.format(d);
    f = new SimpleDateFormat("HH:mm:SS Z");
    output += "T" + f.format(d);
    return output;
  }

  public static Map<String, Object> jsonToPOJO(Response response) throws IOException {
    final ObjectMapper mapper = new ObjectMapper();
    return mapper.readValue(ByteStreams.toByteArray(response.readEntity(InputStream.class)), Map.class);
  }

  public static Map<String, Object> jsonToPOJO(String str) throws IOException, BadRequestException {
    try {
      final ObjectMapper mapper = new ObjectMapper();
      return mapper.readValue(str, Map.class);
    } catch (final Exception e) {
      throw new BadRequestException("Cannot parse json: ", e);
    }
  }
}
