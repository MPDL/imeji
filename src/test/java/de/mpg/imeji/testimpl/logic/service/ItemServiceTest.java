package de.mpg.imeji.testimpl.logic.service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import de.mpg.imeji.exceptions.AuthenticationError;
import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.exceptions.NotAllowedError;
import de.mpg.imeji.exceptions.NotFoundException;
import de.mpg.imeji.exceptions.UnprocessableError;
import de.mpg.imeji.exceptions.WorkflowException;
import de.mpg.imeji.logic.config.Imeji;
import de.mpg.imeji.logic.core.collection.CollectionService;
import de.mpg.imeji.logic.core.content.ContentService;
import de.mpg.imeji.logic.core.item.ItemService;
import de.mpg.imeji.logic.model.CollectionImeji;
import de.mpg.imeji.logic.model.ContentVO;
import de.mpg.imeji.logic.model.Grant;
import de.mpg.imeji.logic.model.Grant.GrantType;
import de.mpg.imeji.logic.model.ImejiLicenses;
import de.mpg.imeji.logic.model.Item;
import de.mpg.imeji.logic.model.License;
import de.mpg.imeji.logic.model.Properties.Status;
import de.mpg.imeji.logic.model.User;
import de.mpg.imeji.logic.model.factory.ImejiFactory;
import de.mpg.imeji.logic.security.user.UserService;
import de.mpg.imeji.logic.security.user.UserService.USER_TYPE;
import de.mpg.imeji.logic.storage.StorageController;
import de.mpg.imeji.logic.util.StorageUtils;
import de.mpg.imeji.logic.util.StringHelper;
import de.mpg.imeji.test.logic.service.SuperServiceTest;
import de.mpg.imeji.util.ImejiTestResources;

/**
 * Tests ItemService. Currently not tested: uploadToStaging, createFromStaging,
 * createWithExternalFile, create using external file and url, updateWithExternalFile,search (and
 * related),reindex
 * 
 * @author jandura
 *
 */
public class ItemServiceTest extends SuperServiceTest {
  private static final Logger LOGGER = Logger.getLogger(ItemServiceTest.class);

  private static CollectionImeji collectionPrivate;
  private static CollectionImeji collectionReleased;
  private static CollectionImeji collectionWithdrawn;

  private static User userAdmin;
  private static User userAdmin2;
  private static User userReadGrant;
  private static User userEditGrant;
  private static User userNoGrant;

  private static Item itemPrivate;
  private static Item itemReleased;
  private static Item itemWithdrawn;

  @BeforeClass
  public static void specificSetup() {
    try {
      init();
    } catch (ImejiException e) {
      LOGGER.error("Error initializing collection or item", e);
    }
  }

  private static void init() throws ImejiException {
    UserService userService = new UserService();
    userAdmin = ImejiFactory.newUser().setEmail("admin@test.org").setPerson("admin", "admin", "org")
        .setPassword("password").setQuota(Long.MAX_VALUE).build();
    userAdmin2 =
        ImejiFactory.newUser().setEmail("admin2@test.org").setPerson("admin2", "admin2", "org")
            .setPassword("password").setQuota(Long.MAX_VALUE).build();
    userReadGrant = ImejiFactory.newUser().setEmail("read@test.org")
        .setPerson("read", "read", "org").setPassword("password").setQuota(Long.MAX_VALUE).build();
    userEditGrant = ImejiFactory.newUser().setEmail("edit@test.org")
        .setPerson("edit", "edit", "org").setPassword("password").setQuota(Long.MAX_VALUE).build();
    userNoGrant = ImejiFactory.newUser().setEmail("no@test.org").setPerson("no", "no", "org")
        .setPassword("password").setQuota(Long.MAX_VALUE).build();
    userService.create(userAdmin, USER_TYPE.ADMIN);
    userService.create(userAdmin2, USER_TYPE.ADMIN);
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
    userService.update(userReadGrant, userAdmin);
    userService.update(userEditGrant, userAdmin);

    ItemService itemService = new ItemService();
    itemPrivate = ImejiFactory.newItem(collectionPrivate);
    itemService.createWithFile(itemPrivate, ImejiTestResources.getTestJpg(), "Test.jpg",
        collectionPrivate, userAdmin);
    itemReleased = ImejiFactory.newItem(collectionReleased);
    itemService.createWithFile(itemReleased, ImejiTestResources.getTestJpg(), "Test.jpg",
        collectionReleased, userAdmin);
    itemWithdrawn = ImejiFactory.newItem(collectionWithdrawn);
    itemService.createWithFile(itemWithdrawn, ImejiTestResources.getTestJpg(), "Test.jpg",
        collectionWithdrawn, userAdmin);

    collectionService.releaseWithDefaultLicense(collectionReleased, userAdmin);
    collectionService.releaseWithDefaultLicense(collectionWithdrawn, userAdmin);
    collectionWithdrawn.setDiscardComment("Default discard comment");
    collectionService.withdraw(collectionWithdrawn, userAdmin);
  }

