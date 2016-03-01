package de.mpg.imeji.testimpl.logic.controller;

import static de.mpg.imeji.test.rest.resources.test.integration.MyTestContainerFactory.STATIC_CONTEXT_STORAGE;
import static junit.framework.TestCase.fail;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.net.URI;
import java.util.Calendar;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.exceptions.NotFoundException;
import de.mpg.imeji.exceptions.QuotaExceededException;
import de.mpg.imeji.exceptions.UnprocessableError;
import de.mpg.imeji.logic.Imeji;
import de.mpg.imeji.logic.controller.CollectionController;
import de.mpg.imeji.logic.controller.CollectionController.MetadataProfileCreationMethod;
import de.mpg.imeji.logic.controller.ItemController;
import de.mpg.imeji.logic.controller.UserController;
import de.mpg.imeji.logic.controller.UserController.USER_TYPE;
import de.mpg.imeji.logic.vo.CollectionImeji;
import de.mpg.imeji.logic.vo.Item;
import de.mpg.imeji.logic.vo.User;
import de.mpg.imeji.presentation.beans.ConfigurationBean;
import de.mpg.imeji.presentation.util.ImejiFactory;
import de.mpg.imeji.test.logic.controller.ControllerTest;
import de.mpg.j2j.helper.DateHelper;
import util.JenaUtil;

public class UserControllerTestClass extends ControllerTest {

  private static final Logger LOGGER = Logger.getLogger(UserControllerTestClass.class);
  private static File file1 = new File(STATIC_CONTEXT_STORAGE + "/test.jpg");
  private static File file2 = new File(STATIC_CONTEXT_STORAGE + "/test2.jpg");

  @Test
  public void createAlreadyExistingUserTest() {
    try {
      UserController c = new UserController(Imeji.adminUser);
      // Create a new user with a new id but with the same email
      User user = JenaUtil.testUser.clone(JenaUtil.TEST_USER_EMAIL);
      c.create(user, USER_TYPE.DEFAULT);
      Assert.fail("User should not be created, since User exists already");
    } catch (Exception e1) {
      // OK
    }
  }

  @Test
  public void updateUserWithEmailAlreadyUsedByAnotherUser() {
    try {
      UserController c = new UserController(Imeji.adminUser);
      // Set Email of user2 to user
      User user = JenaUtil.testUser;
      user.setEmail(JenaUtil.TEST_USER_EMAIL_2);
      user.getPerson().setFamilyName(JenaUtil.TEST_USER_NAME);
      c.update(user, Imeji.adminUser);
      Assert.fail("User should not be updated, since the email is already used by another user");
    } catch (ImejiException e1) {
      // OK
    }
  }

  @Test
  public void createInactiveUserTest() throws ImejiException {
    UserController c = new UserController(null);
    // Create a new user with a new id but with the same email
    User user = new User();
    LOGGER.info("User object has " + user.getId());
    user.setEmail("inactiveuser@imeji.org");
    user.getPerson().setFamilyName(JenaUtil.TEST_USER_NAME);
    user.getPerson().setOrganizations(JenaUtil.testUser.getPerson().getOrganizations());
    c.create(user, USER_TYPE.INACTIVE);
    assertTrue(!user.isActive());
  }

  @Test
  public void createActiveUserTestAsNoUser() {
    UserController c = new UserController(null);
    // Create a new user with a new id but with the same email
    User user = new User();
    user.setEmail("activeuser@imeji.org");
    user.getPerson().setFamilyName(JenaUtil.TEST_USER_NAME);
    user.getPerson().setOrganizations(JenaUtil.testUser.getPerson().getOrganizations());
    try {
      user = c.create(user, USER_TYPE.DEFAULT);
      Assert.fail("User should not be created in other state than inactive!");
    } catch (ImejiException e) {
      // Do Nothing this is fine
    }

  }


