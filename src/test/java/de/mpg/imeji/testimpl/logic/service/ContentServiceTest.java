package de.mpg.imeji.testimpl.logic.service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.exceptions.NotFoundException;
import de.mpg.imeji.logic.collection.CollectionService;
import de.mpg.imeji.logic.content.ContentService;
import de.mpg.imeji.logic.item.ItemService;
import de.mpg.imeji.logic.storage.StorageController;
import de.mpg.imeji.logic.storage.util.StorageUtils;
import de.mpg.imeji.logic.user.UserService;
import de.mpg.imeji.logic.user.UserService.USER_TYPE;
import de.mpg.imeji.logic.vo.CollectionImeji;
import de.mpg.imeji.logic.vo.ContentVO;
import de.mpg.imeji.logic.vo.Item;
import de.mpg.imeji.logic.vo.User;
import de.mpg.imeji.logic.vo.factory.ImejiFactory;
import de.mpg.imeji.test.logic.service.SuperServiceTest;
import de.mpg.imeji.util.ImejiTestResources;

/**
 * Tests ContentService. Currently not tested: create ContentVO for external file, update (b/c it
 * already gets tested in ItemServiceTest.updateFile)
 * 
 * @author jandura
 *
 */
public class ContentServiceTest extends SuperServiceTest {
  private static final Logger LOGGER = Logger.getLogger(ContentServiceTest.class);

  private static Item item;
  private static CollectionImeji collection;
  private static User defaultUser;
  private static ContentVO content;

  @BeforeClass
  public static void specificSetup() {
    try {
      defaultUser =
          ImejiFactory.newUser().setEmail("default@test.org").setPerson("default", "default", "org")
              .setPassword("password").setQuota(Long.MAX_VALUE).build();
      (new UserService()).create(defaultUser, USER_TYPE.DEFAULT);
      collection = ImejiFactory.newCollection().setTitle("Test Collection")
          .setPerson("m", "p", "mpdl").build();
      (new CollectionService()).create(collection, defaultUser);
      item = ImejiFactory.newItem(collection);
      (new ItemService()).create(item, collection, defaultUser);
      content = (new ContentService()).create(item, ImejiTestResources.getTest1Jpg(), defaultUser);
      int a = 5;
      a++;
    } catch (ImejiException e) {
      LOGGER.error("Exception in setup of ContentServiceTest", e);
    }
  }


  @Test
  public void create() {
    // Just check the the content got created correctly in setup
    // Conditions like nonexisting file, ... have already been tested in ItemService.create
    Assert.assertEquals("Item id should match id of item stored in content",
        item.getId().toString(), content.getItemId());
    try {
      String expectedChecksum = StorageUtils.calculateChecksum(ImejiTestResources.getTest1Jpg());
      Assert.assertEquals("Checksum of original should be as expected", expectedChecksum,
          getChecksum(content.getOriginal()));
      Assert.assertEquals("Checksum saved in content should be as expected", expectedChecksum,
          content.getChecksum());
    } catch (ImejiException | IOException e) {
      Assert.fail(e.getMessage());
    }
  }

  @Test
  public void retrieve() {
    try {
      ContentVO contentRetrive = (new ContentService()).retrieve(content.getId().toString());
      Assert.assertEquals("ID should be as expected", content.getId().toString(),
          contentRetrive.getId().toString());
      Assert.assertEquals("URL of original should be as expected", content.getOriginal(),
          contentRetrive.getOriginal());
    } catch (ImejiException e) {
      Assert.fail(e.getMessage());
    }
  }

  @Test
  public void retrieveLazy() {
    try {
      ContentVO contentRetrive = (new ContentService()).retrieveLazy(content.getId().toString());
      Assert.assertEquals("ID should be as exoected", content.getId().toString(),
          contentRetrive.getId().toString());
      Assert.assertEquals("URL of original should be as expected", content.getOriginal(),
          contentRetrive.getOriginal());
    } catch (ImejiException e) {
      Assert.fail(e.getMessage());
    }
  }

  @Test
  public void retrieveBatch() {
    try {
      ContentVO contentRetrive =
          (new ContentService()).retrieveBatch(Arrays.asList(content.getId().toString())).get(0);
      Assert.assertEquals("ID should be as exoected", content.getId().toString(),
          contentRetrive.getId().toString());
      Assert.assertEquals("URL of original should be as expected", content.getOriginal(),
          contentRetrive.getOriginal());
    } catch (ImejiException e) {
      Assert.fail(e.getMessage());
    }
  }

  @Test
  public void retrieveBatchLazy() {
    try {
      ContentVO contentRetrive = (new ContentService())
          .retrieveBatchLazy(Arrays.asList(content.getId().toString())).get(0);
      Assert.assertEquals("ID should be as exoected", content.getId().toString(),
          contentRetrive.getId().toString());
      Assert.assertEquals("URL of original should be as expected", content.getOriginal(),
          contentRetrive.getOriginal());
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
      Thread.sleep(50); // Wait for the content to be processed
      content = service.retrieve(content.getId().toString());
    } catch (ImejiException | InterruptedException e) {
      Assert.fail(e.getMessage());
    }

  }

  @Test
  public void move() {
    ContentService service = new ContentService();
    CollectionImeji collection2 =
        ImejiFactory.newCollection().setTitle("Collection 2").setPerson("m", "p", "mpdl").build();
    try {
      (new CollectionService()).create(collection2, defaultUser);
      service.move(Arrays.asList(item), Arrays.asList(content), collection2.getIdString());
      Assert.assertEquals("New full url should be correct",
          content.getFull().replaceAll(collection.getIdString(), collection2.getIdString()),
          content.getFull());
      // Move back
      service.move(Arrays.asList(item), Arrays.asList(content), collection.getIdString());
    } catch (ImejiException e) {
      Assert.fail(e.getMessage());
    }
  }

  private String getChecksum(String url) throws NotFoundException, ImejiException, IOException {
    StorageController sController = new StorageController();
    File storedFile = File.createTempFile("testFile", null);
    FileOutputStream fos = new FileOutputStream(storedFile);
    sController.read(url, fos, true);
    return StorageUtils.calculateChecksum(storedFile);
  }

}
