package de.mpg.imeji.logic.doi.models;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;

@XmlType
public class DOIResourceType implements Serializable {
  private static final long serialVersionUID = -3303869147073552743L;
  private String resourceTypeGeneral = "Dataset";

  @XmlValue
  private final String resourceType = "";

  public DOIResourceType() {

  }

  @XmlAttribute
  public String getResourceTypeGeneral() {
    return resourceTypeGeneral;
  }

  public void setResourceTypeGeneral(String resourceTypeGeneral) {
    this.resourceTypeGeneral = resourceTypeGeneral;
  }

}
