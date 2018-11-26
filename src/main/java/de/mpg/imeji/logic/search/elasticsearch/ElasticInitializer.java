package de.mpg.imeji.logic.search.elasticsearch;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.apache.logging.log4j.Logger;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpStatus;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.elasticsearch.action.admin.indices.alias.get.GetAliasesRequest;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.cluster.metadata.AliasMetaData;
import org.elasticsearch.common.collect.ImmutableOpenMap;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.node.Node;

import de.mpg.imeji.logic.config.util.PropertyReader;
import de.mpg.imeji.logic.search.elasticsearch.ElasticService.ElasticAnalysers;
import de.mpg.imeji.logic.search.elasticsearch.ElasticService.ElasticIndices;

/**
 * Start/Stop elasticsearch
 *
 * @author saquet
 *
 */
public class ElasticInitializer {
	private static final Logger LOGGER = LogManager.getLogger(ElasticInitializer.class);

	private ElasticInitializer() {
		// avoid constructor
	}

	public static void start() throws IOException, URISyntaxException {
		start(PropertyReader.getProperty("elastic.cluster.name"));
	}

	/*
	 * public static void start(String clusterName) throws IOException,
	 * URISyntaxException { TransportClient tc = TransportClient.builder().settings(
	 * Settings.builder().put("path.home",
	 * ElasticService.CLUSTER_DIR).put("cluster.name", clusterName)) .build();
	 * tc.addTransportAddress(new
	 * InetSocketTransportAddress(InetAddress.getByName("localhost"), 9300));
	 * start(clusterName, tc); }
	 */

	public static void start(String cluster_name) throws IOException, URISyntaxException {
		RestHighLevelClient rhlc = null;
		RestClientBuilder local = RestClient.builder(new HttpHost("localhost", 9200, "http"));
		rhlc = new RestHighLevelClient(local);
		start(cluster_name, rhlc);
	}

	/*
	 * @SuppressWarnings("resource") public static void startLocal(String
	 * clusterName) throws IOException, URISyntaxException { Settings settings =
	 * Settings.builder().put("path.home", ElasticService.CLUSTER_DIR)
	 * .put("cluster.name", clusterName).build(); ElasticService.setNODE(new
	 * Node(settings).start()); start(clusterName,
	 * ElasticService.getNODE().client()); }
	 */

	private static void start(String clusterName, RestHighLevelClient client) throws IOException, URISyntaxException {
		ElasticService.CLUSTER_NAME = clusterName;
		// ElasticService.ANALYSER =
		// ElasticAnalysers.valueOf(PropertyReader.getProperty("elastic.analyser"));
		ElasticService.setClient(client);
		// initializeIndex();
		LOGGER.info("Add elasticsearch mappings...");
		for (final ElasticIndices index : ElasticIndices.values()) {
			initializeIndex(index);
			new ElasticIndexer(index.name()).addMapping();
		}
		LOGGER.info("...done!");
	}

	/*
	 * public static void shutdown() {
	 * LOGGER.warn("SHUTTING DOWN ELASTICSEARCH..."); if (ElasticService.getNODE()
	 * != null) { ElasticService.getNODE().close(); } LOGGER.warn("... DONE!");
	 * 
	 * }
	 */

	/**
	 * Initialize the index. If index exists, don't create one, if not create one.
	 * Index is created with DATA_ALIAS as alias
	 *
	 * @return
	 */
	public synchronized static String initializeIndex(ElasticIndices index) {
		LOGGER.info("Initializing ElasticSearch index.");

		Request req = new Request("HEAD", "/" + index.name());
		Response resp;
		try {
			resp = ElasticService.getClient().getLowLevelClient().performRequest(req);
			if (resp.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
				LOGGER.info("Using existing index: " + index.name());
				return index.name();
			} else {
				return createIndexWithAlias(index);
			}
		} catch (IOException e) {
			LOGGER.error("Error getting index status", e);
		}
		return null;
	}

