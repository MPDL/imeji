package de.mpg.imeji.testimpl.rest.resources;

import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;

import java.io.IOException;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.mpg.imeji.test.rest.resources.test.integration.ImejiTestBase;

/**
 * Created by vlad on 13.01.15.
 */
public class StorageIntegration extends ImejiTestBase {
  private static final Logger LOGGER = LoggerFactory.getLogger(StorageIntegration.class);

  private final String PATH_PREFIX = "/storage";

  @Test
  public void test_1_uploadFormats() throws IOException {

    Response response = target(PATH_PREFIX).request(MediaType.APPLICATION_JSON_TYPE).get();
    assertEquals(response.getStatus(), OK.getStatusCode());
    assertThat(response.readEntity(String.class), anyOf(containsString("uploadWhiteList"), containsString("uploadBlackList")));

  }

}
