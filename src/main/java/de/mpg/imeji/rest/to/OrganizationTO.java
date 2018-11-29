package de.mpg.imeji.rest.to;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import de.mpg.imeji.rest.to.defaultItemTO.DefaultOrganizationTO;

@XmlRootElement
@XmlType(propOrder = {
    // "position",
    "id", "name",})
@JsonInclude(Include.NON_NULL)
public class OrganizationTO implements Serializable {

  private static final long serialVersionUID = 1207566371079322550L;

  @JsonIgnore
  private int position = 0;

  private String id;

  private String name;

  /**
   * Default constructor
   */
  public OrganizationTO() {

  }

  /**
   * Constructor with a {@link DefaultOrganizationTO}
   *
   * @param dTO
   */
  public OrganizationTO(DefaultOrganizationTO dTO) {
    this.name = dTO.getName();
  }

  public int getPosition() {
    return position;
  }

  public void setPosition(int position) {
    this.position = position;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }
}
