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

  /**
   * Constructor for a {@link Organization}
   *
   * @param org
   */
  public ElasticOrganization(Organization org) {
    this.name = org.getName();
  }

  /**
   * @return the name
   */
  public String getName() {
    return name;
  }
}