  /**
   * Tests ItemService.create()
   */
  @Test
  public void create() {
    try {
      CollectionImeji collectionPrivate2 = ImejiFactory.newCollection().setTitle("Collection 2")
          .setPerson("Max", "planck", "mpdl").build();
      (new CollectionService()).create(collectionPrivate2, userAdmin);

      create_Test("Private collection, Edit user", ImejiFactory.newItem(collectionPrivate),
          collectionPrivate, userEditGrant, true, Status.PENDING, null);
      create_Test("Private collection, Admin user", ImejiFactory.newItem(collectionPrivate),
          collectionPrivate, userAdmin2, true, Status.PENDING, null);
      create_Test("Private collection, Read user", ImejiFactory.newItem(collectionPrivate),
          collectionPrivate, userReadGrant, false, Status.PENDING, NotAllowedError.class);
      create_Test("Private collection, No grant user", ImejiFactory.newItem(collectionPrivate),
          collectionPrivate, userNoGrant, false, Status.PENDING, NotAllowedError.class);
      create_Test("Released collection, no grant user", getDefaultReleasedItem(),
          collectionReleased, userNoGrant, false, Status.RELEASED, NotAllowedError.class);
      create_Test("Released collection, admin user", getDefaultReleasedItem(), collectionReleased,
          userAdmin, true, Status.RELEASED, null);
      create_Test("Different Collection, Edit user", ImejiFactory.newItem(collectionPrivate2),
          collectionPrivate, userEditGrant, true, Status.PENDING, null);
      create_Test("Not created Collection", ImejiFactory.newItem(collectionPrivate),
          ImejiFactory.newCollection().setPerson("m", "p", "d").build(), userEditGrant, false,
          Status.PENDING, UnprocessableError.class);
      CollectionImeji collectionWrongId =
          ImejiFactory.newCollection().setPerson("m", "p", "d").setUri("wrongId").build();
      userEditGrant.getGrants()
          .add(new Grant(GrantType.EDIT, collectionWrongId.getId().toString()).toGrantString());
      create_Test("Wrong id Collection", ImejiFactory.newItem(collectionPrivate), collectionWrongId,
          userEditGrant, false, Status.PENDING, NotFoundException.class);

      create_Test("Null Collection", ImejiFactory.newItem(collectionPrivate), null, userEditGrant,
          false, Status.PENDING, UnprocessableError.class);
      create_Test("user not logged in", ImejiFactory.newItem(collectionPrivate), collectionPrivate,
          null, false, Status.PENDING, AuthenticationError.class);
    } catch (ImejiException e) {
      Assert.fail(e.getMessage());
    }
  }

  /**
   * Create a item in the collection
   * 
   * @param msg
   * @param item
   * @param coll
   * @param user
   * @param created
   * @param itemStatus
   * @param exception
   */
  private void create_Test(String msg, Item item, CollectionImeji coll, User user, boolean created,
      Status itemStatus, Class exception) {
    ItemService service = new ItemService();
    try {
      service.create(item, coll, user);
      if (!created) {
        Assert.fail(msg + ", No exception has been thrown");
      }
      Item result = service.retrieve(item.getId(), userAdmin);
      Assert.assertEquals(msg + ", item shound be in correct collection", coll.getId().toString(),
          result.getCollection().toString());
      Assert.assertEquals("Status of the item should be correct", itemStatus.name(),
          result.getStatus().name());
    } catch (Exception e) {
      if (!e.getClass().equals(exception)) {
        e.printStackTrace();
        Assert.fail(msg + ", " + e.getMessage());
      }
    } finally {
      try {
        if (item.getId() == null) {
          Assert.assertEquals(msg + ", item id should only be null, when the item was not created",
              created, false);
        } else {
          Item result = service.retrieve(item.getId(), userAdmin);
          if (!created && result != null) {
            Assert.fail(msg
                + ", Something was retrieved, even though the item should not habe been created");
          }
          if (result.getStatus().equals(Status.PENDING)) {
            service.delete(Arrays.asList(item), userAdmin);
          }
        }
      } catch (ImejiException e) {
        if (!(e instanceof NotFoundException)) {
          Assert.fail(msg + ", " + e.getMessage());
        }
      }
    }
  }

