package de.mpg.imeji.testimpl.rest.resources.item;

import static de.mpg.imeji.logic.util.ResourceHelper.getStringFromPath;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;

import java.io.IOException;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.mpg.imeji.exceptions.BadRequestException;
import de.mpg.imeji.rest.to.defaultItemTO.DefaultItemTO;
import de.mpg.imeji.test.rest.resources.test.integration.ItemTestBase;


@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ItemUpdate extends ItemTestBase {

  private static final Logger LOGGER = LoggerFactory.getLogger(ItemUpdate.class);

  private static String createItemJSON;
  protected static String updateItemJSON;

  private static final String PATH_PREFIX = "/rest/items";
  public static final String METADATA_KEY = "metadata";
  public static final String referenceUrl = "http://imeji.org";

  DefaultItemTO defaultItemTO;


  @BeforeClass
  public static void specificSetup() throws Exception {

    createItemJSON = getStringFromPath("src/test/resources/rest/defaultCreateItem.json");
    updateItemJSON = getStringFromPath("src/test/resources/rest/defaultUpdateItem.json");
    initCollectionWithProfile(getDefaultBasicStatements());
    createItem();
  }


  @Test
  public void test_1_updateItem_updateMetadata_empty() throws IOException, BadRequestException {
    FormDataMultiPart multiPart = new FormDataMultiPart();
    multiPart.field("json",
        updateItemJSON.replace("\"___COLLECTION_ID___\",", "\"" + collectionId + "\", ").replaceAll(
            "\"metadata\"\\s*:\\s*\\{[\\d\\D]*\\}", "\"referenceUrl\":\"" + referenceUrl + "\" }"));
    Response response = getTargetAuth().put(Entity.entity(multiPart, multiPart.getMediaType()));
    assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    defaultItemTO = response.readEntity(DefaultItemTO.class);
    assertThat(defaultItemTO.getMetadata().keySet(), hasSize(0));
  }


  @Test
  public void test_2_updateItem_deleteAllMetadata() throws IOException, BadRequestException {

    FormDataMultiPart multiPart = new FormDataMultiPart();
    String jsonNew = updateItemJSON.replace("___COLLECTION_ID___", collectionId)
        .replaceAll("\"metadata\"\\s*:\\s*\\{[\\d\\D]*\\}", "\"metadata\":{}}");

    multiPart.field("json", jsonNew);
    Response response = getTargetAuth().put(Entity.entity(multiPart, multiPart.getMediaType()));
    assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    defaultItemTO = response.readEntity(DefaultItemTO.class);
    assertThat(defaultItemTO.getMetadata().keySet(), hasSize(0));
  }


  @Test
  public void test_4_updateItem_defaultSyntax_OK() throws IOException, BadRequestException {

    FormDataMultiPart multiPart = new FormDataMultiPart();
    multiPart.field("json", updateItemJSON.replace("___COLLECTION_ID___", collectionId));


    Response response =
        target(PATH_PREFIX).path("/" + itemId).register(authAsUser).register(MultiPartFeature.class)
            .register(JacksonFeature.class).request(MediaType.APPLICATION_JSON_TYPE)
            .put(Entity.entity(multiPart, multiPart.getMediaType()));

    assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    defaultItemTO = response.readEntity(DefaultItemTO.class);
    assertThat(defaultItemTO.getMetadata().keySet(), hasSize(6));

  }

  @Test
  public void test_5_updateItem_defaultSyntax_badTypedValues() throws IOException {
    test_5_defaultSyntax_badTypedValues(itemId, createItemJSON);

  }


  @Test
  public void test_6_updateItem_ExistingDefaultFields() throws IOException {
    test_6_ExistingDefaultFields(itemId, createItemJSON);

  }


  private Invocation.Builder getTargetAuth() {
    return target(PATH_PREFIX).path("/" + itemId).register(authAsUser)
        .register(MultiPartFeature.class).register(JacksonFeature.class)
        .request(MediaType.APPLICATION_JSON_TYPE);
  }


}
