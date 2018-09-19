package de.mpg.imeji.logic.export;

import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import de.mpg.imeji.exceptions.UnprocessableError;
import de.mpg.imeji.logic.config.Imeji;
import de.mpg.imeji.logic.core.collection.CollectionService;
import de.mpg.imeji.logic.model.User;
import de.mpg.imeji.logic.search.Search;
import de.mpg.imeji.logic.search.SearchQueryParser;
import de.mpg.imeji.logic.search.model.SearchResult;

/**
 * {@link ExportAbstract} into SiteMap Format
 *
 * @author saquet
 */
public class SitemapExport extends ExportAbstract {
  private final double priority;
  private static final Logger LOGGER = Logger.getLogger(SitemapExport.class);
  private final String query;

  public SitemapExport(String query, String priority, User user) {
    super(user);
    this.query = query;
    this.priority = priority != null ? Integer.parseInt(priority) : 0.5;
    this.name = "sitemap.xml";
  }

  @Override
  public void export(OutputStream out) throws UnprocessableError {
    final SearchResult sr = search(query);
    final StringWriter writer = new StringWriter();
    writer.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
    writer.append("<urlset xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\""
        + " xsi:schemaLocation=\"http://www.sitemaps.org/schemas/sitemap/0.9 http://www.sitemaps.org/schemas/sitemap/0.9/sitemap.xsd\""
        + " xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">");
    writeURLs(writer, sr);
    writer.append("</urlset>");
    try {
      out.write(writer.getBuffer().toString().getBytes());
    } catch (final IOException e) {
      LOGGER.info("Some problems with exporting Sitemap!", e);
    }
  }

  @Override
  public String getContentType() {
    return "application/xml";
  }

  /**
   * Search for collections
   * 
   * @param query
   * @return
   * @throws UnprocessableError
   */
  private SearchResult search(String query) throws UnprocessableError {
    return new CollectionService().search(SearchQueryParser.parseStringQuery(query), null, user, Search.GET_ALL_RESULTS, Search.SEARCH_FROM_START_INDEX);
  }

  private void writeURLs(StringWriter writer, SearchResult sr) {
    if (sr != null) {
      for (final String url : sr.getResults()) {
        writeURL(writer, url);
      }
    }
  }

  private void writeURL(StringWriter writer, String url) {
    writer.append("<url>");
    writer.append("<loc>" + getRealUrl(url) + "</loc>");
    writer.append("<priority>" + priority + "</priority>");
    writer.append("</url>");
  }

  private String getRealUrl(String url) {
    final URI uri = URI.create(url);
    return Imeji.PROPERTIES.getApplicationURI() + uri.getPath();
  }

  @Override
  public Map<String, Integer> getExportedItemsPerCollection() {
    return new HashMap<>();
  }

}
