package de.mpg.imeji.testimpl.logic.controller;

import static de.mpg.imeji.test.rest.resources.test.integration.MyTestContainerFactory.STATIC_CONTEXT_STORAGE;
import static junit.framework.TestCase.fail;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.net.URI;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.exceptions.QuotaExceededException;
import de.mpg.imeji.logic.Imeji;
import de.mpg.imeji.logic.controller.resource.CollectionController;
import de.mpg.imeji.logic.controller.resource.CollectionController.MetadataProfileCreationMethod;
import de.mpg.imeji.logic.controller.resource.ItemController;
import de.mpg.imeji.logic.user.controller.UserBusinessController;
import de.mpg.imeji.logic.user.controller.UserBusinessController.USER_TYPE;
import de.mpg.imeji.logic.vo.CollectionImeji;
import de.mpg.imeji.logic.vo.Item;
import de.mpg.imeji.logic.vo.User;
import de.mpg.imeji.logic.vo.util.ImejiFactory;
import de.mpg.imeji.test.logic.controller.ControllerTest;
import util.JenaUtil;

public class UserControllerTestClass extends ControllerTest {

  private static final Logger LOGGER = Logger.getLogger(UserControllerTestClass.class);
  private static File file1 = new File(STATIC_CONTEXT_STORAGE + "/test.jpg");
  private static File file2 = new File(STATIC_CONTEXT_STORAGE + "/test2.jpg");

  @Test
  public void createAlreadyExistingUserTest() {
    try {
      UserBusinessController c = new UserBusinessController();
      // Create a new user with a new id but with the same email
      c.create(JenaUtil.testUser, USER_TYPE.DEFAULT);
      Assert.fail("User should not be created, since User exists already");
    } catch (Exception e1) {
      // OK
    }
  }

  @Test
  public void updateUserWithEmailAlreadyUsedByAnotherUser() {
    try {
      UserBusinessController c = new UserBusinessController();
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
  public void testUserDiskSpaceQuota() throws ImejiException {
    // create user
    User user = new User();
    user.setEmail("quotaUser@imeji.org");
    user.getPerson().setFamilyName(JenaUtil.TEST_USER_NAME);
    user.getPerson().setOrganizations(JenaUtil.testUser.getPerson().getOrganizations());

    UserBusinessController c = new UserBusinessController();
    User u = c.create(user, USER_TYPE.DEFAULT);

    // change quota
    long NEW_QUOTA = 25 * 1024;
    user.setQuota(NEW_QUOTA);
    user = c.update(user, Imeji.adminUser);
    assertThat(u.getQuota(), equalTo(NEW_QUOTA));

    // try to exceed quota
    CollectionController cc = new CollectionController();
    CollectionImeji col = ImejiFactory.newCollection("test", "Planck", "Max", "MPG");
    URI uri = cc.create(col, profile, user, MetadataProfileCreationMethod.COPY, null).getId();
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
