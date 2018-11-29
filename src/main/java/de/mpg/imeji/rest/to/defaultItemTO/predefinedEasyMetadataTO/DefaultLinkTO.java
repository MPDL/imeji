package de.mpg.imeji.rest.to.defaultItemTO.predefinedEasyMetadataTO;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlRootElement;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@XmlRootElement
@JsonInclude(Include.NON_NULL)
public class DefaultLinkTO implements Serializable {

  private static final long serialVersionUID = -4439636670664475938L;
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
