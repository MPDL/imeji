package de.mpg.imeji.logic.search.elasticsearch;


import co.elastic.clients.elasticsearch.ElasticsearchClient;
import de.mpg.imeji.logic.search.Search.SearchObjectTypes;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.client.RestClient;

import java.net.URI;

/**
 * elasticsearch service for spot!
 *
 * @author bastiens
 *
 */
public class ElasticService {
  private static ElasticsearchClient client;
  private static RestClient restClient;

  static final String SETTINGS_DEFAULT = "elasticsearch/Settings.json";
  private static final Logger LOGGER = LogManager.getLogger(ElasticService.class);

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


    /**
     * 
     * @param documentId
     * @return
     */
    public static ElasticIndices uriToElasticIndex(URI documentId) {

      // format of id: http://imeji.org/terms/userGroup/idXY
      // extract text in between the last and second but last slash in the id
      String objectType = "";
      String idString = documentId.toString();

      int indexOfLastSlash = idString.lastIndexOf("/");
      if (indexOfLastSlash != -1) {
        String withoutId = idString.substring(0, indexOfLastSlash);
        int indexOfSecondButLastSlash = withoutId.lastIndexOf("/");
        if (indexOfSecondButLastSlash != -1) {
          objectType = withoutId.substring(indexOfSecondButLastSlash + 1, withoutId.length());
          objectType = objectType.toLowerCase();
        }
      }

      switch (objectType) {
        case "item":
          return ElasticIndices.items;
        case "collection":
          return ElasticIndices.folders;
        case "user":
          return ElasticIndices.users;
        case "usergroup":
          return ElasticIndices.usergroups;
        case "content":
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

  public static ElasticsearchClient getClient() {
    return client;
  }

  public static void setClient(ElasticsearchClient client) {
    ElasticService.client = client;
  }

  public static RestClient getRestClient() {
    return restClient;
  }

  public static void setRestClient(RestClient restClient) {
    ElasticService.restClient = restClient;
  }
  /*
   * static Node getNODE() { return NODE; }
   * 
   * static void setNODE(Node nODE) { NODE = nODE; }
   */
}
