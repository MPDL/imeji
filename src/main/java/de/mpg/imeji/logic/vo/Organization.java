/**
 * License: src/main/resources/license/escidoc.license
 */
package de.mpg.imeji.logic.vo;

import java.io.Serializable;
import java.net.URI;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import de.mpg.imeji.j2j.annotations.j2jId;
import de.mpg.imeji.j2j.annotations.j2jLiteral;
import de.mpg.imeji.j2j.annotations.j2jResource;
import de.mpg.imeji.logic.util.IdentifierUtil;
import de.mpg.imeji.logic.util.ObjectHelper;

/**
 * An organization
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
@j2jResource("http://purl.org/escidoc/metadata/profiles/0.1/organizationalunit")
@j2jId(getMethod = "getId", setMethod = "setId")
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "organizationalunit",
    namespace = "http://purl.org/escidoc/metadata/profiles/0.1/")
public class Organization implements Cloneable, Serializable {
  private static final long serialVersionUID = -7541779415288019910L;
  private URI id;
  @j2jLiteral("http://purl.org/dc/terms/title")
  private String name;
  @j2jLiteral("http://purl.org/dc/terms/description")
  private String description;
  @j2jLiteral("http://purl.org/dc/terms/identifier")
  private String identifier;
  @j2jLiteral("http://purl.org/escidoc/metadata/terms/0.1/city")
  private String city;
  @j2jLiteral("http://purl.org/escidoc/metadata/terms/0.1/country")
  private String country;
  @j2jLiteral("http://imeji.org/terms/position")
  private int pos = 0;

  public Organization() {
    id = IdentifierUtil.newURI(Organization.class);
    this.identifier = ObjectHelper.getId(id);
  }

  @XmlElement(name = "title", namespace = "http://purl.org/dc/terms/")
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  @XmlElement(name = "description", namespace = "http://purl.org/dc/terms/")
  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  @XmlElement(name = "identifier", namespace = "http://purl.org/dc/terms/")
  public String getIdentifier() {
    return identifier;
  }

  public void setIdentifier(String identifier) {
    this.identifier = identifier;
  }

  @XmlElement(name = "city", namespace = "http://purl.org/escidoc/metadata/terms/0.1/")
  public String getCity() {
    return city;
  }

  public void setCity(String city) {
    this.city = city;
  }

  @XmlElement(name = "country", namespace = "http://purl.org/escidoc/metadata/terms/0.1/")
  public String getCountry() {
    return country;
  }

  public void setCountry(String country) {
    this.country = country;
  }

  @XmlElement(name = "position", namespace = "http://imeji.org/terms/")
  public int getPos() {
    return pos;
  }

  public void setPos(int pos) {
    this.pos = pos;
  }

  public int compareTo(Organization o) {
    if (o.getPos() > this.pos) {
      return -1;
    } else if (o.getPos() == this.pos) {
      return 0;
    } else {
      return 1;
    }
  }

  public void setId(URI id) {
    this.id = id;
  }

  @XmlAttribute(name = "id")
  public URI getId() {
    return id;
  }

  @Override
  public Organization clone() {
    Organization clone = new Organization();
    clone.city = this.city;
    clone.country = this.country;
    clone.description = this.description;
    if (identifier != null && !"".equals(identifier)) {
      clone.identifier = this.identifier;
    }
    clone.name = this.name;
    clone.pos = this.pos;
    return clone;
  }
}