	/**
	 * Create an Index point an alias to it
	 *
	 * @return
	 */
	public static String createIndexWithAlias(ElasticIndices index) {
		try {
			final String indexName = createIndex(index.name());
			LOGGER.info("Adding Alias to index " + indexName);
			Request req = new Request("PUT", "/" + indexName + "/_alias/" + index.name());
			Response resp = ElasticService.getClient().getLowLevelClient().performRequest(req);
			if (resp.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
				return indexName;
			}
		} catch (final Exception e) {
			LOGGER.info("Index +" + "+ already existing");
		}
		return null;
	}

	/**
	 * Get the First Index pointed by the Alias. This method should be used, when
	 * there is only one Index pointed by the alias (on startup for instance)
	 *
	 * @param aliasName
	 * @return
	 */
	/*
	 * public synchronized static String getIndexNameFromAliasName(final String
	 * aliasName) { try { final ImmutableOpenMap<String, List<AliasMetaData>> map =
	 * ElasticService.getClient().admin().indices() .getAliases(new
	 * GetAliasesRequest(aliasName)).actionGet().getAliases(); if (map.keys().size()
	 * > 1) { LOGGER.error("Alias " + aliasName +
	 * " has more than one index. This is forbidden: All indexes will be removed, please reindex!!!"
	 * ); reset(); return null; } else if (map.keys().size() == 1) { return
	 * map.keys().iterator().next().value; } } catch (Exception e) {
	 * LOGGER.error("getIndexNameFromAliasName error", e); } return null; }
	 */

	/**
	 * Atomically move the alias from the old to the new index
	 *
	 * @param oldIndex
	 * @param newIndex
	 */
	/*
	 * public static void setNewIndexAndRemoveOldIndex(String oldIndex, String
	 * newIndex) { // final String oldIndexName =
	 * getIndexNameFromAliasName(oldIndex);
	 * 
	 * if (oldIndex != null && !oldIndex.equals(newIndex)) {
	 * ElasticService.getClient().admin().indices().prepareAliases().addAlias(
	 * newIndex, ElasticService.DATA_ALIAS) .removeAlias(oldIndex,
	 * ElasticService.DATA_ALIAS).execute().actionGet();
	 * ElasticService.getClient().admin().indices().prepareDelete(oldIndex).execute(
	 * ).actionGet(); } else {
	 * ElasticService.getClient().admin().indices().prepareAliases().addAlias(
	 * newIndex, ElasticService.DATA_ALIAS) .execute().actionGet(); } }
	 */

	/**
	 * Create a new Index (without alias)
	 *
	 * @return
	 */
	public static String createIndex(String name) {
		try {
			final String indexName = name + "-" + System.currentTimeMillis();

			LOGGER.info("Creating a new index " + indexName);

			final String settingsName = ElasticService.SETTINGS_DUCET;

			final Path settingsJson = Paths
					.get(ElasticIndexer.class.getClassLoader().getResource(settingsName).toURI());
			HttpEntity entity = new InputStreamEntity(Files.newInputStream(settingsJson), ContentType.APPLICATION_JSON);
			Request req = new Request("PUT", "/" + indexName);
			req.setEntity(entity);
			Response resp = ElasticService.getClient().getLowLevelClient().performRequest(req);
			if (resp.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
				LOGGER.info("successfully created " + indexName + " " + EntityUtils.toString(resp.getEntity()));
			}
			return indexName;
		} catch (final Exception e) {
			LOGGER.error("Error creating index", e);
		}
		return null;
	}

	/**
	 * DANGER: delete all data from elasticsearch. A new reindex will be necessary
	 */
	public static void reset() {
		LOGGER.warn("Resetting ElasticSearch!!!");
		// clear();
		for (final ElasticIndices index : ElasticIndices.values()) {
			initializeIndex(index);
			// new ElasticIndexer(ElasticService.DATA_ALIAS, index,
			// ElasticService.ANALYSER).addMapping();
		}
	}

	/**
	 * Remove everything from ES
	 */
	/*
	 * public static void clear() { LOGGER.warn("Deleting all indexes...");
	 * ElasticService.getClient().admin().indices().prepareDelete("_all").execute().
	 * actionGet(); LOGGER.warn("...done!"); }
	 */

}
