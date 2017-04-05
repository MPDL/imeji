package de.mpg.imeji.testimpl.logic.service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
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
import de.mpg.imeji.logic.item.ItemService;
import de.mpg.imeji.logic.storage.StorageController;
import de.mpg.imeji.logic.storage.util.StorageUtils;
import de.mpg.imeji.logic.user.UserService;
import de.mpg.imeji.logic.user.UserService.USER_TYPE;
import de.mpg.imeji.logic.vo.CollectionImeji;
import de.mpg.imeji.logic.vo.Grant;
import de.mpg.imeji.logic.vo.Grant.GrantType;
import de.mpg.imeji.logic.vo.Item;
import de.mpg.imeji.logic.vo.User;
import de.mpg.imeji.logic.vo.factory.ImejiFactory;
import de.mpg.imeji.test.logic.service.SuperServiceTest;
import de.mpg.imeji.testimpl.ImejiTestResources;
import de.mpg.imeji.testimpl.logic.controller.ItemControllerTestClass;

public class CollectionServiceTest extends SuperServiceTest {
  private static final Logger LOGGER = Logger.getLogger(ItemControllerTestClass.class);

  private static User defaultUser;
  public static User userEditGrant;
  public static User userReadGrant;
  public static User userNoGrant;
  private static User restrictedUser;
  private static User sysadmin;

  private static CollectionImeji collectionPrivate;
  private static CollectionImeji collectionReleased;

  @BeforeClass
  public static void specificSetup() {
    UserService userService = new UserService();
    try {
      sysadmin =
          ImejiFactory.newUser().setEmail("admin3@test.org").setPerson("admin3", "admin3", "org")
              .setPassword("password").setQuota(Long.MAX_VALUE).build();
      defaultUser =
          ImejiFactory.newUser().setEmail("default@test.org").setPerson("default", "default", "org")
              .setPassword("password").setQuota(Long.MAX_VALUE).build();
      restrictedUser = ImejiFactory.newUser().setEmail("restricted@test.org")
          .setPerson("restricted", "restricted", "org").setPassword("password")
          .setQuota(Long.MAX_VALUE).build();
      userReadGrant =
          ImejiFactory.newUser().setEmail("read2@test.org").setPerson("read", "read", "org")
              .setPassword("password").setQuota(Long.MAX_VALUE).build();
      userEditGrant =
          ImejiFactory.newUser().setEmail("edit2@test.org").setPerson("edit", "edit", "org")
              .setPassword("password").setQuota(Long.MAX_VALUE).build();
      userNoGrant = ImejiFactory.newUser().setEmail("no2@test.org").setPerson("no", "no", "org")
          .setPassword("password").setQuota(Long.MAX_VALUE).build();

      userService.create(sysadmin, USER_TYPE.ADMIN);
      userService.create(defaultUser, USER_TYPE.DEFAULT);
      userService.create(userEditGrant, USER_TYPE.DEFAULT);
      userService.create(userReadGrant, USER_TYPE.DEFAULT);
      userService.create(userNoGrant, USER_TYPE.DEFAULT);
      userService.create(restrictedUser, USER_TYPE.RESTRICTED);

      CollectionService collectionService = new CollectionService();
      collectionPrivate = ImejiFactory.newCollection().setTitle("Private Collection")
          .setPerson("Max", "Planck", "MPDL").build();
      collectionReleased = ImejiFactory.newCollection().setTitle("Released Collection")
          .setPerson("Max", "Planck", "MPDL").build();

      collectionService.create(collectionPrivate, defaultUser);
      collectionService.create(collectionReleased, defaultUser);
      Item releasedItem = ImejiFactory.newItem(collectionReleased);
      (new ItemService()).create(releasedItem, collectionReleased, defaultUser);
      collectionService.releaseWithDefaultLicense(collectionReleased, defaultUser);

      userReadGrant.getGrants()
          .add(new Grant(GrantType.READ, collectionPrivate.getId().toString()).toGrantString());
      userEditGrant.getGrants()
          .add(new Grant(GrantType.EDIT, collectionPrivate.getId().toString()).toGrantString());

    } catch (ImejiException e) {
      e.printStackTrace();
    }
  }

  @Test
  public void create() {
    CollectionImeji testCreate =
        ImejiFactory.newCollection().setTitle("create").setPerson("p", "p", "p").build();
    create_Test("sysadmin", testCreate, sysadmin, null);
    create_Test("default user", testCreate, defaultUser, null);
    create_Test("restricted user", testCreate, restrictedUser, NotAllowedError.class);
  }