  private Item getDefaultReleasedItem() {
    ArrayList<License> licenseList = new ArrayList<License>();
    licenseList.add(getDefaultLicense());
    return ImejiFactory.newItem().setCollection(collectionReleased.getIdString())
        .setLicenses(licenseList).build();
  }

  /**
   * Tests ItemService.createWithFile assumes that ItemService.createWithFile uses only
   * ItemService.create to create the item
   * 
   */
  @Test
  public void createWithFile() {
    try {
      createWithFile_Test("Normal", ImejiFactory.newItem(collectionPrivate),
          ImejiTestResources.getTest1Jpg(), "Test1.jpg", collectionPrivate, userEditGrant, true,
          "Test1.jpg", null);
      createWithFile_Test("Same file twice", ImejiFactory.newItem(collectionPrivate),
          ImejiTestResources.getTestJpg(), "Test.jpg", collectionPrivate, userEditGrant, false,
          "Test.jpg", UnprocessableError.class);
      createWithFile_Test("No filename", ImejiFactory.newItem(collectionPrivate),
          ImejiTestResources.getTest2Jpg(), "", collectionPrivate, userEditGrant, false,
          "Test2.jpg", UnprocessableError.class);
      createWithFile_Test("Nonexisting file", ImejiFactory.newItem(collectionPrivate),
          new File("nonexisting.jpg"), "Test2.jpg", collectionPrivate, userEditGrant, false,
          "Test2.jpg", UnprocessableError.class);
      createWithFile_Test(".exe file", ImejiFactory.newItem(collectionPrivate),
          ImejiTestResources.getTestExe(), "Test.exe", collectionPrivate, userEditGrant, false,
          "Test.exe", UnprocessableError.class);
    } catch (UnprocessableError e) {
      Assert.fail(e.getMessage());
    }
  }

  private void createWithFile_Test(String msg, Item i, File file, String filename,
      CollectionImeji coll, User user, boolean created, String expectedFilename, Class exception) {
    ItemService service = new ItemService();
    try {
      String checksumFile = StorageUtils.calculateChecksum(file);
      service.createWithFile(i, file, filename, coll, user);
      if (!created) {
        Assert.fail(msg + ", no exception has been thrown");
      }
      Item result = service.retrieve(i.getId(), userAdmin);
      String checksumResult = getOriginalChecksum(result);
      Assert.assertEquals(msg + ", checksum should be equal", checksumFile, checksumResult);
      Assert.assertEquals(msg + ": Filename should be the expected filename", expectedFilename,
          i.getFilename());

    } catch (Exception e) {
      if (!e.getClass().equals(exception)) {
        Assert.fail(msg + ", " + e.getMessage());
      }
    }
    try {
      if (i.getId() == null) {
        Assert.assertEquals(msg + ", item id should only be null, when the item was not created",
            created, false);
      } else {
        Item result = service.retrieve(i.getId(), userAdmin);
        if (!created && result != null) {
          Assert.fail(
              msg + ", Something was retrieved, even though the item should not have been created");
        }
        if (result.getStatus().equals(Status.PENDING)) {
          service.delete(Arrays.asList(i), userAdmin);
        }
      }
    } catch (ImejiException e) {
      if (!(e instanceof NotFoundException)) {
        Assert.fail(msg + ", " + e.getMessage());
      }
    }

  }



  /**
   * Get the instance default instance
   *
   * @return
   */
  private License getDefaultLicense() {
    final ImejiLicenses lic = StringHelper.isNullOrEmptyTrim(Imeji.CONFIG.getDefaultLicense())
        ? ImejiLicenses.CC0 : ImejiLicenses.valueOf(Imeji.CONFIG.getDefaultLicense());
    final License license = new License();
    license.setName(lic.name());
    license.setLabel(lic.getLabel());
    license.setUrl(lic.getUrl());
    return license;
  }


