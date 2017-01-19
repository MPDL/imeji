package de.mpg.imeji.testimpl.logic.controller;

import static junit.framework.TestCase.fail;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.net.URI;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.exceptions.QuotaExceededException;
import de.mpg.imeji.logic.collection.CollectionService;
import de.mpg.imeji.logic.collection.CollectionService.MetadataProfileCreationMethod;
import de.mpg.imeji.logic.config.Imeji;
import de.mpg.imeji.logic.item.ItemService;
import de.mpg.imeji.logic.user.UserService;
import de.mpg.imeji.logic.user.UserService.USER_TYPE;
import de.mpg.imeji.logic.vo.CollectionImeji;
import de.mpg.imeji.logic.vo.Item;
import de.mpg.imeji.logic.vo.User;
import de.mpg.imeji.logic.vo.factory.ImejiFactory;
import de.mpg.imeji.test.logic.controller.ControllerTest;
import de.mpg.imeji.testimpl.ImejiTestResources;
import util.JenaUtil;

public class UserControllerTestClass extends ControllerTest {

  private static final Logger LOGGER = Logger.getLogger(UserControllerTestClass.class);

  @Test
  public void createAlreadyExistingUserTest() {
    try {
      UserService c = new UserService();
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
      UserService c = new UserService();
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

    UserService c = new UserService();
    User u = c.create(user, USER_TYPE.DEFAULT);

    // change quota
    long NEW_QUOTA = 25 * 1024;
    user.setQuota(NEW_QUOTA);
    user = c.update(user, Imeji.adminUser);
    assertThat(u.getQuota(), equalTo(NEW_QUOTA));

    // try to exceed quota
    CollectionService cc = new CollectionService();
    CollectionImeji col = ImejiFactory.newCollection("test", "Planck", "Max", "MPG");
    URI uri = cc.create(col, profile, user, MetadataProfileCreationMethod.COPY, null).getId();
    col = cc.retrieve(uri, user);

    item = ImejiFactory.newItem(col);
    user.setQuota(ImejiTestResources.getTestJpg().length());
    ItemService itemController = new ItemService();
    item = itemController.createWithFile(item, ImejiTestResources.getTestJpg(),
        ImejiTestResources.getTestJpg().getName(), col, user);

    Item item2 = ImejiFactory.newItem(col);
    try {
      item2 = itemController.createWithFile(item2, ImejiTestResources.getTest2Jpg(),
          ImejiTestResources.getTest2Jpg().getName(), col, user);
      fail("Disk Quota should be exceeded!");
    } catch (QuotaExceededException e) {
    }
  }

}
