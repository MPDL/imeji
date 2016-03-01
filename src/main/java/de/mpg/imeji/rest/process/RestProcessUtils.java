package de.mpg.imeji.rest.process;

import static com.google.common.base.Strings.isNullOrEmpty;

import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.log4j.Logger;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.google.common.io.ByteStreams;

import de.mpg.imeji.exceptions.AuthenticationError;
import de.mpg.imeji.exceptions.BadRequestException;
import de.mpg.imeji.exceptions.NotAllowedError;
import de.mpg.imeji.exceptions.NotFoundException;
import de.mpg.imeji.exceptions.UnprocessableError;
import de.mpg.imeji.exceptions.WorkflowException;
import de.mpg.imeji.rest.to.HTTPError;
import de.mpg.imeji.rest.to.JSONException;
import de.mpg.imeji.rest.to.JSONResponse;
import de.mpg.imeji.rest.to.SearchResultTO;
import de.mpg.imeji.rest.to.defaultItemTO.DefaultItemTO;

public class RestProcessUtils {

  private static final Logger LOGGER = Logger.getLogger(RestProcessUtils.class);

  /**
   * Parse a json file and construct a new Object of type T
   * 
   * @param json
   * @param type
   * @return
   */
  public static <T> Object buildTOFromJSON(String json, Class<T> type) throws BadRequestException {
    try {
      ObjectReader reader = new ObjectMapper().reader().withType(type);
      return reader.readValue(json);
    } catch (Exception e) {
      throw new BadRequestException("Cannot parse json: " + e.getLocalizedMessage());
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
  public static <T> T buildTOFromJSON(final String json, final TypeReference<T> type)
      throws BadRequestException {
    T data = null;
    try {
      data = new ObjectMapper().readValue(json, type);
    } catch (Exception e) {
      throw new BadRequestException("Cannot parse json: " + e.getLocalizedMessage());
    }
    return data;
  }


  public static JsonNode buildJsonNode(Object obj) {
    ObjectMapper mapper = new ObjectMapper();
    JsonFactory factory = mapper.getFactory();
    return mapper.convertValue(obj, JsonNode.class);
  }

  public static DefaultItemTO buildDefaultItemTOFromJSON(HttpServletRequest req)
      throws BadRequestException {
    DefaultItemTO easyTO = new DefaultItemTO();
    try {
      JsonFactory factory = new JsonFactory();
      ObjectMapper mapper = new ObjectMapper(factory);
      JsonNode rootNode = mapper.readTree(req.getInputStream());
      easyTO.setCollectionId(rootNode.path("collectionId").asText());

      String metadataText = rootNode.path("metadata").toString();
      rootNode = mapper.readTree(metadataText);
      Iterator<Map.Entry<String, JsonNode>> fieldsIterator = rootNode.fields();
      while (fieldsIterator.hasNext()) {
        Map.Entry<String, JsonNode> field = fieldsIterator.next();
        rootNode = mapper.readTree(field.getValue().toString());
        Iterator<Map.Entry<String, JsonNode>> fieldsIterator2 = rootNode.fields();
        while (fieldsIterator2.hasNext()) {
          Map.Entry<String, JsonNode> field2 = fieldsIterator2.next();
        }
      }

    } catch (Exception e) {
      LOGGER.error(e.getMessage());
    }
    return easyTO;

  }

  public static <T> Object buildTOFromJSON(HttpServletRequest req, Class<T> type)
      throws BadRequestException {
    ObjectReader reader = new ObjectMapper().reader().withType(type);
    try {
      return reader.readValue(req.getInputStream());
    } catch (Exception e) {
      throw new BadRequestException("Cannot parse json: " + e.getLocalizedMessage());
    }
  }

  public static <T> List<T> buildTOListFromJSON(String jsonSting, final Class<T> type)
      throws BadRequestException {
    ObjectReader reader = new ObjectMapper().reader()
        .withType(TypeFactory.defaultInstance().constructCollectionType(List.class, type));
    try {
      return reader.readValue(jsonSting);
    } catch (Exception e) {
      throw new BadRequestException("Cannot parse json: " + e.getLocalizedMessage());
    }
  }

  public static String buildJSONFromObject(Object obj) throws BadRequestException {
    ObjectWriter ow = new ObjectMapper().writer().with(SerializationFeature.INDENT_OUTPUT);
    try {
      return ow.writeValueAsString(obj);
    } catch (Exception e) {
      throw new BadRequestException("Cannot parse json: " + e.getLocalizedMessage());
    }
  }

  public static String buildJSONFromObject(Object obj, TypeReference<?> typeReference)
      throws BadRequestException {
    ObjectWriter ow =
        new ObjectMapper().writerWithType(typeReference).with(SerializationFeature.INDENT_OUTPUT);
    try {
      return ow.writeValueAsString(obj);
    } catch (Exception e) {
      throw new BadRequestException("Cannot parse json: " + e.getLocalizedMessage());
    }
  }

  public static Response buildJSONResponse(JSONResponse resp) {
    ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
    String json = "";
    try {
      json = ow.writeValueAsString(resp.getObject());
    } catch (JsonProcessingException e) {
      LOGGER.error("Have a JSON Processing Exception during building JSON Response", e);
    }
    return Response.status(resp.getStatus()).entity(json).type(MediaType.APPLICATION_JSON).build();
  }

  public static Object buildExceptionResponse(int errorCode, String e) {
    JSONException ex = new JSONException();
    HTTPError error = new HTTPError();
    String errorCodeLocal = "1" + errorCode;
    error.setCode(errorCodeLocal);
    String errorTitleLocal = "";
    Status localStatus = Status.fromStatusCode(errorCode);
    if (localStatus != null) {
      errorTitleLocal = localStatus.getReasonPhrase();
    } else {
      if (errorCode == HttpStatus.SC_UNPROCESSABLE_ENTITY) {
        errorTitleLocal = "Unprocessable entity";
      } else {
        errorTitleLocal = Status.INTERNAL_SERVER_ERROR.getReasonPhrase();
      }
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
    JSONResponse resp = new JSONResponse();
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
    JSONResponse resp = new JSONResponse();
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
    String localMessage = message;
    JSONResponse resp;

    if (eX instanceof AuthenticationError) {
      resp = RestProcessUtils.buildJSONAndExceptionResponse(Status.UNAUTHORIZED.getStatusCode(),
          localMessage);
    } else if (eX instanceof NotAllowedError) {
      resp = RestProcessUtils.buildJSONAndExceptionResponse(Status.FORBIDDEN.getStatusCode(),
          localMessage);
    } else if (eX instanceof NotFoundException) {
      resp = RestProcessUtils.buildJSONAndExceptionResponse(Status.NOT_FOUND.getStatusCode(),
          localMessage);
    } else if (eX instanceof UnprocessableError) {
      resp = RestProcessUtils.buildJSONAndExceptionResponse(HttpStatus.SC_UNPROCESSABLE_ENTITY,
          localMessage);
    } else if (eX instanceof WorkflowException) {
      resp = RestProcessUtils.buildJSONAndExceptionResponse(HttpStatus.SC_UNPROCESSABLE_ENTITY,
          localMessage);
    } else if (eX instanceof InternalServerErrorException) {
      resp = RestProcessUtils.buildJSONAndExceptionResponse(
          Status.INTERNAL_SERVER_ERROR.getStatusCode(), localMessage);
    } else if (eX instanceof BadRequestException) {
      resp = RestProcessUtils.buildJSONAndExceptionResponse(Status.BAD_REQUEST.getStatusCode(),
          localMessage);
    } else if (eX instanceof ClassCastException) {
      resp = RestProcessUtils.buildJSONAndExceptionResponse(Status.BAD_REQUEST.getStatusCode(),
          localMessage);
    } else {
      resp = RestProcessUtils.buildJSONAndExceptionResponse(
          Status.INTERNAL_SERVER_ERROR.getStatusCode(), localMessage);
    }

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
    ObjectMapper mapper = new ObjectMapper();
    return mapper.readValue(ByteStreams.toByteArray(response.readEntity(InputStream.class)),
        Map.class);
  }

  public static Map<String, Object> jsonToPOJO(String str) throws IOException, BadRequestException {
    try {
      ObjectMapper mapper = new ObjectMapper();
      return mapper.readValue(str, Map.class);
    } catch (Exception e) {
      throw new BadRequestException("Cannot parse json: " + e.getLocalizedMessage());
    }
  }
}
