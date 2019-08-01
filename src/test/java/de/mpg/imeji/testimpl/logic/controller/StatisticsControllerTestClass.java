package de.mpg.imeji.testimpl.logic.controller;

import static org.junit.Assert.assertEquals;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.junit.Ignore;
import org.junit.Test;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.core.collection.CollectionService;
import de.mpg.imeji.logic.core.item.ItemService;
import de.mpg.imeji.logic.model.CollectionImeji;
import de.mpg.imeji.logic.model.Item;
import de.mpg.imeji.logic.model.User;
import de.mpg.imeji.logic.model.factory.ImejiFactory;
import de.mpg.imeji.logic.security.sharing.ShareService;
import de.mpg.imeji.logic.security.sharing.ShareService.ShareRoles;
import de.mpg.imeji.logic.statistic.StatisticsService;
import de.mpg.imeji.test.logic.service.SuperServiceTest;
import de.mpg.imeji.util.JenaUtil;

@Ignore
public class StatisticsControllerTestClass extends SuperServiceTest {

  @Test
  public void test() throws ImejiException {
    StatisticsService controller = new StatisticsService();
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
    new ShareService().shareToUser(JenaUtil.testUser, JenaUtil.testUser2, col2.getId().toString(), ShareRoles.ADMIN.name());
    Item item4 = createItemWithFile(col2, getThumbnailfile(), JenaUtil.testUser2);
    totalFileSize = totalFileSize + FileUtils.sizeOf(getThumbnailfile());;
    result = controller.getUsedStorageSizeForInstitute("imeji.org");
    assertEquals(totalFileSize, result);
  }

  private CollectionImeji createCollection(User user) throws ImejiException {
    CollectionService controller = new CollectionService();
    collectionBasic = ImejiFactory.newCollection().setTitle("test").setPerson("m", "p", "g").build();
    return controller.create(collectionBasic, user);
  }

  private Item createItemWithFile(CollectionImeji col, File file, User user) throws ImejiException {
    return super.createItemWithFile(file, col, user);
  }
}