  private void create_Test(String msg, CollectionImeji col, User user, Class exception) {
    CollectionService service = new CollectionService();
    try {
      try {
        service.create(col, user);
      } catch (ImejiException e) {
        if (!e.getClass().equals(exception)) {
          Assert.fail(msg + ": " + e.getMessage());
        }
        return;
      }
      if (exception != null) {
        Assert.fail(msg + ": No exception has been thrown");
      }
      try {
        service.retrieve(Arrays.asList(col.getId().toString()), sysadmin); // Check if collection
                                                                           // has been created
        Assert.assertEquals(msg + ": User should have admin grant for collection", true,
            hasGrant(user.getGrants(), new Grant(GrantType.ADMIN, col.getId().toString())));
      } catch (ImejiException e) {
        Assert.fail(msg + ": " + e.getMessage());
      }
    } finally {
      try {
        service.delete(col, sysadmin);
      } catch (ImejiException e) {
      }
    }
  }

  @Test
  public void retrieveAndRetrieveLazy() {
    retrieve_Test("No grant user, private collection", collectionPrivate.getId(), userNoGrant,
        collectionPrivate.getIdString(), collectionPrivate.getTitle(), NotAllowedError.class);
    retrieve_Test("Read grant user, private collection", collectionPrivate.getId(), userReadGrant,
        collectionPrivate.getIdString(), collectionPrivate.getTitle(), null);
    retrieve_Test("No grant user, released collection", collectionReleased.getId(), userNoGrant,
        collectionReleased.getIdString(), collectionReleased.getTitle(), null);
  }

  private void retrieve_Test(String msg, URI uri, User user, String expectedIDString,
      String expectedName, Class exception) {
    CollectionService service = new CollectionService();
    CollectionImeji res = null, res2 = null;
    try {
      res = service.retrieve(uri, user);
      res2 = service.retrieveLazy(uri, user);
    } catch (ImejiException e) {
      if (e.getClass().equals(exception)) {
        return;
      }
      Assert.fail(msg + ": " + e.getMessage());
    }
    if (exception != null) {
      Assert.fail(msg + ": No exception has been thrown");
    }
    Assert.assertEquals(msg + ": ID should be equal", expectedIDString, res.getIdString());
    Assert.assertEquals(msg + ": Name should be equal", expectedName, res.getTitle());
    Assert.assertEquals(msg + " lazy: ID should be equal", expectedIDString, res2.getIdString());
    Assert.assertEquals(msg + " lazy: Name should be equal", expectedName, res2.getTitle());
  }

  @Test
  public void update() {
    collectionPrivate.setTitle("TestTitle");
    update_Test("private colection, user read grant", collectionPrivate, userReadGrant,
        "Private Collection", NotAllowedError.class);
    update_Test("private colection, user edit grant", collectionPrivate, userEditGrant,
        "Private Collection", null);
  }

  private void update_Test(String msg, CollectionImeji col, User user, String oldTitle,
      Class exception) {
    CollectionService service = new CollectionService();
    try {
      service.update(col, user);
    } catch (ImejiException e) {
      if (e.getClass().equals(exception)) {
        CollectionImeji res = null;
        try {
          res = service.retrieve(col.getId(), sysadmin);
        } catch (ImejiException e1) {
          Assert.fail(msg + ": " + e.getMessage());
        }
        Assert.assertEquals(msg + ": Title should not have changed", oldTitle, res.getTitle());
        return;
      }
      Assert.fail(msg + ": " + e.getMessage());
    }
    if (exception != null) {
      Assert.fail(msg + ": No exception has been thrown");
    }
    try {
      CollectionImeji res = service.retrieve(col.getId(), user);
      Assert.assertEquals(msg + ": Title should be updated", col.getTitle(), res.getTitle());
    } catch (ImejiException e) {
      Assert.fail(msg + ": " + e.getMessage());
    }
  }

