package de.mpg.imeji.testimpl.logic.service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.exceptions.NotAllowedError;
import de.mpg.imeji.exceptions.NotFoundException;
import de.mpg.imeji.exceptions.UnprocessableError;
import de.mpg.imeji.logic.content.ContentService;
import de.mpg.imeji.logic.item.ItemService;
import de.mpg.imeji.logic.storage.StorageController;
import de.mpg.imeji.logic.storage.util.StorageUtils;
import de.mpg.imeji.logic.vo.CollectionImeji;
import de.mpg.imeji.logic.vo.ContentVO;
import de.mpg.imeji.logic.vo.Grant;
import de.mpg.imeji.logic.vo.Grant.GrantType;
import de.mpg.imeji.logic.vo.Item;
import de.mpg.imeji.logic.vo.factory.ImejiFactory;
import de.mpg.imeji.test.logic.service.SuperServiceTest;
import de.mpg.imeji.testimpl.ImejiTestResources;
import de.mpg.imeji.testimpl.logic.controller.ItemControllerTestClass;
import util.JenaUtil;

public class ItemServiceTest extends SuperServiceTest {
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
  public void createWithFile_And_delete_Regular() {
    ItemService service = new ItemService();
    try {
      Item item2 = ImejiFactory.newItem(collection);
      File file = ImejiTestResources.getTest2Jpg();
      String checksum = StorageUtils.calculateChecksum(file);
      service.createWithFile(item2, file, file.getName(), collection, JenaUtil.testUser);
      Assert.assertEquals("Check collection", collection.getId(), item2.getCollection());
      Assert.assertEquals("Cheksum of the original", checksum, getOriginalChecksum(item2));
      service.delete(item2.getIdString(), JenaUtil.testUser);
      Assert.assertEquals("Item should be deleted", false,
          doesCollectionContainItemWithChecksum(service.retrieveAll(JenaUtil.testUser), checksum));
    } catch (ImejiException | IOException e) {
      Assert.fail(e.getMessage());
    }
  }

  @Test
  public void createWithFile_CollectionNull() {
    ItemService service = new ItemService();
    Item item2 = new Item();
    File file = ImejiTestResources.getTest2Jpg();
    try {
      service.createWithFile(item2, file, file.getName(), null, JenaUtil.testUser);
      Assert.fail("No exception has been thrown");
    } catch (UnprocessableError e) {
      // Thats correct, do nothing
    } catch (ImejiException e) {
      Assert.fail(e.getMessage());
    } finally {
      try {
        if (doesCollectionContainItemWithChecksum(service.retrieveAll(JenaUtil.adminTestUser),
            StorageUtils.calculateChecksum(ImejiTestResources.getTest2Jpg()))) {
          service.delete(item2.getIdString(), JenaUtil.adminTestUser);
          Assert.fail("Item was added");
        }
      } catch (ImejiException | IOException e) {
        Assert.fail(e.getMessage());
      }
    }
  }

  @Test
  public void createWithFile_CollectionNoId() {
    CollectionImeji collection2 = ImejiFactory.newCollection().setTitle("Test2").build();
    ItemService service = new ItemService();
    Item item2 = new Item();
    File file = ImejiTestResources.getTest2Jpg();
    try {
      service.createWithFile(item2, file, file.getName(), collection2, JenaUtil.testUser);
      Assert.fail("No exception has been thrown");
    } catch (UnprocessableError e) {
      // Thats correct, do nothing
    } catch (ImejiException e) {
      Assert.fail(e.getMessage());
    } finally {
      try {
        if (doesCollectionContainItemWithChecksum(service.retrieveAll(JenaUtil.adminTestUser),
            StorageUtils.calculateChecksum(ImejiTestResources.getTest2Jpg()))) {
          service.delete(item2.getIdString(), JenaUtil.adminTestUser);
          Assert.fail("Item was added");
        }
      } catch (ImejiException | IOException e) {
        Assert.fail(e.getMessage());
      }
    }
  }