  /**
   * Test ItemService.retrive() and ItemService.retrieveLazy() and ItemService.retriveLazyForFile()
   */
  @Test
  public void retriveRetriveLazyRetriveLazyForFile() {
    try {
      retrieve_Test("readGrantUser, privateCollection", itemPrivate.getId(),
          getOriginalUrl(itemPrivate), userReadGrant, itemPrivate.getIdString(), null);
      retrieve_Test("NoGrantUser, privateCollection", itemPrivate.getId(),
          getOriginalUrl(itemPrivate), userNoGrant, itemPrivate.getIdString(),
          NotAllowedError.class);
      retrieve_Test("NoGrantUser, releasedCollection", itemReleased.getId(),
          getOriginalUrl(itemReleased), userNoGrant, itemReleased.getIdString(), null);
      retrieve_Test("readGrantUser, privateCollection, nonexistingId", URI.create("fakeUri"),
          "fake", userReadGrant, "fakeUri", NotFoundException.class);
    } catch (ImejiException e) {
      Assert.fail(e.getMessage());
    }
  }

  private void retrieve_Test(String msg, URI uri, String fileUrl, User user,
      String expectedIdString, Class exception) {
    ItemService service = new ItemService();

    // Retrive Normal
    try {
      Item item = service.retrieve(uri, user);
      Assert.assertEquals(msg + ", ids should be Equal", expectedIdString, item.getIdString());
      if (exception != null) {
        Assert.fail(msg + ", No exception has been thrown");
      }
    } catch (ImejiException e) {
      if (exception == null || !e.getClass().equals(exception)) {
        Assert.fail(msg + ", " + e.getClass().getName());
      }
    }

    // Retrive Lazy
    try {
      Item item = service.retrieveLazy(uri, user);
      Assert.assertEquals(msg + ", Lazy: ids should be Equal", expectedIdString,
          item.getIdString());
      if (exception != null) {
        Assert.fail(msg + ", Lazy: No exception has been thrown");
      }
    } catch (ImejiException e) {
      if (exception == null || !e.getClass().equals(exception)) {
        Assert.fail(msg + ", Lazy: " + e.getMessage());
      }
    }

    // Retrieve Lazy for file
    try {
      Item item = service.retrieveLazyForFile(fileUrl, user);
      Assert.assertEquals(msg + ", LazyForFile: ids should be Equal", expectedIdString,
          item.getIdString());
      if (exception != null) {
        Assert.fail(msg + ", LazyForFile: No exception has been thrown");
      }
    } catch (ImejiException e) {
      if (exception == null || !e.getClass().equals(exception)) {
        Assert.fail(msg + ", LazyForFile: " + e.getMessage());
      }
    }

  }


  /**
   * Test ItemSerivce.retriveBatch() and ItemService.retieveBatchLazy
   */
  @Test
  public void retrieveBatch() {
    ItemService service = new ItemService();
    try {
      Item itemPrivate2 = ImejiFactory.newItem(collectionPrivate);
      service.create(itemPrivate2, collectionPrivate, userAdmin);

      CollectionImeji collectionPrivate2 = ImejiFactory.newCollection().setTitle("Collection 2")
          .setPerson("Max", "planck", "mpdl").build();
      (new CollectionService()).create(collectionPrivate2, userAdmin);
      Item itemPrivate3 = ImejiFactory.newItem(collectionPrivate2);
      service.create(itemPrivate3, collectionPrivate2, userAdmin);

      retriveBatch_Test("User readGrant, private collection",
          new String[] {itemPrivate.getId().toString(), itemPrivate2.getId().toString()},
          userReadGrant, null);
      retriveBatch_Test("User noGrant, private collection",
          new String[] {itemPrivate.getId().toString(), itemPrivate2.getId().toString()},
          userNoGrant, NotAllowedError.class);
      retriveBatch_Test("User readGrant, mixed collection",
          new String[] {itemPrivate.getId().toString(), itemPrivate3.getId().toString()},
          userReadGrant, NotAllowedError.class);
      retriveBatch_Test("User Admin, mixed collection",
          new String[] {itemPrivate.getId().toString(), itemPrivate3.getId().toString()},
          userAdmin2, null);
      retriveBatch_Test("User NoGrant, releasedCollection",
          new String[] {itemReleased.getId().toString()}, userNoGrant, null);
      retriveBatch_Test("User NoGrant, releasedCollection mixed with private Collection",
          new String[] {itemReleased.getId().toString(), itemPrivate2.getId().toString()},
          userNoGrant, NotAllowedError.class);

      service.delete(itemPrivate2.getIdString(), userAdmin);
      (new CollectionService()).delete(collectionPrivate2, userAdmin);

    } catch (ImejiException e) {
      Assert.fail(e.getMessage());
    }
  }

