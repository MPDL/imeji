package de.mpg.imeji.test.rest.resources.test.integration;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.log4j.Logger;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.media.multipart.file.FileDataBodyPart;

import de.mpg.imeji.logic.core.collection.CollectionService;
import de.mpg.imeji.logic.core.item.ItemService;
import de.mpg.imeji.logic.model.CollectionImeji;
import de.mpg.imeji.logic.model.Item;
import de.mpg.imeji.logic.model.factory.ImejiFactory;
import de.mpg.imeji.logic.util.ObjectHelper;
import de.mpg.imeji.rest.to.defaultItemTO.DefaultItemTO;
import de.mpg.imeji.rest.transfer.TransferVOtoTO;
import de.mpg.imeji.util.ImejiTestResources;
import de.mpg.imeji.util.JenaUtil;

/**
 * Created by vlad on 10.06.15.
 */
public class ItemTestBase extends ImejiTestBase {

  private static final Logger LOGGER = Logger.getLogger(ItemTestBase.class);

  public static Item item;
  private static final String TARGET_PATH_PREFIX = "/rest/items";


  protected static void createItem() throws Exception {
    CollectionService cc = new CollectionService();
    ItemService ic = new ItemService();
    CollectionImeji coll =
        cc.retrieve(ObjectHelper.getURI(CollectionImeji.class, collectionId), JenaUtil.testUser);
    item = ImejiFactory.newItem(coll);
    item = ic.create(item, coll, JenaUtil.testUser);
    itemId = item.getIdString();
    itemTO = new DefaultItemTO();
    TransferVOtoTO.transferDefaultItem(item, itemTO);
  }


  private String replaceWithStringValueNotLastField(String jSon, String fieldName) {
    return jSon.replace("___COLLECTION_ID___", collectionId).replace("___FILENAME___", "test.jpg")
        .replaceAll("\"fetchUrl\"\\s*:\\s*\"___FETCH_URL___\",", "")
        .replaceAll("\"referenceUrl\"\\s*:\\s*\"___REFERENCE_URL___\",", "")
        .replaceAll("\"" + fieldName + "\"\\s*:\\s*.*,", "\"" + fieldName + "\": \"sometext\",");
  }

  private String replaceWithStringValueLastField(String jSon, String fieldName) {
    return jSon.replace("___COLLECTION_ID___", collectionId).replace("___FILENAME___", "test.jpg")
        .replaceAll("\"fetchUrl\"\\s*:\\s*\"___FETCH_URL___\",", "")
        .replaceAll("\"referenceUrl\"\\s*:\\s*\"___REFERENCE_URL___\",", "")
        .replaceAll("\"" + fieldName + "\"\\s*:\\s*.*", "\"" + fieldName + "\": \"sometext\"");

  }


  private String replaceWithNumberValueNotLastField(String jSon, String fieldName) {
    return jSon.replace("___COLLECTION_ID___", collectionId).replace("___FILENAME___", "test.jpg")
        .replaceAll("\"fetchUrl\"\\s*:\\s*\"___FETCH_URL___\",", "")
        .replaceAll("\"referenceUrl\"\\s*:\\s*\"___REFERENCE_URL___\",", "")
        .replaceAll("\"" + fieldName + "\"\\s*:\\s*.*,", "\"" + fieldName + "\": 123456,");
  }

  private String replaceWithNumberValueLastField(String jSon, String fieldName) {
    return jSon.replace("___COLLECTION_ID___", collectionId).replace("___FILENAME___", "test.jpg")
        .replaceAll("\"fetchUrl\"\\s*:\\s*\"___FETCH_URL___\",", "")
        .replaceAll("\"referenceUrl\"\\s*:\\s*\"___REFERENCE_URL___\",", "")
        .replaceAll("\"" + fieldName + "\"\\s*:\\s*.*", "\"" + fieldName + "\": 123456");
  }

