package de.mpg.imeji.rest.to.predefinedMetadataTO;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import de.mpg.imeji.rest.to.predefinedMetadataTO.annotations.MetadataTOType;


@XmlRootElement
@MetadataTOType("http://imeji.org/terms/metadata#publication")
@XmlType(propOrder = {"publication", "format", "citation"})
public class PublicationTO extends MetadataTO {
  private static final long serialVersionUID = 5215159848857258368L;
  private String format;
  private String publication;
  private String citation;

  public String getPublication() {
    return publication;
  }

  public void setPublication(String publication) {
    this.publication = publication;
  }

  public String getFormat() {
    return format;
  }

  public void setFormat(String format) {
    this.format = format;
  }

  public String getCitation() {
    return citation;
  }

  public void setCitation(String citation) {
    this.citation = citation;
  }



}
