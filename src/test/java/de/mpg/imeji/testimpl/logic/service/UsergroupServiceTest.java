package de.mpg.imeji.testimpl.logic.service;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.exceptions.NotAllowedError;
import de.mpg.imeji.exceptions.NotFoundException;
import de.mpg.imeji.logic.model.User;
import de.mpg.imeji.logic.model.UserGroup;
import de.mpg.imeji.logic.model.factory.ImejiFactory;
import de.mpg.imeji.logic.security.user.UserService;
import de.mpg.imeji.logic.security.user.UserService.USER_TYPE;
import de.mpg.imeji.logic.security.usergroup.UserGroupService;
import de.mpg.imeji.test.logic.service.SuperServiceTest;

public class UsergroupServiceTest extends SuperServiceTest {
  private static final Logger LOGGER = LogManager.getLogger(UsergroupServiceTest.class);

  private static User defaultUser1;
  private static User defaultUser2;
  private static User adminUser;
  private static User restrictedUser;

  private static UserGroup userGroup;

  private static final String FOO_GRANT = "fooGrantString";

  @BeforeClass
  public static void specificSetup() {
    UserService userService = new UserService();
    try {
      // Delete all other users first
      List<User> users = userService.retrieveAll();
      for (User u : users) {
        userService.delete(u);
      }
      adminUser = ImejiFactory.newUser().setEmail("admin3@test.org").setPerson("admin3", "admin3", "org").setPassword("password")
          .setQuota(Long.MAX_VALUE).build();
      restrictedUser = ImejiFactory.newUser().setEmail("restricted@test.org").setPerson("restricted", "restricted", "org")
          .setPassword("password").setQuota(Long.MAX_VALUE).build();
      defaultUser1 = ImejiFactory.newUser().setEmail("default1@test.org").setPerson("default1", "default1", "org").setPassword("password")
          .setQuota(Long.MAX_VALUE).build();
      defaultUser2 = ImejiFactory.newUser().setEmail("default2@test.org").setPerson("default2", "default2", "org").setPassword("password")
          .setQuota(Long.MAX_VALUE).build();
      userService.create(adminUser, USER_TYPE.ADMIN);
      userService.create(restrictedUser, USER_TYPE.RESTRICTED);
      userService.create(defaultUser1, USER_TYPE.DEFAULT);
      userService.create(defaultUser2, USER_TYPE.DEFAULT);

      userGroup = ImejiFactory.newUserGroup().setName("UserGroup").addUsers(defaultUser1, defaultUser2).addGrants(FOO_GRANT).build();
      (new UserGroupService()).create(userGroup, adminUser);
    } catch (ImejiException e) {
      LOGGER.error(e);
    }
  }

  @Test
  public void create() {
    Assert.assertTrue("group should have grant", userGroup.getGrants().contains(FOO_GRANT));

    // Check that defaultUser is not allowed to create user group
    UserGroup userGroup2 = ImejiFactory.newUserGroup().setName("UserGroup2").addUsers(defaultUser1, defaultUser2).build();
    try {
      (new UserGroupService()).create(userGroup2, defaultUser1);
      Assert.fail("No exception has been thrown");
    } catch (Exception e) {
      if (!(e instanceof NotAllowedError)) {
        Assert.fail(e.getMessage());
      }
    }
    try {
      (new UserGroupService()).retrieve(userGroup2.getId().toString(), adminUser);
      Assert.fail("It was possible to retrive UserGroup2");
    } catch (ImejiException e) {
      if (!(e instanceof NotFoundException)) {
        Assert.fail(e.getMessage());
      }
    }
  }

  @Test
  public void retrive() {
    UserGroupService service = new UserGroupService();
    try {
      UserGroup ret1 = service.retrieve(userGroup.getId().toString(), restrictedUser);
      Assert.assertEquals("ID", userGroup.getId(), ret1.getId());
      Assert.assertEquals("Name", userGroup.getName(), ret1.getName());
      UserGroup ret2 = service.retrieveLazy(userGroup.getId().toString(), restrictedUser);
      Assert.assertEquals("ID", userGroup.getId(), ret2.getId());
      Assert.assertEquals("Name", userGroup.getName(), ret2.getName());
      UserGroup ret3 = service.retrieveBatch(Arrays.asList(userGroup.getId().toString()), restrictedUser).get(0);
      Assert.assertEquals("ID", userGroup.getId(), ret3.getId());
      Assert.assertEquals("Name", userGroup.getName(), ret3.getName());
      UserGroup ret4 = service.retrieveBatchLazy(Arrays.asList(userGroup.getId().toString()), restrictedUser).get(0);
      Assert.assertEquals("ID", userGroup.getId(), ret4.getId());
      Assert.assertEquals("Name", userGroup.getName(), ret4.getName());
    } catch (ImejiException e) {
      Assert.fail(e.getMessage());
    }
  }

