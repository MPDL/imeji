package de.mpg.imeji.logic.model;

import java.io.Serializable;
import java.net.URI;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import de.mpg.imeji.j2j.annotations.j2jId;
import de.mpg.imeji.j2j.annotations.j2jLiteral;
import de.mpg.imeji.j2j.annotations.j2jResource;
import de.mpg.imeji.logic.ImejiNamespaces;
import de.mpg.imeji.logic.model.util.HibernateTechnicalMetadataConverter;
import de.mpg.imeji.logic.model.util.HibernateURIConverter;
import de.mpg.imeji.logic.model.util.JacksonTechicalMetadataConverter;
import jakarta.persistence.*;

/**
 * A technical metadata as parsed from content
 *
 * @author saquet
 *
 */

//@Entity
//@Table(name = "technical-metadata")

@j2jResource(ImejiNamespaces.TECHNICAL_METADATA)
@j2jId(getMethod = "getId", setMethod = "setId")
public class TechnicalMetadata implements Serializable {
  private static final long serialVersionUID = 519330579019278631L;
  //@Convert(converter = HibernateURIConverter.class)
  private URI id;
  @j2jLiteral("http://imeji.org/terms/name")
  private String name;

  @Convert(converter = HibernateTechnicalMetadataConverter.class)
  @j2jLiteral("http://imeji.org/terms/value")
  private String value;

  //@Id
  //private String dbId;

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

  @JsonSerialize(converter = JacksonTechicalMetadataConverter.class)
  public String getValue() {
    return value;
  }

  public URI getId() {
    return id;
  }

  public void setId(URI id) {
    this.id = id;
    //this.dbId = id.toString();
  }

}
