package de.mpg.imeji.testimpl.logic.service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.exceptions.NotFoundException;
import de.mpg.imeji.logic.config.Imeji;
import de.mpg.imeji.logic.core.collection.CollectionService;
import de.mpg.imeji.logic.core.content.ContentService;
import de.mpg.imeji.logic.core.item.ItemService;
import de.mpg.imeji.logic.model.CollectionImeji;
import de.mpg.imeji.logic.model.ContentVO;
import de.mpg.imeji.logic.model.Item;
import de.mpg.imeji.logic.model.User;
import de.mpg.imeji.logic.model.factory.ImejiFactory;
import de.mpg.imeji.logic.security.user.UserService;
import de.mpg.imeji.logic.security.user.UserService.USER_TYPE;
import de.mpg.imeji.logic.storage.StorageController;
import de.mpg.imeji.logic.util.StorageUtils;
import de.mpg.imeji.logic.util.TempFileUtil;
import de.mpg.imeji.test.logic.service.SuperServiceTest;
import de.mpg.imeji.util.ConcurrencyUtil;
import de.mpg.imeji.util.ImejiTestResources;

/**
 * Tests ContentService. Currently not tested: create ContentVO for external file, update (b/c it
 * already gets tested in ItemServiceTest.updateFile)
 * 
 * @author jandura
 *
 */
public class ContentServiceTest extends SuperServiceTest {
  private static final Logger LOGGER = LogManager.getLogger(ContentServiceTest.class);

  private static Item item;
  private static CollectionImeji collection;
  private static User adminUser;
  private static User defaultUser;
  private static ContentVO content;

  @BeforeClass
  public static void specificSetup() {
    try {
      UserService userService = new UserService();
      adminUser = ImejiFactory.newUser().setEmail("admin3@test.org").setPerson("admin3", "admin3", "org").setPassword("password")
          .setQuota(Long.MAX_VALUE).build();
      defaultUser = ImejiFactory.newUser().setEmail("default@test.org").setPerson("default", "default", "org").setPassword("password")
          .setQuota(Long.MAX_VALUE).build();
      userService.create(adminUser, USER_TYPE.ADMIN);
      userService.create(defaultUser, USER_TYPE.DEFAULT);
      collection = ImejiFactory.newCollection().setTitle("Test Collection").setPerson("m", "p", "mpdl").build();
      new CollectionService().create(collection, defaultUser);
      defaultUser = new UserService().retrieve(defaultUser.getId(), adminUser);
      item = ImejiFactory.newItem(collection);
      new ItemService().create(item, collection, defaultUser);
      content = new ContentService().create(item, ImejiTestResources.getTest1Jpg(), defaultUser);

      //Wait for content being completely created -> ContentService().delete() leads to errors otherwise
      ConcurrencyUtil.waitForThreadsToComplete(Imeji.getCONTENT_EXTRACTION_EXECUTOR(), Imeji.getINTERNAL_STORAGE_EXECUTOR());
    } catch (ImejiException e) {
      LOGGER.error("Exception in setup of ContentServiceTest", e);
    }
  }

  @Test
  public void create() {
    // Just check the the content got created correctly in setup
    // Conditions like nonexisting file, ... have already been tested in
    // ItemService.create
    Assert.assertEquals("Item id should match id of item stored in content", item.getId().toString(), content.getItemId());
    try {
      String expectedChecksum = StorageUtils.calculateChecksum(ImejiTestResources.getTest1Jpg());
      Assert.assertEquals("Checksum of original should be as expected", expectedChecksum, getChecksum(content.getOriginal()));
      Assert.assertEquals("Checksum saved in content should be as expected", expectedChecksum, content.getChecksum());
    } catch (ImejiException | IOException e) {
      Assert.fail(e.getMessage());
    }
  }

  @Test
  public void retrieve() {
    try {
      ContentVO contentRetrive = (new ContentService()).retrieve(content.getId().toString());
      Assert.assertEquals("ID should be as expected", content.getId().toString(), contentRetrive.getId().toString());
      Assert.assertEquals("URL of original should be as expected", content.getOriginal(), contentRetrive.getOriginal());
    } catch (ImejiException e) {
      Assert.fail(e.getMessage());
    }
  }

  @Test
  public void retrieveLazy() {
    try {
      ContentVO contentRetrive = (new ContentService()).retrieveLazy(content.getId().toString());
      Assert.assertEquals("ID should be as exoected", content.getId().toString(), contentRetrive.getId().toString());
      Assert.assertEquals("URL of original should be as expected", content.getOriginal(), contentRetrive.getOriginal());
    } catch (ImejiException e) {
      Assert.fail(e.getMessage());
    }
  }

  @Test
  public void retrieveBatch() {
    try {
      ContentVO contentRetrive = (new ContentService()).retrieveBatch(Arrays.asList(content.getId().toString())).get(0);
      Assert.assertEquals("ID should be as exoected", content.getId().toString(), contentRetrive.getId().toString());
      Assert.assertEquals("URL of original should be as expected", content.getOriginal(), contentRetrive.getOriginal());
    } catch (ImejiException e) {
      Assert.fail(e.getMessage());
    }
  }

  @Test
  public void retrieveBatchLazy() {
    try {
      ContentVO contentRetrive = (new ContentService()).retrieveBatchLazy(Arrays.asList(content.getId().toString())).get(0);
      Assert.assertEquals("ID should be as exoected", content.getId().toString(), contentRetrive.getId().toString());
      Assert.assertEquals("URL of original should be as expected", content.getOriginal(), contentRetrive.getOriginal());
    } catch (ImejiException e) {
      Assert.fail(e.getMessage());
    }
  }

  @Test
  public void findContentId() {
    Assert.assertEquals("Found id should be content id", content.getId().toString(),
        (new ContentService()).findContentId(item.getId().toString()));
  }

  // ContentService.update already get's tested in ItemServiceTest.updateWithFile

  @Test
  public void delete() {
    ContentService service = new ContentService();
    try {
      service.delete(content.getId().toString()); // Should not be possible to retrieve content now
      try {
        service.retrieve(content.getId().toString());
        Assert.fail("Content did not get deleted");
      } catch (NotFoundException e) {
        // That is correct
      }
      // Restore content
      content = (new ContentService()).create(item, ImejiTestResources.getTest1Jpg(), defaultUser);

      //Wait for content being completely created -> ContentService().delete() leads to errors otherwise
      ConcurrencyUtil.waitForThreadsToComplete(Imeji.getCONTENT_EXTRACTION_EXECUTOR(), Imeji.getINTERNAL_STORAGE_EXECUTOR());

      content = service.retrieve(content.getId().toString());
    } catch (ImejiException e) {
      Assert.fail(e.getMessage());
    }

  }

  private String getChecksum(String url) throws NotFoundException, ImejiException, IOException {
    StorageController sController = new StorageController();
    File storedFile = File.createTempFile("testFile", null, new File(TempFileUtil.getOrCreateTempDirectory().getCanonicalPath()));
    FileOutputStream fos = new FileOutputStream(storedFile);
    sController.read(url, fos, true);
    return StorageUtils.calculateChecksum(storedFile);
  }

}
