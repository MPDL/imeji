package de.mpg.imeji.logic.search.elasticsearch.model;

import de.mpg.imeji.logic.vo.Organization;

/**
 * The Elastic representation for an {@link Organization}
 * 
 * @author bastiens
 * 
 */
public final class ElasticOrganization {
  private final String name;
  private final String city;
  private final String country;
  private final String description;

  /**
   * Constructor for a {@link Organization}
   * 
   * @param org
   */
  public ElasticOrganization(Organization org) {
    this.name = org.getName();
    this.description = org.getDescription();
    this.city = org.getCity();
    this.country = org.getCountry();
  }

  /**
   * @return the name
   */
  public String getName() {
    return name;
  }

  /**
   * @return the city
   */
  public String getCity() {
    return city;
  }

  /**
   * @return the country
   */
  public String getCountry() {
    return country;
  }


  /**
   * @return the description
   */
  public String getDescription() {
    return description;
  }
}