  @Test
  public void createWithFile_CollectionInvalidId() {
    CollectionImeji collection2 = ImejiFactory.newCollection().setTitle("Test2").build();
    ItemService service = new ItemService();
    Item item2 = new Item();
    File file = ImejiTestResources.getTest2Jpg();
    try {
      collection2.setId(new URI("http://localhost:9998/imeji/collection/ThisIsWrong"));
      service.createWithFile(item2, file, file.getName(), collection2, JenaUtil.testUser);
      Assert.fail("No exception has been thrown");
    } catch (NotFoundException e) {
      // Thats correct, do nothing
    } catch (ImejiException | URISyntaxException e) {
      Assert.fail(e.getClass().getName());
    } finally {
      try {
        if (doesCollectionContainItemWithChecksum(service.retrieveAll(JenaUtil.adminTestUser),
            StorageUtils.calculateChecksum(ImejiTestResources.getTest2Jpg()))) {
          service.delete(item2.getIdString(), JenaUtil.adminTestUser);
          Assert.fail("Item was added");
        }
      } catch (ImejiException | IOException e) {
        Assert.fail(e.getMessage());
      }
    }
  }

  @Test
  public void createWithFile_UserUnauthorized() {
    ItemService service = new ItemService();
    Item item2 = new Item();
    File file = ImejiTestResources.getTest2Jpg();
    removeTestUser2Grant();
    try {
      service.createWithFile(item2, file, file.getName(), collection, JenaUtil.testUser2);
      Assert.fail("No exception has been thrown");
    } catch (NotAllowedError e) {
      // Thats correct, do nothing
    } catch (ImejiException e) {
      Assert.fail(e.getMessage());
    } finally {
      try {
        if (doesCollectionContainItemWithChecksum(service.retrieveAll(JenaUtil.adminTestUser),
            StorageUtils.calculateChecksum(ImejiTestResources.getTest2Jpg()))) {
          service.delete(item2.getIdString(), JenaUtil.adminTestUser);
          Assert.fail("Item was added");
        }
      } catch (ImejiException | IOException e) {
        Assert.fail(e.getMessage());
      }
    }
  }

  @Test
  public void createWithFile_ChecksumAlreadyExists() {
    ItemService service = new ItemService();
    try {
      Item item2 = new Item();
      File file = ImejiTestResources.getTestJpg();
      service.createWithFile(item2, file, file.getName(), collection, JenaUtil.testUser);
      Assert.fail("No exception has been thrown");
    } catch (UnprocessableError e) {
      // That's correct, do nothing
    } catch (ImejiException e) {
      Assert.fail(e.getMessage());
    } finally {
      try {
        Assert.assertEquals("There should be only one item in the colection", 1,
            service.retrieveAll(JenaUtil.adminTestUser).size());
      } catch (ImejiException e) {
        Assert.fail(e.getMessage());
      }
    }

  }


  // Tests retrieve, retrieveLazy and retrieveLazyForFile
  // Always check also for unauthorized User

  public void retrieve_Regular() {
    ItemService service = new ItemService();
    try {
      Item retrieved = service.retrieve(item.getId(), JenaUtil.testUser);
      Assert.assertEquals("Checksums should be equal", getOriginalChecksum(retrieved),
          getOriginalChecksum(item));
      retrieved = service.retrieveLazy(item.getId(), JenaUtil.testUser);
      Assert.assertEquals("Checksums should be equal", getOriginalChecksum(retrieved),
          getOriginalChecksum(item));
      ContentService contentService = new ContentService();
      ContentVO content =
          contentService.retrieve(contentService.findContentId(item.getId().toString()));
      retrieved = service.retrieveLazyForFile(content.getOriginal(), JenaUtil.testUser);
      Assert.assertEquals("Checksums should be equal", getOriginalChecksum(retrieved),
          getOriginalChecksum(item));
      removeTestUser2Grant();
      try {
        retrieved = service.retrieve(item.getId(), JenaUtil.testUser2);
        Assert.fail("No exception has been thrown");
      } catch (NotAllowedError e) {
      }
      try {
        retrieved = service.retrieveLazy(item.getId(), JenaUtil.testUser2);
        Assert.fail("No exception has been thrown");
      } catch (NotAllowedError e) {
      }
      try {
        retrieved = service.retrieveLazyForFile(content.getOriginal(), JenaUtil.testUser2);
        Assert.fail("No exception has been thrown");
      } catch (NotAllowedError e) {
      }
    } catch (ImejiException | IOException e) {
      Assert.fail(e.getMessage());
    }
  }


