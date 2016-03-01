package de.mpg.imeji.testimpl.rest.resources;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.httpclient.HttpStatus;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.mpg.imeji.logic.Imeji;
import de.mpg.imeji.logic.util.ObjectHelper;
import de.mpg.imeji.rest.api.CollectionService;
import de.mpg.imeji.rest.api.ProfileService;
import de.mpg.imeji.rest.to.MetadataProfileTO;
import de.mpg.imeji.test.rest.resources.test.integration.ImejiTestBase;
import util.JenaUtil;

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
}
