package de.mpg.imeji.testimpl.rest.resources.item;

import static de.mpg.imeji.logic.util.ResourceHelper.getStringFromPath;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;

import java.io.IOException;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.media.multipart.file.FileDataBodyPart;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.mpg.imeji.rest.to.defaultItemTO.DefaultItemTO;
import de.mpg.imeji.test.rest.resources.test.integration.ItemTestBase;
import de.mpg.imeji.testimpl.ImejiTestResources;

/**
 * Created by vlad on 09.12.14.
 */

public class ItemDefaultMdCreate extends ItemTestBase {

  private static final Logger LOGGER = LoggerFactory.getLogger(ItemDefaultMdCreate.class);

  private static final String referenceUrl =
      "http://th03.deviantart.net/fs71/PRE/i/2012/242/1/f/png_moon_by_paradise234-d5czhdo.png";

  private static String itemJSON;
  private static final String pathPrefix = "/rest/items";

  DefaultItemTO defaultItemTO;

  @BeforeClass
  public static void specificSetup() throws Exception {
    initCollectionWithProfile(getDefaultBasicStatements());
    itemJSON = getStringFromPath("src/test/resources/rest/defaultCreateItem.json");
  }

  @Test
  public void test_1_createItem_emptySyntaxParameter() throws IOException {
    // Do not provide Syntax Parameter (should be default)
    FormDataMultiPart multiPart = new FormDataMultiPart();
    String replacedJSON =
        itemJSON.replace("___COLLECTION_ID___", collectionId).replace("___FILENAME___", "test.png")
            .replaceAll("\"fetchUrl\"\\s*:\\s*\"___FETCH_URL___\",", "")
            .replaceAll("___REFERENCE_URL___", referenceUrl);
    multiPart.field("json", replacedJSON);


    Response response = getTargetAuth().post(Entity.entity(multiPart, multiPart.getMediaType()));
    assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());

    defaultItemTO = response.readEntity(DefaultItemTO.class);

    assertThat(defaultItemTO.getMetadata().keySet(), hasSize(7)); // check defaultCreateItem.json
    assertThat(defaultItemTO.getCollectionId(), equalTo(collectionId));


  }

  @Test
  public void test_2_createItem_defaultSyntaxParameter() throws IOException {
    // Do provide Syntax Parameter "DEFAULT"


    FormDataMultiPart multiPart = new FormDataMultiPart();
    multiPart.bodyPart(new FileDataBodyPart("file", ImejiTestResources.getTest3Jpg()));
    multiPart.field("json",
        itemJSON.replace("___COLLECTION_ID___", collectionId).replace("___FILENAME___", "test3.jpg")
            .replaceAll("\"fetchUrl\"\\s*:\\s*\"___FETCH_URL___\",", "")
            .replaceAll("\"referenceUrl\"\\s*:\\s*\"___REFERENCE_URL___\",", ""));

    Response response = target(pathPrefix).register(authAsUser).register(MultiPartFeature.class)
        .register(JacksonFeature.class).request(MediaType.APPLICATION_JSON_TYPE)
        .post(Entity.entity(multiPart, multiPart.getMediaType()));

    assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());

    defaultItemTO = response.readEntity(DefaultItemTO.class);
    assertThat(defaultItemTO.getMetadata().keySet(), hasSize(7)); // check defaultCreateItem.json
    assertThat(defaultItemTO.getCollectionId(), equalTo(collectionId));

  }

  @Test
  public void test_3_createItem_empty_defaultSyntax() throws IOException {

    // Create default item without metadata
    FormDataMultiPart multiPart = new FormDataMultiPart();
    multiPart.bodyPart(new FileDataBodyPart("file", ImejiTestResources.getTest4Jpg()));
    multiPart.field("json",
        itemJSON.replace("___COLLECTION_ID___", collectionId).replace("___FILENAME___", "test4.jpg")
            .replaceAll("\"fetchUrl\"\\s*:\\s*\"___FETCH_URL___\",", "")
            .replaceAll("\"referenceUrl\"\\s*:\\s*\"___REFERENCE_URL___\",", "")
            .replaceAll("\"metadata\"\\s*:\\s*\\{[\\d\\D]*\\}", "\"metadata\":{}}"));

    Response response = target(pathPrefix).register(authAsUser).register(MultiPartFeature.class)
        .register(JacksonFeature.class).request(MediaType.APPLICATION_JSON_TYPE)
        .post(Entity.entity(multiPart, multiPart.getMediaType()));

    assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());

    defaultItemTO = response.readEntity(DefaultItemTO.class);
    assertThat(defaultItemTO.getMetadata().keySet(), empty()); // check defaultCreateItem.json
    assertThat(defaultItemTO.getCollectionId(), equalTo(collectionId));

  }

  @Test
  public void test_5_createItem_defaultSyntax_badTypedValues() throws IOException {
    test_5_defaultSyntax_badTypedValues("", itemJSON);
  }

  @Test
  public void test_6_createItem_ExistingDefaultFields() throws IOException {
    test_6_ExistingDefaultFields("", itemJSON);
  }


  @Test
  public void test_5_createItem_defaultSyntax_badJsonSyntax() throws IOException {

    FormDataMultiPart multiPart = new FormDataMultiPart();
    multiPart.bodyPart(new FileDataBodyPart("file", ImejiTestResources.getTest5Jpg()));
    multiPart.field("json",
        itemJSON.replace("___COLLECTION_ID___", collectionId).replace("___FILENAME___", "test5.jpg")
            .replaceAll("\"fetchUrl\"\\s*:\\s*\"___FETCH_URL___\",", "")
            .replaceAll("\"referenceUrl\"\\s*:\\s*\"___REFERENCE_URL___\",", "")
            //
            .replaceAll("\"number\"\\s*:.*,", "some\"text,,,,,"));
    Response response = getTargetAuth().post(Entity.entity(multiPart, multiPart.getMediaType()));

    assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());

  }

  private Invocation.Builder getTargetAuth() {
    return target(pathPrefix).register(authAsUser).register(MultiPartFeature.class)
        .register(JacksonFeature.class).request(MediaType.APPLICATION_JSON_TYPE);
  }


}