  private void retriveBatch_Test(String msg, String[] uris, User user, Class exception) {
    ItemService service = new ItemService();

    // Retrive Normal
    try {
      Collection<Item> itemList = service.retrieveBatch(Arrays.asList(uris), 0, 0, user);
      if (exception != null) {
        Assert.fail(msg + ", No exception has been thrown");
      }
      Item[] items = itemList.toArray(new Item[itemList.size()]);
      Assert.assertEquals(msg + ", there should be 3 items", uris.length, items.length);
      for (int i = 0; i < uris.length; i++) {
        boolean found = false;
        for (int j = 0; j < items.length; j++) {
          if (items[j].getId().toString().equals(uris[i])) {
            found = true;
            break;
          }
        }
        if (!found) {
          Assert.fail(msg + ", uri " + uris[i] + " was not found");
        }
      }

    } catch (ImejiException e) {
      if (exception == null || !e.getClass().equals(exception)) {
        Assert.fail(msg + ", " + e.getMessage());
      }
    }

    // Retrive Lazy
    try {
      Collection<Item> itemList = service.retrieveBatchLazy(Arrays.asList(uris), 0, 0, user);
      if (exception != null) {
        Assert.fail(msg + ", Lazy: No exception has been thrown");
      }
      Item[] items = itemList.toArray(new Item[itemList.size()]);
      Assert.assertEquals(msg + ", there should be 3 items", uris.length, items.length);
      for (int i = 0; i < uris.length; i++) {
        boolean found = false;
        for (int j = 0; j < items.length; j++) {
          if (items[j].getId().toString().equals(uris[i])) {
            found = true;
            break;
          }
        }
        if (!found) {
          Assert.fail(msg + ", uri " + uris[i] + " was not found");
        }
      }

    } catch (ImejiException e) {
      if (exception == null || !e.getClass().equals(exception)) {
        Assert.fail(msg + ", Lazy: " + e.getMessage());
      }
    }
  }

  @Test
  public void retrieveAll() {
    retrieveAll_Test("admin", userAdmin, new String[] {itemPrivate.getId().toString(),
        itemReleased.getId().toString(), itemWithdrawn.getId().toString()}, null);
    retrieveAll_Test("read grant user", userReadGrant, new String[] {itemPrivate.getId().toString(),
        itemReleased.getId().toString(), itemWithdrawn.getId().toString()}, null);
  }

  private void retrieveAll_Test(String msg, User user, String[] uris, Class exception) {
    ItemService itemService = new ItemService();
    try {
      Collection<Item> itemList = itemService.retrieveAll(user);
      if (exception != null) {
        Assert.fail(msg + ", no exception has been thrown");
      }
      Item[] items = itemList.toArray(new Item[itemList.size()]);
      Assert.assertEquals(msg + ", there should be 3 items", uris.length, items.length);
      for (int i = 0; i < uris.length; i++) {
        boolean found = false;
        for (int j = 0; j < items.length; j++) {
          if (items[j].getId().toString().equals(uris[i])) {
            found = true;
            break;
          }
        }
        if (!found) {
          Assert.fail(msg + ", uri " + uris[i] + " was not found");
        }
      }
    } catch (ImejiException e) {
      if (exception == null || !e.getClass().equals(exception)) {
        Assert.fail(msg + ", " + e.getMessage());
      }
    }
  }


  @Test
  public void update() {
    itemPrivate.setFilename("testname.jpg");
    itemPrivate.setDiscardComment("test comment");
    itemReleased.setFilename("testname.jpg");
    update_Test("private collection, edit grant user", itemPrivate, userEditGrant, null);
    update_Test("private collection, read grant user", itemPrivate, userReadGrant,
        NotAllowedError.class);
    update_Test("released collection, no grant user", itemReleased, userNoGrant,
        NotAllowedError.class);

    URI oldId = itemPrivate.getId();
    try {
      itemPrivate.setId(new URI("fakeId"));
      update_Test("private collection, edit grant user, nonexisting id", itemPrivate, userEditGrant,
          NotFoundException.class);
    } catch (URISyntaxException e) {
      Assert.fail(e.getMessage());
    }
    itemPrivate.setId(oldId);
  }