  @Test
  public void update() {
    try {
      User defaultUser3 = ImejiFactory.newUser().setEmail("default3@test.org").setPerson("default3", "default3", "org")
          .setPassword("password").setQuota(Long.MAX_VALUE).build();
      (new UserService()).create(defaultUser3, USER_TYPE.DEFAULT);
      userGroup.getUsers().add(defaultUser3.getId());
      userGroup.getGrants().add("fooGrant2");
      userGroup.setName("changedName");
      (new UserGroupService()).update(userGroup, adminUser);
      try {
        UserGroup ret = (new UserGroupService()).retrieve(userGroup.getId().toString(), adminUser);
        Assert.assertEquals("The name should have changed", userGroup.getName(), ret.getName());
        Assert.assertTrue("Group should have fooGrant2", ret.getGrants().contains("fooGrant2"));
      } finally {
        userGroup.getUsers().remove(defaultUser3);
        (new UserGroupService()).update(userGroup, adminUser);
      }
    } catch (ImejiException e) {
      Assert.fail(e.getMessage());
    }
  }

  @Test
  public void delete() {
    UserGroupService service = new UserGroupService();
    try {
      service.delete(userGroup, adminUser);
      try {
        service.retrieve(userGroup.getId().toString(), adminUser);
        Assert.fail("It was possible to retrieve the UserGroup");
      } catch (NotFoundException e) {
        // This is correct
      }
      Assert.assertFalse("defaultUser1 should have lost his FOO grant", defaultUser1.getGrants().contains(FOO_GRANT));
      // Recreate the usergroup
      service.create(userGroup, adminUser);

      /*
       * try { service.delete(userGroup, defaultUser1);
       * Assert.fail("No exception has been thown"); } catch (NotAllowedError e) { //
       * That is correct }
       */
      // To check usergroup is still there
      service.retrieve(userGroup.getId().toString(), adminUser);
    } catch (ImejiException e) {
      Assert.fail(e.getMessage());
    }
  }

  @Test
  public void retrieveAll_searchByUser_serachByName() {
    UserGroupService service = new UserGroupService();
    UserGroup userGroup2 = ImejiFactory.newUserGroup().setName("SearchName").addUsers(defaultUser1).build();
    UserGroup userGroup3 = ImejiFactory.newUserGroup().setName("UserGroup3").build();
    try {
      service.create(userGroup2, adminUser);
      service.create(userGroup3, adminUser);

      Collection<UserGroup> all = service.retrieveAll();
      Collection<UserGroup> byUser = service.searchByUser(defaultUser1, adminUser);
      Collection<UserGroup> byName = service.searchByName("SearchName", adminUser);
      Assert.assertTrue("All users", doesCollectionConsistOfUserGroups(all, userGroup, userGroup2, userGroup3));
      Assert.assertTrue("By User", doesCollectionConsistOfUserGroups(byUser, userGroup, userGroup2));
      Assert.assertTrue("By Name", doesCollectionConsistOfUserGroups(byName, userGroup2));
    } catch (ImejiException e) {
      Assert.fail(e.getMessage());
    }
  }

  @Test
  public void removeUserFromAllGroups() {
    UserGroupService service = new UserGroupService();
    try {
      // First check for unauthorized defaultUser1
      /*
       * try { service.removeUserFromAllGroups(defaultUser2, defaultUser1);
       * Assert.fail("No exception was thrown"); } catch (NotAllowedError e) { //
       * correct }
       * 
       * Assert.assertTrue("userGroup should still contain defaultUser2",
       * userGroup.getUsers().contains(defaultUser2));
       */
      service.removeUserFromAllGroups(defaultUser2, adminUser);
      Assert.assertEquals("User2 should be in no user group", 0, service.searchByUser(defaultUser2, adminUser).size());
      Assert.assertFalse("User2 should have lost his foo grant", defaultUser2.getGrants().contains(FOO_GRANT));
    } catch (ImejiException e) {
      Assert.fail(e.getMessage());
    }
  }

  private boolean doesCollectionConsistOfUserGroups(Collection<UserGroup> coll, UserGroup... groups) {
    if (coll.size() != groups.length) {
      return false;
    }
    boolean[] found = new boolean[groups.length];
    for (UserGroup u1 : coll) {
      for (int i = 0; i < groups.length; i++) {
        if (u1.getId().toString().equals(groups[i].getId().toString())) {
          found[i] = true;
        }
      }
    }
    for (boolean b : found) {
      if (!b) {
        return false;
      }
    }
    return true;
  }

}