  @Test
  public void retrieveBatch_And_retrieveAll_Regular() {
    ItemService service = new ItemService();
    Item item2 = null;
    Item item3 = null;
    try {
      File[] files = {ImejiTestResources.getTestJpg(), ImejiTestResources.getTest2Jpg(),
          ImejiTestResources.getTest3Jpg()};
      String[] checksums = new String[3];
      for (int i = 0; i < 3; i++) {
        checksums[i] = StorageUtils.calculateChecksum(files[i]);
      }
      item2 = ImejiFactory.newItem(collection);
      item3 = ImejiFactory.newItem(collection);
      service.createWithFile(item2, files[1], files[1].getName(), collection, JenaUtil.testUser);
      service.createWithFile(item3, files[2], files[2].getName(), collection, JenaUtil.testUser);

      ArrayList<String> uris = new ArrayList<String>();
      uris.add(item.getId().toString());
      uris.add(item2.getId().toString());
      Collection<Item> retrieved = service.retrieveBatch(uris, 0, 0, JenaUtil.testUser);
      Assert.assertEquals("Retrieved List should have two elements", 2, retrieved.size());
      Assert.assertEquals("First Item should be in List", true,
          doesCollectionContainItemWithChecksum(retrieved, checksums[0]));
      Assert.assertEquals("Second Item should be in List", true,
          doesCollectionContainItemWithChecksum(retrieved, checksums[1]));

      retrieved = service.retrieveAll(JenaUtil.adminTestUser);
      Assert.assertEquals("Retrieved List should have three elements", 3, retrieved.size());
      Assert.assertEquals("First Item should be in List", true,
          doesCollectionContainItemWithChecksum(retrieved, checksums[0]));
      Assert.assertEquals("Second Item should be in List", true,
          doesCollectionContainItemWithChecksum(retrieved, checksums[1]));
      Assert.assertEquals("Third Item should be in List", true,
          doesCollectionContainItemWithChecksum(retrieved, checksums[2]));

    } catch (ImejiException | IOException e) {
      Assert.fail(e.getMessage());
    } finally {
      try {
        service.delete(item2.getIdString(), JenaUtil.testUser);
        service.delete(item3.getIdString(), JenaUtil.testUser);
      } catch (ImejiException e) {
        Assert.fail(e.getMessage());
      }
    }
  }

  // @Test
  public void updateFile_Regular() {
    ItemService service = new ItemService();
    File newFile = ImejiTestResources.getTest3Jpg();
    try {
      String checksum = StorageUtils.calculateChecksum(newFile);
      service.updateFile(item, collection, newFile, newFile.getName(), JenaUtil.testUser);
      Assert.assertEquals("Checksum should be equal", getOriginalChecksum(item), checksum);
    } catch (ImejiException | IOException e) {
      Assert.fail(e.getMessage());
    }
  }

  // //@Test
  public void deleteList_Regular() {

  }

  // //@Test
  public void delete_Regular() {

  }

  private String getOriginalChecksum(Item i) throws NotFoundException, ImejiException, IOException {
    ContentService contentService = new ContentService();
    ContentVO content = contentService.retrieve(contentService.findContentId(i.getId().toString()));
    StorageController sController = new StorageController();
    File storedFile = File.createTempFile("testFile", null);
    FileOutputStream fos = new FileOutputStream(storedFile);
    sController.read(content.getOriginal(), fos, true);
    return StorageUtils.calculateChecksum(storedFile);
  }

  private void removeTestUser2Grant() {
    JenaUtil.testUser2.getGrants()
        .remove(new Grant(GrantType.ADMIN, collection.getId().toString()).toGrantString());
  }

  private boolean doesCollectionContainItemWithChecksum(Collection<Item> collection,
      String checksum) throws NotFoundException, ImejiException, IOException {
    Object[] items = collection.toArray();
    for (Object i : items) {
      if (getOriginalChecksum((Item) i).equals(checksum)) {
        return true;
      }
    }
    return false;
  }


}
