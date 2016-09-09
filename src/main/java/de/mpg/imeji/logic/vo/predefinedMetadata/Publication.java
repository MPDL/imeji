/**
 * License: src/main/resources/license/escidoc.license
 */
package de.mpg.imeji.logic.vo.predefinedMetadata;

import java.net.URI;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import de.mpg.imeji.j2j.annotations.j2jDataType;
import de.mpg.imeji.j2j.annotations.j2jId;
import de.mpg.imeji.j2j.annotations.j2jLiteral;
import de.mpg.imeji.j2j.annotations.j2jResource;
import de.mpg.imeji.logic.ImejiNamespaces;

/**
 * {@link Metadata} for publication
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
@j2jResource(ImejiNamespaces.METADATA)
@j2jDataType("http://imeji.org/terms/metadata#publication")
@j2jId(getMethod = "getId", setMethod = "setId")
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "publication", namespace = ImejiNamespaces.METADATA)
@XmlType(propOrder = {"citation", "exportFormat", "uri", "statement"})
public class Publication extends Metadata {
  private static final long serialVersionUID = -1847036667920897740L;
  @j2jResource("http://imeji.org/terms/uri")
  private URI uri;
  @j2jLiteral("http://imeji.org/terms/citationStyle")
  private String exportFormat = "APA";
  @j2jLiteral("http://imeji.org/terms/citation")
  private String citation;

  @j2jResource("http://imeji.org/terms/statement")
  private URI statement;

  public Publication() {}

  @XmlElement(name = "uri", namespace = "http://imeji.org/terms/")
  public java.net.URI getUri() {
    return uri;
  }

  public void setUri(URI uri) {
    this.uri = uri;
  }

  @XmlElement(name = "citationStyle", namespace = "http://imeji.org/terms/")
  public String getExportFormat() {
    return exportFormat;
  }

  public void setExportFormat(String exportFormat) {
    this.exportFormat = exportFormat;
  }

  @XmlElement(name = "citation", namespace = "http://imeji.org/terms/")
  public String getCitation() {
    return citation;
  }

  public void setCitation(String citation) {
    this.citation = citation;
  }

  @Override
  @XmlElement(name = "statement", namespace = "http://imeji.org/terms/")
  public URI getStatement() {
    return statement;
  }

  @Override
  public void setStatement(URI namespace) {
    this.statement = namespace;
  }

  @Override
  public void copy(Metadata metadata) {
    if (metadata instanceof Publication) {
      setPos(metadata.getPos());
      this.citation = ((Publication) metadata).getCitation();
      this.exportFormat = ((Publication) metadata).getExportFormat();
      this.uri = ((Publication) metadata).getUri();
      setStatement(((Publication) metadata).getStatement());
    }
  }

  @Override
  public String asFulltext() {
    return citation + " " + (uri != null ? uri.toString() : "");
  }

  @Override
  public void clean() {
    citation = citation != null ? citation.trim() : null;
  }
}
