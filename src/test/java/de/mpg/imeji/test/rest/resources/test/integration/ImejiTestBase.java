package de.mpg.imeji.test.rest.resources.test.integration;

import static de.mpg.imeji.logic.util.ResourceHelper.getStringFromPath;
import static de.mpg.imeji.test.rest.resources.test.integration.MyTestContainerFactory.STATIC_CONTEXT_REST;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;

import javax.ws.rs.core.Application;

import org.apache.log4j.Logger;
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.spi.TestContainerException;
import org.glassfish.jersey.test.spi.TestContainerFactory;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

import de.mpg.imeji.logic.controller.resource.ProfileController;
import de.mpg.imeji.logic.vo.MetadataProfile;
import de.mpg.imeji.logic.vo.Statement;
import de.mpg.imeji.logic.vo.predefinedMetadata.Metadata.Types;
import de.mpg.imeji.rest.ImejiRestService;
import de.mpg.imeji.rest.api.AlbumAPIService;
import de.mpg.imeji.rest.api.CollectionAPIService;
import de.mpg.imeji.rest.api.ItemAPIService;
import de.mpg.imeji.rest.process.RestProcessUtils;
import de.mpg.imeji.rest.to.AlbumTO;
import de.mpg.imeji.rest.to.CollectionTO;
import de.mpg.imeji.rest.to.LicenseTO;
import de.mpg.imeji.rest.to.defaultItemTO.DefaultItemTO;
import de.mpg.imeji.rest.to.defaultItemTO.DefaultItemWithFileTO;
import de.mpg.imeji.testimpl.ImejiTestResources;
import de.mpg.imeji.util.LocalizedString;
import util.JenaUtil;

/**
 * Created by vlad on 09.12.14.
 */
public class ImejiTestBase extends JerseyTest {

  protected static HttpAuthenticationFeature authAsUser =
      HttpAuthenticationFeature.basic(JenaUtil.TEST_USER_EMAIL, JenaUtil.TEST_USER_PWD);
  protected static HttpAuthenticationFeature authAsUser2 =
      HttpAuthenticationFeature.basic(JenaUtil.TEST_USER_EMAIL_2, JenaUtil.TEST_USER_PWD);

  protected static HttpAuthenticationFeature authAsUserFalse =
      HttpAuthenticationFeature.basic("falseuser", "falsepassword");

  protected static String collectionId;
  protected static String albumId;
  protected static String profileId;
  protected static String itemId;
  protected static CollectionTO collectionTO;
  protected static AlbumTO albumTO;
  protected static DefaultItemTO itemTO;
  private static final Logger LOGGER = Logger.getLogger(ImejiTestBase.class);

  private static ImejiRestService app = null;

  @Override
  protected Application configure() {
    if (app == null) {
      app = new ImejiRestService();
    }
    return app;
  }

  @Override
  protected TestContainerFactory getTestContainerFactory() throws TestContainerException {
    return new MyTestContainerFactory();
  }

  @BeforeClass
  public static void setup() throws IOException, URISyntaxException {
    JenaUtil.initJena();
  }

  @AfterClass
  public static void shutdown() throws IOException, URISyntaxException, InterruptedException {
    JenaUtil.closeJena();
    app = null;
  }

  @Rule
  public TestRule watcher = new TestWatcher() {
    protected void starting(Description description) {
      LOGGER.info("Starting test: " + description.getMethodName());
    }
  };

  /**
   * Create a profile
   */
  public static void initProfile() {
    try {
      ProfileController pc = new ProfileController();
      MetadataProfile p = new MetadataProfile();
      p.setTitle("Test");
      p.setDefault(false);
      Statement s = new Statement();
      s.getLabels().add(new LocalizedString("Test", null));
      s.setType(URI.create(Types.TEXT.getClazzNamespace()));
      p.getStatements().add(s);
      profileId = pc.create(p, JenaUtil.testUser).getIdString();
    } catch (Exception e) {
      LOGGER.error("Cannot init profile", e);
    }
  }

  /**
   * Create a new collection and set the collectionid
   * 
   * @throws IOException
   * @throws UnsupportedEncodingException
   * 
   * @throws Exception
   */
  public static String initCollection() {
    CollectionAPIService s = new CollectionAPIService();
    try {
      collectionTO = (CollectionTO) RestProcessUtils.buildTOFromJSON(
          getStringFromPath(STATIC_CONTEXT_REST + "/createCollection.json"), CollectionTO.class);

      collectionTO = s.create(collectionTO, JenaUtil.testUser);
      collectionId = collectionTO.getId();
    } catch (Exception e) {
      LOGGER.error("Cannot init Collection", e);
    }
    return collectionId;
  }

  public static String initCollectionWithProfile(String profileId) {
    CollectionAPIService s = new CollectionAPIService();
    try {
      collectionTO = (CollectionTO) RestProcessUtils.buildTOFromJSON(
          getStringFromPath(STATIC_CONTEXT_REST + "/createCollectionWithProfile.json")
              .replace("___PROFILE_ID___", profileId).replace("___METHOD___", "copy"),
          CollectionTO.class);

      collectionTO = s.create(collectionTO, JenaUtil.testUser);
      collectionId = collectionTO.getId();
    } catch (Exception e) {
      LOGGER.error("Cannot init Collection", e);
    }
    return collectionId;
  }

  /**
   * Create a new album and set the albumid
   * 
   * @throws IOException
   * @throws UnsupportedEncodingException
   * 
   * @throws Exception
   */
  public static void initAlbum() {
    AlbumAPIService s = new AlbumAPIService();
    try {
      albumTO = (AlbumTO) RestProcessUtils.buildTOFromJSON(
          getStringFromPath(STATIC_CONTEXT_REST + "/createAlbum.json"), AlbumTO.class);
      albumTO = s.create(albumTO, JenaUtil.testUser);
      albumId = albumTO.getId();

    } catch (Exception e) {
      LOGGER.error("Cannot init Album", e);
    }
  }

  /**
   * Create an item in the test collection (initCollection must be called before)
   * 
   * @throws Exception
   */
  public static void initItem() {
    initItem(null);
  }

  public static void initItem(String fileName) {
    ItemAPIService s = new ItemAPIService();
    DefaultItemWithFileTO to = new DefaultItemWithFileTO();
    to.setCollectionId(collectionId);
    if (fileName == null) {
      to.setFile(ImejiTestResources.getTestPng());
    } else if ("test1".equals(fileName)) {
      to.setFile(ImejiTestResources.getTest1Jpg());
    } else if ("test2".equals(fileName)) {
      to.setFile(ImejiTestResources.getTest2Jpg());
    } else if ("test3".equals(fileName)) {
      to.setFile(ImejiTestResources.getTest3Jpg());
    } else if ("test4".equals(fileName)) {
      to.setFile(ImejiTestResources.getTest4Jpg());
    } else if ("test5".equals(fileName)) {
      to.setFile(ImejiTestResources.getTest5Jpg());
    } else if ("test6".equals(fileName)) {
      to.setFile(ImejiTestResources.getTest6Jpg());
    } else {
      to.setFile(ImejiTestResources.getTestJpg());
    }
    to.setStatus("PENDING");
    addDummyLicenseToItem(to);
    try {
      itemTO = s.create(to, JenaUtil.testUser);
      itemId = itemTO.getId();
    } catch (Exception e) {
      LOGGER.error("Cannot init Item", e);
    }
  }

  public static void addDummyLicenseToItem(DefaultItemTO item) {
    LicenseTO licTO = new LicenseTO();
    licTO.setName("dummy license");
    item.getLicenses().add(licTO);
  }

}
