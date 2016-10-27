package de.mpg.imeji.test.logic.controller;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.controller.business.ItemBusinessController;
import de.mpg.imeji.logic.controller.resource.CollectionController;
import de.mpg.imeji.logic.controller.resource.CollectionController.MetadataProfileCreationMethod;
import de.mpg.imeji.logic.controller.resource.ProfileController;
import de.mpg.imeji.logic.vo.CollectionImeji;
import de.mpg.imeji.logic.vo.Item;
import de.mpg.imeji.logic.vo.MetadataProfile;
import de.mpg.imeji.logic.vo.User;
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
  private static final File originalFile = new File("src/test/resources/storage/test.jpg");
  private static final File thumbnailFile = new File("src/test/resources/storage/test.png");

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
    ItemBusinessController controller = new ItemBusinessController();
    item = controller.create(ImejiFactory.newItem(collection), collection, JenaUtil.testUser);
    return item;
  }

  protected static Item createItemWithFile() throws ImejiException {
    return createItemWithFile(getOriginalfile());
  }

  protected static Item createItemWithFile(File file) throws ImejiException {
    if (collection == null) {
      createCollection();
    }
    return createItemWithFile(file, collection, JenaUtil.testUser);
  }

  protected static Item createItemWithFile(File file, CollectionImeji collection, User user)
      throws ImejiException {
    ItemBusinessController controller = new ItemBusinessController();
    item = ImejiFactory.newItem(collection);
    item = controller.createWithFile(item, copyFile(file), file.getName(), collection, user);
    return item;
  }

  private static File copyFile(File f) {
    try {
      File copy = File.createTempFile(f.getName(), FilenameUtils.getExtension(f.getName()));
      FileUtils.copyFile(f, copy);
      return copy;
    } catch (Exception e) {
      throw new RuntimeException("Error copying file", e);
    }

  }

  /**
   * @return the originalfile
   */
  protected static File getOriginalfile() {
    return copyFile(originalFile);
  }

  /**
   * @return the thumbnailfile
   */
  protected static File getThumbnailfile() {
    return copyFile(thumbnailFile);
  }
}
