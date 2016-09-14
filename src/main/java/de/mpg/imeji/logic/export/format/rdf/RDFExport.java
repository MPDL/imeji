package de.mpg.imeji.logic.export.format.rdf;

import java.io.OutputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.hp.hpl.jena.query.ReadWrite;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;

import de.mpg.imeji.logic.Imeji;
import de.mpg.imeji.logic.export.format.Export;
import de.mpg.imeji.logic.search.model.SearchResult;
import de.mpg.imeji.logic.vo.User;

/**
 * {@link Export} in rdf
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public abstract class RDFExport extends Export {
  protected String[] filteredTriples = {};
  protected List<String> filteredResources = new ArrayList<String>();
  protected Map<String, String> namespaces;
  protected String modelURI;



  @Override
  public void export(OutputStream out, SearchResult sr, User user) {
    initNamespaces();
    filterResources(sr, super.user);
    exportIntoOut(sr, out);
  }

  @Override
  public String getContentType() {
    return "application/rdf+xml";
  }

  /**
   * Initialize the namespaces used for the export
   */
  protected abstract void initNamespaces();

  /**
   * Write the {@link SearchResult} in an {@link OutputStream}
   *
   * @param sr
   * @param out
   */
  protected void exportIntoOut(SearchResult sr, OutputStream out) {
    namespaces.put("http://www.w3.org/1999/02/22-rdf-syntax-ns#", "rdf");
    Imeji.dataset.begin(ReadWrite.READ);
    try {
      Model model = Imeji.dataset.getNamedModel(modelURI);
      Imeji.dataset.commit();
      StringWriter writer = new StringWriter();
      writer.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
      writer.append("<rdf:RDF");
      for (String key : namespaces.keySet()) {
        writer.append(" xmlns:" + namespaces.get(key) + "=\"" + key + "\"");
      }
      writer.append(">");
      for (String s : sr.getResults()) {
        Resource resource = model.getResource(s);
        newLine(writer);
        writer.append(openTagResource(toUrl(s)));
        writer.append(exportResource(resource).getBuffer());
        writer.append(closeTagResource());
      }
      newLine(writer);
      writer.append("</rdf:RDF>");
      out.write(writer.getBuffer().toString().replace("&", "&amp;").getBytes());
    } catch (Exception e) {
      // Imeji.dataset.abort();
      throw new RuntimeException("Error in export", e);
    } finally {
      Imeji.dataset.end();
    }
  }

  /**
   * Write a {@link Resource} in rdf
   *
   * @param r
   * @return
   */
  protected StringWriter exportResource(Resource r) {
    StringWriter writer = new StringWriter();
    if (isFilteredResource(r)) {
      return writer;
    }
    newLine(writer);
    for (StmtIterator iterator = r.listProperties(); iterator.hasNext();) {
      Statement st = iterator.next();
      if (!isFiltered(st)) {
        try {
          if (st.getResource().listProperties().hasNext()) {
            // Open as parseType
            writer.append(openTagAsParseUri(st));
            // Put Object as resourceId
            writer.append(exportResource(st.getResource()).getBuffer());
            // CloseTheTag for the parseResource element (used also for Literal statements)
            writer.append(closeTag(st));
          } else {
            // if it is a simple resource, without subelements - close the tag asap (without
            // separate closing element)
            writer.append(openAndCloseResourceTag(st, toUrl(st.getResource().getURI())));
          }

        } catch (Exception e) {
          /* Not a resource */
        }
        try {
          if (st.getLiteral().toString() != null) {
            writer.append(openTag(st, null));
            String literal = st.getLiteral().getString();
            // Enclose embedded html
            if (literal.contains("<html>")) {
              literal = "<![CDATA[" + literal + "]]>";
            }
            writer.append(literal);
            writer.append(closeTag(st));
          }
        } catch (Exception e) {
          /* Not a literal */
        }

        newLine(writer);
      }
    }
    return writer;
  }

  /**
   * TRansform an uri (used as id in rdf) to a real url
   *
   * @param uri
   * @return
   */
  protected String toUrl(String uri) {
    return uri.replace(Imeji.PROPERTIES.getBaseURI(), Imeji.PROPERTIES.getApplicationURL());
  }

  /**
   * Open an html tag for an rdf resource
   *
   * @param uri
   * @return
   */
  protected abstract String openTagResource(String uri);

  /**
   * Close an html tag for an rdf resource
   *
   * @return
   */
  protected abstract String closeTagResource();

  /**
   * Open an html tag for statement
   *
   * @param st
   * @param resourceURI
   * @return
   */
  private String openTag(Statement st, String resourceURI) {
    String tag = "<" + getNamespace(st.getPredicate().getNameSpace()) + ":"
        + st.getPredicate().getLocalName();
    if (resourceURI != null) {
      tag += " rdf:resource=\"" + resourceURI + "\"";
    }
    tag += ">";
    return tag;
  }

  /**
   * Open and close tag for statement
   *
   * @param st
   * @param resourceURI
   * @return
   */
  private String openAndCloseResourceTag(Statement st, String resourceURI) {
    // calculate the element of standard resource statement and close the element immediately
    String tag = "<" + getNamespace(st.getPredicate().getNameSpace()) + ":"
        + st.getPredicate().getLocalName();

    if (resourceURI != null) {
      tag += " rdf:resource=\"" + resourceURI + "\"/";
    }
    tag += ">";

    return tag;
  }


  /**
   * Open an html tag for statement
   *
   * @param st
   * @param resourceURI
   * @return
   */
  private String openTagAsParseUri(Statement st) {
    // use the resource URI as separate Element within the rdf:parseResource tag
    String resourceURI = st.getObject().toString();
    // calculate the element name which should be appended with rdf:parseResource attribute
    String tagRegular = "<" + getNamespace(st.getPredicate().getNameSpace()) + ":"
        + st.getPredicate().getLocalName();
    // calculate the inner resourceId element name
    String tagInnerResourceId =
        "<" + getNamespace(st.getPredicate().getNameSpace()) + ":" + "resourceId>";

    // append the parseType attribute
    tagRegular += " rdf:parseType=\"Resource\">\n";
    // append the resourceId element
    tagRegular += tagInnerResourceId + resourceURI + "</"
        + getNamespace(st.getPredicate().getNameSpace()) + ":" + "resourceId>";

    // return the regular complex element as two elements, one with parseResource attribute and
    // another subelement of resourceId

    return tagRegular;
  }


  /**
   * Close an html tag for statement
   *
   * @param st
   * @return
   */
  private String closeTag(Statement st) {
    return "</" + getNamespace(st.getPredicate().getNameSpace()) + ":"
        + st.getPredicate().getLocalName() + ">";
  }

  /**
   * return the namespace
   *
   * @param ns
   * @return
   */
  protected String getNamespace(String ns) {
    String ns1 = namespaces.get(ns);
    if (ns1 != null) {
      return ns1;
    }
    return ns;
  }

  /**
   * Add a new line separator
   *
   * @param writer
   */
  protected void newLine(StringWriter writer) {
    writer.append("\n");
  }

  /**
   * If the {@link Statement} is filtered (namespace has been defined as to be filtered), then
   * return false
   *
   * @param st
   * @return
   */
  private boolean isFiltered(Statement st) {
    // Check if the triple is filtered
    for (String s : filteredTriples) {
      if (s.equals(st.getPredicate().getURI())) {
        return true;
      }
    }
    return false;
  }

  /**
   * If the {@link Resource} is filtered (for instance a metadata which should not be displayed,
   * because restricted to current user), return true
   *
   * @param r
   * @return
   */
  private boolean isFilteredResource(Resource r) {
    for (String s : filteredResources) {
      if (s.equals(r.getURI().toString())) {
        return true;
      }
    }
    return false;
  }

  /**
   * Check which {@link Resource} should be filtered
   *
   * @param sr
   * @param user
   */
  protected abstract void filterResources(SearchResult sr, User user);
}
