/**
 * License: src/main/resources/license/escidoc.license
 */
package de.mpg.imeji.logic.vo.predefinedMetadata;

import java.net.URI;
import java.text.SimpleDateFormat;

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
 * {@link Metadata} for license value
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
@j2jResource(ImejiNamespaces.METADATA)
@j2jDataType("http://imeji.org/terms/metadata#license")
@j2jId(getMethod = "getId", setMethod = "setId")
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "license", namespace = ImejiNamespaces.METADATA)
@XmlType(propOrder = {"externalUri", "license", "statement"})
public class License extends Metadata {
  private static final long serialVersionUID = -3194639210099160225L;
  private SimpleDateFormat date;
  private String dateFormat = "dd/mm/yyyy";
  @j2jLiteral("http://imeji.org/terms/license")
  private String license = null;
  @j2jResource("http://purl.org/dc/elements/1.1/identifier")
  private URI externalUri;

  @j2jResource("http://imeji.org/terms/statement")
  private URI statement;

  public License() {}

  public License(SimpleDateFormat date) {
    this.date = date;
    date.applyPattern(dateFormat);
  }

  public String getDateString() {
    if (date == null || date.isLenient()) {
      return "";
    }
    return date.format(date);
  }

  @XmlElement(name = "license", namespace = "http://imeji.org/terms/")
  public String getLicense() {
    return license;
  }

  public void setLicense(String str) {
    license = str;
  }


  /**
   * @return the externalUri
   */
  @XmlElement(name = "identifier", namespace = "http://purl.org/dc/elements/1.1/")
  public URI getExternalUri() {
    return externalUri;
  }

  /**
   * @param externalUri the externalUri to set
   */
  public void setExternalUri(URI externalUri) {
    this.externalUri = externalUri;
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
    if (metadata instanceof License) {
      setPos(metadata.getPos());
      this.license = ((License) metadata).getLicense();
      setStatement(((License) metadata).getStatement());
      this.externalUri = ((License) metadata).getExternalUri();
    }
  }

  @Override
  public String asFulltext() {
    return license + " " + getDateString();
  }

  @Override
  public void clean() {
    license = license != null ? license.trim() : null;
  }
}
