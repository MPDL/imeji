package de.mpg.imeji.testimpl.logic.service;

import java.util.Arrays;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.statement.StatementService;
import de.mpg.imeji.logic.user.UserService;
import de.mpg.imeji.logic.user.UserService.USER_TYPE;
import de.mpg.imeji.logic.vo.Statement;
import de.mpg.imeji.logic.vo.StatementType;
import de.mpg.imeji.logic.vo.User;
import de.mpg.imeji.logic.vo.factory.ImejiFactory;
import de.mpg.imeji.test.logic.service.SuperServiceTest;

public class StatementServiceTest extends SuperServiceTest {
  private static final Logger LOGGER = Logger.getLogger(SuperServiceTest.class);

  private static User defaultUser;
  private static User sysadmin;

  private static Statement statement;

  @BeforeClass
  public static void specificSetup() {
    try {
      sysadmin =
          ImejiFactory.newUser().setEmail("admin4@test.org").setPerson("admin4", "admin4", "org")
              .setPassword("password").setQuota(Long.MAX_VALUE).build();
      defaultUser = ImejiFactory.newUser().setEmail("default1@test.org")
          .setPerson("default1", "default1", "org").setPassword("password").setQuota(Long.MAX_VALUE)
          .build();
      UserService service = new UserService();
      service.create(sysadmin, USER_TYPE.ADMIN);
      service.create(sysadmin, USER_TYPE.ADMIN);

      statement = ImejiFactory.newStatement().setIndex("Text").setType(StatementType.TEXT)
          .setLiteralsConstraints(Arrays.asList("const a", "const b")).build();
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
    Statement statement2 =
        ImejiFactory.newStatement().setIndex("Statement2").setType(StatementType.TEXT)
            .setLiteralsConstraints(Arrays.asList("const a", "const b")).build();
    try {
      (new StatementService()).create(statement2, defaultUser);
      Assert.fail("No exception was thrown");
    } catch (ImejiException e) {
      // ok
    }
    try {
      (new StatementService()).retrieve(statement2.getUri().toString(), sysadmin);
      Assert.fail("Something was retrieved");
    } catch (ImejiException e) {
      // ok
    }

    try {
      (new StatementService()).create(statement, sysadmin);
      Assert.fail("Statement was created twice");
    } catch (ImejiException e) {
      // ok
    }
  }

  @Test
  public void createIfNotExists() {
    StatementService service = new StatementService();
    Statement statement3 =
        ImejiFactory.newStatement().setIndex("Statement2").setType(StatementType.TEXT)
            .setLiteralsConstraints(Arrays.asList("const a", "const b")).build();
    try {
      service.create(statement3, sysadmin);
      service.create(statement, sysadmin); // This should not throw an exception
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
      Statement ret1 =
          service.retrieve(Arrays.asList(statement.getUri().toString()), defaultUser).get(0);
      Statement ret2 = service.retrieveByIndex("Text", defaultUser);

      Assert.assertEquals("Retrieve statement normale", statement, ret1);
      Assert.assertEquals("Retrieve statement lazy", statement, ret2);
    } catch (ImejiException e) {
      Assert.fail(e.getMessage());
    }
  }


}
