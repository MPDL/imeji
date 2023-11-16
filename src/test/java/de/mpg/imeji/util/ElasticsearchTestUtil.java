package de.mpg.imeji.util;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.json.jsonb.JsonbJsonpMapper;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
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

  private static final String ELASTICSEARCH_DOCKER_IMAGE = "docker.elastic.co/elasticsearch/elasticsearch:8.11.0";
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
    elasticSearchContainer.getEnvMap().put("xpack.security.enabled", "false");
    //elasticSearchContainer.withPassword("s3cret");
    LOGGER.info("... Elasticsearch Container created.");

    LOGGER.info("Staring Elasticsearch Container...");
    elasticSearchContainer.start();
    LOGGER.info("... Elasticsearch Container started.");
  }

  private static void initializeElasticsearch() {
    String url = elasticSearchContainer.getHttpHostAddress();
    RestClientBuilder builder = RestClient.builder(HttpHost.create(url));

    /*
    final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
    credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials("elastic", "s3cret"));
    
    builder.setHttpClientConfigCallback(clientBuilder -> {
      //clientBuilder.setSSLContext(SslUtils.createContextFromCaCert(certAsBytes));
      clientBuilder.setDefaultCredentialsProvider(credentialsProvider);
      return clientBuilder;
    });
    */
    //RestHighLevelClient rhlc = new RestHighLevelClient(local);
    //ElasticsearchClient elClient = new ElasticsearchClient(new RestClientTransport(local.build(), new JacksonJsonpMapper()));
    try {
      ElasticInitializer.start(builder.build());
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
