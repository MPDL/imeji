package de.mpg.imeji.logic.export;

import java.util.Map;

import org.apache.http.client.HttpResponseException;

import de.mpg.imeji.logic.export.format.Export;
import de.mpg.imeji.logic.export.format.JenaExport;
import de.mpg.imeji.logic.export.format.SitemapExport;
import de.mpg.imeji.logic.export.format.ZIPExport;
import de.mpg.imeji.logic.export.format.explain.ExplainExport;
import de.mpg.imeji.logic.export.format.explain.MetadataExplainExport;
import de.mpg.imeji.logic.export.format.explain.SearchExplainExport;
import de.mpg.imeji.logic.export.format.rdf.RDFAlbumExport;
import de.mpg.imeji.logic.export.format.rdf.RDFCollectionExport;
import de.mpg.imeji.logic.export.format.rdf.RDFExport;
import de.mpg.imeji.logic.export.format.rdf.RDFImageExport;
import de.mpg.imeji.logic.export.format.rdf.RDFProfileExport;

/**
 * Factory for export
 * 
 * @author saquet
 *
 */
public class ExportFactory {

  private ExportFactory() {
    // avoid construtor
  }

  /**
   * Factory to create an {@link Export} from url paramters
   *
   * @param params
   * @return
   * @throws HttpResponseException
   */
  public static Export build(Map<String, String[]> params) throws HttpResponseException {
    Export export = null;
    String format = Export.getParam(params, "format");
    String type = Export.getParam(params, "type");
    if (format == null || "".equals(format)) {
      export = buildRdf(type);
    } else if ("rdf".equals(format)) {
      export = buildRdf(type);
    } else if ("jena".equals(format)) {
      export = new JenaExport();
    } else if ("sitemap".equals(format)) {
      export = new SitemapExport();
    } else if ("zip".equals(format)) {
      export = new ZIPExport(type);
    } else if ("explain".equals(format)) {
      export = buidExplain(type);
    } else {
      throw new HttpResponseException(400, "Format " + format + " is not supported.");
    }
    export.setParams(params);
    export.init();
    return export;
  }

  /**
   * Factory for {@link RDFExport}
   *
   * @param type
   * @return
   * @throws HttpResponseException
   */
  private static RDFExport buildRdf(String type) throws HttpResponseException {
    if ("image".equalsIgnoreCase(type)) {
      return new RDFImageExport();
    } else if ("collection".equalsIgnoreCase(type)) {
      return new RDFCollectionExport();
    } else if ("album".equalsIgnoreCase(type)) {
      return new RDFAlbumExport();
    } else if ("profile".equals(type)) {
      return new RDFProfileExport();
    }
    throw new HttpResponseException(400, "Type " + type + " is not supported.");
  }

  /**
   * Factory for {@link ExplainExport}
   *
   * @param type
   * @return
   * @throws HttpResponseException
   */
  public static ExplainExport buidExplain(String type) throws HttpResponseException {
    if ("search".equals(type)) {
      return new SearchExplainExport();
    } else if ("metadata".equals(type)) {
      return new MetadataExplainExport();
    }
    throw new HttpResponseException(400, "Type " + type + " is not supported.");
  }

}
