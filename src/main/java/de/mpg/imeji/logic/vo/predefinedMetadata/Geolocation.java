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
 * {@link Metadata} for geolocation data
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
@j2jResource(ImejiNamespaces.METADATA)
@j2jDataType("http://imeji.org/terms/metadata#geolocation")
@j2jId(getMethod = "getId", setMethod = "setId")
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "geolocation", namespace = ImejiNamespaces.METADATA)
@XmlType(propOrder = {"latitude", "longitude", "name", "statement"})
public class Geolocation extends Metadata {
  private static final long serialVersionUID = 6902473139335929693L;
  @j2jLiteral("http://imeji.org/terms/longitude")
  private double longitude = Double.NaN;
  @j2jLiteral("http://imeji.org/terms/latitude")
  private double latitude = Double.NaN;
  @j2jLiteral("http://purl.org/dc/terms/title")
  private String name;

  @j2jResource("http://imeji.org/terms/statement")
  private URI statement;

  public Geolocation() {}

  public Geolocation(String latitude, String longitude) {}

  @XmlElement(name = "longitude", namespace = "http://imeji.org/terms/")
  public double getLongitude() {
    return longitude;
  }

  public void setLongitude(double longitude) {
    this.longitude = longitude;
  }

  @XmlElement(name = "latitude", namespace = "http://imeji.org/terms/")
  public double getLatitude() {
    return latitude;
  }

  public void setLatitude(double latitude) {
    this.latitude = latitude;
  }

  @XmlElement(name = "title", namespace = "http://purl.org/dc/terms/")
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
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
    if (metadata instanceof Geolocation) {
      setPos(metadata.getPos());
      this.latitude = ((Geolocation) metadata).getLatitude();
      this.longitude = ((Geolocation) metadata).getLongitude();
      this.name = ((Geolocation) metadata).getName();
      setStatement(((Geolocation) metadata).getStatement());
    }
  }

  @Override
  public String asFulltext() {
    return name + " " + " lat:" + latitude + " long:" + longitude;
  }

  @Override
  public void clean() {
    // Nothing to do
  }
}
