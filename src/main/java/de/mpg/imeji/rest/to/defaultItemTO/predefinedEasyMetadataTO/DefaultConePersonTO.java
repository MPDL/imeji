package de.mpg.imeji.rest.to.defaultItemTO.predefinedEasyMetadataTO;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import de.mpg.imeji.rest.to.defaultItemTO.DefaultOrganizationTO;

/**
 * TO for {@link ConePerson}
 *
 * @author bastiens
 *
 */
@JsonInclude(Include.NON_NULL)
public class DefaultConePersonTO implements Serializable {
  private static final long serialVersionUID = 1645461293418318845L;

  private String familyName;
  private String givenName;
  private String completeName;
  private String alternativeName;
  private String role;
  private List<DefaultOrganizationTO> organizations = new ArrayList<DefaultOrganizationTO>();

  /**
   * @return the familyName
   */
  public String getFamilyName() {
    return familyName;
  }

  /**
   * @param familyName the familyName to set
   */
  public void setFamilyName(String familyName) {
    this.familyName = familyName;
  }

  /**
   * @return the givenName
   */
  public String getGivenName() {
    return givenName;
  }

  /**
   * @param givenName the givenName to set
   */
  public void setGivenName(String givenName) {
    this.givenName = givenName;
  }

  /**
   * @return the organizations
   */
  public List<DefaultOrganizationTO> getOrganizations() {
    return organizations;
  }

  /**
   * @param organizations the organizations to set
   */
  public void setOrganizations(List<DefaultOrganizationTO> organizations) {
    this.organizations = organizations;
  }

  /**
   * @return the completeName
   */
  public String getCompleteName() {
    return completeName;
  }

  /**
   * @param completeName the completeName to set
   */
  public void setCompleteName(String completeName) {
    this.completeName = completeName;
  }

  /**
   * @return the alternativeName
   */
  public String getAlternativeName() {
    return alternativeName;
  }

  /**
   * @param alternativeName the alternativeName to set
   */
  public void setAlternativeName(String alternativeName) {
    this.alternativeName = alternativeName;
  }

  /**
   * @return the role
   */
  public String getRole() {
    return role;
  }

  /**
   * @param role the role to set
   */
  public void setRole(String role) {
    this.role = role;
  }

}
