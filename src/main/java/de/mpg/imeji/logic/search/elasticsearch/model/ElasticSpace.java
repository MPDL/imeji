package de.mpg.imeji.logic.search.elasticsearch.model;

import java.util.ArrayList;
import java.util.List;

import de.mpg.imeji.logic.vo.Item;
import de.mpg.imeji.logic.vo.Space;

/**
 * The object which is indexed in Elastic search <br/>
 * !!! IMPORTANT !!!<br/>
 * This File must be synchronized with resources/elasticsearch/ElasticItemsMapping.json
 * 
 * @author bastiens
 * 
 */
public final class ElasticSpace extends ElasticProperties {
  private final List<String> folder;
  private final String slug;
  private final String name;
  private final String description;

  /**
   * Constructor with an {@link Item}
   * 
   * @param item
   */
  public ElasticSpace(Space space) {
    super(space);
    this.folder = new ArrayList<>(space.getSpaceCollections());
    this.slug = space.getSlug();
    this.name = space.getTitle();
    this.description = space.getDescription();
  }

  /**
   * @return the slug
   */
  public String getSlug() {
    return slug;
  }

  /**
   * @return the description
   */
  public String getDescription() {
    return description;
  }

  public List<String> getFolder() {
    return folder;
  }

  public String getName() {
    return name;
  }
}
