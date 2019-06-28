package de.mpg.imeji.testimpl.logic.service;

import static junit.framework.TestCase.fail;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.exceptions.NotAllowedError;
import de.mpg.imeji.exceptions.NotFoundException;
import de.mpg.imeji.exceptions.QuotaExceededException;
import de.mpg.imeji.logic.config.Imeji;
import de.mpg.imeji.logic.core.collection.CollectionService;
import de.mpg.imeji.logic.core.item.ItemService;
import de.mpg.imeji.logic.model.CollectionImeji;
import de.mpg.imeji.logic.model.Item;
import de.mpg.imeji.logic.model.Organization;
import de.mpg.imeji.logic.model.Person;
import de.mpg.imeji.logic.model.User;
import de.mpg.imeji.logic.model.factory.ImejiFactory;
import de.mpg.imeji.logic.security.authorization.AuthorizationPredefinedRoles;
import de.mpg.imeji.logic.security.user.UserService;
import de.mpg.imeji.logic.security.user.UserService.USER_TYPE;
import de.mpg.imeji.test.logic.service.SuperServiceTest;
import de.mpg.imeji.util.ImejiTestResources;
import de.mpg.imeji.util.JenaUtil;

public class UserServiceTest extends SuperServiceTest {
  private static final Logger LOGGER = LogManager.getLogger(UserServiceTest.class);

  private static User adminUser;
  private static User defaultUser;
  private static User restrictedUser;

  @BeforeClass
  public static void specificSetup() {
    UserService service = new UserService();
    try {
      // Delete all other users first
      List<User> users = service.retrieveAll();
      for (User u : users) {
        service.delete(u);
      }

      adminUser = ImejiFactory.newUser().setEmail("admin3@test.org").setPerson("admin3", "admin3", "org").setPassword("password")
          .setQuota(Long.MAX_VALUE).build();
      defaultUser = ImejiFactory.newUser().setEmail("default@test.org").setPerson("default", "default", "org").setPassword("password")
          .setQuota(Long.MAX_VALUE).build();
      restrictedUser = ImejiFactory.newUser().setEmail("restricted@test.org").setPerson("restricted", "restricted", "org")
          .setPassword("password").setQuota(Long.MAX_VALUE).build();
      service.create(adminUser, USER_TYPE.ADMIN);
      service.create(defaultUser, USER_TYPE.DEFAULT);
      service.create(restrictedUser, USER_TYPE.RESTRICTED);
    } catch (ImejiException e) {
      LOGGER.error("Error in UserServiceTest setup", e);
    }
  }

  @Test
  public void create() {
    // Just check if users got created correctly
    Assert.assertEquals("Admin grants should be as expected", AuthorizationPredefinedRoles.imejiAdministrator(adminUser.getId().toString()),
        adminUser.getGrants());
    Assert.assertEquals("Default grants should be as expected", AuthorizationPredefinedRoles.defaultUser(defaultUser.getId().toString()),
        defaultUser.getGrants());
    Assert.assertEquals("Restricted grants should be as expected",
        AuthorizationPredefinedRoles.restrictedUser(restrictedUser.getId().toString()), restrictedUser.getGrants());
  }

  @Test
  public void createAlreadyExistingUserTest() {
    try {
      UserService c = new UserService();
      // Create a new user with a new id but with the same email
      c.create(defaultUser, USER_TYPE.DEFAULT);
      Assert.fail("User should not be created, since User exists already");
    } catch (Exception e1) {
      // OK
    }
  }

  @Test
  public void delete() {
    UserService service = new UserService();
    try {
      service.delete(defaultUser);
      try {
        service.retrieve(defaultUser.getId().toString(), defaultUser);
        Assert.fail("User did not get deleted");
      } catch (NotFoundException e) {
        // That is correct
      }
      // Recreate User
      service.create(defaultUser, USER_TYPE.DEFAULT);
    } catch (ImejiException e) {
      Assert.fail(e.getMessage());
    }
  }

