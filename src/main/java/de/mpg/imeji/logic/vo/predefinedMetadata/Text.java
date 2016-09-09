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
 * {@link Metadata} of type text
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
@j2jResource(ImejiNamespaces.METADATA)
@j2jDataType("http://imeji.org/terms/metadata#text")
@j2jId(getMethod = "getId", setMethod = "setId")
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "text", namespace = ImejiNamespaces.METADATA)
@XmlType(propOrder = {"text", "statement"})
public class Text extends Metadata {
  private static final long serialVersionUID = 3394338221875432545L;
  @j2jLiteral("http://imeji.org/terms/text")
  private String text;

  public Text() {}

  @j2jResource("http://imeji.org/terms/statement")
  private URI statement;

  @XmlElement(name = "text", namespace = "http://imeji.org/terms/")
  public String getText() {
    return text;
  }

  public void setText(String str) {
    text = str;
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
    if (metadata instanceof Text) {
      setPos(metadata.getPos());
      this.text = ((Text) metadata).getText();
      setStatement(metadata.getStatement());
    }
  }

  @Override
  public String asFulltext() {
    return text;
  }

  @Override
  public void clean() {
    text = text != null ? text.trim() : null;;
  }
}