  private void update_Test(String msg, Item item, User user, Class exception) {
    ItemService service = new ItemService();
    try {
      service.updateBatch(Arrays.asList(item), user);
      if (exception != null) {
        Assert.fail(msg + ", no exception has been thrown");
      }
      Item result = service.retrieve(item.getId(), userAdmin);
      // Test Filename and Discard comment as an example for a field of Item and a field of
      // Properties
      Assert.assertEquals(msg + ", filename should be equal", item.getFilename(),
          result.getFilename());
      Assert.assertEquals(msg + ", discardComment should be equal", item.getDiscardComment(),
          result.getDiscardComment());
    } catch (ImejiException e) {
      if (exception == null || !e.getClass().equals(exception)) {
        Assert.fail(msg + ", " + e.getMessage());
      }
    }
  }

  /**
   * Assumes that ItemService.updateFile() uses ItemService.update()
   */
  @Test
  public void updateFile() {
    try {
      (new ItemService()).createWithFile(itemPrivate, ImejiTestResources.getTest3Jpg(), "Test3.jpg",
          collectionPrivate, userAdmin);
      updateFile_Test("Normal", itemPrivate, ImejiTestResources.getTest4Jpg(), "Test4.jpg",
          userEditGrant, null);
      updateFile_Test("No Filename", itemPrivate, ImejiTestResources.getTest5Jpg(), "",
          userEditGrant, UnprocessableError.class);
      updateFile_Test("Nonexisting file", itemPrivate, new File("nonexisting.jpg"),
          "nonexisting.jpg", userEditGrant, UnprocessableError.class);
      updateFile_Test("exe file", itemPrivate, ImejiTestResources.getTestExe(), "test.exe",
          userEditGrant, UnprocessableError.class);
      Item item2 = ImejiFactory.newItem(collectionPrivate);
      (new ItemService()).createWithFile(item2, ImejiTestResources.getTest5Jpg(), "test5.jpg",
          collectionPrivate, userAdmin);
      updateFile_Test("File already existst", item2, ImejiTestResources.getTest4Jpg(), "Test4.jpg",
          userEditGrant, UnprocessableError.class);
    } catch (ImejiException e) {
      Assert.fail(e.getMessage());
    }

  }

  private void updateFile_Test(String msg, Item item, File file, String filename, User user,
      Class exception) {
    ItemService service = new ItemService();
    try {
      String checksum = StorageUtils.calculateChecksum(file);
      CollectionImeji col = (new CollectionService()).retrieve(item.getCollection(), userAdmin);
      service.updateFile(item, col, file, filename, user);
      if (exception != null) {
        Assert.fail(msg + ", no exception has been thrown");
      }
      Item result = service.retrieve(item.getId(), userAdmin);
      Assert.assertEquals(msg + ", checksum should be equal", getOriginalChecksum(result),
          checksum);
    } catch (ImejiException | IOException e) {
      if (exception == null || !e.getClass().equals(exception)) {
        Assert.fail(msg + ", " + e.getMessage());
      }
    }

  }


  @Test
  public void delete() {
    try {
      Item item = ImejiFactory.newItem(collectionPrivate);
      (new ItemService()).createWithFile(item, ImejiTestResources.getTest2Jpg(), "Test2.jpg",
          collectionPrivate, userAdmin);
      delete_Test("private collection, no grant user", item, userNoGrant, NotAllowedError.class);
      delete_Test("released collection, admin user", itemReleased, userAdmin,
          WorkflowException.class);
      delete_Test("private collection, edit user", item, userEditGrant, null);
    } catch (ImejiException e) {
      Assert.fail(e.getMessage());
    }

  }

  private void delete_Test(String msg, Item item, User user, Class exception) {
    ItemService service = new ItemService();
    try {
      File file = null;
      try {
        String url = getOriginalUrl(item);
        String path = url.replace("http://localhost:9998/imeji/file/", "C:/data/imeji_test/files/");
        file = new File(path);
      } catch (ImejiException e) {

      }
      service.delete(item.getIdString(), user);
      if (exception != null) {
        Assert.fail(msg + ", no exception has been thrown");
      }
      try {
        service.retrieve(item.getId(), user);
        Assert.fail(msg + ", retrieve did not fail");
      } catch (NotFoundException e) {
        // That is correct
      }
      if (file != null) {
        Assert.assertEquals(msg + ": File should not exist after delete", false, file.exists());
      }
    } catch (ImejiException e) {
      if (exception == null || !e.getClass().equals(exception)) {
        Assert.fail(msg + ", " + e.getMessage());
      }
    }
  }

