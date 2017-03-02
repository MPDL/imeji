package de.mpg.imeji.testimpl.logic.service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.exceptions.NotAllowedError;
import de.mpg.imeji.exceptions.NotFoundException;
import de.mpg.imeji.exceptions.UnprocessableError;
import de.mpg.imeji.logic.collection.CollectionService;
import de.mpg.imeji.logic.content.ContentService;
import de.mpg.imeji.logic.item.ItemService;
import de.mpg.imeji.logic.storage.StorageController;
import de.mpg.imeji.logic.storage.util.StorageUtils;
import de.mpg.imeji.logic.user.UserService;
import de.mpg.imeji.logic.user.UserService.USER_TYPE;
import de.mpg.imeji.logic.vo.CollectionImeji;
import de.mpg.imeji.logic.vo.ContentVO;
import de.mpg.imeji.logic.vo.Grant;
import de.mpg.imeji.logic.vo.Grant.GrantType;
import de.mpg.imeji.logic.vo.Item;
import de.mpg.imeji.logic.vo.User;
import de.mpg.imeji.logic.vo.factory.ImejiFactory;
import de.mpg.imeji.test.logic.service.SuperServiceTest;
import de.mpg.imeji.testimpl.ImejiTestResources;
import de.mpg.imeji.testimpl.logic.controller.ItemControllerTestClass;
import util.JenaUtil;

public class ItemServiceTest extends SuperServiceTest {
  private static final Logger LOGGER = Logger.getLogger(ItemControllerTestClass.class);

  private static CollectionImeji collectionPrivate;
  private static CollectionImeji collectionReleased;
  private static CollectionImeji collectionWithdrawn;

  private static User userAdmin;
  private static User userReadGrant;
  private static User userEditGrant;
  private static User userNoGrant;

  @BeforeClass
  public static void specificSetup() {
    try {
      createCollection();
      createItemWithFile();
      init();
    } catch (ImejiException e) {
      LOGGER.error("Error initializing collection or item", e);
    }
  }

  private static void init() throws ImejiException {
    UserService userService = new UserService();
    userAdmin = ImejiFactory.newUser().setEmail("admin@test.org").setPerson("admin", "admin", "org")
        .setPassword("password").setQuota(Long.MAX_VALUE).build();
    userReadGrant = ImejiFactory.newUser().setEmail("read@test.org")
        .setPerson("read", "read", "org").setPassword("password").setQuota(Long.MAX_VALUE).build();
    userEditGrant = ImejiFactory.newUser().setEmail("edit@test.org")
        .setPerson("edit", "edit", "org").setPassword("password").setQuota(Long.MAX_VALUE).build();
    userNoGrant = ImejiFactory.newUser().setEmail("no@test.org").setPerson("no", "no", "org")
        .setPassword("password").setQuota(Long.MAX_VALUE).build();
    userService.create(userAdmin, USER_TYPE.ADMIN);
    userService.create(userReadGrant, USER_TYPE.DEFAULT);
    userService.create(userEditGrant, USER_TYPE.DEFAULT);
    userService.create(userNoGrant, USER_TYPE.DEFAULT);

    CollectionService collectionService = new CollectionService();
    collectionPrivate = ImejiFactory.newCollection().setTitle("Private Collection")
        .setPerson("Max", "Planck", "MPDL").build();
    collectionReleased = ImejiFactory.newCollection().setTitle("Private Collection")
        .setPerson("Max", "Planck", "MPDL").build();
    collectionWithdrawn = ImejiFactory.newCollection().setTitle("Private Collection")
        .setPerson("Max", "Planck", "MPDL").build();
    collectionService.create(collectionPrivate, userAdmin);
    collectionService.create(collectionReleased, userAdmin);
    collectionService.create(collectionWithdrawn, userAdmin);

    userReadGrant.getGrants()
        .add(new Grant(GrantType.READ, collectionPrivate.getId().toString()).toGrantString());
    userEditGrant.getGrants()
        .add(new Grant(GrantType.EDIT, collectionPrivate.getId().toString()).toGrantString());

    ItemService itemService = new ItemService();
    itemService.createWithFile(null, ImejiTestResources.getTestJpg(), "Test.jpg", collectionPrivate,
        userAdmin);
    itemService.createWithFile(null, ImejiTestResources.getTestJpg(), "Test.jpg",
        collectionReleased, userAdmin);
    itemService.createWithFile(null, ImejiTestResources.getTestJpg(), "Test.jpg",
        collectionWithdrawn, userAdmin);

    collectionService.releaseWithDefaultLicense(collectionReleased, userAdmin);
    collectionService.releaseWithDefaultLicense(collectionWithdrawn, userAdmin);
    collectionWithdrawn.setDiscardComment("Default discard comment");
    collectionService.withdraw(collectionWithdrawn, userAdmin);
  }

