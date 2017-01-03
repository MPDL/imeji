package de.mpg.imeji.rest.to;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import com.fasterxml.jackson.annotation.JsonInclude;

@XmlRootElement
@XmlType(propOrder = {"versionOf"})
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AlbumTO extends ContainerTO implements Serializable {

  /**
   *
   */
  private static final long serialVersionUID = -6299975725127957067L;

  private String versionOf;

  public String getVersionOf() {
    return versionOf;
  }

  public void setVersionOf(String versionOf) {
    this.versionOf = versionOf;
  }

}
