package de.mpg.imeji.testimpl.rest.resources.item;

import static de.mpg.imeji.logic.util.ResourceHelper.getStringFromPath;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;

import java.io.IOException;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.httpclient.HttpStatus;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.mpg.imeji.rest.to.defaultItemTO.DefaultItemTO;
import de.mpg.imeji.test.rest.resources.test.integration.ItemTestBase;

/**
 * Created by vlad on 09.12.14.
 */

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ItemDefaultMdUpdateHierarchical extends ItemTestBase {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(ItemDefaultMdUpdateHierarchical.class);

  private static final String referenceUrl =
      "http://th03.deviantart.net/fs71/PRE/i/2012/242/1/f/png_moon_by_paradise234-d5czhdo.png";

  private static String itemJSON;
  private static final String pathPrefix = "/rest/items";

  DefaultItemTO defaultItemTO;

  @BeforeClass
  public static void specificSetup() throws Exception {
    initCollectionWithProfile(getDefaultHierarchicalStatements());
    itemJSON = getStringFromPath("src/test/resources/rest/defaultCreateItemHierarchical.json");
    createItem();
  }

  @Test
  public void test_1_updateItem_emptySyntaxParameter() throws IOException {
    // Do not provide Syntax Parameter (should be default)
    FormDataMultiPart multiPart = new FormDataMultiPart();
    // replace fetchUrl with itemId (reuse same hiearachical JSON)
    String replacedJSON =
        itemJSON.replace("___COLLECTION_ID___", collectionId).replace("___FILENAME___", "test.png")
            .replaceAll("\"fetchUrl\"\\s*:\\s*\"___FETCH_URL___\",", "\"id\": \"" + itemId + "\", ")
            .replaceAll("\"referenceUrl\"\\s*:\\s*\"___FETCH_URL___\",", "");
    multiPart.field("json", replacedJSON);


    Response response =
        getTargetAuth(itemId).put(Entity.entity(multiPart, multiPart.getMediaType()));
    assertEquals(Status.OK.getStatusCode(), response.getStatus());

    defaultItemTO = response.readEntity(DefaultItemTO.class);

    assertThat(defaultItemTO.getMetadata().keySet(), hasSize(8)); // check
                                                                  // defaultCreateItemHierarchical.json,
                                                                  // only top level elements count
    assertThat(defaultItemTO.getCollectionId(), equalTo(collectionId));
    assertThat(defaultItemTO.getId(), equalTo(itemId));


  }

  @Test
  public void test_5_updateItem_defaultSyntax_badTypedValues() throws IOException {
    String replacedJSON =
        itemJSON.replace("___COLLECTION_ID___", collectionId).replace("___FILENAME___", "test.png")
            .replaceAll("\"fetchUrl\"\\s*:\\s*\"___FETCH_URL___\",", "\"id\": \"" + itemId + "\", ")
            .replaceAll("\"referenceUrl\"\\s*:\\s*\"___FETCH_URL___\",", "");
    test_5_defaultSyntax_badTypedValues(itemId, replacedJSON);

  }


  @Test
  public void test_6_updateItem_ExistingDefaultFields() throws IOException {
    String replacedJSON =
        itemJSON.replace("___COLLECTION_ID___", collectionId).replace("___FILENAME___", "test.png")
            .replaceAll("\"fetchUrl\"\\s*:\\s*\"___FETCH_URL___\",", "\"id\": \"" + itemId + "\", ")
            .replaceAll("\"referenceUrl\"\\s*:\\s*\"___FETCH_URL___\",", "");
    test_6_ExistingDefaultFields(itemId, replacedJSON);

  }


  @Test
  public void test_5_updateItem_defaultSyntax_badJsonSyntax() throws IOException {

    FormDataMultiPart multiPart = new FormDataMultiPart();

    multiPart.field("json",
        itemJSON.replace("___COLLECTION_ID___", collectionId).replace("___FILENAME___", "test.png")
            .replaceAll("\"fetchUrl\"\\s*:\\s*\"___FETCH_URL___\",", "\"id\": \"" + itemId + "\", ")
            .replaceAll("\"referenceUrl\"\\s*:\\s*\"___FETCH_URL___\",", "")
            .replaceAll("\"number\"\\s*:.*,", "some\"text,,,,,"));
    Response response =
        getTargetAuth(itemId).put(Entity.entity(multiPart, multiPart.getMediaType()));

    assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());

  }



  @Test
  public void test_6_updateItem_defaultSyntax_wrongParent() throws IOException {

    FormDataMultiPart multiPart = new FormDataMultiPart();
    multiPart.field("json",
        itemJSON.replace("___COLLECTION_ID___", collectionId).replace("___FILENAME___", "test.png")
            .replaceAll("\"fetchUrl\"\\s*:\\s*\"___FETCH_URL___\",", "\"id\": \"" + itemId + "\", ")
            .replaceAll("\"referenceUrl\"\\s*:\\s*\"___FETCH_URL___\",", "")
            .replaceAll("\"" + "text" + "\"\\s*:", "\"" + "textChild" + "\":"));

    Response response =
        getTargetAuth(itemId).put(Entity.entity(multiPart, multiPart.getMediaType()));

    assertEquals(HttpStatus.SC_UNPROCESSABLE_ENTITY, response.getStatus());
  }


  @Test
  public void test_6_createItem_defaultSyntax_singleValueWhereMultiple() throws IOException {

    FormDataMultiPart multiPart = new FormDataMultiPart();
    multiPart.field("json",
        getStringFromPath("src/test/resources/rest/defaultCreateItemHierarchicalWrong.json")
            .replace("___COLLECTION_ID___", collectionId).replace("___FILENAME___", "test.png")
            .replaceAll("\"fetchUrl\"\\s*:\\s*\"___FETCH_URL___\",", "\"id\": \"" + itemId + "\", ")
            .replaceAll("\"referenceUrl\"\\s*:\\s*\"___FETCH_URL___\",", ""));

    Response response =
        getTargetAuth(itemId).put(Entity.entity(multiPart, multiPart.getMediaType()));
    // When single valued field is provided in JSON where multiple values are allowed, it will
    // accept the input softly
    assertEquals(Status.OK.getStatusCode(), response.getStatus());
  }


  @Test
  public void test_6_updateItem_defaultSyntax_missingParentEntryInParentObject()
      throws IOException {

    FormDataMultiPart multiPart = new FormDataMultiPart();
    multiPart.field("json",
        itemJSON.replace("___COLLECTION_ID___", collectionId).replace("___FILENAME___", "test.png")
            .replaceAll("\"fetchUrl\"\\s*:\\s*\"___FETCH_URL___\",", "\"id\": \"" + itemId + "\", ")
            .replaceAll("\"referenceUrl\"\\s*:\\s*\"___FETCH_URL___\",", "")
            //
            .replaceAll("\"text\": \"TitleOfItem\",", ""));

    Response response =
        getTargetAuth(itemId).put(Entity.entity(multiPart, multiPart.getMediaType()));

    assertEquals(HttpStatus.SC_UNPROCESSABLE_ENTITY, response.getStatus());
  }

  @Test
  public void test_6_updateItem_defaultSyntax_wrongChild() throws IOException {

    FormDataMultiPart multiPart = new FormDataMultiPart();
    multiPart.field("json",
        itemJSON.replace("___COLLECTION_ID___", collectionId).replace("___FILENAME___", "test.png")
            .replaceAll("\"fetchUrl\"\\s*:\\s*\"___FETCH_URL___\",", "\"id\": \"" + itemId + "\", ")
            .replaceAll("\"referenceUrl\"\\s*:\\s*\"___FETCH_URL___\",", "")
            //
            .replaceAll("\"" + "textChild" + "\"\\s*:", "\"" + "textChildNewLabel" + "\":"));
    Response response =
        getTargetAuth(itemId).put(Entity.entity(multiPart, multiPart.getMediaType()));

    assertEquals(HttpStatus.SC_UNPROCESSABLE_ENTITY, response.getStatus());
  }

  @Test
  public void test_6_updateItem_defaultSyntax_noneMultivalue_noneChildren() throws IOException {

    FormDataMultiPart multiPart = new FormDataMultiPart();
    multiPart.field("json",
        getStringFromPath(
            "src/test/resources/rest/defaultCreateItemHierarchicalNoChildrenNoMultivalue.json")
                .replace("___COLLECTION_ID___", collectionId).replace("___FILENAME___", "test.png")
                .replaceAll("\"fetchUrl\"\\s*:\\s*\"___FETCH_URL___\",",
                    "\"id\": \"" + itemId + "\", ")
            .replaceAll("\"referenceUrl\"\\s*:\\s*\"___FETCH_URL___\",", ""));

    Response response =
        getTargetAuth(itemId).put(Entity.entity(multiPart, multiPart.getMediaType()));

    assertEquals(Status.OK.getStatusCode(), response.getStatus());
  }


  @Test
  public void test_6_updateItem_defaultSyntax_multipleValueInParentNodeValueOfParentObject()
      throws IOException {

    FormDataMultiPart multiPart = new FormDataMultiPart();
    multiPart.field("json",
        itemJSON.replace("___COLLECTION_ID___", collectionId).replace("___FILENAME___", "test.png")
            .replaceAll("\"fetchUrl\"\\s*:\\s*\"___FETCH_URL___\",", "\"id\": \"" + itemId + "\", ")
            .replaceAll("\"referenceUrl\"\\s*:\\s*\"___FETCH_URL___\",", "")
            //
            .replaceAll("\"number\": 1.2345678E7", "\"number\": [234,235]"));

    Response response =
        getTargetAuth(itemId).put(Entity.entity(multiPart, multiPart.getMediaType()));

    // LOGGER.info(response.readEntity(String.class));
    assertEquals(HttpStatus.SC_UNPROCESSABLE_ENTITY, response.getStatus());
  }

  @Test
  public void test_7_updateItem_defaultSyntax_multipleValueInParentNodeValueOfParentObject()
      throws Exception {
    initCollectionWithProfile(getDefaultHierarchicalStatementsMultipleH());
    itemJSON = getStringFromPath(
        "src/test/resources/rest/defaultCreateItemHierarchicalMultipleParentChild.json");
    createItem();

    FormDataMultiPart multiPart = new FormDataMultiPart();

    // itemJSON =
    // getStringFromPath("src/test/resources/rest/defaultCreateItemHierarchicalMultipleParentChild.json");

    multiPart.field("json",
        itemJSON.replace("___COLLECTION_ID___", collectionId).replace("___FILENAME___", "test.png")
            .replaceAll("\"fetchUrl\"\\s*:\\s*\"___FETCH_URL___\",", "\"id\": \"" + itemId + "\", ")
            .replaceAll("\"referenceUrl\"\\s*:\\s*\"___FETCH_URL___\",", ""));


    Response response =
        getTargetAuth(itemId).put(Entity.entity(multiPart, multiPart.getMediaType()));

    // LOGGER.info(response.readEntity(String.class));
    assertEquals(Status.OK.getStatusCode(), response.getStatus());

    Response response2 = getTargetAuth(itemId).get();
    String jSonGet = response2.readEntity(String.class);
    multiPart.field("json", jSonGet);
    response2 = getTargetAuth(itemId).put(Entity.entity(multiPart, multiPart.getMediaType()));

    // LOGGER.info(response.readEntity(String.class));
    assertEquals(Status.OK.getStatusCode(), response2.getStatus());

  }

  @Test
  public void test_7_updateItem_defaultSyntax_multipleValueInParentNodeValueOfParentObjectMissingInnerParentNode()
      throws Exception {
    initCollectionWithProfile(getDefaultHierarchicalStatementsMultipleH());
    itemJSON = getStringFromPath(
        "src/test/resources/rest/defaultCreateItemHierarchicalMultipleParentChild.json");
    createItem();

    FormDataMultiPart multiPart = new FormDataMultiPart();

    // itemJSON =
    // getStringFromPath("src/test/resources/rest/defaultCreateItemHierarchicalMultipleParentChild.json");

    multiPart.field("json",
        itemJSON.replace("___COLLECTION_ID___", collectionId).replace("___FILENAME___", "test.png")
            .replaceAll("\"fetchUrl\"\\s*:\\s*\"___FETCH_URL___\",", "\"id\": \"" + itemId + "\", ")
            .replaceAll("\"referenceUrl\"\\s*:\\s*\"___FETCH_URL___\",", "")
            .replace("\"textChild\": \"this is second correct\",", ""));


    Response response =
        getTargetAuth(itemId).put(Entity.entity(multiPart, multiPart.getMediaType()));

    // LOGGER.info(response.readEntity(String.class));
    assertEquals(HttpStatus.SC_UNPROCESSABLE_ENTITY, response.getStatus());
  }


  private Invocation.Builder getTargetAuth(String itemId) {
    return target(pathPrefix + "/" + itemId).register(authAsUser).register(MultiPartFeature.class)
        .register(JacksonFeature.class).request(MediaType.APPLICATION_JSON_TYPE);
  }


}
