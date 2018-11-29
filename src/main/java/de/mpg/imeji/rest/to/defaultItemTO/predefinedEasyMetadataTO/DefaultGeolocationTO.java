package de.mpg.imeji.rest.to.defaultItemTO.predefinedEasyMetadataTO;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlRootElement;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@XmlRootElement
@JsonInclude(Include.NON_NULL)
public class DefaultGeolocationTO implements Serializable {

  private static final long serialVersionUID = -3900598743915553519L;

  private String name;
  private double longitude;
  private double latitude;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public double getLongitude() {
    return longitude;
  }

  public void setLongitude(double longitude) {
    this.longitude = longitude;
  }

  public double getLatitude() {
    return latitude;
  }

  public void setLatitude(double latitude) {
    this.latitude = latitude;
  }

}
