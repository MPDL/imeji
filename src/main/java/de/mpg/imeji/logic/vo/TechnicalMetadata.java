package de.mpg.imeji.logic.vo;

import java.io.Serializable;
import java.net.URI;

import de.mpg.imeji.j2j.annotations.j2jId;
import de.mpg.imeji.j2j.annotations.j2jLiteral;
import de.mpg.imeji.j2j.annotations.j2jResource;
import de.mpg.imeji.logic.ImejiNamespaces;

/**
 * A technical metadata as parsed from content
 * 
 * @author saquet
 *
 */
@j2jResource(ImejiNamespaces.TECHNICAL_METADATA)
@j2jId(getMethod = "getId", setMethod = "setId")
public class TechnicalMetadata implements Serializable {
  private static final long serialVersionUID = 519330579019278631L;
  private URI id;
  @j2jLiteral("http://imeji.org/terms/name")
  private String name;
  @j2jLiteral("http://imeji.org/terms/value")
  private String value;

  public TechnicalMetadata() {
    // Default Constructor
  }

  public TechnicalMetadata(String name, String value) {
    this.name = name;
    this.value = value;
  }

  public String getName() {
    return name;
  }

  public String getValue() {
    return value;
  }

  public URI getId() {
    return id;
  }

  public void setId(URI id) {
    this.id = id;
  }

}
