package de.mpg.imeji.logic.doi.models;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;

@XmlType
public class DOIIdentifier {


  private String identifierType = "DOI";

  @XmlValue
  private final String identifier = "";


  public DOIIdentifier() {}


  @XmlAttribute
  public String getIdentifierType() {
    return identifierType;
  }

  public void setIdentifierType(String identifierType) {
    this.identifierType = identifierType;
  }



}
