package de.mpg.imeji.testimpl.logic.service;

import java.net.URI;
import java.util.Arrays;
import java.util.Collection;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.exceptions.NotAllowedError;
import de.mpg.imeji.logic.collection.CollectionService;
import de.mpg.imeji.logic.item.ItemService;
import de.mpg.imeji.logic.user.UserService;
import de.mpg.imeji.logic.user.UserService.USER_TYPE;
import de.mpg.imeji.logic.vo.CollectionImeji;
import de.mpg.imeji.logic.vo.Grant;
import de.mpg.imeji.logic.vo.Grant.GrantType;
import de.mpg.imeji.logic.vo.Item;
import de.mpg.imeji.logic.vo.User;
import de.mpg.imeji.logic.vo.factory.ImejiFactory;
import de.mpg.imeji.test.logic.service.SuperServiceTest;
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

  public void create_Test(String msg, CollectionImeji col, User user, Class exception) {
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

  public void retrieve_Test(String msg, URI uri, User user, String expectedIDString,
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

  public void update_Test(String msg, CollectionImeji col, User user, String oldTitle,
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

}
