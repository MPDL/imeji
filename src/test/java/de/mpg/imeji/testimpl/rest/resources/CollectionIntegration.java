package de.mpg.imeji.testimpl.rest.resources;


import static de.mpg.imeji.logic.util.ResourceHelper.getStringFromPath;
import static de.mpg.imeji.rest.process.RestProcessUtils.buildJSONFromObject;
import static de.mpg.imeji.rest.process.RestProcessUtils.jsonToPOJO;
import static de.mpg.imeji.test.rest.resources.test.integration.MyTestContainerFactory.STATIC_CONTEXT_REST;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
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
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

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
import org.apache.commons.lang.StringUtils;
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
import de.mpg.imeji.logic.Imeji;
import de.mpg.imeji.rest.api.CollectionService;
import de.mpg.imeji.rest.api.DefaultItemService;
import de.mpg.imeji.rest.to.CollectionProfileTO;
import de.mpg.imeji.rest.to.CollectionProfileTO.METHOD;
import de.mpg.imeji.rest.to.CollectionTO;
import de.mpg.imeji.rest.to.IdentifierTO;
import de.mpg.imeji.rest.to.OrganizationTO;
import de.mpg.imeji.rest.to.PersonTO;
import de.mpg.imeji.test.rest.resources.test.integration.ImejiTestBase;
import util.JenaUtil;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class CollectionIntegration extends ImejiTestBase {

  private static final Logger LOGGER = LoggerFactory.getLogger(CollectionIntegration.class);

  private static String pathPrefix = "/rest/collections";
  private static String updateJSON;

  @Before
  public void specificSetup() {
    initCollection();
    initProfile();
  }

  @Test
  public void test_1_CreateCollection_1_WithoutProfile() throws IOException, ImejiException {
    String jsonString = getStringFromPath(STATIC_CONTEXT_REST + "/createCollection.json");
    Response response = target(pathPrefix).register(authAsUser).register(MultiPartFeature.class)
        .request(MediaType.APPLICATION_JSON_TYPE)
        .post(Entity.entity(jsonString, MediaType.APPLICATION_JSON_TYPE));
    assertEquals(response.getStatus(), CREATED.getStatusCode());


    Map<String, Object> collData = jsonToPOJO(response);
    assertNotNull("Created collection is null", collData);
    collectionId = (String) collData.get("id");

    assertThat("Empty collection id", collectionId, not(isEmptyOrNullString()));

    CollectionService s = new CollectionService();
    collectionTO = s.read(collectionId, Imeji.adminUser);
    assertNull(collectionTO.getProfile().getId());
  }

  @Test
  public void test_1_CreateCollection_2_CopyProfile()
      throws ImejiException, UnsupportedEncodingException, IOException {
    String jsonString =
        getStringFromPath(STATIC_CONTEXT_REST + "/createCollectionWithProfile.json");
    jsonString = jsonString.replace("___PROFILE_ID___", profileId).replace("___METHOD___", "copy");

    Response response = target(pathPrefix).register(authAsUser).register(MultiPartFeature.class)
        .request(MediaType.APPLICATION_JSON_TYPE)
        .post(Entity.entity(jsonString, MediaType.APPLICATION_JSON_TYPE));

    assertEquals(CREATED.getStatusCode(), response.getStatus());
    Map<String, Object> collData = jsonToPOJO(response);
    assertNotNull("Created collection is null", collData);
    collectionId = (String) collData.get("id");
    assertThat("Empty collection id", collectionId, not(isEmptyOrNullString()));

    CollectionService s = new CollectionService();
    collectionTO = s.read(collectionId, Imeji.adminUser);
    assertNotEquals(profileId, collectionTO.getProfile().getId());

  }

  @Test
  public void test_1_CreateCollection_3_ReferenceProfile()
      throws ImejiException, UnsupportedEncodingException, IOException {
    String jsonString =
        getStringFromPath(STATIC_CONTEXT_REST + "/createCollectionWithProfile.json");
    jsonString =
        jsonString.replace("___PROFILE_ID___", profileId).replace("___METHOD___", "reference");

    Response response =
        target(pathPrefix).register(authAsUser).request(MediaType.APPLICATION_JSON_TYPE)
            .post(Entity.entity(jsonString, MediaType.APPLICATION_JSON_TYPE));

    assertEquals(CREATED.getStatusCode(), response.getStatus());
    Map<String, Object> collData = jsonToPOJO(response);
    assertNotNull("Created collection is null", collData);
    collectionId = (String) collData.get("id");
    assertThat("Empty collection id", collectionId, not(isEmptyOrNullString()));

    CollectionService s = new CollectionService();


    collectionTO = s.read(collectionId, Imeji.adminUser);
    assertEquals(profileId, collectionTO.getProfile().getId());
  }

  @Test
  public void test_1_CreateCollection_4_NotExistedReferenceProfile()
      throws ImejiException, UnsupportedEncodingException, IOException {
    String jsonString =
        getStringFromPath(STATIC_CONTEXT_REST + "/createCollectionWithProfile.json");
    jsonString = jsonString.replace("___PROFILE_ID___", profileId + "shmarrn")
        .replace("___METHOD___", "reference");
    Response response =
        target(pathPrefix).register(authAsUser).request(MediaType.APPLICATION_JSON_TYPE)
            .post(Entity.entity(jsonString, MediaType.APPLICATION_JSON_TYPE));

    assertEquals(response.getStatus(), HttpStatus.SC_UNPROCESSABLE_ENTITY);

  }

  @Ignore
  public void test_1_CreateCollection_5_NotReleasedReferenceProfileByOtherUser()
      throws ImejiException, UnsupportedEncodingException, IOException {

    String jsonString =
        getStringFromPath(STATIC_CONTEXT_REST + "/createCollectionWithProfile.json");
    jsonString =
        jsonString.replace("___PROFILE_ID___", profileId).replace("___METHOD___", "reference");
    Response response =
        target(pathPrefix).register(authAsUser2).request(MediaType.APPLICATION_JSON_TYPE)
            .post(Entity.entity(jsonString, MediaType.APPLICATION_JSON_TYPE));

    assertEquals(HttpStatus.SC_UNPROCESSABLE_ENTITY, response.getStatus());

  }


  @Test
  public void test_1_CreateCollection_5_ReleasedReferenceProfileByOtherUser()
      throws ImejiException, UnsupportedEncodingException, IOException {


    // First set the collection profile, as it is created without a profile
    CollectionProfileTO colProfile = new CollectionProfileTO();
    initProfile();

    colProfile.setId(profileId);
    colProfile.setMethod("copy");
    collectionTO.setProfile(colProfile);

    CollectionService s = new CollectionService();
    collectionTO = s.update(collectionTO, JenaUtil.testUser);
    collectionId = collectionTO.getId();



    initItem();
    // assertEquals("PENDING",itemStatus.read(itemId, JenaUtil.testUser).getStatus());

    Response response = target(pathPrefix).path("/" + collectionId + "/release")
        .register(authAsUser).request(MediaType.APPLICATION_JSON_TYPE).put(Entity.json("{}"));

    assertEquals(OK.getStatusCode(), response.getStatus());

    assertEquals("RELEASED", s.read(collectionId, JenaUtil.testUser).getStatus());
    String jsonString =
        getStringFromPath(STATIC_CONTEXT_REST + "/createCollectionWithProfile.json");

    jsonString = jsonString.replace("___PROFILE_ID___", collectionTO.getProfile().getId())
        .replace("___METHOD___", "reference");

    Response response2 =
        target(pathPrefix).register(authAsUser2).request(MediaType.APPLICATION_JSON_TYPE)
            .post(Entity.entity(jsonString, MediaType.APPLICATION_JSON_TYPE));

    assertEquals(CREATED.getStatusCode(), response2.getStatus());

  }



  @Test
  public void test_1_CreateCollection_5_NoAuth() throws IOException {
    String jsonString = getStringFromPath(STATIC_CONTEXT_REST + "/createCollection.json");
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
  public void test_2_ReadCollection_6_AllItems_WithQuery() throws Exception {
    final int ITEM_AMOUNT = 6;
    for (int i = 1; i <= ITEM_AMOUNT; i++) {
      initItem("test" + i);
    }
    Response response = target(pathPrefix).path(collectionId + "/items")
        .queryParam("q", "test1.jpg test2.jpg test3.jpg test4.jpg test5.jpg test6.jpg")
        .register(authAsUser).request(MediaType.APPLICATION_JSON).get();
    assertEquals(OK.getStatusCode(), response.getStatus());
    String jsonStr = response.readEntity(String.class);
    assertThat(jsonStr, not(isEmptyOrNullString()));
    // check how many times "filename" appears in the result (it should be 6, for all 6 newly
    // created items)
    assertThat(StringUtils.countMatches(jsonStr, "filename"), equalTo(ITEM_AMOUNT));

  }


  @Test
  public void test_3_ReleaseCollection_1_WithAuth() throws ImejiException {


    initCollection();


    DefaultItemService service = new DefaultItemService();

    initItem();
    assertEquals("PENDING", service.read(itemId, JenaUtil.testUser).getStatus());

    Response response = target(pathPrefix).path("/" + collectionId + "/release")
        .register(authAsUser).request(MediaType.APPLICATION_JSON_TYPE).put(Entity.json("{}"));

    assertEquals(OK.getStatusCode(), response.getStatus());

    CollectionService s = new CollectionService();
    assertEquals("RELEASED", s.read(collectionId, JenaUtil.testUser).getStatus());

    assertEquals("RELEASED", service.read(itemId, JenaUtil.testUser).getStatus());

  }

  @Test
  public void test_3_ReleaseCollection_2_WithUnauth() throws ImejiException {
    initCollection();
    initItem();
    DefaultItemService itemService = new DefaultItemService();
    assertEquals("PENDING", itemService.read(itemId, JenaUtil.testUser).getStatus());
    initItem();
    assertEquals("PENDING", itemService.read(itemId, JenaUtil.testUser).getStatus());
    Response response = target(pathPrefix).path("/" + collectionId + "/release")
        .register(authAsUser2).request(MediaType.APPLICATION_JSON_TYPE).put(Entity.json("{}"));
    assertEquals(FORBIDDEN.getStatusCode(), response.getStatus());

    assertEquals("PENDING", itemService.read(itemId, JenaUtil.testUser).getStatus());
  }

  @Test
  public void test_3_ReleaseCollection_3_EmptyCollection() {
    initCollection();
    Response response = target(pathPrefix).path("/" + collectionId + "/release")
        .register(authAsUser).request(MediaType.APPLICATION_JSON_TYPE).put(Entity.json("{}"));
    assertEquals(HttpStatus.SC_UNPROCESSABLE_ENTITY, response.getStatus());
  }

  @Test
  public void test_3_ReleaseCollection_4_WithOutUser() {

    Response response = target(pathPrefix).path("/" + collectionId + "/release")
        .request(MediaType.APPLICATION_JSON_TYPE).put(Entity.json("{}"));
    assertEquals(UNAUTHORIZED.getStatusCode(), response.getStatus());

    response = target(pathPrefix).path("/" + collectionId + "/release").register(authAsUserFalse)
        .request(MediaType.APPLICATION_JSON_TYPE).put(Entity.json("{}"));
    assertEquals(UNAUTHORIZED.getStatusCode(), response.getStatus());
  }

  @Test
  public void test_3_ReleaseCollection_5_ReleaseCollectionTwice() throws ImejiException {
    initItem();
    CollectionService s = new CollectionService();
    s.release(collectionId, JenaUtil.testUser);
    assertEquals("RELEASED", s.read(collectionId, JenaUtil.testUser).getStatus());
    Response response = target(pathPrefix).path("/" + collectionId + "/release")
        .register(authAsUser).request(MediaType.APPLICATION_JSON_TYPE).put(Entity.json("{}"));
    assertEquals(HttpStatus.SC_UNPROCESSABLE_ENTITY, response.getStatus());
  }

  @Test
  public void test_3_ReleaseCollection_6_nonExistingCollection() {
    Response response = target(pathPrefix).path("/" + collectionId + "i_do_not_exist/release")
        .register(authAsUser).request(MediaType.APPLICATION_JSON_TYPE).put(Entity.json("{}"));
    assertEquals(NOT_FOUND.getStatusCode(), response.getStatus());
  }

  @Test
  public void test_4_WithdrawCollection_1_WithAuth() throws ImejiException {
    DefaultItemService itemService = new DefaultItemService();
    initItem();
    CollectionService s = new CollectionService();
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
    CollectionService s = new CollectionService();
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
    CollectionService s = new CollectionService();
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
    CollectionService s = new CollectionService();
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
  public void test_4_WithdrawCollection_5_WithdrawCollectionTwice() throws ImejiException {

    initItem();
    CollectionService s = new CollectionService();
    s.release(collectionId, JenaUtil.testUser);
    s.withdraw(collectionId, JenaUtil.testUser,
        "test_4_WithdrawCollection_5_WithdrawCollectionTwice_" + System.currentTimeMillis());

    assertEquals("WITHDRAWN", s.read(collectionId, JenaUtil.testUser).getStatus());

    Form form = new Form();
    form.param("discardComment", "test_4_WithdrawCollection_5_WithdrawCollectionTwice_SecondTime_"
        + System.currentTimeMillis());
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

    CollectionService colService = new CollectionService();
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
  public void test_6_UpdateCollection_1_Metadata_AllowedChanges()
      throws IOException, BadRequestException, UnprocessableError {

    initCollection();

    String CHANGED = "_changed";

    collectionTO.setTitle(collectionTO.getTitle() + CHANGED);
    collectionTO.setDescription(collectionTO.getDescription() + CHANGED);
    CollectionProfileTO colProfile = new CollectionProfileTO();
    colProfile.setId(profileId);
    collectionTO.setProfile(colProfile);

    String storedProfileId = collectionTO.getProfile().getId();
    collectionTO.getProfile().setMethod(METHOD.COPY.toString());

    for (PersonTO p : collectionTO.getContributors()) {
      p.setFamilyName(p.getFamilyName() + CHANGED);
      p.setGivenName(p.getGivenName() + CHANGED);
      p.setCompleteName(p.getCompleteName() + CHANGED);
      p.setAlternativeName(p.getAlternativeName() + CHANGED);
      p.setRole(p.getRole() + CHANGED);
      for (IdentifierTO i : p.getIdentifiers()) {
        i.setType(i.getType() + CHANGED);
        i.setValue(i.getValue() + CHANGED);
      }
      for (OrganizationTO o : p.getOrganizations()) {
        o.setName(o.getName() + CHANGED);
        o.setDescription(o.getDescription() + CHANGED);
        o.setCountry(o.getCountry() + CHANGED);
        o.setCity(o.getCity() + CHANGED);
        for (IdentifierTO i : o.getIdentifiers()) {
          i.setType(i.getType() + CHANGED);
          i.setValue(i.getValue() + CHANGED);
        }
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
      assertThat(p.getCompleteName(), endsWith(CHANGED));
      assertThat(p.getAlternativeName(), endsWith(CHANGED));
      assertThat(p.getRole(), endsWith(CHANGED));
      for (IdentifierTO i : p.getIdentifiers()) {
        assertThat(i.getType(), not(endsWith(CHANGED)));
        assertThat(i.getValue(), endsWith(CHANGED));
      }
      for (OrganizationTO o : p.getOrganizations()) {
        assertThat(o.getName(), endsWith(CHANGED));
        assertThat(o.getDescription(), endsWith(CHANGED));
        assertThat(o.getCountry(), endsWith(CHANGED));
        assertThat(o.getCity(), endsWith(CHANGED));
        for (IdentifierTO i : o.getIdentifiers()) {
          assertThat(i.getType(), not(endsWith(CHANGED)));
          assertThat(i.getValue(), not(endsWith(CHANGED)));
        }
      }
    }

    // profile COPY
    // Test is wrong
    // in case of profile COPY, only statements are copied from the referenced profile, has nothing
    // to do with the stored ProfileId
    // assertThat("Should be new profileId", uc.getProfile().getProfileId(),
    // not(equalTo(storedProfileId)));


    // profile REFERENCE
    collectionTO.getProfile().setMethod(METHOD.REFERENCE.toString());
    response = getResponse(request, collectionTO);
    assertEquals(OK.getStatusCode(), response.getStatus());
    uc = response.readEntity(CollectionTO.class);
    assertThat("Should be same profileId", uc.getProfile().getId(),
        equalTo(collectionTO.getProfile().getId()));
    collectionTO.getProfile().setMethod("");

  }

  @Test
  public void test_6_UpdateCollection_2_Metadata_NotAllowedChanges()
      throws IOException, BadRequestException {

    initCollection();

    String CHANGED = "_changed";
    String stored;

    Builder request = target(pathPrefix).path("/" + collectionId).register(authAsUser)
        .register(JacksonFeature.class).request(MediaType.APPLICATION_JSON_TYPE);

    // empty collection title
    stored = collectionTO.getTitle();
    collectionTO.setTitle("");
    Response response = getResponse(request, collectionTO);
    assertEquals(HttpStatus.SC_UNPROCESSABLE_ENTITY, response.getStatus());
    // TODO: BAD_REQUEST response message is not correctly generated
    collectionTO.setTitle(stored);

    // wrong collection id
    stored = collectionTO.getId();
    collectionTO.setId(stored + CHANGED);
    response = getResponse(request, collectionTO);
    assertEquals(BAD_REQUEST.getStatusCode(), response.getStatus());
    collectionTO.setId(stored);


    // empty family name
    PersonTO contrib = collectionTO.getContributors().get(0);
    stored = contrib.getFamilyName();
    contrib.setFamilyName("");
    response = getResponse(request, collectionTO);
    assertEquals(HttpStatus.SC_UNPROCESSABLE_ENTITY, response.getStatus());
    contrib.setFamilyName(stored);

    // empty organization name
    OrganizationTO org = contrib.getOrganizations().get(0);
    stored = org.getName();
    org.setName("");
    response = getResponse(request, collectionTO);
    assertEquals(HttpStatus.SC_UNPROCESSABLE_ENTITY, response.getStatus());
    org.setName(stored);

    // wrong profile id
    stored = collectionTO.getProfile().getId();
    collectionTO.getProfile().setId(stored + CHANGED);
    response = getResponse(request, collectionTO);
    assertEquals(HttpStatus.SC_UNPROCESSABLE_ENTITY, response.getStatus());
    collectionTO.getProfile().setId(stored);

    // wrong profile method, we need another collection profile..
    String storedProfileId = collectionTO.getProfile().getId();
    initCollection();
    collectionTO.getProfile().setMethod("wrong_method");
    collectionTO.getProfile().setId(storedProfileId);
    response = getResponse(request, collectionTO);
    assertEquals(BAD_REQUEST.getStatusCode(), response.getStatus());
    collectionTO.getProfile().setMethod("");


  }


  private static Response getResponse(Builder request, CollectionTO collTO)
      throws BadRequestException {
    return request.put(Entity.entity(buildJSONFromObject(collTO), MediaType.APPLICATION_JSON));
  }

}
