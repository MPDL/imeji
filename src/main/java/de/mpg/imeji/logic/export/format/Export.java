/**
 * License: src/main/resources/license/escidoc.license
 */
package de.mpg.imeji.logic.export.format;

import java.io.OutputStream;
import java.io.StringWriter;
import java.util.Map;

import org.apache.http.client.HttpResponseException;

import de.mpg.imeji.logic.export.format.explain.ExplainExport;
import de.mpg.imeji.logic.export.format.rdf.RDFExport;
import de.mpg.imeji.logic.search.model.SearchResult;
import de.mpg.imeji.logic.vo.User;

/**
 * Export of data
 *
 * @author saquet
 */
public abstract class Export {
  private StringWriter writer;

  /**
   * The {@link User} doing the export
   */
  protected User user;
  /**
   * The params in the url ofr the export
   */
  private Map<String, String[]> params;

  /**
   * Export a {@link SearchResult} in an {@link OutputStream}
   *
   * @param out
   * @param sr
   *
   */
  public abstract void export(OutputStream out, SearchResult sr, User user);

  /**
   * Return the Mime-type of the http response
   *
   * @return
   */
  public abstract String getContentType();

  /**
   * Initialize the {@link Export}
   */
  public abstract void init();

  /**
   * Factory to create an {@link Export} from url paramters
   *
   * @param params
   * @return
   * @throws HttpResponseException
   */
  public static Export factory(Map<String, String[]> params) throws HttpResponseException {
    Export export = null;
    String format = getParam(params, "format");
    String type = getParam(params, "type");
    if (format == null || "".equals(format)) {
      export = RDFExport.factory(type);
    } else if ("rdf".equals(format)) {
      export = RDFExport.factory(type);
    } else if ("jena".equals(format)) {
      export = new JenaExport();
    } else if ("sitemap".equals(format)) {
      export = new SitemapExport();
    } else if ("zip".equals(format)) {
      export = new ZIPExport(type);
    } else if ("explain".equals(format)) {
      export = ExplainExport.factory(type);
    } else {
      throw new HttpResponseException(400, "Format " + format + " is not supported.");
    }
    export.setParams(params);
    export.init();
    return export;
  }

  /**
   * REturn the value of a paramter as it has been used for this export
   *
   * @param s
   * @return
   */
  public String getParam(String s) {
    return getParam(params, s);
  }

  /**
   * REturn the value of a Param as defined in a string array
   *
   * @param params
   * @param s
   * @return
   */
  public static String getParam(Map<String, String[]> params, String s) {
    String[] values = params.get(s);
    if (values != null) {
      return values[0];
    }
    return null;
  }

  /**
   * @return
   */
  public Map<String, String[]> getParams() {
    return params;
  }

  /**
   * @param params
   */
  public void setParams(Map<String, String[]> params) {
    this.params = params;
  }

  /**
   * @return
   */
  public User getUser() {
    return user;
  }

  /**
   * @param user
   */
  public void setUser(User user) {
    this.user = user;
  }
}
