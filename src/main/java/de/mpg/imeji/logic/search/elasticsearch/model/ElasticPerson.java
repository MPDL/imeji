package de.mpg.imeji.logic.search.elasticsearch.model;

import java.util.ArrayList;
import java.util.List;

import de.mpg.imeji.logic.vo.Organization;
import de.mpg.imeji.logic.vo.Person;

/**
 * The Elastic representation of a {@link Person}
 * 
 * @author bastiens
 * 
 */
public class ElasticPerson {
  private final String familyname;
  private final String givenname;
  private final String completename;
  private final String identifier;
  private final List<ElasticOrganization> organization = new ArrayList<>();

  public ElasticPerson() {
    this.familyname = null;
    this.givenname = null;
    this.identifier = null;
    this.completename = null;
  }

  /**
   * Constructor for a {@link Person}
   * 
   * @param p
   */
  public ElasticPerson(Person p) {
    this.familyname = p.getFamilyName();
    this.givenname = p.getGivenName();
    this.identifier = p.getIdentifier();
    this.completename = p.getCompleteName();
    for (Organization org : p.getOrganizations()) {
      organization.add(new ElasticOrganization(org));
    }
  }


  /**
   * @return the familyname
   */
  public String getFamilyname() {
    return familyname;
  }

  /**
   * @return the givenname
   */
  public String getGivenname() {
    return givenname;
  }

  /**
   * @return the completename
   */
  public String getCompletename() {
    return completename;
  }

  /**
   * @return the identifier
   */
  public String getIdentifier() {
    return identifier;
  }

  /**
   * @return the organisation
   */
  public List<ElasticOrganization> getOrganization() {
    return organization;
  }

}