  @Test
  public void create() {
    try {
      createTest("Private collection, Edit user", ImejiFactory.newItem(collectionPrivate),
          collectionPrivate, userEditGrant, true, null);
      createTest("Private collection, Read user", ImejiFactory.newItem(collectionPrivate),
          collectionPrivate, userReadGrant, false, NotAllowedError.class);
      createTest("Private collection, No grant user", ImejiFactory.newItem(collectionPrivate),
          collectionPrivate, userNoGrant, false, NotAllowedError.class);
      createTest("Released collection, no grant user", ImejiFactory.newItem(collectionReleased),
          collectionReleased, userNoGrant, true, null);
    } catch (UnprocessableError e) {
      Assert.fail(e.getMessage());
    }
  }

  private void createTest(String msg, Item item, CollectionImeji coll, User user, boolean created,
      Class exception) {
    ItemService service = new ItemService();
    try {
      service.create(item, coll, user);
      if (!created) {
        Assert.fail(msg + ", No exception has been thrown");
      }
    } catch (Exception e) {
      if (!e.getClass().equals(exception)) {
        Assert.fail(msg + ", " + e.getMessage());
      }
    } finally {
      try {
        if (item.getId() == null) {
          Assert.assertEquals(msg + ", item id should only be null, when the item was not created",
              created, false);
        } else {
          Item result = service.retrieve(item.getId(), userAdmin);
          Assert.assertEquals(msg + ", item shound be in correct collection",
              coll.getId().toString(), result.getCollection().toString());

          service.delete(Arrays.asList(item), userAdmin);
        }
      } catch (ImejiException e) {
        if (!(e instanceof NotFoundException)) {
          Assert.fail(msg + ", " + e.getMessage());
        }
      }
    }
  }

  // @Test
  public void createWithFile_WithEditGrant() {
    ItemService service = new ItemService();
    try {
      Item item2 = ImejiFactory.newItem(collectionBasic);
      File file = ImejiTestResources.getTest2Jpg();
      String checksum = StorageUtils.calculateChecksum(file);
      service.createWithFile(item2, file, file.getName(), collectionBasic, JenaUtil.testUser);
      Assert.assertEquals("Check collection", collectionBasic.getId(), item2.getCollection());
      Assert.assertEquals("Cheksum of the original", checksum, getOriginalChecksum(item2));
      service.delete(item2.getIdString(), JenaUtil.testUser);
      Assert.assertEquals("Item should be deleted", false, doesCollectionContainItemWithChecksum(
          service.retrieveAll(JenaUtil.adminTestUser), checksum));
    } catch (ImejiException | IOException e) {
      Assert.fail(e.getMessage());
    }
  }


  // @Test
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

  // @Test
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

  // Test
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

  // @Test
  public void createWithFile_UserUnauthorized() {
    ItemService service = new ItemService();
    Item item2 = new Item();
    File file = ImejiTestResources.getTest2Jpg();
    removeTestUser2Grant();
    try {
      service.createWithFile(item2, file, file.getName(), collectionBasic, JenaUtil.testUser2);
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

  // @Test
  public void createWithFile_ChecksumAlreadyExists() {
    ItemService service = new ItemService();
    try {
      Item item2 = new Item();
      File file = ImejiTestResources.getTestJpg();
      service.createWithFile(item2, file, file.getName(), collectionBasic, JenaUtil.testUser);
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

  public void retrieve_WithReadGrant() {
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
    } catch (ImejiException | IOException e) {
      Assert.fail(e.getMessage());
    }
  }


  // @Test
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
      item2 = ImejiFactory.newItem(collectionBasic);
      item3 = ImejiFactory.newItem(collectionBasic);
      service.createWithFile(item2, files[1], files[1].getName(), collectionBasic,
          JenaUtil.testUser);
      service.createWithFile(item3, files[2], files[2].getName(), collectionBasic,
          JenaUtil.testUser);

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
        service.delete(item2.getIdString(), JenaUtil.adminTestUser);
        service.delete(item3.getIdString(), JenaUtil.adminTestUser);
      } catch (ImejiException e) {
        Assert.fail(e.getMessage());
      }
    }
  }

  /**
   * Check for: item created by User 1 in collection not visible to user 2 item created by user 2 in
   * collection not visible to user 2
   */
  // @Test
  public void retrieveBatch_MultipleUsers() {

  }

  // @Test
  public void updateFile_Regular() {
    ItemService service = new ItemService();
    File newFile = ImejiTestResources.getTest3Jpg();
    try {
      String checksum = StorageUtils.calculateChecksum(newFile);
      service.updateFile(item, collectionBasic, newFile, newFile.getName(), JenaUtil.testUser);
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
        .remove(new Grant(GrantType.ADMIN, collectionBasic.getId().toString()).toGrantString());
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
