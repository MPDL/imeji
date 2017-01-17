package de.mpg.imeji.logic.search.elasticsearch.model;

import de.mpg.imeji.logic.vo.CollectionImeji;

/**
 * The elastic Version of a {@link CollectionImeji}
 *
 * @author bastiens
 *
 */
public final class ElasticFolder extends ElasticContainerProperties {

  public ElasticFolder(CollectionImeji c) {
    super(c);
  }

}
