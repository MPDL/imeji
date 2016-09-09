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
 * {@link Metadata} for links (URL)
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
@j2jResource(ImejiNamespaces.METADATA)
@j2jDataType("http://imeji.org/terms/metadata#link")
@j2jId(getMethod = "getId", setMethod = "setId")
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "link", namespace = ImejiNamespaces.METADATA)
@XmlType(propOrder = {"label", "uri", "statement"})
public class Link extends Metadata {
  private static final long serialVersionUID = -8486219552564749433L;
  @j2jResource("http://imeji.org/terms/uri")
  private URI uri;
  @j2jLiteral("http://www.w3.org/2000/01/rdf-schema#label")
  private String label;
  @j2jResource("http://imeji.org/terms/statement")
  private URI statement;

  public Link() {}

  @XmlElement(name = "uri", namespace = "http://imeji.org/terms/")
  public java.net.URI getUri() {
    return uri;
  }

  public void setUri(java.net.URI uri) {
    this.uri = uri;
  }

  public void setLabel(String label) {
    this.label = label;
  }

  @XmlElement(name = "label", namespace = "http://www.w3.org/2000/01/rdf-schema#")
  public String getLabel() {
    return label;
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
    if (metadata instanceof Link) {
      setPos(metadata.getPos());
      this.label = ((Link) metadata).getLabel();
      this.uri = ((Link) metadata).getUri();
      setStatement(((Link) metadata).getStatement());
    }
  }

  @Override
  public String asFulltext() {
    if (uri != null) {
      return label + " " + uri.toString();
    }
    return label;
  }

  @Override
  public void clean() {
    label = label != null ? label.trim() : null;
  }
}
