package de.mpg.imeji.util;

import java.util.concurrent.TimeUnit;

import org.junit.AfterClass;
import org.junit.BeforeClass;

public class SuperTestSuite {
  private static final int MAX_START_RETRY = 3;

  //TODO: Embedded Elasticsearch is not supported any more => A new approach to start/stop Elasticsearch for the tests is needed

  @BeforeClass
  public static void startSuite() throws Exception {
    startElasticSearch(0);
  }

  /**
   * Start elasticsearch and retry if a problem occurs (happens when ES is still being shutdown by
   * another test)
   * 
   * @param count
   * @throws Exception
   */
  private static void startElasticSearch(int count) throws Exception {
    try {
      //      ElasticInitializer.startLocal(PropertyReader.getProperty("elastic.cluster.name"));
    } catch (Exception e) {
      if (count < MAX_START_RETRY) {
        TimeUnit.SECONDS.wait(1);
        startElasticSearch(count++);
      } else {
        throw e;
      }
    }
  }

  @AfterClass
  public static void endSuite() {
    //    ElasticInitializer.shutdown();
  }
}
