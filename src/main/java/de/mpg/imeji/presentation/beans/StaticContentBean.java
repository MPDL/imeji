/**
 * License: src/main/resources/license/escidoc.license
 */
package de.mpg.imeji.presentation.beans;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.net.URL;

import de.mpg.imeji.logic.Imeji;
import de.mpg.imeji.logic.config.util.PropertyReader;

/**
 * JavaBean to read static content externally stored and to display it in imeji
 *
 * @author saquet
 */
public class StaticContentBean {

  /**
   * Read the URL of the logo from the imeji.preporties and return an CSS snippet
   *
   * @return
   */
  public String getHeaderLogo() {
    try {
      String html = "background-image: url( " + PropertyReader.getProperty("imeji.logo.url") + ");";
      return html;
    } catch (Exception e) {
      return " ";
    }
  }

  /**
   * Read the link to use hover the logo from the imeji.propertis.
   *
   * @return
   */
  public String getLogoLink() {
    try {
      return PropertyReader.getProperty("imeji.logo.link.url");
    } catch (Exception e) {
      return "#";
    }
  }

  /**
   * Get the HTML content of the Help page. URL of the Help page is defined in properties.
   *
   * @return
   * @throws URISyntaxException
   * @throws IOException
   */
  public String getHelpContent() throws IOException, URISyntaxException {
    String html = "";
    try {
      String helpProp = Imeji.CONFIG.getHelpUrl();
      String supportEmail = Imeji.CONFIG.getContactEmail();
      html = getContent(new URL(helpProp));
      html = html.replaceAll("XXX_SUPPORT_EMAIL_XXX", supportEmail);
    } catch (Exception e) {
      html =
          Imeji.CONFIG.getHelpUrl() + " couldn't be loaded. Url might be either wrong or protected."
              + "<br/><br/>" + "Error message:" + "<br/><br/>" + e.toString();
    }
    return html;
  }

  /**
   * Get the html content of an {@link URL}
   *
   * @param url
   * @return
   * @throws Exception
   */
  private String getContent(URL url) throws Exception {
    BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
    try {
      String inputLine = "";
      String content = "";
      while (inputLine != null) {
        inputLine = in.readLine();
        if (inputLine != null) {
          content += inputLine + "  ";
        }
      }
      return content;
    } finally {
      in.close();
    }
  }
}
