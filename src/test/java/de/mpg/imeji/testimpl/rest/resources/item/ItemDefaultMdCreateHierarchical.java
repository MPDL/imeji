package de.mpg.imeji.testimpl.rest.resources.item;

import static de.mpg.imeji.logic.util.ResourceHelper.getStringFromPath;
import static de.mpg.imeji.test.rest.resources.test.integration.MyTestContainerFactory.STATIC_CONTEXT_STORAGE;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;

import java.io.File;
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
import org.glassfish.jersey.media.multipart.file.FileDataBodyPart;
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
public class ItemDefaultMdCreateHierarchical extends ItemTestBase {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(ItemDefaultMdCreateHierarchical.class);

  private static final String referenceUrl =
      "http://th03.deviantart.net/fs71/PRE/i/2012/242/1/f/png_moon_by_paradise234-d5czhdo.png";

  private static String itemJSON;
  private static final String pathPrefix = "/rest/items";

  DefaultItemTO defaultItemTO;

  @BeforeClass
  public static void specificSetup() throws Exception {
    initCollectionWithProfile(getDefaultHierarchicalStatements());
    itemJSON = getStringFromPath("src/test/resources/rest/defaultCreateItemHierarchical.json");
  }

  @Test
  public void test_0_itemTemplateCheck() {
    // First get the ItemTemplate
    Response response = target("/rest/collections/" + collectionId + "/items/template")
        .register(authAsUser).register(MultiPartFeature.class).register(JacksonFeature.class)
        .request(MediaType.APPLICATION_JSON_TYPE).get();
    String itemTemplate = response.readEntity(String.class);

    FormDataMultiPart multiPart = new FormDataMultiPart();
    multiPart.field("json",
        itemTemplate.replace("filename", "referenceUrl").replace(
            "<change-the-file-name-here-or-provide-separate-field-for-fetch-or-reference-url-see-API-Documentation>",
            referenceUrl));

    Response response2 = getTargetAuth().post(Entity.entity(multiPart, multiPart.getMediaType()));
    assertEquals(Status.CREATED.getStatusCode(), response2.getStatus());
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
    assertEquals(Status.CREATED.getStatusCode(), response.getStatus());

    defaultItemTO = response.readEntity(DefaultItemTO.class);

    assertThat(defaultItemTO.getMetadata().keySet(), hasSize(8)); // check
                                                                  // defaultCreateItemHierarchical.json,
                                                                  // only top level elements count
    assertThat(defaultItemTO.getCollectionId(), equalTo(collectionId));


  }

  @Test
  public void test_2_createItem_defaultSyntaxParameter() throws IOException {
    // Do provide Syntax Parameter "DEFAULT"


    FormDataMultiPart multiPart = new FormDataMultiPart();
    multiPart
        .bodyPart(new FileDataBodyPart("file", new File(STATIC_CONTEXT_STORAGE + "/test3.jpg")));
    multiPart.field("json",
        itemJSON.replace("___COLLECTION_ID___", collectionId).replace("___FILENAME___", "test3.jpg")
            .replaceAll("\"fetchUrl\"\\s*:\\s*\"___FETCH_URL___\",", "")
            .replaceAll("\"referenceUrl\"\\s*:\\s*\"___REFERENCE_URL___\",", ""));

    // LOGGER.info("ITEM = "+multiPart.getField("json").getValue());
    Response response = target(pathPrefix).register(authAsUser).register(MultiPartFeature.class)
        .register(JacksonFeature.class).request(MediaType.APPLICATION_JSON_TYPE)
        .post(Entity.entity(multiPart, multiPart.getMediaType()));

    assertEquals(Status.CREATED.getStatusCode(), response.getStatus());

    defaultItemTO = response.readEntity(DefaultItemTO.class);
    assertThat(defaultItemTO.getMetadata().keySet(), hasSize(8)); // check defaultCreateItem.json
                                                                  // (only top level elements count)
    assertThat(defaultItemTO.getCollectionId(), equalTo(collectionId));

  }

