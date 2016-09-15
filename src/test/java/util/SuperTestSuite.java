package util;


import java.io.IOException;
import java.net.URISyntaxException;

import org.junit.AfterClass;
import org.junit.BeforeClass;

import de.mpg.imeji.logic.search.elasticsearch.ElasticInitializer;

public class SuperTestSuite {
  @BeforeClass
  public static void startSuite() throws IOException, URISyntaxException {
    ElasticInitializer.start("Integration test - " + System.currentTimeMillis());
  }

  @AfterClass
  public static void endSuite() {
    ElasticInitializer.shutdown();
  }
}
