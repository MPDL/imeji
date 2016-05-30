package de.mpg.imeji.testimpl.rest.resources;

import static javax.ws.rs.core.Response.Status.FORBIDDEN;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.OK;
import static javax.ws.rs.core.Response.Status.UNAUTHORIZED;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.stream.Collectors;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.httpclient.HttpStatus;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import util.JenaUtil;
import de.mpg.imeji.exceptions.BadRequestException;
import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.Imeji;
import de.mpg.imeji.logic.util.ObjectHelper;
import de.mpg.imeji.rest.api.CollectionService;
import de.mpg.imeji.rest.api.ProfileService;
import de.mpg.imeji.rest.process.RestProcessUtils;
import de.mpg.imeji.rest.to.MetadataProfileTO;
import de.mpg.imeji.rest.to.StatementTO;
import de.mpg.imeji.test.rest.resources.test.integration.ImejiTestBase;
import de.mpg.j2j.misc.LocalizedString;

public class ProfileIntegration extends ImejiTestBase {

  private static String pathPrefix = "/rest/profiles";
  private static final Logger LOGGER = LoggerFactory.getLogger(ProfileIntegration.class);

  @Before
  public void specificSetup() {
    initProfile();
    initCollectionWithProfile(profileId);
    initItem();

  }

 @Test
  public void test_1_ReadProfiles() {
    String profileId = collectionTO.getProfile().getId();
    Response response = target(pathPrefix).path(profileId).register(authAsUser)
        .request(MediaType.APPLICATION_JSON).get();
    assertEquals(Status.OK.getStatusCode(), response.getStatus());
  }

  @Test
  public void test_1_ReadProfiles_ReleaseCollection() throws Exception {
    CollectionService cs = new CollectionService();
    cs.release(collectionId, JenaUtil.testUser);
    String profileId = collectionTO.getProfile().getId();
    Response response = target(pathPrefix).path(profileId).register(authAsUser2)
        .request(MediaType.APPLICATION_JSON).get();
    assertEquals(Status.OK.getStatusCode(), response.getStatus());
  }

  // Everybody can read any profiles until the bug is fixed
  @Test
  public void test_1_ReadProfiles_Unauthorized() {
    String profileId = collectionTO.getProfile().getId();

    Response response =
        target(pathPrefix).path(profileId).request(MediaType.APPLICATION_JSON).get();
    assertEquals(Status.UNAUTHORIZED.getStatusCode(), response.getStatus());

    Response response2 = target(pathPrefix).path(profileId).register(authAsUserFalse)
        .request(MediaType.APPLICATION_JSON).get();
    assertEquals(Status.UNAUTHORIZED.getStatusCode(), response2.getStatus());
  }

