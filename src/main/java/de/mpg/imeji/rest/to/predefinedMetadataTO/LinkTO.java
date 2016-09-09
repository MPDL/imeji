package de.mpg.imeji.rest.to.predefinedMetadataTO;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import de.mpg.imeji.rest.to.predefinedMetadataTO.annotations.MetadataTOType;

@XmlRootElement
@MetadataTOType("http://imeji.org/terms/metadata#link")
@XmlType(propOrder = {"link", "url"})
public class LinkTO extends MetadataTO {
  private static final long serialVersionUID = -7245573760046279074L;
  private String link;
  private String url;

  public String getLink() {
    return link;
  }

  public void setLink(String link) {
    this.link = link;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }



}
