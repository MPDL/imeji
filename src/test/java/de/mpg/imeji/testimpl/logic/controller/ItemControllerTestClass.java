package de.mpg.imeji.testimpl.logic.controller;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.controller.ItemController;
import de.mpg.imeji.logic.storage.StorageController;
import de.mpg.imeji.logic.storage.util.StorageUtils;
import de.mpg.imeji.test.logic.controller.ControllerTest;
import util.JenaUtil;

/**
 * Unit Tests for the {@link ItemController}
 * 
 * @author bastiens
 * 
 */
public class ItemControllerTestClass extends ControllerTest {
  private static final Logger LOGGER = Logger.getLogger(ItemControllerTestClass.class);


  @BeforeClass
  public static void specificSetup() {
    try {
      createCollection();
      createItemWithFile();
    } catch (ImejiException e) {
      LOGGER.error("Error initializing collection or item", e);
    }

  }

  @Test
  public void replaceItemThumbnail() throws ImejiException, IOException {
    ItemController controller = new ItemController();
    try {
      item = controller.updateThumbnail(item, thumbnailFile, JenaUtil.testUser);
    } catch (ImejiException e) {
      Assert.fail("Thubmnail could not be replaced" + e.getMessage());
    }
    StorageController sController = new StorageController();
    File storedFile = File.createTempFile("testFile", null);
    FileOutputStream fos = new FileOutputStream(storedFile);
    sController.read(item.getFullImageUrl().toString(), fos, true);
    Assert.assertEquals(StorageUtils.calculateChecksum(originalFile),
        StorageUtils.calculateChecksum(storedFile));
    Assert.assertEquals(StorageUtils.calculateChecksum(originalFile), item.getChecksum());

  }

  @Test
  public void replaceItemFile() throws ImejiException, IOException {
    ItemController controller = new ItemController();
    try {
      item = controller.updateFile(item, thumbnailFile, "test.tmp", JenaUtil.testUser);
    } catch (ImejiException e) {
      Assert.fail("File could not be replaced" + e.getMessage());
    }
    StorageController sController = new StorageController();
    File storedFile = File.createTempFile("testFile", null);
    FileOutputStream fos = new FileOutputStream(storedFile);
    sController.read(item.getFullImageUrl().toString(), fos, true);
    Assert.assertNotEquals(StorageUtils.calculateChecksum(originalFile),
        StorageUtils.calculateChecksum(storedFile));
    Assert.assertEquals(StorageUtils.calculateChecksum(thumbnailFile),
        StorageUtils.calculateChecksum(storedFile));
    Assert.assertEquals(StorageUtils.calculateChecksum(thumbnailFile), item.getChecksum());

  }

}
