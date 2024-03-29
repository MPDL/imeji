package de.mpg.imeji.test.rest.resources.test.integration;

import static de.mpg.imeji.logic.util.ResourceHelper.getStringFromPath;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;

import javax.ws.rs.core.Application;

import de.mpg.imeji.logic.security.user.UserService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.StaticHttpHandler;
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.servlet.ServletContainer;
import org.glassfish.jersey.test.DeploymentContext;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.ServletDeploymentContext;
import org.glassfish.jersey.test.TestProperties;
import org.glassfish.jersey.test.grizzly.GrizzlyWebTestContainerFactory;
import org.glassfish.jersey.test.spi.TestContainerException;
import org.glassfish.jersey.test.spi.TestContainerFactory;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

import de.mpg.imeji.rest.ImejiRestService;
import de.mpg.imeji.rest.api.CollectionAPIService;
import de.mpg.imeji.rest.api.ItemAPIService;
import de.mpg.imeji.rest.process.RestProcessUtils;
import de.mpg.imeji.rest.to.CollectionTO;
import de.mpg.imeji.rest.to.LicenseTO;
import de.mpg.imeji.rest.to.defaultItemTO.DefaultItemTO;
import de.mpg.imeji.rest.to.defaultItemTO.DefaultItemWithFileTO;
import de.mpg.imeji.util.ConcurrencyUtil;
import de.mpg.imeji.util.ElasticsearchTestUtil;
import de.mpg.imeji.util.ImejiTestResources;
import de.mpg.imeji.util.JenaUtil;

/**
 * Created by vlad on 09.12.14.
 */
public class ImejiTestBase extends JerseyTest {

  public static final String STATIC_SERVER_URL = "http://localhost:9999";
  public static final String STATIC_CONTEXT_PATH = "/static";
  public static final String STATIC_CONTEXT_STORAGE = "src/test/resources/storage";
  public static final String STATIC_CONTEXT_REST = "src/test/resources/rest";

  private static HttpServer staticServer;
  protected static HttpAuthenticationFeature authAsUser = HttpAuthenticationFeature.basic(JenaUtil.TEST_USER_EMAIL, JenaUtil.TEST_USER_PWD);
  protected static HttpAuthenticationFeature authAsUser2 =
      HttpAuthenticationFeature.basic(JenaUtil.TEST_USER_EMAIL_2, JenaUtil.TEST_USER_PWD);

  protected static HttpAuthenticationFeature authAsUserFalse = HttpAuthenticationFeature.basic("falseuser", "falsepassword");

  protected static String collectionId;
  protected static String albumId;
  protected static String profileId;
  protected static String itemId;
  protected static CollectionTO collectionTO;
  protected static DefaultItemTO itemTO;
  private static final Logger LOGGER = LogManager.getLogger(ImejiTestBase.class);

  private static ImejiRestService app = null;

  @Override
  protected Application configure() {
    enable(TestProperties.LOG_TRAFFIC);
    enable(TestProperties.DUMP_ENTITY);

    return new ImejiRestService(null);
  }

  @Override
  protected DeploymentContext configureDeployment() {
    return ServletDeploymentContext.forServlet(new ServletContainer(new ImejiRestService(null))).servletPath("rest").build();
  }



  @Override
  protected TestContainerFactory getTestContainerFactory() throws TestContainerException {
    return new GrizzlyWebTestContainerFactory();
  }



  @BeforeClass
  public static void setup() throws IOException, URISyntaxException {
    ElasticsearchTestUtil.startElasticsearch();
    JenaUtil.initJena();
    //Start a server for the static content
    staticServer = GrizzlyHttpServerFactory.createHttpServer(new URI(STATIC_SERVER_URL));
    staticServer.getServerConfiguration().addHttpHandler(new StaticHttpHandler(STATIC_CONTEXT_REST, STATIC_CONTEXT_STORAGE),
        STATIC_CONTEXT_PATH);
    staticServer.start();

  }

  @AfterClass
  public static void shutdown() throws IOException, URISyntaxException, InterruptedException {
    ConcurrencyUtil.waitForImejiThreadsToComplete();
    ElasticsearchTestUtil.stopElasticsearch();
    JenaUtil.closeJena();
    staticServer.shutdownNow();
    app = null;
  }

  @Rule
  public TestRule watcher = new TestWatcher() {
    @Override
    protected void starting(Description description) {
      LOGGER.info("Starting test: " + description.getMethodName());
    }
  };

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
      collectionTO =
          (CollectionTO) RestProcessUtils.buildTOFromJSON(getStringFromPath("src/test/resources/rest/collection.json"), CollectionTO.class);

      collectionTO = s.create(collectionTO, JenaUtil.testUser);
      collectionId = collectionTO.getId();
      //Update user
      JenaUtil.testUser = new UserService().retrieve(JenaUtil.testUser.getId(), JenaUtil.testUser);
    } catch (Exception e) {
      LOGGER.error("Cannot init Collection", e);
    }
    return collectionId;
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

      //Update user
      JenaUtil.testUser = new UserService().retrieve(JenaUtil.testUser.getId(), JenaUtil.testUser);
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
