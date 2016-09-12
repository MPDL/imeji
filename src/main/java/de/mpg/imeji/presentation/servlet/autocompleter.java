package de.mpg.imeji.presentation.servlet;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URLEncoder;
import java.util.Collection;
import java.util.Iterator;
import java.util.regex.Pattern;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import de.mpg.imeji.logic.config.util.ProxyHelper;
import de.mpg.imeji.logic.storage.util.StorageUtils;
import de.mpg.imeji.logic.user.controller.UserBusinessController;
import de.mpg.imeji.logic.vo.Organization;
import de.mpg.imeji.logic.vo.Person;
import de.mpg.imeji.presentation.session.SessionBean;

/**
 * Servlet implementation class autocompleter
 */
@WebServlet(
    description = "act as bridge for front javascript query since javascript cannot query cross domain, e.g., from imeji to google",
    urlPatterns = {"/autocompleter"})
public class autocompleter extends HttpServlet {
  private static final long serialVersionUID = 1L;
  private Pattern conePattern =
      Pattern.compile("http.*/cone/.*?format=json.*", Pattern.CASE_INSENSITIVE);
  private Pattern coneAuthorPattern =
      Pattern.compile("http.*/cone/persons/.*?format=json.*", Pattern.CASE_INSENSITIVE);
  private Pattern googleGeoAPIPattern = Pattern.compile(
      "https://maps.googleapis.com/maps/api/geocode/json.*address=", Pattern.CASE_INSENSITIVE);
  private Pattern ccLicensePattern =
      Pattern.compile("http.*://api.creativecommons.org/rest/.*/simple/chooser.*");

  /**
   * @see HttpServlet#HttpServlet()
   */
  public autocompleter() {
    super();
  }

  /**
   * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
   */
  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    String suggest = request.getParameter("searchkeyword");
    String datasource = request.getParameter("datasource");

