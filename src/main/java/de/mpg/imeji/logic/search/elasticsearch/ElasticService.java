package de.mpg.imeji.logic.search.elasticsearch;

import org.elasticsearch.client.Client;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.node.Node;

import de.mpg.imeji.logic.search.Search.SearchObjectTypes;

/**
 * elasticsearch service for spot!
 *
 * @author bastiens
 *
 */
public class ElasticService {
  private static RestHighLevelClient client;

  static final String SETTINGS_DEFAULT = "elasticsearch/Settings.json";


  /**
   * The Index where all data are indexed
   */
  public static String DATA_ALIAS = "data";

  /**
   * The Types in Elasticsearch
   *
   * @author bastiens
   *
   */
  public enum ElasticIndices {
    items,
    folders,
    users,
    usergroups;

    /**
     * Map a {@link SearchObjectTypes} to an ElasticIndex
     * 
     * @param type
     * @return
     */
    public static ElasticIndices toElasticIndex(SearchObjectTypes type) {
      switch (type) {
        case ITEM:
          return ElasticIndices.items;
        case COLLECTION:
          return ElasticIndices.folders;
        case USER:
          return ElasticIndices.users;
        case USERGROUPS:
          return ElasticIndices.usergroups;
        case CONTENT:
          return ElasticIndices.items;
        default:
          return ElasticIndices.items;
      }
    }
  }

  public enum ElasticAnalysers {
    standard,
    ducet_sort,
    simple,
    keyword;
  }

  public static RestHighLevelClient getClient() {
    return client;
  }

  public static void setClient(RestHighLevelClient client) {
    ElasticService.client = client;
  }
  /*
   * static Node getNODE() { return NODE; }
   * 
   * static void setNODE(Node nODE) { NODE = nODE; }
   */
}
