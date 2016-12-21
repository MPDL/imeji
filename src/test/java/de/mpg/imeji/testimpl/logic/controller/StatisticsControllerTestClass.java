package de.mpg.imeji.testimpl.logic.controller;


import static org.junit.Assert.assertEquals;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.junit.Test;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.controller.business.StatisticsBusinessController;
import de.mpg.imeji.logic.controller.resource.CollectionController;
import de.mpg.imeji.logic.controller.resource.CollectionController.MetadataProfileCreationMethod;
import de.mpg.imeji.logic.service.item.ItemService;
import de.mpg.imeji.logic.user.collaboration.share.ShareBusinessController;
import de.mpg.imeji.logic.user.collaboration.share.ShareBusinessController.ShareRoles;
import de.mpg.imeji.logic.vo.CollectionImeji;
import de.mpg.imeji.logic.vo.Item;
import de.mpg.imeji.logic.vo.User;
import de.mpg.imeji.logic.vo.factory.ImejiFactory;
import de.mpg.imeji.test.logic.controller.ControllerTest;
import util.JenaUtil;

public class StatisticsControllerTestClass extends ControllerTest {


  @Test
  public void test() throws ImejiException {
    StatisticsBusinessController controller = new StatisticsBusinessController();
    CollectionImeji col1 = createCollection(JenaUtil.testUser);
    Item item1 = createItemWithFile(col1, getOriginalfile(), JenaUtil.testUser);
    long totalFileSize = FileUtils.sizeOf(getOriginalfile());
    long result = controller.getUsedStorageSizeForInstitute("imeji.org");
    assertEquals(totalFileSize, result);
    // add again
    Item item2 = createItemWithFile(col1, getThumbnailfile(), JenaUtil.testUser);
    totalFileSize = totalFileSize + FileUtils.sizeOf(getThumbnailfile());;
    result = controller.getUsedStorageSizeForInstitute("imeji.org");
    assertEquals(totalFileSize, result);
    // deleteItem
    ItemService itemController = new ItemService();
    itemController.delete(item2.getIdString(), JenaUtil.testUser);
    totalFileSize = totalFileSize - FileUtils.sizeOf(getThumbnailfile());
    result = controller.getUsedStorageSizeForInstitute("imeji.org");
    assertEquals(totalFileSize, result);
    // Upload in another collection
    CollectionImeji col2 = createCollection(JenaUtil.testUser);
    Item item3 = createItemWithFile(col2, getOriginalfile(), JenaUtil.testUser);
    totalFileSize = totalFileSize + FileUtils.sizeOf(getOriginalfile());;
    result = controller.getUsedStorageSizeForInstitute("imeji.org");
    assertEquals(totalFileSize, result);
    // Upload by another user
    new ShareBusinessController().shareToUser(JenaUtil.testUser, JenaUtil.testUser2,
        col2.getId().toString(), ShareBusinessController.rolesAsList(ShareRoles.CREATE));
    Item item4 = createItemWithFile(col2, getThumbnailfile(), JenaUtil.testUser2);
    totalFileSize = totalFileSize + FileUtils.sizeOf(getThumbnailfile());;
    result = controller.getUsedStorageSizeForInstitute("imeji.org");
    assertEquals(totalFileSize, result);
  }

  private CollectionImeji createCollection(User user) throws ImejiException {
    CollectionController controller = new CollectionController();
    collection = ImejiFactory.newCollection("test", "Planck", "Max", "MPG");
    return controller.create(collection, profile, user, MetadataProfileCreationMethod.COPY, null);
  }

  private Item createItemWithFile(CollectionImeji col, File file, User user) throws ImejiException {
    return super.createItemWithFile(file, col, user);
  }
}
