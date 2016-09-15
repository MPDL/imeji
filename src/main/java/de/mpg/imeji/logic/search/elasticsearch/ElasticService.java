package de.mpg.imeji.logic.search.elasticsearch;

import org.elasticsearch.client.Client;
import org.elasticsearch.node.Node;


/**
 * elasticsearch service for spot
 *
 * @author bastiens
 *
 */
public class ElasticService {
  private static Node NODE;
  private static Client client;
  static String CLUSTER_NAME = "name of my cluster";
  static boolean CLUSTER_LOCAL = true;
  static boolean CLUSTER_DATA = true;
  static String CLUSTER_DIR = "null";
  public static ElasticAnalysers ANALYSER;
  static final String SETTINGS_DEFAULT = "elasticsearch/Settings_default.json";
  static final String SETTINGS_DUCET = "elasticsearch/Settings_ducet.json";

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
  public enum ElasticTypes {
    items, folders, albums, spaces, users, usergroups;
  }

  public enum ElasticAnalysers {
    standard, ducet_sort, simple;
  }

  public static Client getClient() {
    return client;
  }

  public static void setClient(Client client) {
    ElasticService.client = client;
  }

  static Node getNODE() {
    return NODE;
  }

  static void setNODE(Node nODE) {
    NODE = nODE;
  }
}