  private String replaceFieldName(String jSon, String oldFieldName) {
    String newFieldName = oldFieldName + "-changed";
    return jSon.replace("___COLLECTION_ID___", collectionId).replace("___FILENAME___", "test.jpg")
        .replaceAll("\"fetchUrl\"\\s*:\\s*\"___FETCH_URL___\",", "")
        .replaceAll("\"referenceUrl\"\\s*:\\s*\"___REFERENCE_URL___\",", "")
        .replaceAll("\"" + oldFieldName + "\"\\s*:", "\"" + newFieldName + "\":");
  }



  public void test_5_defaultSyntax_badTypedValues(String itemId, String jSon) throws IOException {

    FormDataMultiPart multiPart = new FormDataMultiPart();
    multiPart.bodyPart(new FileDataBodyPart("file", ImejiTestResources.getTestJpg()));

    LOGGER.info("Checking textual values ... ");
    // Put Number Value to a String metadata
    multiPart.field("json", replaceWithNumberValueNotLastField(jSon, "text"));
    Response response = itemId.equals("")
        ? getCreateTargetAuth().post(Entity.entity(multiPart, multiPart.getMediaType()))
        : getUpdateTargetAuth(itemId).put(Entity.entity(multiPart, multiPart.getMediaType()));
    assertEquals(HttpStatus.SC_UNPROCESSABLE_ENTITY, response.getStatus());

    LOGGER.info("Checking number values ... ");
    // Put String to Number Value metadata
    multiPart.getField("json").setValue(replaceWithStringValueNotLastField(jSon, "number"));
    response = itemId.equals("")
        ? getCreateTargetAuth().post(Entity.entity(multiPart, multiPart.getMediaType()))
        : getUpdateTargetAuth(itemId).put(Entity.entity(multiPart, multiPart.getMediaType()));
    assertEquals(HttpStatus.SC_UNPROCESSABLE_ENTITY, response.getStatus());

    LOGGER.info("Checking date values ... ");
    // Put "sometext" String to Date Value metadata
    multiPart.getField("json").setValue(replaceWithStringValueNotLastField(jSon, "date"));
    response = itemId.equals("")
        ? getCreateTargetAuth().post(Entity.entity(multiPart, multiPart.getMediaType()))
        : getUpdateTargetAuth(itemId).put(Entity.entity(multiPart, multiPart.getMediaType()));
    assertEquals(HttpStatus.SC_UNPROCESSABLE_ENTITY, response.getStatus());

    LOGGER.info("Checking longitude values with text ... ");
    // Put "sometext" String to Longitude Value metadata
    multiPart.getField("json").setValue(replaceWithStringValueNotLastField(jSon, "longitude"));
    response = itemId.equals("")
        ? getCreateTargetAuth().post(Entity.entity(multiPart, multiPart.getMediaType()))
        : getUpdateTargetAuth(itemId).put(Entity.entity(multiPart, multiPart.getMediaType()));
    assertEquals(HttpStatus.SC_UNPROCESSABLE_ENTITY, response.getStatus());

    LOGGER.info("Checking latitude values with text... ");
    // Put "sometext" String to Latitude (last) Value metadata
    multiPart.getField("json").setValue(replaceWithStringValueLastField(jSon, "latitude"));
    response = itemId.equals("")
        ? getCreateTargetAuth().post(Entity.entity(multiPart, multiPart.getMediaType()))
        : getUpdateTargetAuth(itemId).put(Entity.entity(multiPart, multiPart.getMediaType()));
    assertEquals(HttpStatus.SC_UNPROCESSABLE_ENTITY, response.getStatus());

    LOGGER.info("Checking longitude values with wrong value... ");
    // Put bad Value to Longitude Value metadata
    multiPart.getField("json").setValue(replaceWithNumberValueNotLastField(jSon, "longitude"));
    response = itemId.equals("")
        ? getCreateTargetAuth().post(Entity.entity(multiPart, multiPart.getMediaType()))
        : getUpdateTargetAuth(itemId).put(Entity.entity(multiPart, multiPart.getMediaType()));
    assertEquals(HttpStatus.SC_UNPROCESSABLE_ENTITY, response.getStatus());

    LOGGER.info("Checking latitude values with wrong value... ");
    // Put bad Value to Longitude Value metadata
    multiPart.getField("json").setValue(replaceWithNumberValueLastField(jSon, "latitude"));
    response = itemId.equals("")
        ? getCreateTargetAuth().post(Entity.entity(multiPart, multiPart.getMediaType()))
        : getUpdateTargetAuth(itemId).put(Entity.entity(multiPart, multiPart.getMediaType()));
    assertEquals(HttpStatus.SC_UNPROCESSABLE_ENTITY, response.getStatus());
  }



