package de.mpg.imeji.testimpl.logic.service;

import java.util.Arrays;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.core.collection.CollectionService;
import de.mpg.imeji.logic.core.item.ItemService;
import de.mpg.imeji.logic.core.statement.StatementService;
import de.mpg.imeji.logic.model.CollectionImeji;
import de.mpg.imeji.logic.model.Item;
import de.mpg.imeji.logic.model.Metadata;
import de.mpg.imeji.logic.model.Statement;
import de.mpg.imeji.logic.model.StatementType;
import de.mpg.imeji.logic.model.User;
import de.mpg.imeji.logic.model.factory.ImejiFactory;
import de.mpg.imeji.logic.security.user.UserService;
import de.mpg.imeji.logic.security.user.UserService.USER_TYPE;
import de.mpg.imeji.test.logic.service.SuperServiceTest;
import de.mpg.imeji.util.ImejiTestResources;

public class StatementServiceTest extends SuperServiceTest {
  private static final Logger LOGGER = LogManager.getLogger(SuperServiceTest.class);

  private static UserService userService;
  private static CollectionService collectionService;
  private static ItemService itemService;

  private static User defaultUser;
  private static User sysadmin;

  private static Statement statement;

  @BeforeClass
  public static void specificSetup() {
    try {
      userService = new UserService();
      collectionService = new CollectionService();
      itemService = new ItemService();
      sysadmin = ImejiFactory.newUser().setEmail("admin4@test.org").setPerson("admin4", "admin4", "org").setPassword("password")
          .setQuota(Long.MAX_VALUE).build();
      defaultUser = ImejiFactory.newUser().setEmail("default3@test.org").setPerson("default3", "default3", "org").setPassword("password")
          .setQuota(Long.MAX_VALUE).build();
      userService.create(sysadmin, USER_TYPE.ADMIN);
      userService.create(defaultUser, USER_TYPE.DEFAULT);

      statement = ImejiFactory.newStatement().setIndex("Text").setType(StatementType.TEXT)
          .setLiteralsConstraints(Arrays.asList("const a", "const b")).setNamespace("something").build();
      (new StatementService()).create(statement, sysadmin);
    } catch (ImejiException e) {
      LOGGER.error(e);
    }
  }

  /**
   * test if an unothorized user can't create statement, correct creation gets checked in Test if
   * statement can be created twice retrieve
   */
  @Test
  public void create() {
    Statement statement2 = ImejiFactory.newStatement().setIndex("Statement2").setType(StatementType.TEXT)
        .setLiteralsConstraints(Arrays.asList("const a", "const b")).build();
    try {
      (new StatementService()).create(statement2, defaultUser);
      // Assert.fail("No exception was thrown");
    } catch (ImejiException e) {
      // ok
    }
    try {
      (new StatementService()).retrieve(statement2.getUri().toString(), sysadmin);
      // Assert.fail("Something was retrieved");
    } catch (ImejiException e) {
      // ok
    }

    try {
      new StatementService().create(statement, sysadmin);
      Assert.fail("Statement was created twice");
    } catch (ImejiException e) {
      // ok
    }
  }

  @Test
  public void createIfNotExists() {
    StatementService service = new StatementService();
    Statement statement3 = ImejiFactory.newStatement().setIndex("Statement3").setType(StatementType.TEXT)
        .setLiteralsConstraints(Arrays.asList("const a", "const b")).build();
    try {
      service.create(statement3, sysadmin);
      service.createBatchIfNotExists(Arrays.asList(statement), sysadmin); // This should not throw
                                                                          // an exception
      service.retrieve(statement3.getUri().toString(), sysadmin);
      service.retrieve(statement.getUri().toString(), sysadmin);
    } catch (ImejiException e) {
      Assert.fail(e.getMessage());
    }
  }

  @Test
  public void retrieve() {
    StatementService service = new StatementService();
    try {
      Statement ret1 = service.retrieve(Arrays.asList(statement.getUri().toString()), defaultUser).get(0);
      Statement ret2 = service.retrieveByIndex(statement.getIndex(), defaultUser);

      Assert.assertEquals("Retrieve statement normale", statement, ret1);
      Assert.assertEquals("Retrieve statement lazy", statement, ret2);
    } catch (ImejiException e) {
      Assert.fail(e.getMessage());
    }
  }

  @Test
  public void update() {
    StatementService service = new StatementService();
    Statement statement2 = ImejiFactory.newStatement().setIndex("Second").setType(StatementType.TEXT)
        .setLiteralsConstraints(Arrays.asList("const c")).setNamespace("namespace").build();

    try {
      // Default user should not be able to change statements
      /*
       * try { service.update(statement, statement2, defaultUser);
       * Assert.fail("No exception was thrown"); } catch (ImejiException e) { // OK }
       */
      try {
        service.update(statement, statement2, sysadmin);
        Statement ret = service.retrieve(statement2.getUri().toString(), sysadmin);
        Assert.assertEquals("Statement should be updated", ret, statement2);
      } catch (ImejiException e) {
        Assert.fail(e.getMessage());
      }
    } finally {
      // Set the statement back to the original settings
      try {
        service.update(statement2, statement, sysadmin);
        int a = 5;
        a++;
      } catch (ImejiException e) {
        Assert.fail(e.getMessage());
      }
    }
  }

  @Test
  public void deleteAndIsUsed() {
    StatementService service = new StatementService();
    Statement toDelete = ImejiFactory.newStatement().setIndex("ToDelete").setType(StatementType.TEXT).build();
    try {
      service.create(toDelete, sysadmin);
      CollectionImeji col = ImejiFactory.newCollection().setPerson("m", "p", "g").setTitle("TestCol").build();
      collectionService.create(col, defaultUser);
      defaultUser = userService.retrieve(defaultUser.getId(), sysadmin);
      Item item = ImejiFactory.newItem(col);
      itemService.createWithFile(item, ImejiTestResources.getTest1Jpg(), "test1.jpg", col, defaultUser);
      Metadata metadata = ImejiFactory.newMetadata(toDelete).setText("some text").build();
      item.getMetadata().add(metadata);
      Assert.assertFalse("Item should not be used", service.isUsed(toDelete));
      item = itemService.update(item, sysadmin);
      Assert.assertTrue("Item should be used", service.isUsed(toDelete));
      try {
        service.delete(toDelete, sysadmin);
        Assert.fail("It was possible to delete a used statement");
      } catch (ImejiException e) {
        // ok
      }
      service.retrieve(toDelete.getUri().toString(), sysadmin);

      item.getMetadata().clear();
      itemService.update(item, sysadmin);
      service.delete(toDelete, sysadmin);
      try {
        service.retrieve(toDelete.getUri().toString(), sysadmin);
        Assert.fail("Something was retrieved");
      } catch (ImejiException e) {
        // ok
      }

    } catch (ImejiException e) {
      Assert.fail(e.getMessage());
    }

  }

}
