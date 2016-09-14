/**
 * License: src/main/resources/license/escidoc.license
 */
package de.mpg.imeji.logic.util;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;

import javax.faces.context.FacesContext;

import org.apache.commons.validator.routines.UrlValidator;
import org.apache.log4j.Logger;

/**
 * Some Method to read URLs
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public class UrlHelper {

  private static final Logger LOGGER = Logger.getLogger(UrlHelper.class);

  /**
   * Private Constructor
   */
  private UrlHelper() {
    // avoid constructor
  }

  /**
   * Return the value of a parameter in an url
   *
   * @param parameterName
   * @return
   */
  public static String getParameterValue(String parameterName) {
    return FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap()
        .get(parameterName);
  }

  /**
   * Return a value as boolean of a parameter in a url: true if value is1, false if value is -1
   *
   * @param parameterName
   * @return
   */
  public static boolean getParameterBoolean(String parameterName) {
    String str = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap()
        .get(parameterName);
    if ("1".equals(str)) {
      return true;
    }
    return false;
  }

  /**
   * Check if the URL is valid, i.e. well formed
   *
   * @param uri
   * @return
   */
  public static boolean isValidURL(String url) {
    return UrlValidator.getInstance().isValid(url);

  }

  /**
   * Add to the url the parameter
   *
   * @param url
   * @param param
   * @param value
   * @return
   */
  public static String addParameter(String url, String param, String value) {
    String[] params = url.split("\\?", 2);
    if (params.length > 1 && !"".equals(params[1])) {
      return url + "&" + param + "=" + value;
    }
    return url + "?" + param + "=" + value;
  }

  /**
   * Decode a value from with UTF-8
   *
   * @param s
   * @return
   */
  public static String decode(String s) {
    try {
      return URLDecoder.decode(s, "UTF-8");
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Encode a value with UTF-8
   *
   * @param s
   * @return
   */
  public static String encode(String s) {
    try {
      return URLEncoder.encode(s, "UTF-8");
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Encode url with query. Escape blanks with %20 etc.
   *
   * @param urlStr
   * @return
   */
  public static String encodeQuery(String urlStr) {
    try {
      URL url = new URL(urlStr);
      return new URI(url.getProtocol(), url.getUserInfo(), url.getHost(), url.getPort(),
          url.getPath(), url.getQuery(), url.getRef()).toURL().toString();
    } catch (MalformedURLException | URISyntaxException e) {
      LOGGER.info("Cannot parse url: " + urlStr + "\n" + e.getLocalizedMessage());
      return urlStr;
    }
  }

  /**
   * Return true or false about the existence of parameter in the Url
   *
   * @param parameterName
   * @return
   */
  public static Boolean hasParameter(String parameterName) {
    return FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap()
        .containsKey(parameterName);
  }

}
