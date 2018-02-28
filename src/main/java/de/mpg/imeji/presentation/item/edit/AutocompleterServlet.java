package de.mpg.imeji.presentation.item.edit;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URLEncoder;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import de.mpg.imeji.exceptions.UnprocessableError;
import de.mpg.imeji.logic.config.Imeji;
import de.mpg.imeji.logic.config.util.ProxyHelper;
import de.mpg.imeji.logic.model.Organization;
import de.mpg.imeji.logic.model.Person;
import de.mpg.imeji.logic.model.SearchFields;
import de.mpg.imeji.logic.model.User;
import de.mpg.imeji.logic.search.factory.SearchFactory;
import de.mpg.imeji.logic.search.model.SearchPair;
import de.mpg.imeji.logic.search.model.SearchLogicalRelation.LOGICAL_RELATIONS;
import de.mpg.imeji.logic.security.user.UserService;
import de.mpg.imeji.logic.util.StorageUtils;

/**
 * Servlet implementation class autocompleter
 */
@WebServlet(
    description = "act as bridge for front javascript query since javascript cannot query cross domain, e.g., from imeji to google",
    urlPatterns = {"/autocompleter"}, asyncSupported = true)
public class AutocompleterServlet extends HttpServlet {
  private static final long serialVersionUID = -5503792080963195242L;
  private static final Logger LOGGER = Logger.getLogger(AutocompleterServlet.class);
  private final Pattern conePattern =
      Pattern.compile("http.*/cone/.*?format=json.*", Pattern.CASE_INSENSITIVE);
  private final Pattern coneAuthorPattern =
      Pattern.compile("http.*/cone/persons/.*?format=json.*", Pattern.CASE_INSENSITIVE);
  private final Pattern googleGeoAPIPattern = Pattern.compile(
      "https://maps.googleapis.com/maps/api/geocode/json.*address=", Pattern.CASE_INSENSITIVE);
  private final HttpClient client = new HttpClient(new MultiThreadedHttpConnectionManager());

  /**
   * @see HttpServlet#HttpServlet()
   */
  public AutocompleterServlet() {
    super();
  }

