package de.mpg.imeji.rest.to;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@XmlRootElement
@XmlType(propOrder = {"type", "value"})
@JsonInclude(Include.NON_EMPTY)
public class IdentifierTO implements Serializable {
  private static final long serialVersionUID = 1633020264591234236L;

  @JsonIgnore
  private String type = "imeji";

  private String value;

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.value = value;
  }



}