  @Test
  public void test_1_ReadProfiles_InvalidProfileId() {
    String profileId = collectionTO.getProfile().getId();
    Response response = target(pathPrefix).path(profileId + "invalidID").register(authAsUser)
        .request(MediaType.APPLICATION_JSON).get();
    assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());
  }

  @Test
  public void test_1_ReadProfiles_RegularProfileId() {
    String profileId = collectionTO.getProfile().getId();
    Response response = target(pathPrefix).path(profileId).register(authAsUser)
        .request(MediaType.APPLICATION_JSON).get();
    assertEquals(Status.OK.getStatusCode(), response.getStatus());
  }


  // Everybody can read any profiles until the bug is fixed
  @Test
  public void test_1_ReadProfiles_NotAllowedUser() {
    String profileId = collectionTO.getProfile().getId();
    Response response = target(pathPrefix).path(profileId).register(authAsUser2)
        .request(MediaType.APPLICATION_JSON).get();
    assertEquals(Status.FORBIDDEN.getStatusCode(), response.getStatus());
  }

  @Test
  public void test_2_ReadProfiles_Default() {
    String profileId = ProfileService.DEFAULT_METADATA_PROFILE_ID;
    Response response = target(pathPrefix).path(profileId).register(authAsUser2)
        .request(MediaType.APPLICATION_JSON).get();
    assertEquals(Status.OK.getStatusCode(), response.getStatus());

    MetadataProfileTO profile = response.readEntity(MetadataProfileTO.class);
    assertThat(profile.getId(), equalTo(ObjectHelper.getId(Imeji.defaultMetadataProfile.getId())));
  }
  
  @Test
  public void test_1_ReadProfiles_ItemTemplate() {
    String profileId = collectionTO.getProfile().getId();
    Response response = target(pathPrefix).path(profileId + "/template").register(authAsUser)
        .request(MediaType.APPLICATION_JSON).get();

    assertEquals(Status.OK.getStatusCode(), response.getStatus());
  }

  @Test
  public void test_3_DeleteProfile_NotAuthorized() {
    Response response = target(pathPrefix).path(profileId).register(authAsUser2)
        .request(MediaType.APPLICATION_JSON).delete();
    assertEquals(Status.FORBIDDEN.getStatusCode(), response.getStatus());
  }

  @Test
  public void test_3_DeleteProfile_Unauthorized() {

    Response response =
        target(pathPrefix).path(profileId).request(MediaType.APPLICATION_JSON).delete();
    assertEquals(Status.UNAUTHORIZED.getStatusCode(), response.getStatus());

    response = target(pathPrefix).path(profileId).register(authAsUserFalse)
        .request(MediaType.APPLICATION_JSON).delete();
    assertEquals(Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
  }

  @Test
  public void test_3_DeleteProfile_Referenced() {
    String profileId = collectionTO.getProfile().getId();
    Response response = target(pathPrefix).path(profileId).register(authAsUser)
        .request(MediaType.APPLICATION_JSON).delete();
    assertEquals(HttpStatus.SC_UNPROCESSABLE_ENTITY, response.getStatus());
  }

  @Test
  public void test_3_DeleteProfile_notExists() {
    String profileId = collectionTO.getProfile().getId() + "_doesNotExist";
    Response response = target(pathPrefix).path(profileId).register(authAsUser)
        .request(MediaType.APPLICATION_JSON).delete();
    assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());
  }
  
  @Test
  public void test_3_CreateDoubleDefaultProfiles() throws BadRequestException {
    String profileId = ProfileService.DEFAULT_METADATA_PROFILE_ID;
    Response response = target(pathPrefix).path(profileId).register(authAsUser2)
        .request(MediaType.APPLICATION_JSON).get();
    assertEquals(Status.OK.getStatusCode(), response.getStatus());

    MetadataProfileTO profile = response.readEntity(MetadataProfileTO.class);
    assertThat(profile.getId(), equalTo(ObjectHelper.getId(Imeji.defaultMetadataProfile.getId())));

    profile.setTitle(profile.getTitle()+" A COPY OF DEFAULT PROFILE");
    profile.setDefault(true);
    
    String jSonProfile = RestProcessUtils.buildJSONFromObject(profile); 
    
    Response response1 = target(pathPrefix).register(authAsUser).register(MultiPartFeature.class)
        .request(MediaType.APPLICATION_JSON_TYPE)
        .post(Entity.entity(jSonProfile, MediaType.APPLICATION_JSON_TYPE));
    
    assertEquals(HttpStatus.SC_UNPROCESSABLE_ENTITY, response1.getStatus());

  }

  @Test
  public void test_4_DeleteProfile_1_WithAuth() throws ImejiException {
    initProfile();

    Response response = target(pathPrefix).path("/" + profileId).register(authAsUser)
        .request(MediaType.APPLICATION_JSON_TYPE).delete();

    assertEquals(Status.NO_CONTENT.getStatusCode(), response.getStatus());

    response = target(pathPrefix).path(profileId).register(authAsUser)
        .request(MediaType.APPLICATION_JSON).get();

    assertEquals(NOT_FOUND.getStatusCode(), response.getStatus());

  }

  @Test
  public void test_4_DeleteProfile_2_WithUnauth() throws ImejiException {
    initProfile();
    Response response = target(pathPrefix).path("/" + profileId).register(authAsUser2)
        .request(MediaType.APPLICATION_JSON_TYPE).delete();

    assertEquals(Status.FORBIDDEN.getStatusCode(), response.getStatus());
  }

  @Test
  public void test_5_DeleteProfile_3_NotPendingProfile() {
    initProfile();

    ProfileService proService = new ProfileService();
    try {
      proService.release(profileId, JenaUtil.testUser);
      assertEquals("RELEASED", proService.read(profileId, JenaUtil.testUser).getStatus());
    } catch (ImejiException e) {
      LOGGER.error("Could not release the Metadata profile");
    }

    Response response = target(pathPrefix).path("/" + profileId).register(authAsUser)
        .request(MediaType.APPLICATION_JSON_TYPE).delete();

    assertEquals(HttpStatus.SC_UNPROCESSABLE_ENTITY, response.getStatus());

    try {
      proService.withdraw(profileId, JenaUtil.testUser,
          "test_3_DeleteProfile_3_NotPendingProfile");
      assertEquals("WITHDRAWN", proService.read(profileId, JenaUtil.testUser).getStatus());
    } catch (ImejiException e) {
      LOGGER.error("Could not discard the Metadata profile");
    }

    response = target(pathPrefix).path("/" + profileId).register(authAsUser)
        .request(MediaType.APPLICATION_JSON_TYPE).delete();

    assertEquals(HttpStatus.SC_UNPROCESSABLE_ENTITY, response.getStatus());

  }
  
  @Test
  public void test_6_ReleaseProfile_1_WithAuth() throws ImejiException {


    initProfile();
    ProfileService service = new ProfileService();
    assertEquals("PENDING", service.read(profileId, JenaUtil.testUser).getStatus());

    Response response = target(pathPrefix).path("/" + profileId + "/release")
        .register(authAsUser).request(MediaType.APPLICATION_JSON_TYPE).put(Entity.json("{}"));

    assertEquals(OK.getStatusCode(), response.getStatus());
    assertEquals("RELEASED", service.read(profileId, JenaUtil.testUser).getStatus());

  }

  @Test
  public void test_6_ReleaseProfile_2_WithUnauth() throws ImejiException {
    initProfile();
    ProfileService service = new ProfileService();
    assertEquals("PENDING", service.read(profileId, JenaUtil.testUser).getStatus());

    Response response = target(pathPrefix).path("/" + profileId + "/release")
        .register(authAsUser2).request(MediaType.APPLICATION_JSON_TYPE).put(Entity.json("{}"));
    assertEquals(FORBIDDEN.getStatusCode(), response.getStatus());

    assertEquals("PENDING", service.read(profileId, JenaUtil.testUser).getStatus());
  }

  @Test
  public void test_6_Releaseprofile_4_WithOutUser() {

    Response response = target(pathPrefix).path("/" + profileId + "/release")
        .request(MediaType.APPLICATION_JSON_TYPE).put(Entity.json("{}"));
    assertEquals(UNAUTHORIZED.getStatusCode(), response.getStatus());

    response = target(pathPrefix).path("/" + profileId + "/release").register(authAsUserFalse)
        .request(MediaType.APPLICATION_JSON_TYPE).put(Entity.json("{}"));
    assertEquals(UNAUTHORIZED.getStatusCode(), response.getStatus());
  }

  @Test
  public void test_6_ReleaseProfile_5_ReleaseProfileTwice() throws ImejiException {
    initProfile();
    ProfileService s = new ProfileService();
    s.release(profileId, JenaUtil.testUser);
    assertEquals("RELEASED", s.read(profileId, JenaUtil.testUser).getStatus());
    Response response = target(pathPrefix).path("/" + profileId + "/release")
        .register(authAsUser).request(MediaType.APPLICATION_JSON_TYPE).put(Entity.json("{}"));
    assertEquals(HttpStatus.SC_UNPROCESSABLE_ENTITY, response.getStatus());
  }

  @Test
  public void test_6_ReleaseProfile_6_nonExistingProfile() {
    Response response = target(pathPrefix).path("/" + profileId + "i_do_not_exist/release")
        .register(authAsUser).request(MediaType.APPLICATION_JSON_TYPE).put(Entity.json("{}"));
    assertEquals(NOT_FOUND.getStatusCode(), response.getStatus());
  }

  @Test
  public void test_7_WithdrawProfile_1_WithAuth() throws ImejiException {
    initProfile();
    ProfileService s = new ProfileService();
    s.release(profileId, JenaUtil.testUser);

    assertEquals("RELEASED", s.read(profileId, JenaUtil.testUser).getStatus());
    Form form = new Form();
    form.param("discardComment",
        "test_6_WithdrawProfile_1_WithAuth_" + System.currentTimeMillis());
    Response response = target(pathPrefix).path("/" + profileId + "/discard")
        .register(authAsUser).request((MediaType.APPLICATION_JSON_TYPE))
        .put(Entity.entity(form, MediaType.APPLICATION_FORM_URLENCODED_TYPE));

    
    assertEquals(OK.getStatusCode(), response.getStatus());

    assertEquals("WITHDRAWN", s.read(profileId, JenaUtil.testUser).getStatus());

  }


  @Test
  public void test_7_WithdrawProfile_2_WithUnauth() throws ImejiException {

    initProfile();
    ProfileService s = new ProfileService();
    s.release(profileId, JenaUtil.testUser);

    assertEquals("RELEASED", s.read(profileId, JenaUtil.testUser).getStatus());

    Form form = new Form();
    form.param("discardComment",
        "test_6_WithdrawProfile_2_WithUnAuth_" + System.currentTimeMillis());
    Response response = target(pathPrefix).path("/" + profileId + "/discard")
        .register(authAsUser2).request((MediaType.APPLICATION_JSON_TYPE))
        .put(Entity.entity(form, MediaType.APPLICATION_FORM_URLENCODED_TYPE));

    assertEquals(FORBIDDEN.getStatusCode(), response.getStatus());
  }

  @Test
  public void test_7_WithdrawProfile_3_WithNonAuth() throws ImejiException {

    initProfile();
    ProfileService s = new ProfileService();
    s.release(profileId, JenaUtil.testUser);
    assertEquals("RELEASED", s.read(profileId, JenaUtil.testUser).getStatus());

    Form form = new Form();
    form.param("discardComment",
        "test_7_WithdrawProfile_3_WithNonAuth_" + System.currentTimeMillis());
    Response response = target(pathPrefix).path("/" + profileId + "/discard")
        .request((MediaType.APPLICATION_JSON_TYPE))
        .put(Entity.entity(form, MediaType.APPLICATION_FORM_URLENCODED_TYPE));

    assertEquals(UNAUTHORIZED.getStatusCode(), response.getStatus());

    response = target(pathPrefix).path("/" + profileId + "/discard").register(authAsUserFalse)
        .request((MediaType.APPLICATION_JSON_TYPE))
        .put(Entity.entity(form, MediaType.APPLICATION_FORM_URLENCODED_TYPE));

    assertEquals(UNAUTHORIZED.getStatusCode(), response.getStatus());
  }


  @Test
  public void test_7_WithdrawProfile_4_NotReleasedProfile() throws ImejiException {

    initProfile();
    ProfileService s = new ProfileService();
    assertEquals("PENDING", s.read(profileId, JenaUtil.testUser).getStatus());

    Form form = new Form();
    form.param("discardComment",
        "test_7_WithdrawProfile_4_NotReleasedProfile_" + System.currentTimeMillis());
    Response response = target(pathPrefix).path("/" + profileId + "/discard")
        .register(authAsUser).request((MediaType.APPLICATION_JSON_TYPE))
        .put(Entity.entity(form, MediaType.APPLICATION_FORM_URLENCODED_TYPE));

    assertEquals(HttpStatus.SC_UNPROCESSABLE_ENTITY, response.getStatus());
  }

  @Test
  public void test_7_WithdrawProfile_5_WithdrawProfileTwice() throws ImejiException {

    initProfile();
    ProfileService s = new ProfileService();
    s.release(profileId, JenaUtil.testUser);
    s.withdraw(profileId, JenaUtil.testUser,
        "test_7_WithdrawProfile_5_WithdrawProfileTwice_" + System.currentTimeMillis());

    assertEquals("WITHDRAWN", s.read(profileId, JenaUtil.testUser).getStatus());

    Form form = new Form();
    form.param("discardComment", "test_7_WithdrawProfile_5_WithdrawProfileTwice_SecondTime_"
        + System.currentTimeMillis());
    Response response = target(pathPrefix).path("/" + profileId + "/discard")
        .register(authAsUser).request((MediaType.APPLICATION_JSON_TYPE))
        .put(Entity.entity(form, MediaType.APPLICATION_FORM_URLENCODED_TYPE));

    assertEquals(HttpStatus.SC_UNPROCESSABLE_ENTITY, response.getStatus());
  }

  @Test
  public void test_7_WithdrawProfile_6_NotExistingProfile() throws ImejiException {

    Form form = new Form();
    form.param("discardComment",
        "test_7_WithdrawProfile_6_NotExistingProfile_" + System.currentTimeMillis());
    Response response = target(pathPrefix).path("/" + profileId + "i_do_not_exist/discard")
        .register(authAsUser).request((MediaType.APPLICATION_JSON_TYPE))
        .put(Entity.entity(form, MediaType.APPLICATION_FORM_URLENCODED_TYPE));

    assertEquals(NOT_FOUND.getStatusCode(), response.getStatus());
  }
  

  @Test
  public void test_8_ConsistentStatements_when_copy() throws ImejiException {
    String profileId = ProfileService.DEFAULT_METADATA_PROFILE_ID;
    Response response = target(pathPrefix).path(profileId).register(authAsUser2)
        .request(MediaType.APPLICATION_JSON).get();
    assertEquals(Status.OK.getStatusCode(), response.getStatus());
    String stringJson = response.readEntity(String.class);
    
    MetadataProfileTO profile = (MetadataProfileTO) RestProcessUtils.buildTOFromJSON(stringJson, MetadataProfileTO.class);
    MetadataProfileTO testProfile = (MetadataProfileTO) RestProcessUtils.buildTOFromJSON(stringJson, MetadataProfileTO.class);
    
    assertThat(profile.getId(), equalTo(ObjectHelper.getId(Imeji.defaultMetadataProfile.getId())));
    
    String copyJson= stringJson.replace(profile.getTitle(), " A COPY OF DEFAULT PROFILE AS A NEW ONE")
           .replace("\"default\" : true", "\"default\" : false");
    
    //FLAT PROFILES TEST
    Response response1 = target(pathPrefix).register(authAsUser).register(MultiPartFeature.class)
        .request(MediaType.APPLICATION_JSON_TYPE)
        .post(Entity.entity(copyJson, MediaType.APPLICATION_JSON_TYPE));
    assertEquals(Status.CREATED.getStatusCode(), response1.getStatus());

    MetadataProfileTO copiedProfile = response1.readEntity(MetadataProfileTO.class);

    assertFalse(copiedProfile.getId().equals(profile.getId()));
    assertEquals(copiedProfile.getStatements().size(), profile.getStatements().size());
    assertEquals(testProfile.getStatements().size(), profile.getStatements().size());
    
    List<String> copiedProfileStatements =
        copiedProfile.getStatements().stream()
          .map((StatementTO statement) -> statement.getId())
          .collect(Collectors.toList());
    
    List<String> profileStatements =
        profile.getStatements().stream()
          .map((StatementTO statement) -> statement.getId())
          .collect(Collectors.toList());

    List<String> testProfileStatements =
        testProfile.getStatements().stream()
          .map((StatementTO statement) -> statement.getId())
          .collect(Collectors.toList());
    
    //Test profile here checked just in case to ensure other assertions are fine
    assertEquals(CollectionUtils.disjunction(testProfileStatements, profileStatements).size(), 0);
    assertEquals(CollectionUtils.disjunction(copiedProfileStatements, profileStatements).size(), 
          copiedProfileStatements.size()+ profileStatements.size());
    assertFalse(CollectionUtils.isEqualCollection(copiedProfileStatements, profileStatements));
    assertTrue(CollectionUtils.isEqualCollection(testProfileStatements, profileStatements));
    
    //Hierarchy Profiles Test
    StatementTO child = new StatementTO();
    child.setType(copiedProfile.getStatements().get(0).getType());
    child.setId(copiedProfile.getStatements().get(0).getId()+"-child");

    
    //Map Labels
    child.setLabels(copiedProfile.getStatements().get(0).getLabels().stream()
          .map(label->new LocalizedString(label.getValue()+"-child", label.getLang()))
          .collect(Collectors.toList()));

    child.setParentStatementId(copiedProfile.getStatements().get(0).getId());
    child.setPos(0);

    copiedProfile.getStatements().add(child);
    
    String jSonNew = RestProcessUtils.buildJSONFromObject(copiedProfile);
    
    //Create the new Hierarchical Profile
    Response response2 = target(pathPrefix).register(authAsUser).register(MultiPartFeature.class)
        .request(MediaType.APPLICATION_JSON_TYPE)
        .post(Entity.entity(jSonNew, MediaType.APPLICATION_JSON_TYPE));
    assertEquals(Status.CREATED.getStatusCode(), response2.getStatus());
    copiedProfile = response2.readEntity(MetadataProfileTO.class);
    
    //Create another new Hierarchical Profile to compare to previous one (make no other changes)
    //Create the new Hierarchical Profile
    Response response3 = target(pathPrefix).register(authAsUser).register(MultiPartFeature.class)
        .request(MediaType.APPLICATION_JSON_TYPE)
        .post(Entity.entity(jSonNew, MediaType.APPLICATION_JSON_TYPE));
    assertEquals(Status.CREATED.getStatusCode(), response3.getStatus());
    MetadataProfileTO newCopiedProfile = response3.readEntity(MetadataProfileTO.class);
    
    //Check if profiles have all different Ids
    List<String> copiedHProfileStatements =
        copiedProfile.getStatements().stream()
          .map((StatementTO statement) -> statement.getId()).collect(Collectors.toList());

    List<String> newCopiedHProfileStatements =
        newCopiedProfile.getStatements().stream()
          .map((StatementTO statement) -> statement.getId()).collect(Collectors.toList());
    
    assertFalse(CollectionUtils.isEqualCollection(copiedHProfileStatements, newCopiedHProfileStatements));
    assertEquals(CollectionUtils.disjunction(copiedHProfileStatements, newCopiedHProfileStatements).size(), 
                  copiedHProfileStatements.size()+ newCopiedHProfileStatements.size());
    
    //Now Check that Parent-Child Nodes are with correct id (i.e. all with different Ids, but same Labels)
    StatementTO childStatementCopiedProfile = copiedProfile.getStatements().stream()
          .filter( s-> s.getLabels().get(0).getValue().contains("-child"))
          .findFirst()
          .get();
    
    StatementTO childStatementNewCopiedProfile = newCopiedProfile.getStatements().stream()
          .filter( s-> s.getLabels().get(0).getValue().contains("-child"))
          .findFirst()
          .get();
    
    assertFalse(childStatementCopiedProfile.getId().equals(childStatementNewCopiedProfile.getId()));
    assertFalse(childStatementCopiedProfile.getParentStatementId().equals(childStatementNewCopiedProfile.getParentStatementId()));
    
    StatementTO parentStatementCopiedProfile = copiedProfile.getStatements().stream()
          .filter( s-> s.getId().equals(childStatementCopiedProfile.getParentStatementId()))
          .findFirst()
          .get();
    
    StatementTO parentStatementNewCopiedProfile = newCopiedProfile.getStatements().stream()
          .filter( s-> s.getId().equals(childStatementNewCopiedProfile.getParentStatementId()))
          .findFirst()
          .get();
    
    assertTrue(parentStatementCopiedProfile != null);
    assertTrue(parentStatementNewCopiedProfile != null);
    assertTrue(!parentStatementCopiedProfile.getId().equals(parentStatementNewCopiedProfile.getId()));

    
  }
}
