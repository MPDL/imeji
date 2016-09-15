package de.mpg.imeji.logic.search.elasticsearch;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import org.apache.log4j.Logger;
import org.elasticsearch.action.admin.indices.alias.get.GetAliasesRequest;
import org.elasticsearch.cluster.metadata.AliasMetaData;
import org.elasticsearch.common.collect.ImmutableOpenMap;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.node.NodeBuilder;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.config.util.PropertyReader;
import de.mpg.imeji.logic.search.elasticsearch.ElasticService.ElasticAnalysers;
import de.mpg.imeji.logic.search.elasticsearch.ElasticService.ElasticTypes;
import de.mpg.imeji.logic.user.controller.UserBusinessController;
import de.mpg.imeji.logic.vo.User;

/**
 * Start/Stop elasticsearch
 * 
 * @author saquet
 *
 */
public class ElasticInitializer {
  private static final Logger LOGGER = Logger.getLogger(ElasticInitializer.class);

  private ElasticInitializer() {
    // avoid constructor
  }

  public static void start() throws IOException, URISyntaxException {
    start(PropertyReader.getProperty("elastic.cluster.name"));
  }

  public static void start(String clusterName) throws IOException, URISyntaxException {
    ElasticService.CLUSTER_NAME = clusterName;
    ElasticService.CLUSTER_DATA =
        Boolean.parseBoolean(PropertyReader.getProperty("elastic.cluster.data"));
    ElasticService.CLUSTER_LOCAL =
        Boolean.parseBoolean(PropertyReader.getProperty("elastic.cluster.local"));
    ElasticService.CLUSTER_DIR = PropertyReader.getProperty("elastic.cluster.home");
    ElasticService.ANALYSER =
        ElasticAnalysers.valueOf(PropertyReader.getProperty("elastic.analyser"));
    LOGGER.info("Connecting Node to " + ElasticService.CLUSTER_NAME + " (local="
        + ElasticService.CLUSTER_LOCAL + ", data=" + ElasticService.CLUSTER_DATA + ")");
    ElasticService.setNODE(NodeBuilder.nodeBuilder().data(ElasticService.CLUSTER_DATA)
        .local(ElasticService.CLUSTER_LOCAL).clusterName(ElasticService.CLUSTER_NAME)
        .settings(Settings.builder().put("path.home", ElasticService.CLUSTER_DIR)).node());
    ElasticService.setClient(ElasticService.getNODE().client());
    initializeIndex();
    LOGGER.info("Add elasticsearch mappings..." + ElasticService.getNODE().isClosed());
    for (ElasticTypes type : ElasticTypes.values()) {
      new ElasticIndexer(ElasticService.DATA_ALIAS, type, ElasticService.ANALYSER).addMapping();
    }
    LOGGER.info("...done!");
  }

  public static void shutdown() {
    LOGGER.warn("SHUTTING DOWN ELASTICSEARCH...");
    ElasticService.getNODE().close();
    LOGGER.warn("... DONE!");

  }

  /**
   * Initialize the index. If index exists, don't create one, if not create one. Index is created
   * with DATA_ALIAS as alias
   *
   * @return
   */
  public synchronized static String initializeIndex() {
    LOGGER.info("Initializing ElasticSearch index.");
    String indexName = getIndexNameFromAliasName(ElasticService.DATA_ALIAS);
    if (indexName != null) {
      LOGGER.info("Using existing index: " + indexName);
      return indexName;
    } else {
      return createIndexWithAlias();
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
      ElasticService.getClient().admin().indices().prepareAliases()
          .addAlias(indexName, ElasticService.DATA_ALIAS).execute().actionGet();
      return indexName;
    } catch (Exception e) {
      LOGGER.info("Index +" + "+ already existing");
    }
    return null;
  }

  /**
   * Get the First Index pointed by the Alias. This method should be used, when there is only one
   * Index pointed by the alias (on startup for instance)
   *
   * @param aliasName
   * @return
   */
  public synchronized static String getIndexNameFromAliasName(final String aliasName) {
    ImmutableOpenMap<String, List<AliasMetaData>> map = ElasticService.getClient().admin().indices()
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
   * Atomically move the alias from the old to the new index
   *
   * @param oldIndex
   * @param newIndex
   */
  public static void setNewIndexAndRemoveOldIndex(String newIndex) {
    String oldIndex = getIndexNameFromAliasName(ElasticService.DATA_ALIAS);

    if (oldIndex != null && !oldIndex.equals(newIndex)) {
      ElasticService.getClient().admin().indices().prepareAliases()
          .addAlias(newIndex, ElasticService.DATA_ALIAS)
          .removeAlias(oldIndex, ElasticService.DATA_ALIAS).execute().actionGet();
      ElasticService.getClient().admin().indices().prepareDelete(oldIndex).execute().actionGet();
    } else {
      ElasticService.getClient().admin().indices().prepareAliases()
          .addAlias(newIndex, ElasticService.DATA_ALIAS).execute().actionGet();
    }
  }

  /**
   * Create a new Index (without alias)
   *
   * @return
   */
  public static String createIndex() {
    try {
      String indexName = ElasticService.DATA_ALIAS + "-" + System.currentTimeMillis();
      LOGGER.info("Creating a new index " + indexName);
      String settingsName = ElasticService.ANALYSER == ElasticAnalysers.ducet_sort
          ? ElasticService.SETTINGS_DUCET : ElasticService.SETTINGS_DEFAULT;
      String settingsJson = new String(
          Files.readAllBytes(
              Paths.get(ElasticIndexer.class.getClassLoader().getResource(settingsName).toURI())),
          "UTF-8");
      ElasticService.getClient().admin().indices().prepareCreate(indexName)
          .setSettings(settingsJson).execute().actionGet();
      return indexName;
    } catch (Exception e) {
      LOGGER.error("Error creating index", e);
    }
    return null;
  }

  /**
   * DANGER: delete all data from elasticsearch. A new reindex will be necessary
   */
  public static void reset() {
    LOGGER.warn("Resetting ElasticSearch!!!");
    clear();
    initializeIndex();
    for (ElasticTypes type : ElasticTypes.values()) {
      new ElasticIndexer(ElasticService.DATA_ALIAS, type, ElasticService.ANALYSER).addMapping();
    }
    indexUsers(ElasticService.DATA_ALIAS);
  }

  public static void indexUsers(String index) {
    try {
      LOGGER.info("Indexing users...");
      ElasticIndexer indexer =
          new ElasticIndexer(index, ElasticTypes.users, ElasticService.ANALYSER);
      List<User> users = new UserBusinessController().retrieveAll();
      LOGGER.info("+++ " + users.size() + " users to index +++");
      indexer.indexBatch(users);
      indexer.commit();
      LOGGER.info("...users reindexed!");
    } catch (ImejiException e) {
      LOGGER.error("Error reindexing users", e);
    }
  }

  /**
   * Remove everything from ES
   */
  public static void clear() {
    LOGGER.warn("Deleting all indexes...");
    ElasticService.getClient().admin().indices().prepareDelete("_all").execute().actionGet();
    LOGGER.warn("...done!");
  }

}