  /**
   * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
   */
  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    String suggest = request.getParameter("searchkeyword");
    final String datasource = request.getParameter("datasource");
    String responseString = "";
    if (suggest == null || suggest.isEmpty()) {
      suggest = "a";
    } else if (datasource != null && !datasource.isEmpty()) {
      if ("imeji_persons".equals(datasource)) {
        responseString = autoCompleteForInternalUsers(suggest);
      } else if ("imeji_orgs".equals(datasource)) {
        responseString = autoCompleteForInternalOrganisations(suggest);
      } else {
        final GetMethod getMethod =
            new GetMethod(datasource + URLEncoder.encode(suggest.toString(), "UTF-8"));
        try {
          ProxyHelper.executeMethod(client, getMethod);
          responseString =
              new String(StorageUtils.toBytes(getMethod.getResponseBodyAsStream()), "UTF-8");
          if (datasource != null && responseString != null) {
            responseString = parseResult(responseString, datasource);
          }
        } catch (final Exception e) {
          LOGGER.error("Error doing autocaompletion", e);
        } finally {
          getMethod.releaseConnection();
        }
      }

    }
    response.setContentType("application/json");
    response.setCharacterEncoding("UTF-8");
    final PrintWriter out = response.getWriter();
    try {
      out.print(responseString);
    } finally {
      out.flush();
      out.close();
    }
  }

  /**
   * Autocomplete for imeji users
   * 
   * @param suggest
   * @return
   */
  private String autoCompleteForInternalUsers(String suggest) {
    final UserService uc = new UserService();
    String responseString = "";
    try {
      Collection<User> users = uc.searchAndRetrieveLazy(new SearchFactory()
          .addElement(new SearchPair(SearchFields.family, suggest + "*"), LOGICAL_RELATIONS.AND)
          .build(), null, Imeji.adminUser, 0, 5);
      final Collection<Person> persons =
          users.stream().map(u -> u.getPerson()).collect(Collectors.toList());
      for (final Person p : persons) {
        responseString = appendResponseForInternalSuggestion(responseString,
            p.getCompleteName() + "(" + p.getOrganizationString() + ")", p.getId().toString());
      }
      return "[" + responseString + "]";
    } catch (UnprocessableError e) {
      LOGGER.error("Error doing autosuggest for imeji users");
    }
    return "[]";
  }

  /**
   * Autocomplete for internal orgs
   * 
   * @param suggest
   * @return
   */
  private String autoCompleteForInternalOrganisations(String suggest) {
    final UserService uc = new UserService();
    final Collection<Organization> orgs = uc.searchOrganizationByName(suggest);
    String responseString = "";
    for (final Organization o : orgs) {
      responseString =
          appendResponseForInternalSuggestion(responseString, o.getName(), o.getId().toString());
    }
    return "[" + responseString + "]";
  }

  private String appendResponseForInternalSuggestion(String response, String label, String value) {
    if (!"".equals(response)) {
      response += ",";
    }
    response += "{";
    response += "\"label\": \"" + label + "\",";
    response += "\"value\" : \"";
    response += value;
    response += "\"}";
    return response;
  }

  /**
   * parse JSON string returned from remote source by JSON-simple add properties [ { label:
   * "Choice1", value: "value1" }, ... ] to fit JQuery UI auto-complete pop format
   *
   * @param input
   * @param source
   * @return
   * @throws IOException
   */
  private String parseResult(String input, String source) throws IOException {
    if (coneAuthorPattern.matcher(source).matches()) {
      return parseConeAuthor(input);
    } else if (conePattern.matcher(source).matches()) {
      parseConeVocabulary(input);
    } else if (googleGeoAPIPattern.matcher(source).matches()) {
      return parseGoogleGeoAPI(input);
    }
    return input;
  }

  /**
   * Parse a CoNE Vocabulary (read the title value)
   *
   * @param cone
   * @return
   * @throws IOException
   */
  @SuppressWarnings("unchecked")
  private String parseConeVocabulary(String cone) throws IOException {
    final Object obj = JSONValue.parse(cone);
    final JSONArray array = (JSONArray) obj;
    final JSONArray result = new JSONArray();
    for (int i = 0; i < array.size(); ++i) {
      final JSONObject parseObject = (JSONObject) array.get(i);
      final JSONObject sendObject = new JSONObject();
      sendObject.put("label", parseObject.get("http_purl_org_dc_elements_1_1_title"));
      sendObject.put("value", parseObject.get("http_purl_org_dc_elements_1_1_title"));
      result.add(sendObject);
    }
    final StringWriter out = new StringWriter();
    result.writeJSONString(out);
    return out.toString();
  }

  /**
   * Parse a json input from Google Geo API
   *
   * @param google
   * @return
   * @throws IOException
   */
  @SuppressWarnings("unchecked")
  private String parseGoogleGeoAPI(String google) throws IOException {
    final JSONObject obj = (JSONObject) JSONValue.parse(google);
    final JSONArray array = (JSONArray) obj.get("results");
    final JSONArray result = new JSONArray();
    for (int i = 0; i < array.size(); ++i) {
      final JSONObject parseObject = (JSONObject) array.get(i);
      final JSONObject sendObject = new JSONObject();
      sendObject.put("label", parseObject.get("formatted_address"));
      sendObject.put("value", parseObject.get("formatted_address"));
      final JSONObject location =
          (JSONObject) ((JSONObject) parseObject.get("geometry")).get("location");
      sendObject.put("latitude", location.get("lat"));
      sendObject.put("longitude", location.get("lng"));
      result.add(sendObject);
    }
    final StringWriter out = new StringWriter();
    result.writeJSONString(out);
    return out.toString();
  }

  /**
   * Parse a JSON file from CoNE with authors, and return a JSON which can be read by imeji
   * autocomplete
   *
   * @param cone
   * @return
   * @throws IOException
   */
  @SuppressWarnings("unchecked")
  private String parseConeAuthor(String cone) throws IOException {
    final Object obj = JSONValue.parse(cone);
    final JSONArray array = (JSONArray) obj;
    final JSONArray result = new JSONArray();
    Set<String> identifierSet = new HashSet<>();
    for (int i = 0; i < array.size(); ++i) {
      final JSONObject parseObject = (JSONObject) array.get(i);
      final JSONObject sendObject = new JSONObject();
      if (!identifierSet.contains(parseObject.get("id").toString())) {
        sendObject.put("label", parseObject.get("http_purl_org_dc_elements_1_1_title"));
        sendObject.put("value", parseObject.toJSONString());
        sendObject.put("family", parseObject.get("http_xmlns_com_foaf_0_1_family_name"));
        sendObject.put("givenname", parseObject.get("http_xmlns_com_foaf_0_1_givenname"));
        sendObject.put("id", parseObject.get("id"));
        sendObject.put("organization", parseConeAuthorOrgs(JSONValue
            .toJSONString(parseObject.get("http_purl_org_escidoc_metadata_terms_0_1_position"))));
        identifierSet.add(parseObject.get("id").toString());
        result.add(sendObject);
      }
    }
    final StringWriter out = new StringWriter();
    JSONArray.writeJSONString(result, out);
    return out.toString();
  }

  private String parseConeAuthorOrgs(String json) throws IOException {
    Object obj = JSONValue.parse(json);
    if (obj instanceof JSONObject) {
      return ((JSONObject) obj).get("http_purl_org_eprint_terms_affiliatedInstitution").toString();
    } else if (obj instanceof JSONArray) {
      JSONObject jsonObj = (JSONObject) ((JSONArray) obj).get(0);
      return jsonObj.get("http_purl_org_eprint_terms_affiliatedInstitution").toString();
    }
    return "";
  }

  /**
   * Read a JSON Object as a String, whether it is an {@link JSONArray}, a {@link String} or a
   * {@link JSONObject}
   *
   * @param jsonObj
   * @param jsonName
   * @return
   */
  private String writeJsonArrayToOneString(Object jsonObj, String jsonName) {
    String str = "";
    if (jsonObj instanceof JSONArray) {
      for (final Iterator<?> iterator = ((JSONArray) jsonObj).iterator(); iterator.hasNext();) {
        if (!"".equals(str)) {
          str += ", ";
        }
        str += writeJsonArrayToOneString(iterator.next(), jsonName);
      }
    } else if (jsonObj instanceof JSONObject) {
      str = (String) ((JSONObject) jsonObj).get(jsonName);
    } else if (jsonObj instanceof String) {
      str = (String) jsonObj;
    }
    return str;
  }
}