  @Test
  public void release() {
    try {
      ItemService service = new ItemService();
      Item itemToRelease = ImejiFactory.newItem(collectionPrivate);
      service.create(itemToRelease, collectionPrivate, userAdmin);
      Item itemToRelease2 = ImejiFactory.newItem(collectionPrivate);
      service.create(itemToRelease2, collectionPrivate, userAdmin);


      release_Test("Read grant user, not yet released item, no licence",
          Arrays.asList(itemToRelease), userReadGrant, getDefaultLicense(), getDefaultLicense(),
          NotAllowedError.class);

      /*
       * release_Test("Edit grant user, not yet released item, no licence",
       * Arrays.asList(itemToRelease), userEditGrant, getDefaultLicense(), getDefaultLicense(),
       * NotAllowedError.class);
       */


      release_Test("Admin grant user, not yet released item, no licence",
          Arrays.asList(itemToRelease, itemToRelease2), userAdmin, getDefaultLicense(),
          getDefaultLicense(), null);
      service.withdraw(Arrays.asList(itemToRelease), "standart comment", userAdmin);
      service.withdraw(Arrays.asList(itemToRelease2), "standart comment", userAdmin);

      itemToRelease = ImejiFactory.newItem(collectionPrivate);
      License lic = getDefaultLicense();
      lic.setName("Different License");
      itemToRelease.getLicenses().add(lic);
      service.create(itemToRelease, collectionPrivate, userAdmin);
      release_Test("Edit grant user, not yet released item, item already has license",
          Arrays.asList(itemToRelease), userEditGrant, getDefaultLicense(), lic, null);
      service.withdraw(Arrays.asList(itemToRelease), "standart comment", userAdmin);



    } catch (ImejiException e) {
      Assert.fail(e.getMessage());
    }
  }

  private void release_Test(String msg, List<Item> items, User user, License defaultLicense,
      License expectedLicense, Class exception) {
    ItemService service = new ItemService();
    try {
      service.release(items, user, defaultLicense);
      if (exception != null) {
        Assert.fail(msg + ": No exception has been thrown");
      }
    } catch (Exception e) {
      if (!e.getClass().equals(exception)) {
        Assert.fail(msg + ": " + e.getMessage());
      }
    }
    for (Item i : items) {
      Assert.assertEquals(msg + ": Status should be released", i.getStatus(), Status.RELEASED);
      boolean itemHasLicense = false;
      for (License l : i.getLicenses()) {
        if (l.getName().equals(expectedLicense.getName())) {
          itemHasLicense = true;
          break;
        }
      }
      Assert.assertEquals(msg + ": Item should have correct license", true, itemHasLicense);
    }
  }

  @Test
  public void withdraw() {
    ItemService service = new ItemService();
    try {
      Item withdrawReleased = ImejiFactory.newItem(collectionPrivate);
      service.createWithFile(withdrawReleased, ImejiTestResources.getTest2Jpg(), "Test2.jpg",
          collectionPrivate, userAdmin);
      service.releaseWithDefaultLicense(Arrays.asList(withdrawReleased), userAdmin);

      withdraw_Test("released item, edit user", withdrawReleased, userEditGrant, null);

      withdrawReleased = ImejiFactory.newItem(collectionPrivate);
      service.createWithFile(withdrawReleased, ImejiTestResources.getTest7Jpg(), "Test7.jpg",
          collectionPrivate, userAdmin);
      service.releaseWithDefaultLicense(Arrays.asList(withdrawReleased), userAdmin);
      withdraw_Test("released item, admin user", withdrawReleased, userAdmin, null);
    } catch (ImejiException e) {
      Assert.fail(e.getMessage());
    }
  }

