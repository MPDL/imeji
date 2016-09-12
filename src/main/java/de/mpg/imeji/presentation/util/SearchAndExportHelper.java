/**
 * License: src/main/resources/license/escidoc.license
 */
package de.mpg.imeji.presentation.util;

import java.net.URI;
import java.net.URLEncoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;

import de.mpg.imeji.logic.config.util.ProxyHelper;
import de.mpg.imeji.logic.util.UrlHelper;
import de.mpg.imeji.logic.vo.predefinedMetadata.Publication;

/**
 * Utility class to work with PubMan search and export interface
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public class SearchAndExportHelper {
  /**
   * {@link Pattern} to find an escidoc id
   */
  private static Pattern ESCIDOC_ID_PATTERN = Pattern.compile("escidoc:[0-9]+");

  /**
   * Private Constructor
   */
  private SearchAndExportHelper() {}

  /**
   * Used the Search and export interface to return the styled citation of the current
   * {@link Publication}
   *
   * @param publication
   * @return
   */
  public static String getCitation(URI uri, String format) {
    if (uri != null) {
      URI searchAndExportUri = URI.create("http://" + uri.getHost() + "/search/SearchAndExport");
      String itemId = null;
      Matcher matcher = ESCIDOC_ID_PATTERN.matcher(uri.toString());
      if (matcher.find()) {
        itemId = matcher.group();
      }
      if (UrlHelper.isValidURL(searchAndExportUri.toString()) && itemId != null) {
        try {
          HttpClient client = new HttpClient();
          String exportUri = searchAndExportUri.toString() + "?cqlQuery="
              + URLEncoder.encode(
                  "escidoc.objid=" + itemId + " or escidoc.property.version.objid=" + itemId,
                  "UTF-8")
              + "&exportFormat=" + format + "&outputFormat=html_linked";
          GetMethod method = new GetMethod(exportUri);
          // client.executeMethod(method);
          ProxyHelper.executeMethod(client, method);
          return method.getResponseBodyAsString();
        } catch (Exception e) {
          return null;
        }
      }
    }
    return null;
  }
}