  @Test
  public void updateLogo() {
    try {
      updateLogo_Test("collectionPrivate, edit grant user", collectionPrivate,
          ImejiTestResources.getTest1Jpg(), userEditGrant, null, null);

      updateLogo_Test("collectionPrivate, edit grant user, invalid logo", collectionPrivate,
          ImejiTestResources.getTestExe(), userEditGrant,
          StorageUtils.calculateChecksum(ImejiTestResources.getTest1Jpg()),
          UnprocessableError.class);


      updateLogo_Test("collectionPrivate, read grant user", collectionPrivate,
          ImejiTestResources.getTest2Jpg(), userEditGrant,
          StorageUtils.calculateChecksum(ImejiTestResources.getTest1Jpg()), NotAllowedError.class);


      // Check extra for removing logo
      try {
        (new CollectionService()).updateLogo(collectionPrivate, null, userEditGrant);
        if (collectionPrivate.getLogoUrl() != null) {
          Assert.fail("Remove collection: URL should be null");
        }
      } catch (IOException | URISyntaxException e) {
        Assert.fail(e.getMessage());
      }
    } catch (ImejiException e) {
      Assert.fail(e.getMessage());
    }
  }

  private void updateLogo_Test(String msg, CollectionImeji col, File file, User user,
      String previousChecksum, Class exception) {
    CollectionService service = new CollectionService();
    boolean exc = false;
    try {
      service.updateLogo(col, file, user);
    } catch (Exception e) {
      exc = true;
      if (!e.getClass().equals(exception)) {
        Assert.fail(msg + ": " + e.getMessage());
      }
    }
    if (exception != null && !exc) {
      Assert.fail(msg + ": No exception has been thrown");
    }
    try {
      // Just check if the checksum changed, don't compare it with the checksum of the uploaded
      // file, because we want to allow modification, e.g resizing
      Assert.assertEquals(
          msg + ": If there should be no exception, the checksum should have changed",
          getLogoChecksum(col).equals(previousChecksum), exception != null);
    } catch (IOException | ImejiException e) {
      Assert.fail(e.getMessage());
    }
  }

  @Test
  public void delete() {
    CollectionService collectionService = new CollectionService();
    ItemService itemService = new ItemService();
    CollectionImeji collectionToDelete =
        ImejiFactory.newCollection().setTitle("Delete").setPerson("p", "p", "o").build();
    try {
      collectionService.create(collectionToDelete, defaultUser);
      Item itemToDelete = ImejiFactory.newItem(collectionToDelete);
      itemService.create(itemToDelete, collectionToDelete, defaultUser);

      delete_Test("private Collection, edit grant user", collectionToDelete, userEditGrant,
          NotAllowedError.class);
      delete_Test("private Collection, collection admin user", collectionToDelete, defaultUser,
          null);
      delete_Test("released Collection, collection admin use", collectionReleased, defaultUser,
          UnprocessableError.class);
    } catch (ImejiException e) {
      Assert.fail(e.getMessage());
    }

  }

  private void delete_Test(String msg, CollectionImeji col, User user, Class exception) {
    CollectionService service = new CollectionService();
    ItemService itemService = new ItemService();
    URI collectionID = col.getId();
    Collection<URI> itemIDs = col.getImages();
    boolean exc = false;
    try {
      service.delete(col, user);
    } catch (ImejiException e) {
      exc = true;
      if (!e.getClass().equals(exception)) {
        Assert.fail(msg + ": " + e.getMessage());
      }
    }
    if (exception != null && !exc) {
      Assert.fail(msg + ": No exception has been thrown");
    }
    if (exception == null) {
      try {
        service.retrieve(collectionID, sysadmin);
        Assert.fail(msg + ": It was possible to retrive collection after delete");
      } catch (ImejiException e) {
        if (!(e instanceof NotFoundException)) {
          Assert.fail(e.getMessage());
        }
      }
      for (URI id : itemIDs) {
        try {
          itemService.retrieve(id, sysadmin);
          Assert.fail(msg + ": It was possible to retrive item after delete");
        } catch (ImejiException e) {
          if (!(e instanceof NotFoundException)) {
            Assert.fail(e.getMessage());
          }
        }
      }
    } else {
      try {
        service.retrieve(collectionID, sysadmin);
        for (URI id : itemIDs) {
          itemService.retrieve(id, sysadmin);
        }
      } catch (ImejiException e) {
        Assert.fail(e.getMessage());
      }
    }
  }



  /**
   * True if the {@link Grant} is found in the collection
   *
   * @param grants
   * @param grant
   * @return
   */
  private boolean hasGrant(Collection<String> grants, Grant grant) {
    return grants != null && grants.contains(grant.toGrantString());
  }

  private String getLogoChecksum(CollectionImeji c) throws IOException, ImejiException {
    StorageController sController = new StorageController();
    File storedFile = File.createTempFile("testFile", null);
    FileOutputStream fos = new FileOutputStream(storedFile);
    sController.read(c.getLogoUrl().toString(), fos, true);
    return StorageUtils.calculateChecksum(storedFile);
  }

}
