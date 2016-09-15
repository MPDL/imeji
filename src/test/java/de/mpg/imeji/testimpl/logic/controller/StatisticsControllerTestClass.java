package de.mpg.imeji.testimpl.logic.controller;


import static org.junit.Assert.assertEquals;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.junit.Test;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.controller.business.StatisticsBusinessController;
import de.mpg.imeji.logic.controller.resource.CollectionController;
import de.mpg.imeji.logic.controller.resource.CollectionController.MetadataProfileCreationMethod;
import de.mpg.imeji.logic.controller.resource.ItemController;
import de.mpg.imeji.logic.vo.CollectionImeji;
import de.mpg.imeji.logic.vo.Item;
import de.mpg.imeji.logic.vo.User;
import de.mpg.imeji.logic.vo.util.ImejiFactory;
import de.mpg.imeji.test.logic.controller.ControllerTest;
import util.JenaUtil;

public class StatisticsControllerTestClass extends ControllerTest {


  @Test
  public void test() throws ImejiException {
    StatisticsBusinessController controller = new StatisticsBusinessController();
    CollectionImeji col1 = createCollection(JenaUtil.testUser);
    Item item1 = createItemWithFile(col1, originalFile, JenaUtil.testUser);
    long totalFileSize = FileUtils.sizeOf(originalFile);
    long result = controller.getUsedStorageSizeForInstitute("imeji.org");
    assertEquals(totalFileSize, result);
    // add again
    Item item2 = createItemWithFile(col1, thumbnailFile, JenaUtil.testUser);
    totalFileSize = totalFileSize + FileUtils.sizeOf(thumbnailFile);;
    result = controller.getUsedStorageSizeForInstitute("imeji.org");
    assertEquals(totalFileSize, result);
    // deleteItem
    ItemController itemController = new ItemController();
    itemController.delete(item2.getIdString(), JenaUtil.testUser);
    totalFileSize = totalFileSize - FileUtils.sizeOf(thumbnailFile);
    result = controller.getUsedStorageSizeForInstitute("imeji.org");
    assertEquals(totalFileSize, result);
    // Upload in another collection
    CollectionImeji col2 = createCollection(JenaUtil.testUser);
    Item item3 = createItemWithFile(col2, originalFile, JenaUtil.testUser);
    totalFileSize = totalFileSize + FileUtils.sizeOf(originalFile);;
    result = controller.getUsedStorageSizeForInstitute("imeji.org");
    assertEquals(totalFileSize, result);
    // Upload by another user
    Item item4 = createItemWithFile(col2, thumbnailFile, JenaUtil.testUser2);
    totalFileSize = totalFileSize + FileUtils.sizeOf(thumbnailFile);;
    result = controller.getUsedStorageSizeForInstitute("imeji.org");
    assertEquals(totalFileSize, result);
  }

  private CollectionImeji createCollection(User user) throws ImejiException {
    CollectionController controller = new CollectionController();
    collection = ImejiFactory.newCollection("test", "Planck", "Max", "MPG");
    return controller.create(collection, profile, user, MetadataProfileCreationMethod.COPY, null);
  }

  private Item createItemWithFile(CollectionImeji col, File file, User user) throws ImejiException {
    ItemController controller = new ItemController();
    Item item = ImejiFactory.newItem(col);
    item = controller.createWithFile(item, file, "test.jpg", collection, JenaUtil.testUser);
    return item;
  }
}
