package de.mpg.imeji.test.logic.service;

import java.io.File;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.core.collection.CollectionService;
import de.mpg.imeji.logic.core.item.ItemService;
import de.mpg.imeji.logic.model.CollectionImeji;
import de.mpg.imeji.logic.model.Item;
import de.mpg.imeji.logic.model.User;
import de.mpg.imeji.logic.model.factory.CollectionFactory;
import de.mpg.imeji.logic.model.factory.ImejiFactory;
import de.mpg.imeji.logic.security.user.UserService;
import de.mpg.imeji.util.ConcurrencyUtil;
import de.mpg.imeji.util.ElasticsearchTestUtil;
import de.mpg.imeji.util.ImejiTestResources;
import de.mpg.imeji.util.JenaUtil;

/**
 * Created by vlad on 15.04.15.
 */
public class SuperServiceTest {

  private static final Logger LOGGER = LogManager.getLogger(SuperServiceTest.class);

  protected static CollectionImeji collectionBasic = null;
  protected static Item item = null;

  @Rule
  public TestRule watcher = new TestWatcher() {
    @Override
    protected void starting(Description description) {
      LOGGER.info("Starting test: " + description.getClassName() + "." + description.getMethodName());
    }
  };

  @BeforeClass
  public static void setup() {
    ElasticsearchTestUtil.startElasticsearch();
    JenaUtil.initJena();
  }

  @AfterClass
  public static void tearDown() throws Exception {
    ConcurrencyUtil.waitForImejiThreadsToComplete();
    ElasticsearchTestUtil.stopElasticsearch();
    JenaUtil.closeJena();
  }

  /**
   * Create collection with JenaUtil.testUser
   * 
   * @return
   * @throws ImejiException
   */
  protected static CollectionImeji createCollection() throws ImejiException {
    CollectionService service = new CollectionService();
    CollectionFactory factory = ImejiFactory.newCollection();
    factory.setPerson("Max", "Planck", "MPG");
    collectionBasic = factory.build();
    collectionBasic.setTitle("Test");
    CollectionImeji createdCollection = service.create(collectionBasic, JenaUtil.testUser);
    JenaUtil.testUser = (new UserService()).retrieve(JenaUtil.testUser.getId(), JenaUtil.adminTestUser);
    return createdCollection;
  }

  /**
   * Create Item in current collection with JenaUtil.testUser
   * 
   * @return
   * @throws ImejiException
   */
  protected static Item createItem() throws ImejiException {
    ItemService service = new ItemService();
    item = service.create(ImejiFactory.newItem(collectionBasic), collectionBasic, JenaUtil.testUser);
    return item;
  }

  protected static Item createItemWithFile() throws ImejiException {
    return createItemWithFile(getOriginalfile());
  }

  protected static Item createItemWithFile(File file) throws ImejiException {
    if (collectionBasic == null) {
      createCollection();
    }
    return createItemWithFile(file, collectionBasic, JenaUtil.testUser);
  }

  protected static Item createItemWithFile(File file, CollectionImeji collection, User user) throws ImejiException {
    ItemService service = new ItemService();
    item = ImejiFactory.newItem(collection);
    item = service.createWithFile(item, file, file.getName(), collection, user);
    return item;
  }

  /**
   * @return the originalfile
   */
  protected static File getOriginalfile() {
    return ImejiTestResources.getTestJpg();
  }

  /**
   * @return the thumbnailfile
   */
  protected static File getThumbnailfile() {
    return ImejiTestResources.getTestPng();
  }
}
