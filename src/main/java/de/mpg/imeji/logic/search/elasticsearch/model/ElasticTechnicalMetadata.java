package de.mpg.imeji.logic.search.elasticsearch.model;

import de.mpg.imeji.logic.vo.TechnicalMetadata;

/**
 * Elastic object for technical metadata
 * 
 * @author saquet
 *
 */
public class ElasticTechnicalMetadata {
  private final String name;
  private final String value;

  /**
   * Constructor for a technical metadata
   * 
   * @param tmd
   */
  public ElasticTechnicalMetadata(TechnicalMetadata tmd) {
    this.name = tmd.getName();
    this.value = tmd.getValue();
  }

  public String getName() {
    return name;
  }

  public String getValue() {
    return value;
  }

}
