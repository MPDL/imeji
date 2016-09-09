package de.mpg.imeji.rest.to.predefinedMetadataTO;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import de.mpg.imeji.rest.to.predefinedMetadataTO.annotations.MetadataTOType;

@XmlRootElement
@MetadataTOType("http://imeji.org/terms/metadata#date")
@XmlType(propOrder = {"date"})
public class DateTO extends MetadataTO {
  private static final long serialVersionUID = -2728940704203315449L;
  private String date;

  public String getDate() {
    return date;
  }

  public void setDate(String date) {
    this.date = date;
  }



}
