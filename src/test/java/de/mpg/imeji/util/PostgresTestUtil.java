package de.mpg.imeji.util;

import de.mpg.imeji.logic.config.util.PropertyReader;
import de.mpg.imeji.logic.db.writer.EntityManagerHelper;
import de.mpg.imeji.logic.search.elasticsearch.ElasticInitializer;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import org.apache.http.HttpHost;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.elasticsearch.ElasticsearchContainer;

import java.io.IOException;
import java.util.Properties;

/**
 * Class for the integration tests to connect to ElasticSearch via Testcontainers.
 * 
 * @author helk
 *
 */
public class PostgresTestUtil {

  private static final Logger LOGGER = LogManager.getLogger(PostgresTestUtil.class);

  private static PostgreSQLContainer postgreSQLContainer;

  /**
   * Start the ElasticsearchContainer and initialize the imeji Elasticsearch index.
   */
  public static void startPostgres() {
    startPostgresqlContainer();
  }

  private static void startPostgresqlContainer() {
    LOGGER.info("Creating Postgres Container...");
    postgreSQLContainer = new PostgreSQLContainer("postgres:15");


    LOGGER.info("... Postgres Container created.");

    LOGGER.info("Starting Postgres Container...");
    postgreSQLContainer.withDatabaseName("imeji-test").withPassword("postgres").withUsername("postgres").start();
    LOGGER.info("... Postgres Container started.");

    try {
      Properties p = PropertyReader.loadProperties("imeji-db.properties");
      p.setProperty("jakarta.persistence.jdbc.url", postgreSQLContainer.getJdbcUrl());
      EntityManagerHelper.factory = Persistence.createEntityManagerFactory(EntityManagerHelper.PERSISTENCE_UNIT_NAME, p);
    } catch (IOException e) {
      LOGGER.error("Error reading imeji-db.properties", e);
    }


  }


  private static void stopPostgreSqlContainer() {
    LOGGER.info("Stopping Postgres Container...");
    EntityManagerHelper.factory.close();
    postgreSQLContainer.stop();
    LOGGER.info("... Postgres Container stopped.");
  }

  /**
   * Shutdown Elasticsearch and stop the ElasticsearchContainer.
   */
  public static void stopPostgresqlContainer() {}

}
