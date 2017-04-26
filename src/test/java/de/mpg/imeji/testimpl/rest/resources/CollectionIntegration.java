package de.mpg.imeji.testimpl.rest.resources;


import static de.mpg.imeji.logic.util.ResourceHelper.getStringFromPath;
import static de.mpg.imeji.rest.process.RestProcessUtils.buildJSONFromObject;
import static de.mpg.imeji.rest.process.RestProcessUtils.jsonToPOJO;
import static de.mpg.imeji.test.rest.resources.test.integration.MyTestContainerFactory.STATIC_CONTEXT_REST;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.FORBIDDEN;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.OK;
import static javax.ws.rs.core.Response.Status.UNAUTHORIZED;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Map;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.httpclient.HttpStatus;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.mpg.imeji.exceptions.BadRequestException;
import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.exceptions.UnprocessableError;
import de.mpg.imeji.rest.api.CollectionAPIService;
import de.mpg.imeji.rest.api.ItemAPIService;
import de.mpg.imeji.rest.to.CollectionTO;
import de.mpg.imeji.rest.to.OrganizationTO;
import de.mpg.imeji.rest.to.PersonTO;
import de.mpg.imeji.test.rest.resources.test.integration.ImejiTestBase;
import de.mpg.imeji.util.JenaUtil;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class CollectionIntegration extends ImejiTestBase {

  private static final Logger LOGGER = LoggerFactory.getLogger(CollectionIntegration.class);

  private static String pathPrefix = "/rest/collections";
  private static String updateJSON;

  @Before
  public void specificSetup() {
    initCollection();
  }

  @Test
  public void test_1_CreateCollection() throws IOException, ImejiException {
    String jsonString = getStringFromPath(STATIC_CONTEXT_REST + "/collection.json");
    Response response = target(pathPrefix).register(authAsUser).register(MultiPartFeature.class)
        .request(MediaType.APPLICATION_JSON_TYPE)
        .post(Entity.entity(jsonString, MediaType.APPLICATION_JSON_TYPE));
    assertEquals(response.getStatus(), CREATED.getStatusCode());
    Map<String, Object> collData = jsonToPOJO(response);
    assertNotNull("Created collection is null", collData);
    collectionId = (String) collData.get("id");
    assertThat("Empty collection id", collectionId, not(isEmptyOrNullString()));
  }


  @Test
  public void test_1_CreateCollection_5_NoAuth() throws IOException {
    String jsonString = getStringFromPath(STATIC_CONTEXT_REST + "/collection.json");
    Response response =
        target(pathPrefix).register(MultiPartFeature.class).request(MediaType.APPLICATION_JSON_TYPE)
            .post(Entity.entity(jsonString, MediaType.APPLICATION_JSON_TYPE));
    assertEquals(UNAUTHORIZED.getStatusCode(), response.getStatus());

    response = target(pathPrefix).register(authAsUserFalse).register(MultiPartFeature.class)
        .request(MediaType.APPLICATION_JSON_TYPE)
        .post(Entity.entity(jsonString, MediaType.APPLICATION_JSON_TYPE));
    assertEquals(UNAUTHORIZED.getStatusCode(), response.getStatus());
  }

  // TODO: TEST for user who does not have right to create collection

  @Test
  public void test_2_ReadCollection_1() throws ImejiException {
    Response response = target(pathPrefix).path(collectionId).register(authAsUser)
        .request(MediaType.APPLICATION_JSON).get();

    String jsonString = response.readEntity(String.class);
    assertThat("Empty collection", jsonString, not(isEmptyOrNullString()));
  }

  @Test
  public void test_2_ReadCollection_3_Unauthorized() throws ImejiException {
    Response response =
        target(pathPrefix).path(collectionId).request(MediaType.APPLICATION_JSON).get();
    assertThat(response.getStatus(), equalTo(UNAUTHORIZED.getStatusCode()));

    response = target(pathPrefix).path(collectionId).register(authAsUserFalse)
        .request(MediaType.APPLICATION_JSON).get();
    assertThat(response.getStatus(), equalTo(UNAUTHORIZED.getStatusCode()));

  }

  @Test
  public void test_2_ReadCollection_4_Forbidden() throws ImejiException {
    Response response = target(pathPrefix).path(collectionId).register(authAsUser2)
        .request(MediaType.APPLICATION_JSON).get();
    assertThat(response.getStatus(), equalTo(FORBIDDEN.getStatusCode()));
  }

  @Test
  public void test_2_ReadCollection_4_DoesNotExist() throws IOException {
    Response response = target(pathPrefix).path(collectionId + "i_do_not_exist")
        .register(authAsUser).request(MediaType.APPLICATION_JSON).get();
    assertThat(response.getStatus(), equalTo(NOT_FOUND.getStatusCode()));
  }

  @Test
  public void test_2_ReadCollection_5_AllItems() throws Exception {
    initItem();
    Response response = target(pathPrefix).path(collectionId + "/items").register(authAsUser)
        .request(MediaType.APPLICATION_JSON).get();
    assertEquals(OK.getStatusCode(), response.getStatus());
    String jsonStr = response.readEntity(String.class);
    assertThat(jsonStr, not(isEmptyOrNullString()));
  }

  @Test
  public void test_3_ReleaseCollection_1_WithAuth() throws ImejiException {
    initCollection();
    ItemAPIService service = new ItemAPIService();

    initItem();
    assertEquals("PENDING", service.read(itemId, JenaUtil.testUser).getStatus());

    Response response = target(pathPrefix).path("/" + collectionId + "/release")
        .register(authAsUser).request(MediaType.APPLICATION_JSON_TYPE).put(Entity.json("{}"));

    assertEquals(OK.getStatusCode(), response.getStatus());

    CollectionAPIService s = new CollectionAPIService();
    assertEquals("RELEASED", s.read(collectionId, JenaUtil.testUser).getStatus());

    assertEquals("RELEASED", service.read(itemId, JenaUtil.testUser).getStatus());

  }

  @Test
  public void test_3_ReleaseCollection_2_WithUnauth() throws ImejiException {
    initCollection();
    initItem();
    ItemAPIService itemService = new ItemAPIService();
    assertEquals("PENDING", itemService.read(itemId, JenaUtil.testUser).getStatus());
    initItem();
    assertEquals("PENDING", itemService.read(itemId, JenaUtil.testUser).getStatus());
    Response response = target(pathPrefix).path("/" + collectionId + "/release")
        .register(authAsUser2).request(MediaType.APPLICATION_JSON_TYPE).put(Entity.json("{}"));
    assertEquals(FORBIDDEN.getStatusCode(), response.getStatus());

    assertEquals("PENDING", itemService.read(itemId, JenaUtil.testUser).getStatus());
  }

  // TODO move to collection service test
  @Ignore
  @Test
  public void test_3_ReleaseCollection_3_EmptyCollection() {
    initCollection();
    Response response = target(pathPrefix).path("/" + collectionId + "/release")
        .register(authAsUser).request(MediaType.APPLICATION_JSON_TYPE).put(Entity.json("{}"));
    assertEquals(HttpStatus.SC_UNPROCESSABLE_ENTITY, response.getStatus());
  }

  @Test
  public void test_4_WithdrawCollection_1_WithAuth() throws ImejiException {
    ItemAPIService itemService = new ItemAPIService();
    initItem();
    CollectionAPIService s = new CollectionAPIService();
    s.release(collectionId, JenaUtil.testUser);

    assertEquals("RELEASED", s.read(collectionId, JenaUtil.testUser).getStatus());
    assertEquals("RELEASED", itemService.read(itemId, JenaUtil.testUser).getStatus());

    Form form = new Form();
    form.param("discardComment",
        "test_4_WithdrawCollection_1_WithAuth_" + System.currentTimeMillis());
    Response response = target(pathPrefix).path("/" + collectionId + "/discard")
        .register(authAsUser).request((MediaType.APPLICATION_JSON_TYPE))
        .put(Entity.entity(form, MediaType.APPLICATION_FORM_URLENCODED_TYPE));

    assertEquals(OK.getStatusCode(), response.getStatus());

    assertEquals("WITHDRAWN", s.read(collectionId, JenaUtil.testUser).getStatus());

    assertEquals("WITHDRAWN", itemService.read(itemId, JenaUtil.testUser).getStatus());

  }


  @Test
  public void test_4_WithdrawCollection_2_WithUnauth() throws ImejiException {

    initCollection();
    initItem();
    CollectionAPIService s = new CollectionAPIService();
    s.release(collectionId, JenaUtil.testUser);

    assertEquals("RELEASED", s.read(collectionId, JenaUtil.testUser).getStatus());

    Form form = new Form();
    form.param("discardComment",
        "test_4_WithdrawCollection_2_WithUnAuth_" + System.currentTimeMillis());
    Response response = target(pathPrefix).path("/" + collectionId + "/discard")
        .register(authAsUser2).request((MediaType.APPLICATION_JSON_TYPE))
        .put(Entity.entity(form, MediaType.APPLICATION_FORM_URLENCODED_TYPE));

    assertEquals(FORBIDDEN.getStatusCode(), response.getStatus());
  }

  @Test
  public void test_4_WithdrawCollection_3_WithNonAuth() throws ImejiException {

    initItem();
    CollectionAPIService s = new CollectionAPIService();
    s.release(collectionId, JenaUtil.testUser);
    assertEquals("RELEASED", s.read(collectionId, JenaUtil.testUser).getStatus());

    Form form = new Form();
    form.param("discardComment",
        "test_4_WithdrawCollection_3_WithNonAuth_" + System.currentTimeMillis());
    Response response = target(pathPrefix).path("/" + collectionId + "/discard")
        .request((MediaType.APPLICATION_JSON_TYPE))
        .put(Entity.entity(form, MediaType.APPLICATION_FORM_URLENCODED_TYPE));

    assertEquals(UNAUTHORIZED.getStatusCode(), response.getStatus());

    response = target(pathPrefix).path("/" + collectionId + "/discard").register(authAsUserFalse)
        .request((MediaType.APPLICATION_JSON_TYPE))
        .put(Entity.entity(form, MediaType.APPLICATION_FORM_URLENCODED_TYPE));

    assertEquals(UNAUTHORIZED.getStatusCode(), response.getStatus());
  }


  @Test
  public void test_4_WithdrawCollection_4_NotReleasedCollection() throws ImejiException {

    initItem();
    CollectionAPIService s = new CollectionAPIService();
    assertEquals("PENDING", s.read(collectionId, JenaUtil.testUser).getStatus());

    Form form = new Form();
    form.param("discardComment",
        "test_4_WithdrawCollection_4_NotReleasedCollection_" + System.currentTimeMillis());
    Response response = target(pathPrefix).path("/" + collectionId + "/discard")
        .register(authAsUser).request((MediaType.APPLICATION_JSON_TYPE))
        .put(Entity.entity(form, MediaType.APPLICATION_FORM_URLENCODED_TYPE));

    assertEquals(HttpStatus.SC_UNPROCESSABLE_ENTITY, response.getStatus());
  }

  @Test
  public void test_4_WithdrawCollection_6_NotExistingCollection() throws ImejiException {

    Form form = new Form();
    form.param("discardComment",
        "test_4_WithdrawCollection_6_NotExistingCollection_" + System.currentTimeMillis());
    Response response = target(pathPrefix).path("/" + collectionId + "i_do_not_exist/discard")
        .register(authAsUser).request((MediaType.APPLICATION_JSON_TYPE))
        .put(Entity.entity(form, MediaType.APPLICATION_FORM_URLENCODED_TYPE));

    assertEquals(NOT_FOUND.getStatusCode(), response.getStatus());
  }


  @Test
  public void test_5_DeleteCollection_1_WithAuth() throws ImejiException {
    initCollection();

    Response response = target(pathPrefix).path("/" + collectionId).register(authAsUser)
        .request(MediaType.APPLICATION_JSON_TYPE).delete();

    assertEquals(Status.NO_CONTENT.getStatusCode(), response.getStatus());

    response = target(pathPrefix).path(collectionId).register(authAsUser)
        .request(MediaType.APPLICATION_JSON).get();

    assertEquals(NOT_FOUND.getStatusCode(), response.getStatus());

  }


  @Test
  public void test_5_DeleteCollection_2_WithUnauth() throws ImejiException {
    initCollection();
    Response response = target(pathPrefix).path("/" + collectionId).register(authAsUser2)
        .request(MediaType.APPLICATION_JSON_TYPE).delete();

    assertEquals(Status.FORBIDDEN.getStatusCode(), response.getStatus());
  }

  @Test
  public void test_5_DeleteCollection_3_NotPendingCollection() {
    initCollection();
    initItem();

    CollectionAPIService colService = new CollectionAPIService();
    try {
      colService.release(collectionId, JenaUtil.testUser);
      assertEquals("RELEASED", colService.read(collectionId, JenaUtil.testUser).getStatus());
    } catch (ImejiException e) {
      LOGGER.error("Could not release collection");
    }

    Response response = target(pathPrefix).path("/" + collectionId).register(authAsUser)
        .request(MediaType.APPLICATION_JSON_TYPE).delete();

    assertEquals(HttpStatus.SC_UNPROCESSABLE_ENTITY, response.getStatus());

    try {
      colService.withdraw(collectionId, JenaUtil.testUser,
          "test_3_DeleteCollection_3_NotPendingCollection");
      assertEquals("WITHDRAWN", colService.read(collectionId, JenaUtil.testUser).getStatus());
    } catch (ImejiException e) {
      LOGGER.error("Could not discard the collection");
    }

    response = target(pathPrefix).path("/" + collectionId).register(authAsUser)
        .request(MediaType.APPLICATION_JSON_TYPE).delete();

    assertEquals(HttpStatus.SC_UNPROCESSABLE_ENTITY, response.getStatus());

  }

  @Test
  public void test_5_DeleteCollection_4_WithOutUser() {
    initCollection();

    Response response = target(pathPrefix).path("/" + collectionId)
        .request(MediaType.APPLICATION_JSON_TYPE).delete();
    assertEquals(UNAUTHORIZED.getStatusCode(), response.getStatus());

    response = target(pathPrefix).path("/" + collectionId).register(authAsUserFalse)
        .request(MediaType.APPLICATION_JSON_TYPE).delete();
    assertEquals(UNAUTHORIZED.getStatusCode(), response.getStatus());
  }


  @Test
  public void test_5_DeleteCollection_1_nonExistingCollection() {
    Response response = target(pathPrefix).path("/" + collectionId + "i_do_not_exist")
        .register(authAsUser).request(MediaType.APPLICATION_JSON_TYPE).delete();

    assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());
  }



  @Test
  public void test_6_UpdateCollection()
      throws IOException, BadRequestException, UnprocessableError {
    initCollection();
    String CHANGED = "_changed";
    collectionTO.setTitle(collectionTO.getTitle() + CHANGED);
    collectionTO.setDescription(collectionTO.getDescription() + CHANGED);

    for (PersonTO p : collectionTO.getContributors()) {
      p.setFamilyName(p.getFamilyName() + CHANGED);
      p.setGivenName(p.getGivenName() + CHANGED);
      for (OrganizationTO o : p.getOrganizations()) {
        o.setName(o.getName() + CHANGED);
      }
    }

    Builder request = target(pathPrefix).path("/" + collectionId).register(authAsUser)
        .register(JacksonFeature.class).request(MediaType.APPLICATION_JSON_TYPE);

    Response response =
        request.put(Entity.entity(buildJSONFromObject(collectionTO), MediaType.APPLICATION_JSON));
    assertEquals(OK.getStatusCode(), response.getStatus());

    CollectionTO uc = response.readEntity(CollectionTO.class);

    assertEquals(collectionTO.getId(), uc.getId());

    assertThat(uc.getTitle(), endsWith(CHANGED));
    assertThat(uc.getDescription(), endsWith(CHANGED));

    for (PersonTO p : uc.getContributors()) {
      assertThat(p.getFamilyName(), endsWith(CHANGED));
      assertThat(p.getGivenName(), endsWith(CHANGED));
      for (OrganizationTO o : p.getOrganizations()) {
        assertThat(o.getName(), endsWith(CHANGED));
      }
    }
  }


  @Test
  public void test_6_CreateCollection_1_AdditionalInfos()
      throws ImejiException, UnsupportedEncodingException, IOException {
    String originalJsonString = getStringFromPath(STATIC_CONTEXT_REST + "/collection.json");

    String jsonString = originalJsonString;
    jsonString = jsonString.replace("\"label\": \"Label1\",", "");

    // Additional info without label
    Response response = target(pathPrefix).register(authAsUser).register(MultiPartFeature.class)
        .request(MediaType.APPLICATION_JSON_TYPE)
        .post(Entity.entity(jsonString, MediaType.APPLICATION_JSON_TYPE));
    assertEquals(HttpStatus.SC_UNPROCESSABLE_ENTITY, response.getStatus());

    // Additional info without label and text
    jsonString = jsonString.replace("\"text\": \"This is the text of Label 1\",", "");
    Response response1 = target(pathPrefix).register(authAsUser).register(MultiPartFeature.class)
        .request(MediaType.APPLICATION_JSON_TYPE)
        .post(Entity.entity(jsonString, MediaType.APPLICATION_JSON_TYPE));
    assertEquals(HttpStatus.SC_UNPROCESSABLE_ENTITY, response1.getStatus());

    // Additional info with label, no text and no value
    jsonString = originalJsonString.replace("Label1\",", "Label1\"")
        .replace("\"text\": \"This is the text of Label 1\",", "")
        .replace("\"url\": \"http://example.org\"", "");
    Response response2 = target(pathPrefix).register(authAsUser).register(MultiPartFeature.class)
        .request(MediaType.APPLICATION_JSON_TYPE)
        .post(Entity.entity(jsonString, MediaType.APPLICATION_JSON_TYPE));
    assertEquals(HttpStatus.SC_UNPROCESSABLE_ENTITY, response2.getStatus());
  }

  @Test
  public void test_6_UpdateCollection_1_AdditionalInfos()
      throws ImejiException, UnsupportedEncodingException, IOException {
    String originalJsonString = getStringFromPath(STATIC_CONTEXT_REST + "/collection.json");

    // Create the collection
    Response response = target(pathPrefix).register(authAsUser).register(MultiPartFeature.class)
        .request(MediaType.APPLICATION_JSON_TYPE)
        .post(Entity.entity(originalJsonString, MediaType.APPLICATION_JSON_TYPE));

    assertEquals(CREATED.getStatusCode(), response.getStatus());

    Map<String, Object> collData = jsonToPOJO(response);
    assertNotNull("Created collection is null", collData);
    collectionId = (String) collData.get("id");

    originalJsonString =
        originalJsonString.replaceFirst("\\{", "{ \"id\": \"" + collectionId + "\",");

    // Update the collection
    String jsonString = originalJsonString;

    // Additional info without label
    jsonString = jsonString.replace("\"label\": \"Label1\",", "");

    Response response1 = target(pathPrefix).path("/" + collectionId).register(authAsUser)
        .register(MultiPartFeature.class).request(MediaType.APPLICATION_JSON_TYPE)
        .put(Entity.entity(jsonString, MediaType.APPLICATION_JSON_TYPE));
    assertEquals(HttpStatus.SC_UNPROCESSABLE_ENTITY, response1.getStatus());


    // Additional info without label and text
    jsonString = jsonString.replace("\"text\": \"This is the text of Label 1\",", "");
    Response response2 = target(pathPrefix).path("/" + collectionId).register(authAsUser)
        .register(MultiPartFeature.class).request(MediaType.APPLICATION_JSON_TYPE)
        .put(Entity.entity(jsonString, MediaType.APPLICATION_JSON_TYPE));

    assertEquals(HttpStatus.SC_UNPROCESSABLE_ENTITY, response2.getStatus());

    // Additional info with label, no text and no value
    jsonString = originalJsonString.replace("Label1\",", "Label1\"")
        .replace("\"text\": \"This is the text of Label 1\",", "")
        .replace("\"url\": \"http://example.org\"", "");
    Response response3 = target(pathPrefix).path("/" + collectionId).register(authAsUser)
        .register(MultiPartFeature.class).request(MediaType.APPLICATION_JSON_TYPE)
        .put(Entity.entity(jsonString, MediaType.APPLICATION_JSON_TYPE));

    assertEquals(HttpStatus.SC_UNPROCESSABLE_ENTITY, response3.getStatus());
  }
}
