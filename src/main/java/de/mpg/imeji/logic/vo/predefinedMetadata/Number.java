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
 * {@link Metadata} for number values
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
@j2jResource(ImejiNamespaces.METADATA)
@j2jDataType("http://imeji.org/terms/metadata#number")
@j2jId(getMethod = "getId", setMethod = "setId")
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "number", namespace = ImejiNamespaces.METADATA)
@XmlType(propOrder = {"number", "statement"})
public class Number extends Metadata {
  private static final long serialVersionUID = 1465887901391010292L;
  @j2jLiteral("http://imeji.org/terms/number")
  private double number = Double.NaN;

  @j2jResource("http://imeji.org/terms/statement")
  private URI statement;

  public Number() {}

  public Number(double value) {
    setNumber(value);
  }

  public void setNumber(double number) {
    this.number = number;
  }

  @XmlElement(name = "number", namespace = "http://imeji.org/terms/")
  public double getNumber() {
    return number;
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
    if (metadata instanceof Number) {
      setPos(metadata.getPos());
      this.number = ((Number) metadata).getNumber();
      setStatement(((Number) metadata).getStatement());
    }
  }

  @Override
  public String asFulltext() {
    return Double.toString(number);
  }

  @Override
  public void clean() {
    // Nothing to do
  }
}
