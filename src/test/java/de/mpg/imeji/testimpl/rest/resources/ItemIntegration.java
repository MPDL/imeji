package de.mpg.imeji.testimpl.rest.resources;

import static de.mpg.imeji.logic.util.ResourceHelper.getStringFromPath;
import static de.mpg.imeji.logic.util.StorageUtils.calculateChecksum;
import static de.mpg.imeji.rest.process.RestProcessUtils.jsonToPOJO;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.endsWith;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.text.IsEmptyString.isEmptyOrNullString;
import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.http.HttpStatus;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.media.multipart.file.FileDataBodyPart;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;

import de.mpg.imeji.exceptions.BadRequestException;
import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.core.item.ItemService;
import de.mpg.imeji.rest.api.CollectionAPIService;
import de.mpg.imeji.rest.api.ItemAPIService;
import de.mpg.imeji.rest.process.RestProcessUtils;
import de.mpg.imeji.rest.to.SearchResultTO;
import de.mpg.imeji.rest.to.defaultItemTO.DefaultItemTO;
import de.mpg.imeji.test.rest.resources.test.integration.ItemTestBase;
import de.mpg.imeji.util.ImejiTestResources;
import de.mpg.imeji.util.JenaUtil;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ItemIntegration extends ItemTestBase {
  private static final Logger LOGGER = LoggerFactory.getLogger(ItemIntegration.class);
  private static String itemJSON;
  private static String updateJSON;
  private static final String PATH_PREFIX = "/items";
  private static final String UPDATED_FILE_NAME = "updated_filename.png";
  private static String storedFileURL;

  @BeforeClass
  public static void specificSetup() throws Exception {
    initCollection();
    initItem();
    itemJSON = getStringFromPath("src/test/resources/rest/itemCreateBasic.json");
    updateJSON = getStringFromPath("src/test/resources/rest/item.json");
  }

  @Test
  public void createItemWithEmptyFilename() throws Exception {
    initCollection();
    FileDataBodyPart filePart = new FileDataBodyPart("file", ImejiTestResources.getTestPng());
    FormDataMultiPart multiPart = new FormDataMultiPart();
    multiPart.bodyPart(filePart);
    multiPart.field("json", itemJSON.replace("___COLLECTION_ID___", collectionId).replace("___FILENAME___", "")
        .replace("___REFERENCE_URL___", "").replace("___FETCH_URL___", ""));
    // LOGGER.info(multiPart.getField("json").getValue());
    Response response = getAuthTarget().post(Entity.entity(multiPart, multiPart.getMediaType()));
    assertEquals(Status.CREATED.getStatusCode(), response.getStatus());
    DefaultItemTO item = (DefaultItemTO) response.readEntity(DefaultItemTO.class);
    assertThat("File name is wrong", item.getFilename(), equalTo(filePart.getFileEntity().getName()));
  }

  // see bug https://github.com/imeji-community/imeji/issues/1023

  @Test
  public void createItemWithFile_NullAsExtensionInFileUrl_Bug1023() throws IOException {
    initCollection();
    FileDataBodyPart filePart = new FileDataBodyPart("file", ImejiTestResources.getTest());
    FormDataMultiPart multiPart = new FormDataMultiPart();
    multiPart.bodyPart(filePart);
    multiPart.field("json",
        itemJSON.replace("___COLLECTION_ID___", collectionId).replaceAll("\\s*\"filename\":\\s*\"___FILENAME___\"\\s*,", "")
            .replace("___FILENAME___", "").replace("___REFERENCE_URL___", "").replace("___FETCH_URL___", ""));

    // LOGGER.info(multiPart.getField("json").getValue());
    Response response = getAuthTarget().post(Entity.entity(multiPart, multiPart.getMediaType()));
    assertEquals(CREATED.getStatusCode(), response.getStatus());
    response.readEntity(DefaultItemTO.class);
  }

  @Test
  public void createItemWithoutFilename() throws IOException {
    FileDataBodyPart filePart = new FileDataBodyPart("file", ImejiTestResources.getTestJpg());
    FormDataMultiPart multiPart = new FormDataMultiPart();
    multiPart.bodyPart(filePart);
    multiPart.field("json",
        itemJSON.replace("___COLLECTION_ID___", collectionId).replaceAll("\\s*\"filename\":\\s*\"___FILENAME___\"\\s*,", ""));

    Response response = getAuthTarget().post(Entity.entity(multiPart, multiPart.getMediaType()));

    assertEquals(CREATED.getStatusCode(), response.getStatus());
    DefaultItemTO item = response.readEntity(DefaultItemTO.class);
    assertThat("File name is wrong", item.getFilename(), equalTo(filePart.getFileEntity().getName()));

  }

  @Test
  public void createItemWithFilename() throws IOException {

    FileDataBodyPart filePart = new FileDataBodyPart("file", ImejiTestResources.getTest4Jpg());
    FormDataMultiPart multiPart = new FormDataMultiPart();
    multiPart.bodyPart(filePart);
    multiPart.field("json", itemJSON.replace("___COLLECTION_ID___", collectionId).replace("___FILENAME___", "test4.jpg"));

    Response response = getAuthTarget().post(Entity.entity(multiPart, multiPart.getMediaType()));

    assertEquals(CREATED.getStatusCode(), response.getStatus());
    Map<String, Object> itemData = jsonToPOJO(response);
    assertEquals(Long.toString(ImejiTestResources.getTest4Jpg().length()), Integer.toString((Integer) itemData.get("fileSize")));
  }

  @Test
  public void createItemWithOutCollection() throws IOException {

    FileDataBodyPart filePart = new FileDataBodyPart("file", ImejiTestResources.getTestPng());
    FormDataMultiPart multiPart = new FormDataMultiPart();
    multiPart.bodyPart(filePart);
    multiPart.field("json", itemJSON.replace("___COLLECTION_ID___", ""));

    Response response = getAuthTarget().post(Entity.entity(multiPart, multiPart.getMediaType()));

    assertEquals(HttpStatus.SC_UNPROCESSABLE_ENTITY, response.getStatus());
  }

  @Test
  public void createItemWithoutFile() throws IOException {

    FormDataMultiPart multiPart = new FormDataMultiPart();
    multiPart.field("json", itemJSON.replace("___COLLECTION_ID___", collectionId).replace("___FILENAME___", "test.png")
        .replaceAll("\"fetchUrl\"\\s*:\\s*\"___FETCH_URL___\",", "").replaceAll("\"referenceUrl\"\\s*:\\s*\"___REFERENCE_URL___\",", ""));

    Response response = getAuthTarget().post(Entity.entity(multiPart, multiPart.getMediaType()));

    assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
  }

  @Test
  public void createItemInNotExistingCollection() throws IOException {
    FileDataBodyPart filePart = new FileDataBodyPart("file", ImejiTestResources.getTestPng());
    FormDataMultiPart multiPart = new FormDataMultiPart();
    multiPart.bodyPart(filePart);
    multiPart.field("json", itemJSON.replace("___COLLECTION_ID___", collectionId + "i_do_not_exist").replace("___FILENAME___", "test.png"));

    Response response = getAuthTarget().post(Entity.entity(multiPart, multiPart.getMediaType()));

    assertEquals(HttpStatus.SC_NOT_FOUND, response.getStatus());
  }

  @Test
  public void createItem_NotLoggedIn() throws IOException {
    FileDataBodyPart filePart = new FileDataBodyPart("file", ImejiTestResources.getTestPng());
    FormDataMultiPart multiPart = new FormDataMultiPart();
    multiPart.bodyPart(filePart);
    multiPart.field("json", itemJSON.replace("___COLLECTION_ID___", collectionId).replace("___FILENAME___", "test.png"));

    Response response = target(PATH_PREFIX).register(MultiPartFeature.class).register(JacksonFeature.class)
        .request(MediaType.APPLICATION_JSON_TYPE).post(Entity.entity(multiPart, multiPart.getMediaType()));

    assertEquals(Status.UNAUTHORIZED.getStatusCode(), response.getStatus());

    Response response2 = target(PATH_PREFIX).register(authAsUserFalse).register(MultiPartFeature.class).register(JacksonFeature.class)
        .request(MediaType.APPLICATION_JSON_TYPE).post(Entity.entity(multiPart, multiPart.getMediaType()));

    assertEquals(Status.UNAUTHORIZED.getStatusCode(), response2.getStatus());

  }

  @Test
  public void createItem_InReleasedCollection() throws Exception {
    initItem("test6");
    CollectionAPIService sc = new CollectionAPIService();
    sc.release(collectionId, JenaUtil.testUser);
    assertEquals("RELEASED", sc.read(collectionId, JenaUtil.testUser).getStatus());
    FileDataBodyPart filePart = new FileDataBodyPart("file", ImejiTestResources.getTest2Jpg());
    FormDataMultiPart multiPart = new FormDataMultiPart();
    multiPart.bodyPart(filePart);
    multiPart.field("json", itemJSON.replace("___COLLECTION_ID___", collectionId).replace("___FILENAME___", "test.png"));
    Response response = getAuthTarget().post(Entity.entity(multiPart, multiPart.getMediaType()));
    assertEquals(Status.CREATED.getStatusCode(), response.getStatus());

    ItemAPIService is = new ItemAPIService();
    assertEquals("RELEASED", is.read(itemId, JenaUtil.testUser).getStatus());

  }

  @Test
  public void createItem_InWithdrawnCollection() throws Exception {
    initCollection();
    initItem("test5");
    CollectionAPIService sc = new CollectionAPIService();
    sc.release(collectionId, JenaUtil.testUser);
    assertEquals("RELEASED", sc.read(collectionId, JenaUtil.testUser).getStatus());
    sc.withdraw(collectionId, JenaUtil.testUser, "ItemCreateTest_createItem_InWithdrawnCollection");

    FileDataBodyPart filePart = new FileDataBodyPart("file", ImejiTestResources.getTestPng());
    FormDataMultiPart multiPart = new FormDataMultiPart();
    multiPart.bodyPart(filePart);
    multiPart.field("json", itemJSON.replace("___COLLECTION_ID___", collectionId).replace("___FILENAME___", "test.png"));

    Response response = getAuthTarget().post(Entity.entity(multiPart, multiPart.getMediaType()));

    assertEquals(HttpStatus.SC_UNPROCESSABLE_ENTITY, response.getStatus());

  }

  @Test
  public void createItem_WithNotAllowedUser() throws Exception {
    initCollection();
    FileDataBodyPart filePart = new FileDataBodyPart("file", ImejiTestResources.getTestPng());
    FormDataMultiPart multiPart = new FormDataMultiPart();
    multiPart.bodyPart(filePart);
    multiPart.field("json", itemJSON.replace("___COLLECTION_ID___", collectionId).replace("___FILENAME___", "test.png"));

    Response response = target(PATH_PREFIX).register(authAsUser2).register(MultiPartFeature.class).register(JacksonFeature.class)
        .request(MediaType.APPLICATION_JSON_TYPE).post(Entity.entity(multiPart, multiPart.getMediaType()));

    assertEquals(Status.FORBIDDEN.getStatusCode(), response.getStatus());

  }

  @Test
  public void createItem_SyntaxInvalidJSONFile() throws Exception {
    FileDataBodyPart filePart = new FileDataBodyPart("file", ImejiTestResources.getTestPng());
    FormDataMultiPart multiPart = new FormDataMultiPart();
    multiPart.bodyPart(filePart);
    String wrongJSON = getStringFromPath("src/test/resources/rest/wrongSyntax.json");

    multiPart.field("json", wrongJSON.replace("___COLLECTION_ID___", collectionId).replace("___FILENAME___", "test.png"));

    // LOGGER.info(multiPart.getField("json").getValue());

    Response response = getAuthTarget().post(Entity.entity(multiPart, multiPart.getMediaType()));

    // LOGGER.info(response.readEntity(String.class));
    assertEquals(BAD_REQUEST.getStatusCode(), response.getStatus());

  }

  @Test
  public void createItem_WithFile_Fetched() throws IOException {
    initCollection();
    final String fileURL = STATIC_SERVER_URL + STATIC_CONTEXT_PATH + "/test3.jpg";

    FormDataMultiPart multiPart = new FormDataMultiPart();
    multiPart.field("json", itemJSON.replace("___COLLECTION_ID___", collectionId).replace("___FETCH_URL___", fileURL)

    );

    Response response = getAuthTarget().post(Entity.entity(multiPart, multiPart.getMediaType()));

    assertEquals(Status.CREATED.getStatusCode(), response.getStatus());

  }

  @Test
  public void createItem_WithFile_Fetched_WithEmptyFileName() throws IOException {
    initCollection();
    final String fileURL = STATIC_SERVER_URL + STATIC_CONTEXT_PATH + "/test7.jpg";

    FormDataMultiPart multiPart = new FormDataMultiPart();
    multiPart.field("json",
        itemJSON.replace("___COLLECTION_ID___", collectionId).replace("___FETCH_URL___", fileURL).replace("___FILENAME___", ""));

    Response response = getAuthTarget().post(Entity.entity(multiPart, multiPart.getMediaType()));

    assertEquals(Status.CREATED.getStatusCode(), response.getStatus());

  }

  // TODO: Reference is still not supported with Content
  @Ignore
  @Test
  public void createItem_WithFile_Referenced() throws IOException {

    FormDataMultiPart multiPart = new FormDataMultiPart();
    multiPart.field("json",
        itemJSON.replace("___COLLECTION_ID___", collectionId).replace("___FILENAME___", "test.png")
            .replace("___REFERENCE_URL___", "http://th03.deviantart.net/fs71/PRE/i/2012/242/1/f/png_moon_by_paradise234-d5czhdo.png")
            .replaceAll("\"fetchUrl\"\\s*:\\s*\"___FETCH_URL___\",", "")

    );

    Response response = getAuthTarget().post(Entity.entity(multiPart, multiPart.getMediaType()));

    assertEquals(Status.CREATED.getStatusCode(), response.getStatus());

  }

  @Test
  public void createItem_InvalidFetchURL() throws IOException {

    FormDataMultiPart multiPart = new FormDataMultiPart();

    multiPart.field("json", itemJSON.replace("___COLLECTION_ID___", collectionId).replace("___FETCH_URL___", "invalid url")

    );

    Response response = getAuthTarget().post(Entity.entity(multiPart, multiPart.getMediaType()));

    assertEquals(HttpStatus.SC_UNPROCESSABLE_ENTITY, response.getStatus());

  }

  @Test
  public void createItem_FetchURL_NoFile() throws IOException {

    FormDataMultiPart multiPart = new FormDataMultiPart();

    multiPart.field("json", itemJSON.replace("___COLLECTION_ID___", collectionId).replace("___FETCH_URL___", "www.google.de")

    );

    Response response = getAuthTarget().post(Entity.entity(multiPart, multiPart.getMediaType()));

    assertEquals(HttpStatus.SC_UNPROCESSABLE_ENTITY, response.getStatus());

  }

  // TODO seems that checksum is not indexed when second file is uploaded
  @Ignore
  @Test
  public void createItemChecksumTest() throws Exception {
    String collectionId = initCollection();
    initItem();
    // init Item creates already one item with test.png file , thus checksum
    // is expected
    FileDataBodyPart filePart = new FileDataBodyPart("file", ImejiTestResources.getTestPng());
    FormDataMultiPart multiPart = new FormDataMultiPart();
    multiPart.bodyPart(filePart);
    multiPart.field("json", itemJSON.replace("___COLLECTION_ID___", collectionId).replace("___FILENAME___", "test.png"));

    Response response = getAuthTarget().post(Entity.entity(multiPart, multiPart.getMediaType()));

    assertEquals(HttpStatus.SC_UNPROCESSABLE_ENTITY, response.getStatus());
  }

  @Test
  public void createItemExtensionsTest() throws IOException {
    // NOTE: test assumes .exe file will never be allowed!!!!
    initItem();
    // init Item creates already one item with test.png file , thus checksum
    // is expected
    FileDataBodyPart filePart = new FileDataBodyPart("file", ImejiTestResources.getTestPng());
    FormDataMultiPart multiPart = new FormDataMultiPart();
    multiPart.bodyPart(filePart);
    multiPart.field("json", itemJSON.replace("___COLLECTION_ID___", collectionId).replace("___FILENAME___", "test.exe"));

    Response response = getAuthTarget().post(Entity.entity(multiPart, multiPart.getMediaType()));

    assertEquals(HttpStatus.SC_UNPROCESSABLE_ENTITY, response.getStatus());
  }

  // Default Authorized Target with imeji syntax
  private Invocation.Builder getAuthTarget() {
    return target(PATH_PREFIX).register(authAsUser).register(MultiPartFeature.class).register(JacksonFeature.class)
        .request(MediaType.APPLICATION_JSON_TYPE);
  }

  @Test
  public void test_1_ReadItem_Default() throws Exception {
    // DEFAULT Format
    Response response = (target(PATH_PREFIX).path("/" + itemId).register(authAsUser).register(MultiPartFeature.class)
        .request(MediaType.APPLICATION_JSON_TYPE)).get();
    assertEquals(Status.OK.getStatusCode(), response.getStatus());
    DefaultItemTO itemWithFileTO = response.readEntity(DefaultItemTO.class);
    assertEquals(itemId, itemWithFileTO.getId());
  }

  @Test
  public void test_2_ReadItem_Unauthorized() throws IOException {
    // Default format
    // Read no user
    Response response =
        (target(PATH_PREFIX).path("/" + itemId).register(MultiPartFeature.class).request(MediaType.APPLICATION_JSON_TYPE)).get();
    assertEquals(Status.FORBIDDEN.getStatusCode(), response.getStatus());

    // Read user , but not allowed
    Response response2 = (target(PATH_PREFIX).path("/" + itemId).register(authAsUser2).register(MultiPartFeature.class)
        .request(MediaType.APPLICATION_JSON_TYPE)).get();
    assertEquals(Status.FORBIDDEN.getStatusCode(), response2.getStatus());

    // Read user false credentials
    Response response3 = (target(PATH_PREFIX).path("/" + itemId).register(authAsUserFalse).register(MultiPartFeature.class)
        .request(MediaType.APPLICATION_JSON_TYPE)).get();
    assertEquals(Status.UNAUTHORIZED.getStatusCode(), response3.getStatus());
  }

  @Test
  public void test_3_ReadItem_Forbidden() throws IOException {
    initCollection();
    initItem();
    Response response2 = (target(PATH_PREFIX).path("/" + itemId).register(authAsUser2).register(MultiPartFeature.class)
        .request(MediaType.APPLICATION_JSON_TYPE)).get();
    assertEquals(Status.FORBIDDEN.getStatusCode(), response2.getStatus());

  }

  @Test
  public void test_4_ReadItem_InReleaseCollection() throws Exception {
    CollectionAPIService s = new CollectionAPIService();
    s.release(collectionId, JenaUtil.testUser);
    assertEquals("RELEASED", s.read(collectionId, JenaUtil.testUser).getStatus());

    // RAW FORMAT
    Response response =
        (target(PATH_PREFIX).path("/" + itemId).register(MultiPartFeature.class).request(MediaType.APPLICATION_JSON_TYPE)).get();
    assertEquals(Status.OK.getStatusCode(), response.getStatus());

    Response response1 = (target(PATH_PREFIX).path("/" + itemId).register(authAsUserFalse).register(MultiPartFeature.class)
        .request(MediaType.APPLICATION_JSON_TYPE)).get();
    assertEquals(Status.UNAUTHORIZED.getStatusCode(), response1.getStatus());

    Response response2 = (target(PATH_PREFIX).path("/" + itemId).register(authAsUser2).register(MultiPartFeature.class)
        .request(MediaType.APPLICATION_JSON_TYPE)).get();
    assertEquals(Status.OK.getStatusCode(), response2.getStatus());

  }

  @Test
  public void test_5_ReadItem_InWithDrawnCollection() throws Exception {
    initCollection();
    initItem();
    CollectionAPIService s = new CollectionAPIService();
    s.release(collectionId, JenaUtil.testUser);
    assertEquals("RELEASED", s.read(collectionId, JenaUtil.testUser).getStatus());
    s.withdraw(collectionId, JenaUtil.testUser, "test_5_ReadItem_InWithDrawnCollection_" + System.currentTimeMillis());

    assertEquals("WITHDRAWN", s.read(collectionId, JenaUtil.testUser).getStatus());

    // Default Format
    Response response = (target(PATH_PREFIX).path("/" + itemId).register(authAsUser).register(MultiPartFeature.class)
        .request(MediaType.APPLICATION_JSON_TYPE)).get();
    assertEquals(Status.OK.getStatusCode(), response.getStatus());

    Response response1 = (target(PATH_PREFIX).path("/" + itemId).register(authAsUserFalse).register(MultiPartFeature.class)
        .request(MediaType.APPLICATION_JSON_TYPE)).get();
    assertEquals(Status.UNAUTHORIZED.getStatusCode(), response1.getStatus());

    Response response2 =
        (target(PATH_PREFIX).path("/" + itemId).register(MultiPartFeature.class).request(MediaType.APPLICATION_JSON_TYPE)).get();
    assertEquals(Status.OK.getStatusCode(), response2.getStatus());

  }

  @Test
  public void test_6_ReadItem_NotFound() throws Exception {
    LOGGER.info("Start  test_6_ReadItem_NotFound");
    Response response = (target(PATH_PREFIX).path("/" + itemId + "_not_exist_item").register(authAsUser).register(MultiPartFeature.class)
        .request(MediaType.APPLICATION_JSON_TYPE)).get();
    assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());

    response = (target(PATH_PREFIX).path("/" + itemId + "_not_exist_item").register(MultiPartFeature.class)
        .request(MediaType.APPLICATION_JSON_TYPE)).get();
    assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());

    response = (target(PATH_PREFIX).path("/" + itemId + "_not_exist_item").register(authAsUser2).register(MultiPartFeature.class)
        .request(MediaType.APPLICATION_JSON_TYPE)).get();
    assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());

  }

  @Test
  public void test_7_ReadItemsWithQuery() throws Exception {
    initCollection();
    initItem();
    // DEFAULT FORMAT
    Response response = (target(PATH_PREFIX).queryParam("q", "\"" + itemTO.getFilename() + "\"").register(authAsUser)
        .register(MultiPartFeature.class).request(MediaType.APPLICATION_JSON_TYPE)).get();

    assertEquals(Status.OK.getStatusCode(), response.getStatus());
    SearchResultTO<DefaultItemTO> resultTO =
        RestProcessUtils.buildTOFromJSON(response.readEntity(String.class), new TypeReference<SearchResultTO<DefaultItemTO>>() {});
    List<DefaultItemTO> itemList = resultTO.getResults();
    assertThat(itemList, not(empty()));
    assertThat(itemList.get(0).getFilename(), equalTo(itemTO.getFilename()));
  }

  @Test
  public void test_1_UpdateItem_1_WithFile_Attached() throws IOException, BadRequestException {
    initCollection();
    initItem();
    File testFile = ImejiTestResources.getTest2Jpg();
    String filename = testFile.getName();
    FileDataBodyPart filePart = new FileDataBodyPart("file", testFile);
    FormDataMultiPart multiPart = new FormDataMultiPart();
    multiPart.bodyPart(filePart);
    itemTO.setFilename(null);
    multiPart.field("json", RestProcessUtils.buildJSONFromObject(itemTO));

    Response response = target(PATH_PREFIX).path("/" + itemId).register(authAsUser).register(MultiPartFeature.class)
        .register(JacksonFeature.class).request(MediaType.APPLICATION_JSON_TYPE).put(Entity.entity(multiPart, multiPart.getMediaType()));

    assertEquals(OK.getStatusCode(), response.getStatus());
    DefaultItemTO itemWithFileTO = response.readEntity(DefaultItemTO.class);
    assertThat("Wrong file name", itemWithFileTO.getFilename(), equalTo(filename));
    storedFileURL = target().getUri() + itemWithFileTO.getFileUrl().getPath().substring(1);
    assertEquals(ImejiTestResources.getTest2Jpg().length(), itemWithFileTO.getFileSize());
    // LOGGER.info(RestProcessUtils.buildJSONFromObject(itemWithFileTO));
  }

  @Ignore
  @Test
  public void test_1_UpdateItem_2_WithFile_Fetched() throws ImejiException, IOException {

    initCollection();
    initItem();
    final String fileURL = STATIC_SERVER_URL + STATIC_CONTEXT_PATH + "/test2.jpg";

    FormDataMultiPart multiPart = new FormDataMultiPart();
    multiPart.field("json", getStringFromPath(updateJSON).replace("___FILE_NAME___", UPDATED_FILE_NAME).replace("___FETCH_URL___", fileURL)
        .replaceAll("\"id\"\\s*:\\s*\"__ITEM_ID__\",", "").replace("___REFERENCE_URL___", ""));

    Response response = target(PATH_PREFIX).path("/" + itemId).register(authAsUser).register(MultiPartFeature.class)
        .register(JacksonFeature.class).request(MediaType.APPLICATION_JSON_TYPE).put(Entity.entity(multiPart, multiPart.getMediaType()));

    assertEquals(OK.getStatusCode(), response.getStatus());
    DefaultItemTO itemWithFileTO = response.readEntity(DefaultItemTO.class);
    assertThat("Checksum of stored file deos not match the source file", itemWithFileTO.getChecksumMd5(),
        equalTo(calculateChecksum(ImejiTestResources.getTest2Jpg())));
    // LOGGER.info(RestProcessUtils.buildJSONFromObject(itemWithFileTO));
  }

  @Ignore
  @Test
  public void test_1_UpdateItem_3_WithFile_Referenced() throws IOException {

    FormDataMultiPart multiPart = new FormDataMultiPart();
    multiPart.field("json", getStringFromPath(updateJSON).replace("___FILE_NAME___", UPDATED_FILE_NAME).replace("___FETCH_URL___", "")
        .replaceAll("\"id\"\\s*:\\s*\"__ITEM_ID__\",", "").replace("___REFERENCE_URL___", storedFileURL));

    Response response = target(PATH_PREFIX).path("/" + itemId).register(authAsUser).register(MultiPartFeature.class)
        .register(JacksonFeature.class).request(MediaType.APPLICATION_JSON_TYPE).put(Entity.entity(multiPart, multiPart.getMediaType()));

    assertEquals(OK.getStatusCode(), response.getStatus());
    DefaultItemTO itemWithFileTO = response.readEntity(DefaultItemTO.class);
    assertThat("Reference URL does not match", storedFileURL, equalTo(itemWithFileTO.getFileUrl().toString()));
    assertThat("Should be link to NO_THUMBNAIL image:", itemWithFileTO.getWebResolutionUrlUrl().toString(),
        endsWith(ItemService.NO_THUMBNAIL_URL));
    assertThat("Should be link to NO_THUMBNAIL image:", itemWithFileTO.getThumbnailUrl().toString(),
        endsWith(ItemService.NO_THUMBNAIL_URL));
  }

  @Ignore
  @Test
  public void test_1_UpdateItem_4_WithFile_Attached_Fetched() throws IOException, ImejiException {

    File newFile = ImejiTestResources.getTestPng();

    final String fileURL = STATIC_SERVER_URL + STATIC_CONTEXT_PATH + "/test.jpg";

    FileDataBodyPart filePart = new FileDataBodyPart("file", newFile);

    FormDataMultiPart multiPart = new FormDataMultiPart();
    multiPart.bodyPart(filePart);
    multiPart.field("json", getStringFromPath(updateJSON).replace("___FILE_NAME___", newFile.getName()).replace("___FETCH_URL___", fileURL)
        .replaceAll("\"id\"\\s*:\\s*\"__ITEM_ID__\",", "").replace("___REFERENCE_URL___", ""));

    Response response = target(PATH_PREFIX).path("/" + itemId).register(authAsUser).register(MultiPartFeature.class)
        .register(JacksonFeature.class).request(MediaType.APPLICATION_JSON_TYPE).put(Entity.entity(multiPart, multiPart.getMediaType()));

    assertEquals(OK.getStatusCode(), response.getStatus());
    DefaultItemTO itemWithFileTO = response.readEntity(DefaultItemTO.class);
    assertThat("Checksum of stored file does not match the source file", itemWithFileTO.getChecksumMd5(),
        equalTo(calculateChecksum(newFile)));
    assertThat(itemWithFileTO.getThumbnailUrl().toString(), not(endsWith(ItemService.NO_THUMBNAIL_URL)));
    assertThat(itemWithFileTO.getWebResolutionUrlUrl().toString(), not(endsWith(ItemService.NO_THUMBNAIL_URL)));

  }

  @Ignore
  @Test
  public void test_1_UpdateItem_5_WithFile_Attached_Referenced() throws IOException, ImejiException {
    initCollection();
    initItem();
    File newFile = ImejiTestResources.getTest2Jpg();

    FileDataBodyPart filePart = new FileDataBodyPart("file", newFile);

    FormDataMultiPart multiPart = new FormDataMultiPart();
    multiPart.bodyPart(filePart);
    multiPart.field("json", getStringFromPath(updateJSON).replace("___FILE_NAME___", newFile.getName()).replace("___FETCH_URL___", "")
        .replaceAll("\"id\"\\s*:\\s*\"__ITEM_ID__\",", "").replace("___REFERENCE_URL___", storedFileURL));

    Response response = target(PATH_PREFIX).path("/" + itemId).register(authAsUser).register(MultiPartFeature.class)
        .register(JacksonFeature.class).request(MediaType.APPLICATION_JSON_TYPE).put(Entity.entity(multiPart, multiPart.getMediaType()));

    assertEquals(OK.getStatusCode(), response.getStatus());
    DefaultItemTO itemWithFileTO = response.readEntity(DefaultItemTO.class);
    assertThat("Checksum of stored file does not match the source file", itemWithFileTO.getChecksumMd5(),
        equalTo(calculateChecksum(newFile)));
    assertThat(itemWithFileTO.getThumbnailUrl().toString(), not(endsWith(ItemService.NO_THUMBNAIL_URL)));
    assertThat(itemWithFileTO.getWebResolutionUrlUrl().toString(), not(endsWith(ItemService.NO_THUMBNAIL_URL)));
  }

  @Ignore
  @Test
  public void test_1_UpdateItem_6_WithFile_Fetched_Referenced() throws IOException, ImejiException {

    final String fileURL = STATIC_SERVER_URL + STATIC_CONTEXT_PATH + "/test.jpg";

    FormDataMultiPart multiPart = new FormDataMultiPart();
    multiPart.field("json", getStringFromPath(updateJSON).replace("___FILE_NAME___", UPDATED_FILE_NAME).replace("___FETCH_URL___", fileURL)
        .replaceAll("\"id\"\\s*:\\s*\"__ITEM_ID__\",", "").replace("___REFERENCE_URL___", storedFileURL));

    Response response = target(PATH_PREFIX).path("/" + itemId).register(authAsUser).register(MultiPartFeature.class)
        .register(JacksonFeature.class).request(MediaType.APPLICATION_JSON_TYPE).put(Entity.entity(multiPart, multiPart.getMediaType()));

    assertEquals(OK.getStatusCode(), response.getStatus());
    DefaultItemTO itemWithFileTO = response.readEntity(DefaultItemTO.class);
    assertThat("Checksum of stored file does not match the source file", itemWithFileTO.getChecksumMd5(),
        equalTo(calculateChecksum(ImejiTestResources.getTestJpg())));

    assertThat(itemWithFileTO.getFileUrl().toString(), not(isEmptyOrNullString()));
    assertThat(itemWithFileTO.getFileUrl().toString(), not(equalTo(storedFileURL)));
    assertThat(itemWithFileTO.getThumbnailUrl().toString(), not(endsWith(ItemService.NO_THUMBNAIL_URL)));
    assertThat(itemWithFileTO.getWebResolutionUrlUrl().toString(), not(endsWith(ItemService.NO_THUMBNAIL_URL)));
  }

  @Ignore
  @Test
  public void test_1_UpdateItem_7_WithFile_Attached_Fetched_Referenced() throws IOException, ImejiException {
    initCollection();
    initItem();

    File newFile = ImejiTestResources.getTest1Jpg();
    FileDataBodyPart filePart = new FileDataBodyPart("file", newFile);

    final String fileURL = STATIC_SERVER_URL + STATIC_CONTEXT_PATH + "/test1.jpg";

    FormDataMultiPart multiPart = new FormDataMultiPart();
    multiPart.bodyPart(filePart);
    multiPart.field("json", getStringFromPath(updateJSON).replaceAll("\"id\"\\s*:\\s*\"__ITEM_ID__\",", "")
        .replace("___FILE_NAME___", newFile.getName()).replace("___FETCH_URL___", fileURL).replace("___REFERENCE_URL___", storedFileURL));

    Response response = target(PATH_PREFIX).path("/" + itemId).register(authAsUser).register(MultiPartFeature.class)
        .register(JacksonFeature.class).request(MediaType.APPLICATION_JSON_TYPE).put(Entity.entity(multiPart, multiPart.getMediaType()));

    assertEquals(OK.getStatusCode(), response.getStatus());
    DefaultItemTO itemWithFileTO = response.readEntity(DefaultItemTO.class);
    assertThat("Checksum of stored file does not match the source file", itemWithFileTO.getChecksumMd5(),
        equalTo(calculateChecksum(newFile)));
    assertThat(itemWithFileTO.getFileUrl().toString(), not(equalTo(fileURL)));
    assertThat(itemWithFileTO.getFileUrl().toString(), not(equalTo(storedFileURL)));
    assertThat(itemWithFileTO.getThumbnailUrl().toString(), not(containsString(ItemService.NO_THUMBNAIL_URL)));
    assertThat(itemWithFileTO.getWebResolutionUrlUrl().toString(), not(containsString(ItemService.NO_THUMBNAIL_URL)));
  }

  @Ignore
  @Test
  public void test_1_UpdateItem_8_InvalidFetchURL() throws IOException {

    FormDataMultiPart multiPart = new FormDataMultiPart();

    multiPart.field("json",
        getStringFromPath(updateJSON).replaceAll("\"id\"\\s*:\\s*\"__ITEM_ID__\",", "").replace("___FETCH_URL___", "invalid url")

    );

    Response response = target(PATH_PREFIX).path("/" + itemId).register(authAsUser).register(MultiPartFeature.class)
        .register(JacksonFeature.class).request(MediaType.APPLICATION_JSON_TYPE).put(Entity.entity(multiPart, multiPart.getMediaType()));

    assertEquals(HttpStatus.SC_UNPROCESSABLE_ENTITY, response.getStatus());

  }

  @Ignore
  @Test
  public void test_1_UpdateItem_9_FetchURL_NoFile() throws IOException {

    FormDataMultiPart multiPart = new FormDataMultiPart();

    multiPart.field("json",
        getStringFromPath(updateJSON).replaceAll("\"id\"\\s*:\\s*\"__ITEM_ID__\",", "").replace("___FETCH_URL___", "www.google.de")

    );

    Response response = target(PATH_PREFIX).path("/" + itemId).register(authAsUser).register(MultiPartFeature.class)
        .register(JacksonFeature.class).request(MediaType.APPLICATION_JSON_TYPE).put(Entity.entity(multiPart, multiPart.getMediaType()));

    assertEquals(HttpStatus.SC_UNPROCESSABLE_ENTITY, response.getStatus());

  }

  @Test
  public void test_2_UpdateItem_1_TypeDetection_JPG() throws IOException, BadRequestException {
    initCollection();
    initItem();
    itemTO.setFilename(null);
    File testFile = ImejiTestResources.getTest2Jpg();
    String filename = testFile.getName();
    FileDataBodyPart filePart = new FileDataBodyPart("file", testFile);
    FormDataMultiPart multiPart = new FormDataMultiPart();
    multiPart.bodyPart(filePart);
    itemTO.setFilename(filename);
    multiPart.field("json", RestProcessUtils.buildJSONFromObject(itemTO));

    Response response = target(PATH_PREFIX).path("/" + itemId).register(authAsUser).register(MultiPartFeature.class)
        .register(JacksonFeature.class).request(MediaType.APPLICATION_JSON_TYPE).put(Entity.entity(multiPart, multiPart.getMediaType()));

    assertEquals(OK.getStatusCode(), response.getStatus());
    DefaultItemTO itemWithFileTO = response.readEntity(DefaultItemTO.class);
    assertThat("Wrong file name", itemWithFileTO.getFilename(), equalTo(filename));
  }

  // TODO checksum is not indexed when the upload happens
  @Ignore
  @Test
  public void test_3_UpdateItem_1_WithFile_AndCheckSumTest() throws IOException, BadRequestException {
    initCollection();
    initItem("test2");
    FileDataBodyPart filePart = new FileDataBodyPart("file", ImejiTestResources.getTest2Jpg());
    FormDataMultiPart multiPart = new FormDataMultiPart();
    multiPart.bodyPart(filePart);
    itemTO.setFilename(ImejiTestResources.getTest2Jpg().getName());
    multiPart.field("json", RestProcessUtils.buildJSONFromObject(itemTO));

    Response response = target(PATH_PREFIX).path("/" + itemId).register(authAsUser).register(MultiPartFeature.class)
        .register(JacksonFeature.class).request(MediaType.APPLICATION_JSON_TYPE).put(Entity.entity(multiPart, multiPart.getMediaType()));
    assertEquals(HttpStatus.SC_UNPROCESSABLE_ENTITY, response.getStatus());
  }

  @Test
  public void test_1_deleteItem_WithNonAuth() throws Exception {
    initCollection();
    initItem();
    ItemAPIService s = new ItemAPIService();
    assertEquals("PENDING", s.read(itemId, JenaUtil.testUser).getStatus());

    Form form = new Form();
    form.param("id", itemId);
    Response response = target(PATH_PREFIX).path("/" + itemId).request(MediaType.APPLICATION_JSON_TYPE).delete();
    assertEquals(Status.FORBIDDEN.getStatusCode(), response.getStatus());

    Response response2 = target(PATH_PREFIX).register(authAsUserFalse).path("/" + itemId).request(MediaType.APPLICATION_JSON_TYPE).delete();
    assertEquals(Status.UNAUTHORIZED.getStatusCode(), response2.getStatus());

  }

  @Test
  public void test_2_deleteItem_NotAllowed() throws Exception {
    initCollection();
    initItem();
    ItemAPIService s = new ItemAPIService();
    assertEquals("PENDING", s.read(itemId, JenaUtil.testUser).getStatus());

    Form form = new Form();
    form.param("id", itemId);
    Response response = target(PATH_PREFIX).register(authAsUser2).path("/" + itemId).request(MediaType.APPLICATION_JSON_TYPE).delete();

    assertEquals(Status.FORBIDDEN.getStatusCode(), response.getStatus());
  }

  @Test
  public void test_3_deleteItem_NotExist() throws Exception {
    initItem();
    ItemAPIService s = new ItemAPIService();
    assertEquals("PENDING", s.read(itemId, JenaUtil.testUser).getStatus());

    Form form = new Form();
    form.param("id", itemId + "i_do_not_exist");
    Response response =
        target(PATH_PREFIX).register(authAsUser).path("/" + itemId + "i_do_not_exist").request(MediaType.APPLICATION_JSON_TYPE).delete();

    assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());
  }

  @Test
  public void test_2_deleteItem_Released() throws Exception {
    initCollection();
    initItem();
    ItemAPIService s = new ItemAPIService();
    assertEquals("PENDING", s.read(itemId, JenaUtil.testUser).getStatus());
    CollectionAPIService cs = new CollectionAPIService();
    cs.release(s.read(itemId, JenaUtil.testUser).getCollectionId(), JenaUtil.testUser);
    assertEquals("RELEASED", s.read(itemId, JenaUtil.testUser).getStatus());

    Form form = new Form();
    form.param("id", itemId);
    Response response = target(PATH_PREFIX).register(authAsUser).path("/" + itemId).request(MediaType.APPLICATION_JSON_TYPE).delete();

    assertEquals(HttpStatus.SC_UNPROCESSABLE_ENTITY, response.getStatus());
  }

  @Test
  public void test_2_deleteItem_Withdrawn() throws Exception {
    initCollection();
    initItem();
    ItemAPIService s = new ItemAPIService();
    assertEquals("PENDING", s.read(itemId, JenaUtil.testUser).getStatus());
    CollectionAPIService cs = new CollectionAPIService();
    cs.release(s.read(itemId, JenaUtil.testUser).getCollectionId(), JenaUtil.testUser);
    assertEquals("RELEASED", s.read(itemId, JenaUtil.testUser).getStatus());
    cs.withdraw(s.read(itemId, JenaUtil.testUser).getCollectionId(), JenaUtil.testUser, "ItemDeleteTest.test_2_deleteItemWithdrawn");
    assertEquals("WITHDRAWN", s.read(itemId, JenaUtil.testUser).getStatus());

    Form form = new Form();
    form.param("id", itemId);
    Response response = target(PATH_PREFIX).register(authAsUser).path("/" + itemId).request(MediaType.APPLICATION_JSON_TYPE).delete();

    assertEquals(HttpStatus.SC_UNPROCESSABLE_ENTITY, response.getStatus());
  }

  @Test
  public void test_3_deleteItem() throws Exception {
    initCollection();
    initItem();
    ItemAPIService s = new ItemAPIService();
    assertEquals("PENDING", s.read(itemId, JenaUtil.testUser).getStatus());

    Form form = new Form();
    form.param("id", itemId);
    Response response = target(PATH_PREFIX).register(authAsUser).path("/" + itemId).request(MediaType.APPLICATION_JSON_TYPE).delete();

    assertEquals(Status.NO_CONTENT.getStatusCode(), response.getStatus());

  }
}