    String responseString = "";
    if (suggest == null || suggest.isEmpty()) {
      suggest = "a";
    } else if (datasource != null && !datasource.isEmpty()) {
      if ("imeji_persons".equals(datasource)) {
        UserBusinessController uc = new UserBusinessController();
        Collection<Person> persons = uc.searchPersonByName(suggest);
        for (Person p : persons) {
          responseString = appendResponseForInternalSuggestion(responseString,
              p.getCompleteName() + "(" + p.getOrganizationString() + ")", p.getId().toString());
        }
        responseString = "[" + responseString + "]";

      } else if ("imeji_orgs".equals(datasource)) {
        UserBusinessController uc = new UserBusinessController();
        Collection<Organization> orgs = uc.searchOrganizationByName(suggest);
        for (Organization o : orgs) {
          responseString = appendResponseForInternalSuggestion(responseString, o.getName(),
              o.getId().toString());
        }
        responseString = "[" + responseString + "]";

      } else {
        HttpClient client = new HttpClient();
        GetMethod getMethod =
            new GetMethod(datasource + URLEncoder.encode(suggest.toString(), "UTF-8"));
        try {
          // client.executeMethod(getMethod);
          ProxyHelper.executeMethod(client, getMethod);
          responseString =
              new String(StorageUtils.toBytes(getMethod.getResponseBodyAsStream()), "UTF-8");
          if (datasource != null && responseString != null) {
            responseString = passResult(responseString, datasource);
          }
        } catch (Exception e) {
          throw new RuntimeException(e);
        } finally {
          getMethod.releaseConnection();
        }
      }

    }
    response.setContentType("application/json");
    PrintWriter out = response.getWriter();
    try {
      out.print(responseString);
    } finally {
      out.flush();
      out.close();
    }
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
  private String passResult(String input, String source) throws IOException {
    if (conePattern.matcher(source).matches()) {
      if (coneAuthorPattern.matcher(source).matches()) {
        return parseConeAuthor(input);
      } else {
        return parseConeVocabulary(input);
      }
    } else if (googleGeoAPIPattern.matcher(source).matches()) {
      return parseGoogleGeoAPI(input);
    } else if (ccLicensePattern.matcher(source).matches()) {
      return parseCCLicense(input);
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
    Object obj = JSONValue.parse(cone);
    JSONArray array = (JSONArray) obj;
    JSONArray result = new JSONArray();
    for (int i = 0; i < array.size(); ++i) {
      JSONObject parseObject = (JSONObject) array.get(i);
      JSONObject sendObject = new JSONObject();
      sendObject.put("label", parseObject.get("http_purl_org_dc_elements_1_1_title"));
      sendObject.put("value", parseObject.get("http_purl_org_dc_elements_1_1_title"));
      result.add(sendObject);
    }
    StringWriter out = new StringWriter();
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
    JSONObject obj = (JSONObject) JSONValue.parse(google);
    JSONArray array = (JSONArray) obj.get("results");
    JSONArray result = new JSONArray();
    for (int i = 0; i < array.size(); ++i) {
      JSONObject parseObject = (JSONObject) array.get(i);
      JSONObject sendObject = new JSONObject();
      sendObject.put("label", parseObject.get("formatted_address"));
      sendObject.put("value", parseObject.get("formatted_address"));
      JSONObject location = (JSONObject) ((JSONObject) parseObject.get("geometry")).get("location");
      sendObject.put("latitude", location.get("lat"));
      sendObject.put("longitude", location.get("lng"));
      result.add(sendObject);
    }
    StringWriter out = new StringWriter();
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
    Object obj = JSONValue.parse(cone);
    JSONArray array = (JSONArray) obj;
    JSONArray result = new JSONArray();
    for (int i = 0; i < array.size(); ++i) {
      JSONObject parseObject = (JSONObject) array.get(i);
      JSONObject sendObject = new JSONObject();
      sendObject.put("label", parseObject.get("http_purl_org_dc_elements_1_1_title"));
      sendObject.put("value", parseObject.toJSONString());
      sendObject.put("family", parseObject.get("http_xmlns_com_foaf_0_1_family_name"));
      sendObject.put("givenname", parseObject.get("http_xmlns_com_foaf_0_1_givenname"));
      sendObject.put("id", parseObject.get("id"));
      sendObject.put("orgs", parseObject.get("http_purl_org_escidoc_metadata_terms_0_1_position"));
      sendObject.put("alternatives",
          writeJsonArrayToOneString(parseObject.get("http_purl_org_dc_terms_alternative"), ""));
      result.add(sendObject);
    }
    StringWriter out = new StringWriter();
    JSONArray.writeJSONString(result, out);
    return out.toString();
  }

  @SuppressWarnings("unchecked")
  private String parseCCLicense(String str) {
    str = "<licences>" + str.trim() + "</licences>";
    try {
      JSONArray json = new JSONArray();
      DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
      DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
      org.w3c.dom.Document doc = dBuilder.parse(new ByteArrayInputStream(str.getBytes()));
      doc.getDocumentElement().normalize();
      NodeList nList = doc.getElementsByTagName("option");
      for (int i = 0; i < nList.getLength(); i++) {
        Node nNode = nList.item(i);
        if (nNode.getNodeType() == Node.ELEMENT_NODE) {
          JSONObject license = new JSONObject();
          Element eElement = (Element) nNode;
          license.put("label", eElement.getTextContent());
          license.put("value", eElement.getTextContent());
          license.put("licenseId", eElement.getAttribute("value"));
          json.add(license);
        }
      }
      StringWriter out = new StringWriter();
      JSONArray.writeJSONString(json, out);
      return out.toString();
    } catch (Exception e) {
      log("Error Parsing CC License", e);
    }
    return str;
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
      for (Iterator<?> iterator = ((JSONArray) jsonObj).iterator(); iterator.hasNext();) {
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

  /**
   * Return the {@link SessionBean} form the {@link HttpSession}
   *
   * @param req
   * @return
   */
  private SessionBean getSession(HttpServletRequest req) {
    return (SessionBean) req.getSession(true).getAttribute(SessionBean.class.getSimpleName());
  }

}