  @Test
  public void retrive() {
    UserService service = new UserService();
    try {
      User defaultByEmail = service.retrieve(defaultUser.getEmail(), adminUser);
      User defaultById = service.retrieve(defaultUser.getId(), adminUser);
      User defaultByBatch = (User) service.retrieveBatch(Arrays.asList(defaultUser.getId().toString()), 10).toArray()[0];
      User defaultByBatchLazy = (User) service.retrieveBatchLazy(Arrays.asList(defaultUser.getId().toString()), 10).toArray()[0];
      Assert.assertEquals("ID should be as expected for email retrive", defaultUser.getId().toString(), defaultByEmail.getId().toString());
      Assert.assertEquals("ID should be as expected for ID retrive", defaultUser.getId().toString(), defaultById.getId().toString());
      Assert.assertEquals("email should be as expected for email retrive", defaultUser.getEmail(), defaultByEmail.getEmail());
      Assert.assertEquals("email should be as expected for ID retrive", defaultUser.getEmail(), defaultById.getEmail());
      Assert.assertEquals("ID should be as expected for batch retrive", defaultUser.getId(), defaultByBatch.getId());
      Assert.assertEquals("ID should be as expected for batch retrive lazy", defaultUser.getId(), defaultByBatchLazy.getId());

      try {
        service.retrieve(adminUser.getEmail(), defaultUser);
        Assert.fail("No exception has been thrown");
      } catch (NotAllowedError e) {
        // That is correct
      }
      try {
        service.retrieve(adminUser.getId(), defaultUser);
        Assert.fail("No exception has been thrown");
      } catch (NotAllowedError e) {
        // That is correct
      }
    } catch (ImejiException e) {
      Assert.fail(e.getMessage());
    }
  }

  @Test
  public void retrieveAll() {
    UserService service = new UserService();
    try {
      List<User> users = service.retrieveAll();
      boolean containsAdmin = false;
      boolean containsDefault = false;
      boolean containsRestricted = false;
      for (int i = 0; i < users.size(); i++) {
        if (users.get(i).getId().toString().equals(adminUser.getId().toString())) {
          containsAdmin = true;
        }
        if (users.get(i).getId().toString().equals(defaultUser.getId().toString())) {
          containsDefault = true;
        }
        if (users.get(i).getId().toString().equals(restrictedUser.getId().toString())) {
          containsRestricted = true;
        }
      }
      Assert.assertTrue("All users should have been retrived", containsAdmin && containsDefault && containsRestricted);
    } catch (ImejiException e) {
      Assert.fail(e.getMessage());
    }
  }

  @Test
  public void updateUser() {
    UserService service = new UserService();
    defaultUser.setPerson(ImejiFactory.newPerson("modified", "modified", "mod"));
    String previousGivenName = adminUser.getPerson().getGivenName();
    adminUser.setPerson(ImejiFactory.newPerson("adminMod", "adminMod", "mod"));
    try {
      service.update(defaultUser, adminUser);
      User retDefault = service.retrieve(defaultUser.getId(), adminUser);
      Assert.assertEquals("Default users person shoud be modified", "modified", retDefault.getPerson().getGivenName());

      User retAdmin = service.retrieve(adminUser.getId(), adminUser);
      Assert.assertEquals("Admin should not have been modified by default user", previousGivenName, retAdmin.getPerson().getGivenName());
    } catch (ImejiException e) {
      Assert.fail(e.getMessage());
    }
  }

  @Test
  public void updateUserWithEmailAlreadyUsedByAnotherUser() {
    try {
      UserService c = new UserService();
      // Set Email of user2 to user
      defaultUser.setEmail(restrictedUser.getEmail());
      c.update(defaultUser, Imeji.adminUser);
      Assert.fail("User should not be updated, since the email is already used by another user");
    } catch (ImejiException e1) {
      // OK
    } finally {
      defaultUser.setEmail("default@test.org");
    }
  }

  @Test
  public void searchUserByName() {
    UserService service = new UserService();
    defaultUser.setPerson(ImejiFactory.newPerson("searchName", "last", "mod"));
    restrictedUser.setPerson(ImejiFactory.newPerson("first", "searchName", "mod"));
    try {
      service.update(defaultUser, adminUser);
      service.update(restrictedUser, adminUser);

      Object[] searchResult = service.searchUserByName("searchName").toArray();
      Assert.assertEquals("Thhere should be two users found", 2, searchResult.length);
      boolean defaultFound = false;
      boolean restrictedFound = false;
      for (int i = 0; i < searchResult.length; i++) {
        if (((User) searchResult[i]).getId().toString().equals(defaultUser.getId().toString())) {
          defaultFound = true;
        }
        if (((User) searchResult[i]).getId().toString().equals(restrictedUser.getId().toString())) {
          restrictedFound = true;
        }
      }
      Assert.assertTrue("All users should have been found", defaultFound && restrictedFound);
    } catch (ImejiException e) {
      Assert.fail(e.getMessage());
    }
  }