  private void withdraw_Test(String msg, Item i, User user, Class exception) {
    ItemService service = new ItemService();
    try {
      File file = null;
      try {
        String url = getOriginalUrl(i);
        String path = url.replace("http://localhost:9998/imeji/file/", "C:/data/imeji_test/files/");
        file = new File(path);
      } catch (ImejiException e) {

      }
      service.withdraw(Arrays.asList(i), "default comment", user);
      if (exception != null) {
        Assert.fail(msg + ", no exception has been thrown");
      }
      Assert.assertEquals(msg + ": Status should be Withdrawn", Status.WITHDRAWN, i.getStatus());
      if (file != null) {
        Assert.assertEquals(msg + ": File should not exist after delete", false, file.exists());
      }
    } catch (ImejiException e) {
      if (exception == null || !e.getClass().equals(exception)) {
        Assert.fail(msg + ", " + e.getMessage());
      }
    } finally {
      try {
        service.withdraw(Arrays.asList(i), "default comment", userAdmin);
      } catch (ImejiException e) {
      }
      try {
        service.delete(Arrays.asList(i), userAdmin);
      } catch (ImejiException e) {
      }
    }
  }

  @Test
  public void moveItems() {
    CollectionImeji col1 =
        ImejiFactory.newCollection().setTitle("Col1").setPerson("m", "p", "g").build();
    CollectionImeji col2 =
        ImejiFactory.newCollection().setTitle("Col2").setPerson("m", "p", "g").build();
    try {
      User user =
          ImejiFactory.newUser().setEmail("extra@test.org").setPerson("extra", "extra", "org")
              .setPassword("password").setQuota(Long.MAX_VALUE).build();
      Item itemToMove = ImejiFactory.newItem(col1);
      (new CollectionService()).create(col1, userAdmin);
      (new CollectionService()).create(col2, userAdmin);
      (new UserService()).create(user, USER_TYPE.DEFAULT);
      (new ItemService()).createWithFile(itemToMove, ImejiTestResources.getTest1Jpg(), "Test1.jpg",
          col1, userAdmin);
      Thread.sleep(50);// Otherwise the item won't be completly created when we move it

      user.getGrants().add(new Grant(GrantType.EDIT, col1.getId().toString()).toGrantString());
      move_Test("only edit grant on col 1", itemToMove, col2, user, NotAllowedError.class);
      user.setGrants(new ArrayList<String>());
      user.getGrants().add(new Grant(GrantType.EDIT, col2.getId().toString()).toGrantString());
      move_Test("only edit grant on col 2", itemToMove, col2, user, NotAllowedError.class);
      user.getGrants().add(new Grant(GrantType.EDIT, col1.getId().toString()).toGrantString());
      move_Test("normal", itemToMove, col2, user, null);
      (new CollectionService()).release(col2, userAdmin, getDefaultLicense());

      // Check, that moving back is not possible because item is released now, seperaly
      ItemService service = new ItemService();
      service.moveItems(Arrays.asList(itemToMove), col1, userAdmin, getDefaultLicense());
      // Item should still be in old collection
      Item ret = service.retrieve(itemToMove.getId().toString(), userAdmin);
      Assert.assertEquals("Should be in old collection", ret.getCollection().toString(),
          col2.getId().toString());

    } catch (ImejiException | InterruptedException e) {
      Assert.fail(e.getMessage());
    }


  }

  private void move_Test(String msg, Item item, CollectionImeji col, User user, Class exception) {
    ItemService service = new ItemService();
    try {
      service.moveItems(Arrays.asList(item), col, user, getDefaultLicense());
      if (exception != null) {
        Assert.fail(msg + ": No exception was thrown");
      }
    } catch (ImejiException e) {
      if (!e.getClass().equals(exception)) {
        Assert.fail(msg + ": " + e.getMessage());
      }
    }
    try {
      Item ret = service.retrieve(item.getId().toString(), userAdmin);
      if (exception == null) {
        Assert.assertEquals(msg + ": Item should be in new collection", col.getId().toString(),
            ret.getCollection().toString());
      } else {
        Assert.assertEquals(msg + ": Item should be in old collection",
            item.getCollection().toString(), ret.getCollection().toString());
      }
    } catch (ImejiException e) {
      Assert.fail(e.getMessage());
    }
  }


  private String getOriginalChecksum(Item i) throws NotFoundException, ImejiException, IOException {
    StorageController sController = new StorageController();
    File storedFile = File.createTempFile("testFile", null);
    FileOutputStream fos = new FileOutputStream(storedFile);
    sController.read(getOriginalUrl(i), fos, true);
    return StorageUtils.calculateChecksum(storedFile);
  }

  private String getOriginalUrl(Item i) throws ImejiException {
    ContentService contentService = new ContentService();
    ContentVO content = contentService.retrieve(contentService.findContentId(i.getId().toString()));
    return content.getOriginal();
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