  @Test
  public void createAndActivateInactiveUserTest() throws ImejiException {
    User user = new User();
    user.setEmail("inactive-activate@imeji.org");
    user.getPerson().setFamilyName(JenaUtil.TEST_USER_NAME);
    user.getPerson().setOrganizations(JenaUtil.testUser.getPerson().getOrganizations());
    UserController c = new UserController(user);
    try {
      // Create a new user with a new id but with the same email
      user = c.create(user, USER_TYPE.INACTIVE);
      assertTrue(!user.isActive());
    } catch (Exception e1) {
      Assert.fail("An error happened by cretaion an inactive User " + e1.getMessage());
    }

    try {
      user = c.activate(user.getRegistrationToken() + "RR");

    } catch (NotFoundException e1) {
      // Do Nothing this is OK
      LOGGER.info("OK, invalid registration token");
    } catch (Exception e2) {
      Assert.fail("An error happened by activating the user with false Registration Token"
          + e2.getMessage());
    }

    Calendar originalCreateDate = user.getCreated();
    try {
      Calendar now = DateHelper.getCurrentDate();
      // Tests assumes registration token is valid for not more than 20 days
      // ToDo: Change to read from Configuration Bean
      now.add(Calendar.DAY_OF_MONTH, ConfigurationBean.getRegistrationTokenExpiryStatic() - 5);
      user.setCreated(now);
      user = c.update(user, c.getControllerUser());
      user = c.activate(user.getRegistrationToken());

    } catch (UnprocessableError e1) {
      LOGGER.info("OK, expired registration token");
    } catch (Exception e2) {
      Assert.fail("An error happened by activating the user with expired Registration Token"
          + e2.getMessage());
    }

    user.setCreated(originalCreateDate);
    user = c.update(user, c.getControllerUser());

    try {
      user = c.activate(user.getRegistrationToken());
      assertTrue(user.isActive());

    } catch (Exception e1) {
      Assert.fail("An error happened by activating the user " + e1.getMessage());
    }

    try {
      user = c.activate(user.getRegistrationToken());
      LOGGER.info("OK, double registration!");
      Assert.fail("An error happened by activating the user again!");
    } catch (Exception e) {
      // Do Nothing all is fine
    }
  }



  @Test
  public void createAndCleanInactiveUserTest() throws ImejiException {
    String email = "clean-inactive-user@imeji.org";
    UserController c = new UserController(Imeji.adminUser);
    Calendar now = DateHelper.getCurrentDate();
    for (int i = 1; i < 10; i++) {
      User user = new User();
      user.setEmail(email + i);
      user.getPerson().setFamilyName(JenaUtil.TEST_USER_NAME);
      user.getPerson().setOrganizations(JenaUtil.testUser.getPerson().getOrganizations());
      user = c.create(user, USER_TYPE.INACTIVE);
      assertTrue(!user.isActive());
      Calendar originalCreateDate = user.getCreated();
      if (i < 7) {
        now.add(Calendar.DAY_OF_MONTH, ConfigurationBean.getRegistrationTokenExpiryStatic() - 5);
        user.setCreated(now);
        user = c.update(user, c.getControllerUser());
      }
    }

    int numCleaned = c.cleanInactiveUsers();
    assertTrue(numCleaned == 6);
  }

  @Test
  public void testUserDiskSpaceQuota() throws ImejiException {
    // create user
    User user = new User();
    user.setEmail("quotaUser@imeji.org");
    user.getPerson().setFamilyName(JenaUtil.TEST_USER_NAME);
    user.getPerson().setOrganizations(JenaUtil.testUser.getPerson().getOrganizations());

    UserController c = new UserController(Imeji.adminUser);
    User u = c.create(user, USER_TYPE.DEFAULT);

    // change quota
    long NEW_QUOTA = 25 * 1024;
    user.setQuota(NEW_QUOTA);
    user = c.update(user, Imeji.adminUser);
    assertThat(u.getQuota(), equalTo(NEW_QUOTA));

    // try to exceed quota
    CollectionController cc = new CollectionController();
    CollectionImeji col = ImejiFactory.newCollection("test", "Planck", "Max", "MPG");
    URI uri = cc.create(col, profile, user, MetadataProfileCreationMethod.COPY, null);
    col = cc.retrieve(uri, user);

    item = ImejiFactory.newItem(col);
    user.setQuota(file1.length());
    ItemController itemController = new ItemController();
    item = itemController.createWithFile(item, file1, file1.getName(), col, user);

    Item item2 = ImejiFactory.newItem(col);
    try {
      item2 = itemController.createWithFile(item2, file2, file2.getName(), col, user);
      fail("Disk Quota should be exceeded!");
    } catch (QuotaExceededException e) {
    }
  }

}
