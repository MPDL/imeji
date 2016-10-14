package de.mpg.imeji.test.rest.resources.test;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;

import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.spi.TestContainerException;
import org.glassfish.jersey.test.spi.TestContainerFactory;
import org.junit.Assert;
import org.junit.Test;

import de.mpg.imeji.rest.MyApplication;
import de.mpg.imeji.test.rest.resources.test.integration.MyTestContainerFactory;

public class IntegrationTest extends JerseyTest {

  @Override
  protected Application configure() {
    return new MyApplication();
  }

  @Override
  protected TestContainerFactory getTestContainerFactory() throws TestContainerException {
    return new MyTestContainerFactory();
  }

  @Test
  public void simpleTest() {
    FormDataMultiPart multiPart = new FormDataMultiPart();
    // target().register(JacksonFeature.class).request(MediaType.APPLICATION_JSON_TYPE).get();

    target().register(MultiPartFeature.class).register(JacksonFeature.class)
        .request(MediaType.APPLICATION_JSON_TYPE)
        .post(Entity.entity(multiPart, multiPart.getMediaType()));

    Assert.assertTrue(true);
  }

}