  @Test
  public void getCompleteName() {
    UserService service = new UserService();
    defaultUser.setPerson(ImejiFactory.newPerson("Planck", "Max", "MPDL"));
    try {
      service.update(defaultUser, adminUser);
      Assert.assertEquals("User name", "Planck, Max", service.getCompleteName(defaultUser.getId(), Locale.ENGLISH));
    } catch (ImejiException e) {
    }
  }

  @Test
  public void retrivePersonById() {
    Person ret = (new UserService()).retrievePersonById(defaultUser.getPerson().getId().toString());
    Assert.assertEquals("given name", defaultUser.getPerson().getGivenName(), ret.getGivenName());
    Assert.assertEquals("family name", defaultUser.getPerson().getFamilyName(), ret.getFamilyName());
  }

  @Test
  public void retriveOrganizationById_SearchOrganizationByName() {
    UserService service = new UserService();
    Organization org = ImejiFactory.newOrganization("ORGA");
    defaultUser.getPerson().getOrganizations().add(org);
    try {
      service.update(defaultUser, adminUser);
      Organization ret = service.retrieveOrganizationById(org.getId().toString());
      Assert.assertEquals("Organization name should be correct", "ORGA", ret.getName());
      Organization ret2 = (Organization) service.searchOrganizationByName("ORGA").toArray()[0];
      Assert.assertEquals("ID should be as expected", org.getId(), ret2.getId());
    } catch (ImejiException e) {
      Assert.fail(e.getMessage());
    }
  }

  @Test
  public void adminUserExists() {
    UserService service = new UserService();
    Assert.assertTrue("There is an admin", service.adminUserExist());
    try {
      service.delete(adminUser);
      Assert.assertFalse("No admin anymore", service.adminUserExist());
      // Restore admin
      service.create(adminUser, USER_TYPE.ADMIN);
    } catch (ImejiException e) {
      Assert.fail(e.getMessage());
    }
  }

  @Test
  public void retrieveAllAdmins() {
    UserService service = new UserService();
    try {
      User adminUser2 = ImejiFactory.newUser().setEmail("admin4@test.org").setPerson("admin4", "admin4", "org").setPassword("password")
          .setQuota(Long.MAX_VALUE).build();
      service.create(adminUser2, USER_TYPE.ADMIN);
      List<User> admins = service.retrieveAllAdmins();
      Assert.assertEquals("There are two admins", 2, admins.size());
      boolean admin1Found = false;
      boolean admin2Found = false;
      for (User u : admins) {
        if (u.getId().toString().equals(adminUser.getId().toString())) {
          admin1Found = true;
        }
        if (u.getId().toString().equals(adminUser2.getId().toString())) {
          admin2Found = true;
        }
      }
      Assert.assertTrue("All Admins are found", admin1Found && admin2Found);
      service.delete(adminUser2);
    } catch (ImejiException e) {
      e.printStackTrace();
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
    CollectionImeji col = ImejiFactory.newCollection().setTitle("test").setPerson("m", "p", "g").build();
    URI uri = cc.create(col, user).getId();
    user = c.retrieve(user.getId(), adminUser);
    col = cc.retrieve(uri, user);

    item = ImejiFactory.newItem(col);
    user.setQuota(ImejiTestResources.getTestJpg().length());
    ItemService itemController = new ItemService();
    item = itemController.createWithFile(item, ImejiTestResources.getTestJpg(), ImejiTestResources.getTestJpg().getName(), col, user);

    Item item2 = ImejiFactory.newItem(col);
    try {
      item2 = itemController.createWithFile(item2, ImejiTestResources.getTest2Jpg(), ImejiTestResources.getTest2Jpg().getName(), col, user);
      fail("Disk Quota should be exceeded!");
    } catch (QuotaExceededException e) {
    }
  }

}
