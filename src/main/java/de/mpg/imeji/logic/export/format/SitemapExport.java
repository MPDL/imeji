/**
 * License: src/main/resources/license/escidoc.license
 */
package de.mpg.imeji.logic.export.format;

import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.net.URI;

import org.apache.log4j.Logger;

import de.mpg.imeji.logic.export.Export;
import de.mpg.imeji.logic.search.SearchResult;
import de.mpg.imeji.presentation.beans.Navigation;
import de.mpg.imeji.presentation.util.BeanHelper;

/**
 * {@link Export} into SiteMap Format
 * 
 * @author saquet
 */
public class SitemapExport extends Export {
  private double priority = 0.5;
  private static final Logger LOGGER = Logger.getLogger(SitemapExport.class);

  @Override
  public void init() {
    readPriority();
  }

  @Override
  public void export(OutputStream out, SearchResult sr) {
    StringWriter writer = new StringWriter();
    writer.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
    writer
        .append("<urlset xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\""
            + " xsi:schemaLocation=\"http://www.sitemaps.org/schemas/sitemap/0.9 http://www.sitemaps.org/schemas/sitemap/0.9/sitemap.xsd\""
            + " xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">");
    writeURLs(writer, sr);
    writer.append("</urlset>");
    try {
      out.write(writer.getBuffer().toString().getBytes());
    } catch (IOException e) {
      LOGGER.info("Some problems with exporting Sitemap!", e);
    }
  }

  @Override
  public String getContentType() {
    return "application/xml";
  }

  private void writeURLs(StringWriter writer, SearchResult sr) {
    if (sr != null) {
      for (String url : sr.getResults()) {
        writeURL(writer, url);
      }
    }
  }

  private void writeURL(StringWriter writer, String url) {
    writer.append("<url>");
    writer.append("<loc>" + getReaUrl(url) + "</loc>");
    writer.append("<priority>" + priority + "</priority>");
    writer.append("</url>");
  }

  private void readPriority() {
    String p = getParam("priority");
    if (p != null) {
      priority = Double.parseDouble(p);
    }
  }

  private String getReaUrl(String url) {
    Navigation navigation = (Navigation) BeanHelper.getApplicationBean(Navigation.class);
    URI uri = URI.create(url);
    return navigation.getApplicationUri() + uri.getPath();
  }
}
