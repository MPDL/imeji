package de.mpg.imeji.logic.search.elasticsearch;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import org.apache.log4j.Logger;
import org.elasticsearch.action.admin.indices.alias.get.GetAliasesRequest;
import org.elasticsearch.client.Client;
import org.elasticsearch.cluster.metadata.AliasMetaData;
import org.elasticsearch.common.collect.ImmutableOpenMap;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;

import de.mpg.imeji.logic.util.PropertyReader;

/**
 * elasticsearch service for spot
 *
 * @author bastiens
 *
 */
public class ElasticService {
  private static Node node;
  public static Client client;
  private static String CLUSTER_NAME = "name of my cluster";
  private static boolean CLUSTER_LOCAL = true;
  private static boolean CLUSTER_DATA = true;
  private static String CLUSTER_DIR = "null";
  public static ElasticAnalysers ANALYSER;
  private static final Logger LOGGER = Logger.getLogger(ElasticService.class);
  private static final String SETTINGS_DEFAULT = "elasticsearch/Settings_default.json";
  private static final String SETTINGS_DUCET = "elasticsearch/Settings_ducet.json";

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
    items, folders, albums, spaces;
  }

  public enum ElasticAnalysers {
    standard, ducet_sort, simple;
  }

  public static void start() throws IOException, URISyntaxException {
    start(PropertyReader.getProperty("elastic.cluster.name"));
  }

  public static void start(String clusterName) throws IOException, URISyntaxException {
    CLUSTER_NAME = clusterName;
    CLUSTER_DATA = Boolean.parseBoolean(PropertyReader.getProperty("elastic.cluster.data"));
    CLUSTER_LOCAL = Boolean.parseBoolean(PropertyReader.getProperty("elastic.cluster.local"));
    CLUSTER_DIR = PropertyReader.getProperty("elastic.cluster.home");
    ANALYSER = ElasticAnalysers.valueOf(PropertyReader.getProperty("elastic.analyser"));
    LOGGER.info("Connecting Node to " + CLUSTER_NAME + " (local=" + CLUSTER_LOCAL + ", data="
        + CLUSTER_DATA + ")");
    node =
        NodeBuilder.nodeBuilder().data(CLUSTER_DATA).local(CLUSTER_LOCAL).clusterName(CLUSTER_NAME)
            .settings(Settings.builder().put("path.home", CLUSTER_DIR)).node();

    client = node.client();
    initializeIndex();
    LOGGER.info("Add elasticsearch mappings...");
    new ElasticIndexer(DATA_ALIAS, ElasticTypes.items, ANALYSER).addMapping();
    new ElasticIndexer(DATA_ALIAS, ElasticTypes.folders, ANALYSER).addMapping();
    new ElasticIndexer(DATA_ALIAS, ElasticTypes.albums, ANALYSER).addMapping();
    new ElasticIndexer(DATA_ALIAS, ElasticTypes.spaces, ANALYSER).addMapping();
    LOGGER.info("...done!");
  }

  /**
   * Initialize the index. If index exists, don't create one, if not create one. Index is created
   * with DATA_ALIAS as alias
   *
   * @return
   */
  public synchronized static String initializeIndex() {
    LOGGER.info("Initializing ElasticSearch index.");
    String indexName = getIndexNameFromAliasName(DATA_ALIAS);
    if (indexName != null) {
      LOGGER.info("Using existing index: " + indexName);
      return indexName;
    } else {
      return createIndexWithAlias();
    }
  }


  /**
   * Get the First Index pointed by the Alias. This method should be used, when there is only one
   * Index pointed by the alias (on startup for instance)
   *
   * @param aliasName
   * @return
   */
  public synchronized static String getIndexNameFromAliasName(final String aliasName) {
    ImmutableOpenMap<String, List<AliasMetaData>> map = client.admin().indices()
        .getAliases(new GetAliasesRequest(aliasName)).actionGet().getAliases();
    if (map.keys().size() > 1) {
      LOGGER.error("Alias " + aliasName
          + " has more than one index. This is forbidden: All indexes will be removed, please reindex!!!");
      reset();
      return null;
    } else if (map.keys().size() == 1) {
      return map.keys().iterator().next().value;
    } else {
      return null;
    }
  }

  /**
   * Create an Index point an alias to it
   *
   * @return
   */
  public static String createIndexWithAlias() {
    try {
      String indexName = createIndex();
      LOGGER.info("Adding Alias to index " + indexName);
      ElasticService.client.admin().indices().prepareAliases().addAlias(indexName, DATA_ALIAS)
          .execute().actionGet();
      return indexName;
    } catch (Exception e) {
      LOGGER.info("Index +" + "+ already existing");
    }
    return null;
  }

  /**
   * Create a new Index (without alias)
   *
   * @return
   */
  public static String createIndex() {
    try {
      String indexName = DATA_ALIAS + "-" + System.currentTimeMillis();
      LOGGER.info("Creating a new index " + indexName);
      String settingsName =
          ANALYSER == ElasticAnalysers.ducet_sort ? SETTINGS_DUCET : SETTINGS_DEFAULT;
      String settingsJson = new String(
          Files.readAllBytes(
              Paths.get(ElasticIndexer.class.getClassLoader().getResource(settingsName).toURI())),
          "UTF-8");
      ElasticService.client.admin().indices().prepareCreate(indexName).setSettings(settingsJson)
          .execute().actionGet();
      return indexName;
    } catch (Exception e) {
      LOGGER.error("Error creating index", e);
    }
    return null;
  }

  /**
   * Atomically move the alias from the old to the new index
   *
   * @param oldIndex
   * @param newIndex
   */
  public static void setNewIndexAndRemoveOldIndex(String newIndex) {
    String oldIndex = getIndexNameFromAliasName(DATA_ALIAS);

    if (oldIndex != null && !oldIndex.equals(newIndex)) {
      ElasticService.client.admin().indices().prepareAliases().addAlias(newIndex, DATA_ALIAS)
          .removeAlias(oldIndex, DATA_ALIAS).execute().actionGet();
      ElasticService.client.admin().indices().prepareDelete(oldIndex).execute().actionGet();
    } else {
      ElasticService.client.admin().indices().prepareAliases().addAlias(newIndex, DATA_ALIAS)
          .execute().actionGet();
    }

  }

  /**
   * DANGER: delete all data from elasticsearch. A new reindex will be necessary
   */
  public static void reset() {
    LOGGER.warn("Resetting ElasticSearch!!!");
    clear();
    initializeIndex();
    new ElasticIndexer(DATA_ALIAS, ElasticTypes.items, ANALYSER).addMapping();
    new ElasticIndexer(DATA_ALIAS, ElasticTypes.folders, ANALYSER).addMapping();
    new ElasticIndexer(DATA_ALIAS, ElasticTypes.albums, ANALYSER).addMapping();
    new ElasticIndexer(DATA_ALIAS, ElasticTypes.spaces, ANALYSER).addMapping();
  }

  /**
   * Remove everything from ES
   */
  public static void clear() {
    LOGGER.warn("Deleting all indexes...");
    ElasticService.client.admin().indices().prepareDelete(DATA_ALIAS).execute().actionGet();
    LOGGER.warn("...done!");
  }

  public static void shutdown() {
    node.close();
  }
}