  public void test_6_ExistingDefaultFields(String itemId, String jSon) throws IOException {
    // validates the name of each predefined metadata from a metadata record
    FormDataMultiPart multiPart = new FormDataMultiPart();
    multiPart.bodyPart(new FileDataBodyPart("file", ImejiTestResources.getTestJpg()));

    LOGGER.info("Checking text field label  ");
    multiPart.field("json", replaceFieldName(jSon, "text"));
    Response response = itemId.equals("")
        ? getCreateTargetAuth().post(Entity.entity(multiPart, multiPart.getMediaType()))
        : getUpdateTargetAuth(itemId).put(Entity.entity(multiPart, multiPart.getMediaType()));
    assertEquals(HttpStatus.SC_UNPROCESSABLE_ENTITY, response.getStatus());

    LOGGER.info("Checking geolocation field label  ");
    // Put Number Value to a String metadata
    multiPart.field("json", replaceFieldName(jSon, "geolocation"));
    response = itemId.equals("")
        ? getCreateTargetAuth().post(Entity.entity(multiPart, multiPart.getMediaType()))
        : getUpdateTargetAuth(itemId).put(Entity.entity(multiPart, multiPart.getMediaType()));
    assertEquals(HttpStatus.SC_UNPROCESSABLE_ENTITY, response.getStatus());

    LOGGER.info("Checking name field label  ");
    multiPart.field("json", replaceFieldName(jSon, "name"));
    response = itemId.equals("")
        ? getCreateTargetAuth().post(Entity.entity(multiPart, multiPart.getMediaType()))
        : getUpdateTargetAuth(itemId).put(Entity.entity(multiPart, multiPart.getMediaType()));
    assertEquals(HttpStatus.SC_UNPROCESSABLE_ENTITY, response.getStatus());

    LOGGER.info("Checking latitude field label  ");
    multiPart.field("json", replaceFieldName(jSon, "latitude"));
    response = itemId.equals("")
        ? getCreateTargetAuth().post(Entity.entity(multiPart, multiPart.getMediaType()))
        : getUpdateTargetAuth(itemId).put(Entity.entity(multiPart, multiPart.getMediaType()));
    assertEquals(HttpStatus.SC_UNPROCESSABLE_ENTITY, response.getStatus());


    LOGGER.info("Checking longitude field label  ");
    multiPart.field("json", replaceFieldName(jSon, "longitude"));
    response = itemId.equals("")
        ? getCreateTargetAuth().post(Entity.entity(multiPart, multiPart.getMediaType()))
        : getUpdateTargetAuth(itemId).put(Entity.entity(multiPart, multiPart.getMediaType()));
    assertEquals(HttpStatus.SC_UNPROCESSABLE_ENTITY, response.getStatus());

    LOGGER.info("Checking number field label  ");
    multiPart.field("json", replaceFieldName(jSon, "number"));
    response = itemId.equals("")
        ? getCreateTargetAuth().post(Entity.entity(multiPart, multiPart.getMediaType()))
        : getUpdateTargetAuth(itemId).put(Entity.entity(multiPart, multiPart.getMediaType()));
    assertEquals(HttpStatus.SC_UNPROCESSABLE_ENTITY, response.getStatus());


    LOGGER.info("Checking conePerson field label  ");
    multiPart.field("json", replaceFieldName(jSon, "conePerson"));
    response = itemId.equals("")
        ? getCreateTargetAuth().post(Entity.entity(multiPart, multiPart.getMediaType()))
        : getUpdateTargetAuth(itemId).put(Entity.entity(multiPart, multiPart.getMediaType()));
    assertEquals(HttpStatus.SC_UNPROCESSABLE_ENTITY, response.getStatus());

    LOGGER.info("Checking familyName field label  ");
    multiPart.field("json", replaceFieldName(jSon, "familyName"));
    response = itemId.equals("")
        ? getCreateTargetAuth().post(Entity.entity(multiPart, multiPart.getMediaType()))
        : getUpdateTargetAuth(itemId).put(Entity.entity(multiPart, multiPart.getMediaType()));
    assertEquals(HttpStatus.SC_UNPROCESSABLE_ENTITY, response.getStatus());

    LOGGER.info("Checking completeName field label  ");
    multiPart.field("json", replaceFieldName(jSon, "completeName"));
    response = itemId.equals("")
        ? getCreateTargetAuth().post(Entity.entity(multiPart, multiPart.getMediaType()))
        : getUpdateTargetAuth(itemId).put(Entity.entity(multiPart, multiPart.getMediaType()));
    assertEquals(HttpStatus.SC_UNPROCESSABLE_ENTITY, response.getStatus());

    LOGGER.info("Checking alternativeName field label  ");
    multiPart.field("json", replaceFieldName(jSon, "alternativeName"));
    response = itemId.equals("")
        ? getCreateTargetAuth().post(Entity.entity(multiPart, multiPart.getMediaType()))
        : getUpdateTargetAuth(itemId).put(Entity.entity(multiPart, multiPart.getMediaType()));
    assertEquals(HttpStatus.SC_UNPROCESSABLE_ENTITY, response.getStatus());

    LOGGER.info("Checking role field label  ");
    multiPart.field("json", replaceFieldName(jSon, "role"));
    response = itemId.equals("")
        ? getCreateTargetAuth().post(Entity.entity(multiPart, multiPart.getMediaType()))
        : getUpdateTargetAuth(itemId).put(Entity.entity(multiPart, multiPart.getMediaType()));
    assertEquals(HttpStatus.SC_UNPROCESSABLE_ENTITY, response.getStatus());

    LOGGER.info("Checking organizations field label  ");
    multiPart.field("json", replaceFieldName(jSon, "organizations"));
    response = itemId.equals("")
        ? getCreateTargetAuth().post(Entity.entity(multiPart, multiPart.getMediaType()))
        : getUpdateTargetAuth(itemId).put(Entity.entity(multiPart, multiPart.getMediaType()));
    assertEquals(HttpStatus.SC_UNPROCESSABLE_ENTITY, response.getStatus());

    LOGGER.info("Checking description field label  ");
    multiPart.field("json", replaceFieldName(jSon, "description"));
    response = itemId.equals("")
        ? getCreateTargetAuth().post(Entity.entity(multiPart, multiPart.getMediaType()))
        : getUpdateTargetAuth(itemId).put(Entity.entity(multiPart, multiPart.getMediaType()));
    assertEquals(HttpStatus.SC_UNPROCESSABLE_ENTITY, response.getStatus());

    LOGGER.info("Checking city field label  ");
    multiPart.field("json", replaceFieldName(jSon, "city"));
    response = itemId.equals("")
        ? getCreateTargetAuth().post(Entity.entity(multiPart, multiPart.getMediaType()))
        : getUpdateTargetAuth(itemId).put(Entity.entity(multiPart, multiPart.getMediaType()));
    assertEquals(HttpStatus.SC_UNPROCESSABLE_ENTITY, response.getStatus());

    LOGGER.info("Checking country field label  ");
    multiPart.field("json", replaceFieldName(jSon, "country"));
    response = itemId.equals("")
        ? getCreateTargetAuth().post(Entity.entity(multiPart, multiPart.getMediaType()))
        : getUpdateTargetAuth(itemId).put(Entity.entity(multiPart, multiPart.getMediaType()));
    assertEquals(HttpStatus.SC_UNPROCESSABLE_ENTITY, response.getStatus());

    LOGGER.info("Checking date field label  ");
    multiPart.field("json", replaceFieldName(jSon, "date"));
    response = itemId.equals("")
        ? getCreateTargetAuth().post(Entity.entity(multiPart, multiPart.getMediaType()))
        : getUpdateTargetAuth(itemId).put(Entity.entity(multiPart, multiPart.getMediaType()));
    assertEquals(HttpStatus.SC_UNPROCESSABLE_ENTITY, response.getStatus());

    LOGGER.info("Checking license field label  ");
    multiPart.field("json", replaceFieldName(jSon, "license"));
    response = itemId.equals("")
        ? getCreateTargetAuth().post(Entity.entity(multiPart, multiPart.getMediaType()))
        : getUpdateTargetAuth(itemId).put(Entity.entity(multiPart, multiPart.getMediaType()));
    assertEquals(HttpStatus.SC_UNPROCESSABLE_ENTITY, response.getStatus());

    LOGGER.info("Checking url field label  ");
    multiPart.field("json", replaceFieldName(jSon, "url"));
    response = itemId.equals("")
        ? getCreateTargetAuth().post(Entity.entity(multiPart, multiPart.getMediaType()))
        : getUpdateTargetAuth(itemId).put(Entity.entity(multiPart, multiPart.getMediaType()));
    assertEquals(HttpStatus.SC_UNPROCESSABLE_ENTITY, response.getStatus());

    LOGGER.info("Checking link field label  ");
    multiPart.field("json", replaceFieldName(jSon, "link"));
    response = itemId.equals("")
        ? getCreateTargetAuth().post(Entity.entity(multiPart, multiPart.getMediaType()))
        : getUpdateTargetAuth(itemId).put(Entity.entity(multiPart, multiPart.getMediaType()));
    assertEquals(HttpStatus.SC_UNPROCESSABLE_ENTITY, response.getStatus());

    LOGGER.info("Checking publication field label  ");
    multiPart.field("json", replaceFieldName(jSon, "publication"));
    response = itemId.equals("")
        ? getCreateTargetAuth().post(Entity.entity(multiPart, multiPart.getMediaType()))
        : getUpdateTargetAuth(itemId).put(Entity.entity(multiPart, multiPart.getMediaType()));
    assertEquals(HttpStatus.SC_UNPROCESSABLE_ENTITY, response.getStatus());

    LOGGER.info("Checking citation field label  ");
    multiPart.field("json", replaceFieldName(jSon, "citation"));
    response = itemId.equals("")
        ? getCreateTargetAuth().post(Entity.entity(multiPart, multiPart.getMediaType()))
        : getUpdateTargetAuth(itemId).put(Entity.entity(multiPart, multiPart.getMediaType()));
    assertEquals(HttpStatus.SC_UNPROCESSABLE_ENTITY, response.getStatus());
  }


  private Invocation.Builder getUpdateTargetAuth(String itemId) {
    return target(TARGET_PATH_PREFIX).path("/" + itemId).register(authAsUser)
        .register(MultiPartFeature.class).register(JacksonFeature.class)
        .request(MediaType.APPLICATION_JSON_TYPE);
  }

  private Invocation.Builder getCreateTargetAuth() {
    return target(TARGET_PATH_PREFIX).register(authAsUser).register(MultiPartFeature.class)
        .register(JacksonFeature.class).request(MediaType.APPLICATION_JSON_TYPE);
  }

}