  @Test
  public void test_3_createItem_empty_defaultSyntax() throws IOException {

    // Create default item without metadata
    FormDataMultiPart multiPart = new FormDataMultiPart();
    multiPart
        .bodyPart(new FileDataBodyPart("file", new File(STATIC_CONTEXT_STORAGE + "/test4.jpg")));
    multiPart.field("json",
        itemJSON.replace("___COLLECTION_ID___", collectionId).replace("___FILENAME___", "test4.jpg")
            .replaceAll("\"fetchUrl\"\\s*:\\s*\"___FETCH_URL___\",", "")
            .replaceAll("\"referenceUrl\"\\s*:\\s*\"___REFERENCE_URL___\",", "")
            .replaceAll("\"metadata\"\\s*:\\s*\\{[\\d\\D]*\\}", "\"metadata\":{}}"));

    Response response = target(pathPrefix).register(authAsUser).register(MultiPartFeature.class)
        .register(JacksonFeature.class).request(MediaType.APPLICATION_JSON_TYPE)
        .post(Entity.entity(multiPart, multiPart.getMediaType()));

    assertEquals(Status.CREATED.getStatusCode(), response.getStatus());

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
    multiPart
        .bodyPart(new FileDataBodyPart("file", new File(STATIC_CONTEXT_STORAGE + "/test5.jpg")));
    multiPart.field("json",
        itemJSON.replace("___COLLECTION_ID___", collectionId).replace("___FILENAME___", "test5.jpg")
            .replaceAll("\"fetchUrl\"\\s*:\\s*\"___FETCH_URL___\",", "")
            .replaceAll("\"referenceUrl\"\\s*:\\s*\"___REFERENCE_URL___\",", "")
            //
            .replaceAll("\"number\"\\s*:.*,", "some\"text,,,,,"));
    Response response = getTargetAuth().post(Entity.entity(multiPart, multiPart.getMediaType()));

    assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());

  }

  @Test
  public void test_6_createItem_defaultSyntax_wrongParent() throws IOException {

    FormDataMultiPart multiPart = new FormDataMultiPart();
    multiPart
        .bodyPart(new FileDataBodyPart("file", new File(STATIC_CONTEXT_STORAGE + "/test5.jpg")));
    multiPart.field("json",
        itemJSON.replace("___COLLECTION_ID___", collectionId).replace("___FILENAME___", "test5.jpg")
            .replaceAll("\"fetchUrl\"\\s*:\\s*\"___FETCH_URL___\",", "")
            .replaceAll("\"referenceUrl\"\\s*:\\s*\"___REFERENCE_URL___\",", "")
            //
            .replaceAll("\"" + "text" + "\"\\s*:", "\"" + "textChild" + "\":"));
    Response response = getTargetAuth().post(Entity.entity(multiPart, multiPart.getMediaType()));

    assertEquals(HttpStatus.SC_UNPROCESSABLE_ENTITY, response.getStatus());
  }


  @Test
  public void test_6_createItem_defaultSyntax_singleValueWhereMultiple() throws IOException {

    FormDataMultiPart multiPart = new FormDataMultiPart();
    multiPart
        .bodyPart(new FileDataBodyPart("file", new File(STATIC_CONTEXT_STORAGE + "/test5.jpg")));
    multiPart.field("json",
        getStringFromPath("src/test/resources/rest/defaultCreateItemHierarchicalWrong.json")
            .replace("___COLLECTION_ID___", collectionId).replace("___FILENAME___", "test5.jpg")
            .replaceAll("\"fetchUrl\"\\s*:\\s*\"___FETCH_URL___\",", "")
            .replaceAll("\"referenceUrl\"\\s*:\\s*\"___REFERENCE_URL___\",", ""));
    Response response = getTargetAuth().post(Entity.entity(multiPart, multiPart.getMediaType()));
    // When single valued field is provided in JSON where multiple values are allowed, it will
    // accept the input softly
    assertEquals(Status.CREATED.getStatusCode(), response.getStatus());
  }


  @Test
  public void test_6_createItem_defaultSyntax_missingParentEntryInParentObject()
      throws IOException {

    FormDataMultiPart multiPart = new FormDataMultiPart();
    multiPart
        .bodyPart(new FileDataBodyPart("file", new File(STATIC_CONTEXT_STORAGE + "/test2.jpg")));
    multiPart.field("json",
        itemJSON.replace("___COLLECTION_ID___", collectionId).replace("___FILENAME___", "test2.jpg")
            .replaceAll("\"fetchUrl\"\\s*:\\s*\"___FETCH_URL___\",", "")
            .replaceAll("\"referenceUrl\"\\s*:\\s*\"___REFERENCE_URL___\",", "")
            //
            .replaceAll("\"text\": \"TitleOfItem\",", ""));
    Response response = getTargetAuth().post(Entity.entity(multiPart, multiPart.getMediaType()));

    assertEquals(HttpStatus.SC_UNPROCESSABLE_ENTITY, response.getStatus());
  }



  @Test
  public void test_6_createItem_defaultSyntax_wrongChild() throws IOException {

    FormDataMultiPart multiPart = new FormDataMultiPart();
    multiPart
        .bodyPart(new FileDataBodyPart("file", new File(STATIC_CONTEXT_STORAGE + "/test5.jpg")));
    multiPart.field("json",
        itemJSON.replace("___COLLECTION_ID___", collectionId).replace("___FILENAME___", "test5.jpg")
            .replaceAll("\"fetchUrl\"\\s*:\\s*\"___FETCH_URL___\",", "")
            .replaceAll("\"referenceUrl\"\\s*:\\s*\"___REFERENCE_URL___\",", "")
            //
            .replaceAll("\"" + "textChild" + "\"\\s*:", "\"" + "textChildNewLabel" + "\":"));
    Response response = getTargetAuth().post(Entity.entity(multiPart, multiPart.getMediaType()));

    assertEquals(HttpStatus.SC_UNPROCESSABLE_ENTITY, response.getStatus());
  }

  @Test
  public void test_6_createItem_defaultSyntax_noneMultivalue_noneChildren() throws IOException {

    FormDataMultiPart multiPart = new FormDataMultiPart();
    multiPart
        .bodyPart(new FileDataBodyPart("file", new File(STATIC_CONTEXT_STORAGE + "/test6.jpg")));
    multiPart.field("json",
        getStringFromPath(
            "src/test/resources/rest/defaultCreateItemHierarchicalNoChildrenNoMultivalue.json")
                .replace("___COLLECTION_ID___", collectionId).replace("___FILENAME___", "test6.jpg")
                .replaceAll("\"fetchUrl\"\\s*:\\s*\"___FETCH_URL___\",", "")
                .replaceAll("\"referenceUrl\"\\s*:\\s*\"___REFERENCE_URL___\",", ""));
    Response response = getTargetAuth().post(Entity.entity(multiPart, multiPart.getMediaType()));

    assertEquals(Status.CREATED.getStatusCode(), response.getStatus());
  }


  @Test
  public void test_6_createItem_defaultSyntax_multipleValueInParentNodeValueOfParentObject()
      throws IOException {

    FormDataMultiPart multiPart = new FormDataMultiPart();
    multiPart
        .bodyPart(new FileDataBodyPart("file", new File(STATIC_CONTEXT_STORAGE + "/test3.jpg")));
    multiPart.field("json",
        itemJSON.replace("___COLLECTION_ID___", collectionId).replace("___FILENAME___", "test3.jpg")
            .replaceAll("\"fetchUrl\"\\s*:\\s*\"___FETCH_URL___\",", "")
            .replaceAll("\"referenceUrl\"\\s*:\\s*\"___REFERENCE_URL___\",", "")
            //
            .replaceAll("\"number\": 1.2345678E7", "\"number\": [234,235]"));
    Response response = getTargetAuth().post(Entity.entity(multiPart, multiPart.getMediaType()));

    // LOGGER.info(response.readEntity(String.class));
    assertEquals(HttpStatus.SC_UNPROCESSABLE_ENTITY, response.getStatus());
  }

  private Invocation.Builder getTargetAuth() {
    return target(pathPrefix).register(authAsUser).register(MultiPartFeature.class)
        .register(JacksonFeature.class).request(MediaType.APPLICATION_JSON_TYPE);
  }


}
