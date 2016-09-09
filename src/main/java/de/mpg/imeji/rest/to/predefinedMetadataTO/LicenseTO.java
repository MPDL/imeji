package de.mpg.imeji.rest.to.predefinedMetadataTO;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import de.mpg.imeji.rest.to.predefinedMetadataTO.annotations.MetadataTOType;

@XmlRootElement
@MetadataTOType("http://imeji.org/terms/metadata#license")
@XmlType(propOrder = {"license", "url"})
public class LicenseTO extends MetadataTO {
  private static final long serialVersionUID = 5493360284361097587L;

  private String license;

  private String url;



  public String getLicense() {
    return license;
  }

  public void setLicense(String license) {
    this.license = license;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }



}
