package de.mpg.imeji.util;

import org.apache.http.HttpHost;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.testcontainers.elasticsearch.ElasticsearchContainer;

import de.mpg.imeji.logic.search.elasticsearch.ElasticInitializer;

/**
 * Class for the integration tests to connect to ElasticSearch via Testcontainers.
 * 
 * @author helk
 *
 */
public class ElasticsearchTestUtil {

  private static final Logger LOGGER = LogManager.getLogger(ElasticsearchTestUtil.class);

  private static final String ELASTICSEARCH_DOCKER_IMAGE = "docker.elastic.co/elasticsearch/elasticsearch:6.5.4";
  private static ElasticsearchContainer elasticSearchContainer;

  /**
   * Start the ElasticsearchContainer and initialize the imeji Elasticsearch index.
   */
  public static void startElasticsearch() {
    startElatsicSearchContainer();

    initializeElasticsearch();
  }

  private static void startElatsicSearchContainer() {
    LOGGER.info("Creating Elasticsearch Container...");
    elasticSearchContainer = new ElasticsearchContainer(ELASTICSEARCH_DOCKER_IMAGE);
    LOGGER.info("... Elasticsearch Container created.");

    LOGGER.info("Staring Elasticsearch Container...");
    elasticSearchContainer.start();
    LOGGER.info("... Elasticsearch Container started.");
  }

  private static void initializeElasticsearch() {
    String url = elasticSearchContainer.getHttpHostAddress();
    RestClientBuilder local = RestClient.builder(HttpHost.create(url));
    RestHighLevelClient rhlc = new RestHighLevelClient(local);

    try {
      ElasticInitializer.start(rhlc);
    } catch (Exception e) {
      LOGGER.error("Error starting Elasticsearch.", e);
    }
  }

  private static void stopElatsicSearchContainer() {
    LOGGER.info("Stopping Elasticsearch Container...");
    elasticSearchContainer.stop();
    LOGGER.info("... Elasticsearch Container stopped.");
  }

  /**
   * Shutdown Elasticsearch and stop the ElasticsearchContainer.
   */
  public static void stopElasticsearch() {
    ElasticInitializer.shutdown();
    stopElatsicSearchContainer();
  }

}
