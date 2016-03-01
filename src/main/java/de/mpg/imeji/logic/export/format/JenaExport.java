/**
 * License: src/main/resources/license/escidoc.license
 */
package de.mpg.imeji.logic.export.format;

import java.io.OutputStream;

import org.apache.log4j.Logger;

import com.hp.hpl.jena.query.ReadWrite;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;

import de.mpg.imeji.logic.Imeji;
import de.mpg.imeji.logic.export.Export;
import de.mpg.imeji.logic.search.SearchResult;

/**
 * Export data as they are in stored in Jena
 * 
 * @author saquet
 */
public class JenaExport extends Export {

  private static final Logger LOGGER = Logger.getLogger(JenaExport.class);

  @Override
  public void init() {
    // Not initialization so far
  }

  @Override
  public void export(OutputStream out, SearchResult sr) {
    Model m = exportIntoModel(sr);
    m.write(out, "RDF/XML");
  }

  @Override
  public String getContentType() {
    return "application/xml";
  }

  /**
   * Create a model with all Search results
   * 
   * @param sr
   * @return
   */
  private Model exportIntoModel(SearchResult sr) {
    Model exportModel = ModelFactory.createDefaultModel();
    for (String s : sr.getResults()) {
      try {
        Imeji.dataset.begin(ReadWrite.READ);
        Model m = Imeji.dataset.getNamedModel(Imeji.imageModel);
        Resource resource = m.getResource(s);
        exportResource(resource, exportModel);
        Imeji.dataset.commit();
      } catch (Exception e) {
        Imeji.dataset.abort();
        LOGGER.info("Some problems with Exporting of Imeji data", e);
      } finally {
        Imeji.dataset.end();
      }
    }
    return exportModel;
  }

  /**
   * Write all properties of a resource in the model
   * 
   * @param r
   * @param m
   * @return
   */
  private void exportResource(Resource r, Model m) {
    for (StmtIterator iterator = r.listProperties(); iterator.hasNext();) {
      Statement st = iterator.next();
      try {
        exportResource(st.getResource(), m);
        if (st.getResource().getURI() == null) {
          exportResource(st.getResource(), m);
        }
      } catch (Exception e) {
        // Not to be handle
      }
      m.add(st);
    }
  }
}
