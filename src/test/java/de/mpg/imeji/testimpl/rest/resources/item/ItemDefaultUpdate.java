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
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.mpg.imeji.exceptions.BadRequestException;
import de.mpg.imeji.rest.api.DefaultItemService;
import de.mpg.imeji.rest.process.RestProcessUtils;
import de.mpg.imeji.rest.to.defaultItemTO.DefaultItemTO;
import de.mpg.imeji.rest.to.defaultItemTO.DefaultItemWithFileTO;
import de.mpg.imeji.test.rest.resources.test.integration.ItemTestBase;
import de.mpg.imeji.testimpl.ImejiTestResources;
import util.JenaUtil;


@Ignore
public class ItemDefaultUpdate extends ItemTestBase {

  private static final Logger LOGGER = LoggerFactory.getLogger(ItemDefaultUpdate.class);

  private static String createItemJSON;
  protected static String updateItemJSON;
  private static final String PATH_PREFIX = "/rest/items";
  public static final String METADATA_KEY = "metadata";

  DefaultItemTO defaultItemTO;


  @BeforeClass
  public static void specificSetup() throws Exception {

    initCollectionWithProfile(getDefaultBasicStatements());
    createItemJSON = getStringFromPath("src/test/resources/rest/defaultCreateItem.json");
    defaultCreatItemWithFullMetadata();
    updateItemJSON = getStringFromPath("src/test/resources/rest/defaultUpdateItem.json");
  }

  @Test
  public void test_1_updateItem_updateMetadata_emptySyntax()
      throws IOException, BadRequestException {
    FormDataMultiPart multiPart = new FormDataMultiPart();
    multiPart.field("json", updateItemJSON.replace("___COLLECTION_ID___", collectionId));


    Response response = getTargetAuth().put(Entity.entity(multiPart, multiPart.getMediaType()));

    assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
  }

  @Ignore
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

  }

  protected static void defaultCreatItemWithFullMetadata() throws Exception {
    createItemJSON = createItemJSON.replace("___COLLECTION_ID___", collectionId)
        .replace("___FILENAME___", "test.png")
        .replaceAll("\"fetchUrl\"\\s*:\\s*\"___FETCH_URL___\",", "")
        .replaceAll("___REFERENCE_URL___", "");

    itemTO = (DefaultItemWithFileTO) RestProcessUtils.buildTOFromJSON(createItemJSON,
        DefaultItemWithFileTO.class);

    itemTO.setCollectionId(collectionId);
    ((DefaultItemWithFileTO) itemTO).setFile(ImejiTestResources.getTestJpg());

    DefaultItemService s = new DefaultItemService();
    itemTO = s.create(itemTO, JenaUtil.testUser);
    itemId = itemTO.getId();
  }

  private Invocation.Builder getTargetAuth() {
    return target(PATH_PREFIX).path("/" + itemId).register(authAsUser)
        .register(MultiPartFeature.class).register(JacksonFeature.class)
        .request(MediaType.APPLICATION_JSON_TYPE);
  }


}
