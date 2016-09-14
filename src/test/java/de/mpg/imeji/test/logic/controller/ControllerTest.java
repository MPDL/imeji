package de.mpg.imeji.test.logic.controller;

import java.io.File;

import org.junit.AfterClass;
import org.junit.BeforeClass;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.controller.resource.CollectionController;
import de.mpg.imeji.logic.controller.resource.CollectionController.MetadataProfileCreationMethod;
import de.mpg.imeji.logic.controller.resource.ItemController;
import de.mpg.imeji.logic.controller.resource.ProfileController;
import de.mpg.imeji.logic.vo.CollectionImeji;
import de.mpg.imeji.logic.vo.Item;
import de.mpg.imeji.logic.vo.MetadataProfile;
import de.mpg.imeji.logic.vo.predefinedMetadata.Metadata.Types;
import de.mpg.imeji.logic.vo.util.ImejiFactory;
import util.JenaUtil;

/**
 * Created by vlad on 15.04.15.
 */
public class ControllerTest {

  protected static CollectionImeji collection = null;
  protected static MetadataProfile profile = null;
  protected static Item item = null;
  protected static final File originalFile = new File("src/test/resources/storage/test.jpg");
  protected static final File thumbnailFile = new File("src/test/resources/storage/test.png");

  @BeforeClass
  public static void setup() {
    JenaUtil.initJena();
  }

  @AfterClass
  public static void tearDown() throws Exception {
    JenaUtil.closeJena();
  }

  /**
   * Create collection with JenaUtil.testUser
   * 
   * @return
   * @throws ImejiException
   */
  protected static CollectionImeji createCollection() throws ImejiException {
    CollectionController controller = new CollectionController();
    collection = ImejiFactory.newCollection("test", "Planck", "Max", "MPG");
    return controller.create(collection, profile, JenaUtil.testUser,
        MetadataProfileCreationMethod.COPY, null);
  }

  /**
   * Create Profile for current collection with JenaUtil.testUser
   * 
   * @return
   * @throws ImejiException
   */
  protected static MetadataProfile createProfile() throws ImejiException {
    ProfileController controller = new ProfileController();
    profile = new MetadataProfile();
    profile.setTitle("test");
    profile.getStatements().add(ImejiFactory.newStatement("md", "en", Types.TEXT));
    profile = controller.create(profile, JenaUtil.testUser);
    return profile;
  }

  /**
   * Create Item in current collection with JenaUtil.testUser
   * 
   * @return
   * @throws ImejiException
   */
  protected static Item createItem() throws ImejiException {
    ItemController controller = new ItemController();
    item = controller.create(ImejiFactory.newItem(collection), collection, JenaUtil.testUser);
    return item;
  }

  protected static Item createItemWithFile() throws ImejiException {
    ItemController controller = new ItemController();
    if (collection == null) {
      createCollection();
    }
    item = ImejiFactory.newItem(collection);
    item = controller.createWithFile(item, originalFile, "test.jpg", collection, JenaUtil.testUser);
    return item;
  }
}
