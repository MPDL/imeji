package de.mpg.imeji.test.rest.resources.test.integration;

import static de.mpg.imeji.logic.util.ResourceHelper.getStringFromPath;
import static de.mpg.imeji.test.rest.resources.test.integration.MyTestContainerFactory.STATIC_CONTEXT_REST;
import static de.mpg.imeji.test.rest.resources.test.integration.MyTestContainerFactory.STATIC_CONTEXT_STORAGE;

import java.io.File;
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
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import de.mpg.imeji.logic.controller.ProfileController;
import de.mpg.imeji.logic.search.elasticsearch.ElasticService;
import de.mpg.imeji.logic.vo.Metadata.Types;
import de.mpg.imeji.logic.vo.MetadataProfile;
import de.mpg.imeji.logic.vo.Statement;
import de.mpg.imeji.rest.MyApplication;
import de.mpg.imeji.rest.api.AlbumService;
import de.mpg.imeji.rest.api.CollectionService;
import de.mpg.imeji.rest.api.ItemService;
import de.mpg.imeji.rest.process.RestProcessUtils;
import de.mpg.imeji.rest.to.AlbumTO;
import de.mpg.imeji.rest.to.CollectionTO;
import de.mpg.imeji.rest.to.ItemTO;
import de.mpg.imeji.rest.to.ItemWithFileTO;
import de.mpg.j2j.misc.LocalizedString;
import util.JenaUtil;

/**
 * Created by vlad on 09.12.14.
 */
public class ImejiTestBase extends JerseyTest {

  protected static HttpAuthenticationFeature authAsUser =
      HttpAuthenticationFeature.basic(JenaUtil.TEST_USER_EMAIL, JenaUtil.TEST_USER_PWD);
  protected static HttpAuthenticationFeature authAsUser2 =
      HttpAuthenticationFeature.basic(JenaUtil.TEST_USER_EMAIL_2, JenaUtil.TEST_USER_PWD);

  protected static String collectionId;
  protected static String albumId;
  protected static String profileId;
  protected static String itemId;
  protected static CollectionTO collectionTO;
  protected static AlbumTO albumTO;
  protected static ItemTO itemTO;
  private static Logger logger = Logger.getLogger(ImejiTestBase.class);

  private static MyApplication app = null;

  @Override
  protected Application configure() {
    if (app == null) {
      app = new MyApplication();
    }
    return app;
  }

  @Override
  protected TestContainerFactory getTestContainerFactory() throws TestContainerException {
    return new MyTestContainerFactory();
  }

  @After
  public void reeindex() {
    ElasticService.reset();
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

  /**
   * Create a profile
   */
  public static void initProfile() {
    try {
      ProfileController pc = new ProfileController();
      MetadataProfile p = new MetadataProfile();
      p.setTitle("Test");
      Statement s = new Statement();
      s.getLabels().add(new LocalizedString("Test", null));
      s.setType(URI.create(Types.TEXT.getClazzNamespace()));
      p.getStatements().add(s);
      profileId = pc.create(p, JenaUtil.testUser).getIdString();
    } catch (Exception e) {
      logger.error("Cannot init profile", e);
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
    CollectionService s = new CollectionService();
    try {
      collectionTO = (CollectionTO) RestProcessUtils.buildTOFromJSON(
          getStringFromPath(STATIC_CONTEXT_REST + "/createCollection.json"), CollectionTO.class);
      collectionTO = s.create(collectionTO, JenaUtil.testUser);
      collectionId = collectionTO.getId();
    } catch (Exception e) {
      logger.error("Cannot init Collection", e);
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
    AlbumService s = new AlbumService();
    try {
      albumTO = (AlbumTO) RestProcessUtils.buildTOFromJSON(
          getStringFromPath(STATIC_CONTEXT_REST + "/createAlbum.json"), AlbumTO.class);
      albumTO = s.create(albumTO, JenaUtil.testUser);
      albumId = albumTO.getId();

    } catch (Exception e) {
      logger.error("Cannot init Album", e);
    }
  }

  /**
   * Create an item in the test collection (initCollection must be called before)
   * 
   * @throws Exception
   */
  public static void initItem() {
    ItemService s = new ItemService();
    ItemWithFileTO to = new ItemWithFileTO();
    to.setCollectionId(collectionId);
    to.setFile(new File(STATIC_CONTEXT_STORAGE + "/test.png"));
    to.setStatus("PENDING");
    try {
      itemTO = s.create(to, JenaUtil.testUser);
      itemId = itemTO.getId();
    } catch (Exception e) {
      logger.error("Cannot init Item", e);
    }
  }

  public static void initItem(String fileName) {
    ItemService s = new ItemService();
    ItemWithFileTO to = new ItemWithFileTO();
    to.setCollectionId(collectionId);
    to.setFile(new File(STATIC_CONTEXT_STORAGE + "/" + fileName + ".jpg"));
    to.setStatus("PENDING");
    try {
      itemTO = s.create(to, JenaUtil.testUser);
      itemId = itemTO.getId();
    } catch (Exception e) {
      logger.error("Cannot init Item", e);

    }
  }

}