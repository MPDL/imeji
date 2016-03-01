package de.mpg.imeji.logic.search.elasticsearch.model;

import de.mpg.imeji.logic.vo.CollectionImeji;

/**
 * The elastic Version of a {@link CollectionImeji}
 * 
 * @author bastiens
 * 
 */
public final class ElasticFolder extends ElasticContainerProperties {
  private final String profile;

  public ElasticFolder(CollectionImeji c) {
    super(c);
    if (c.getProfile() != null) {
      profile = c.getProfile().toString();
    } else {
      profile = null;
    }
  }

  /**
   * @return the profile
   */
  public String getProfile() {
    return profile;
  }

}
