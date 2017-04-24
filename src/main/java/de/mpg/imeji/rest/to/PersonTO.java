package de.mpg.imeji.rest.to;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@XmlRootElement
@XmlType(propOrder = {
    // "position",
    "id", "familyName", "givenName", "identifiers", "organizations"})
@JsonInclude(Include.NON_EMPTY)
public class PersonTO implements Serializable {
  private static final long serialVersionUID = 2752588435466650389L;
  @JsonIgnore
  private int position;
  private String id;

  private String familyName;

  private String givenName;

  private List<IdentifierTO> identifiers = new ArrayList<IdentifierTO>();

  private List<OrganizationTO> organizations = new ArrayList<OrganizationTO>();



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

  public String getFamilyName() {
    return familyName;
  }

  public void setFamilyName(String familyName) {
    this.familyName = familyName;
  }

  public String getGivenName() {
    return givenName;
  }

  public void setGivenName(String givenName) {
    this.givenName = givenName;
  }

  public List<IdentifierTO> getIdentifiers() {
    return identifiers;
  }

  public void setIdentifiers(List<IdentifierTO> identifiers) {
    this.identifiers = identifiers;
  }

  public List<OrganizationTO> getOrganizations() {
    return organizations;
  }

  public void setOrganizations(List<OrganizationTO> organizations) {
    this.organizations = organizations;
  }



}
