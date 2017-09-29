package de.mpg.imeji.testimpl.logic.service;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.exceptions.NotAllowedError;
import de.mpg.imeji.logic.config.Imeji;
import de.mpg.imeji.logic.model.Grant;
import de.mpg.imeji.logic.model.User;
import de.mpg.imeji.logic.model.UserGroup;
import de.mpg.imeji.logic.model.Grant.GrantType;
import de.mpg.imeji.logic.model.factory.ImejiFactory;
import de.mpg.imeji.logic.security.authorization.AuthorizationPredefinedRoles;
import de.mpg.imeji.logic.security.sharing.ShareService;
import de.mpg.imeji.logic.security.user.UserService;
import de.mpg.imeji.logic.security.user.UserService.USER_TYPE;
import de.mpg.imeji.logic.security.usergroup.UserGroupService;
import de.mpg.imeji.test.logic.service.SuperServiceTest;

public class ShareServiceTest extends SuperServiceTest {
  private static final Logger LOGGER = Logger.getLogger(ShareServiceTest.class);

  private static User defaultUser1;
  private static User defaultUser2;
  private static User sysadmin;

  private static UserGroup group;

  private static Grant sysadminGrant;
  private static Grant adminGrant;
  private static Grant editGrant;
  private static Grant readGrant;


  @BeforeClass
  public static void specificSetup() {
    try {
      UserService service = new UserService();
      sysadmin =
          ImejiFactory.newUser().setEmail("admin4@test.org").setPerson("admin4", "admin4", "org")
              .setPassword("password").setQuota(Long.MAX_VALUE).build();
      defaultUser1 = ImejiFactory.newUser().setEmail("default1@test.org")
          .setPerson("default1", "default1", "org").setPassword("password").setQuota(Long.MAX_VALUE)
          .build();
      defaultUser2 = ImejiFactory.newUser().setEmail("default2@test.org")
          .setPerson("default2", "default2", "org").setPassword("password").setQuota(Long.MAX_VALUE)
          .build();
      service.create(sysadmin, USER_TYPE.ADMIN);
      service.create(defaultUser1, USER_TYPE.DEFAULT);
      service.create(defaultUser2, USER_TYPE.DEFAULT);

      group = ImejiFactory.newUserGroup().setName("Test Group").addUsers(defaultUser1).build();
      (new UserGroupService()).create(group, sysadmin);

      sysadminGrant = new Grant(GrantType.ADMIN, AuthorizationPredefinedRoles.IMEJI_GLOBAL_URI);
      adminGrant = new Grant(GrantType.ADMIN, "fooGrant");
      editGrant = new Grant(GrantType.EDIT, "fooGrant");
      readGrant = new Grant(GrantType.READ, "fooGrant");
    } catch (ImejiException e) {
      LOGGER.error(e);
    }
  }

  @Test
  public void shareAndUnshareSysadmin() {
    ShareService service = new ShareService();
    try {
      // Nonadmin shares:
      try {
        service.shareSysAdmin(defaultUser2, defaultUser1);
        Assert.fail("Nonadmin shares: No exception");
      } catch (ImejiException e) {
        // ok
      }
      defaultUser1 = (new UserService()).retrieve(defaultUser1.getId(), sysadmin);
      Assert.assertFalse("User 1 should not have sysadmin grant",
          defaultUser1.getGrants().contains(sysadminGrant.toGrantString()));

      // Admin shares
      try {
        service.shareSysAdmin(sysadmin, defaultUser1);
      } catch (ImejiException e) {
        Assert.fail(e.getMessage());
      }
      defaultUser1 = (new UserService()).retrieve(defaultUser1.getId(), sysadmin);
      Assert.assertTrue("User 1 should have sysadmin grant",
          defaultUser1.getGrants().contains(sysadminGrant.toGrantString()));

      // Nonadmin unshares:
      try {
        service.unshareSysAdmin(defaultUser2, defaultUser1);
        Assert.fail("Nonadmin unshares: No exception");
      } catch (ImejiException e) {
        // ok
      }
      defaultUser1 = (new UserService()).retrieve(defaultUser1.getId(), sysadmin);
      Assert.assertTrue("User 1 should have sysadmin grant",
          defaultUser1.getGrants().contains(sysadminGrant.toGrantString()));

      // Admin unshares
      try {
        service.unshareSysAdmin(sysadmin, defaultUser1);
      } catch (ImejiException e) {
        Assert.fail(e.getMessage());
      }
      defaultUser1 = (new UserService()).retrieve(defaultUser1.getId(), sysadmin);
      Assert.assertFalse("User 1 should not have sysadmin grant",
          defaultUser1.getGrants().contains(sysadminGrant.toGrantString()));
    } catch (ImejiException e1) {
      Assert.fail(e1.getMessage());
    } finally {
      defaultUser1
          .setGrants(AuthorizationPredefinedRoles.defaultUser(defaultUser1.getId().toString()));
      try {
        (new UserService()).update(defaultUser1, sysadmin);
      } catch (ImejiException e) {
        Assert.fail(e.getMessage());
      }
    }
  }

  @Test
  public void shareToUser() {
    try {
      defaultUser2.getGrants().add(editGrant.toGrantString());
      shareToUser_Test("User 2 not allowed, read grant", defaultUser2, defaultUser1, readGrant,
          NotAllowedError.class);
      shareToUser_Test("User 2 not allowed, edit grant", defaultUser2, defaultUser1, editGrant,
          NotAllowedError.class);
      shareToUser_Test("User 2 not allowed, admin grant", defaultUser2, defaultUser1, adminGrant,
          NotAllowedError.class);

      new ShareService().shareToUser(Imeji.adminUser, defaultUser2, adminGrant.getGrantFor(),
          adminGrant.getGrantType());
      shareToUser_Test("User 2  allowed, read grant", defaultUser2, defaultUser1, readGrant, null);
      shareToUser_Test("User 2  allowed, edit grant", defaultUser2, defaultUser1, editGrant, null);
      shareToUser_Test("User 2  allowed, admin grant", defaultUser2, defaultUser1, adminGrant,
          null);
    } catch (ImejiException e) {
      Assert.fail("Error giving collection admin grant to defaultUser2 with imeji.adminuser");
    } finally {
      defaultUser2.getGrants().remove(editGrant.toGrantString());
      defaultUser2.getGrants().remove(adminGrant.toGrantString());
    }

  }

  private void shareToUser_Test(String msg, User fromUser, User toUser, Grant grant,
      Class exception) {
    ShareService service = new ShareService();
    try {
      service.shareToUser(fromUser, toUser, grant.getGrantFor(), grant.getGrantType());
      if (exception != null) {
        Assert.fail(msg + ": No exception was thrown");
      }
    } catch (ImejiException e) {
      if (!e.getClass().equals(exception)) {
        Assert.fail(msg + ": " + e.getMessage());
      }
    }
    try {
      User retUser = (new UserService()).retrieve(toUser.getId(), sysadmin);
      if (exception == null) {
        Assert.assertTrue(msg + ": User should have grant",
            retUser.getGrants().contains(grant.toGrantString()));
      } else {
        Assert.assertFalse(msg + ": User should not have grant",
            retUser.getGrants().contains(grant.toGrantString()));
      }
    } catch (ImejiException e) {
      Assert.fail(e.getMessage());
    }
  }


  @Test
  public void shareToUserGroup() {
    ShareService service = new ShareService();

    try {
      defaultUser2.getGrants().add(editGrant.toGrantString());
      shareToUserGroup_Test("User 2 not allowed, read grant", defaultUser2, group, readGrant,
          NotAllowedError.class);
      shareToUserGroup_Test("User 2 not allowed, edit grant", defaultUser2, group, editGrant,
          NotAllowedError.class);
      shareToUserGroup_Test("User 2 not allowed, admin grant", defaultUser2, group, adminGrant,
          NotAllowedError.class);

      defaultUser2.getGrants().add(adminGrant.toGrantString());
      shareToUserGroup_Test("User 2  allowed, read grant", defaultUser2, group, readGrant, null);
      shareToUserGroup_Test("User 2  allowed, edit grant", defaultUser2, group, editGrant, null);
      shareToUserGroup_Test("User 2  allowed, admin grant", defaultUser2, group, adminGrant, null);
    } finally {
      defaultUser2.getGrants().remove(editGrant.toGrantString());
      defaultUser2.getGrants().remove(adminGrant.toGrantString());
    }

  }

  private void shareToUserGroup_Test(String msg, User fromUser, UserGroup group, Grant grant,
      Class exception) {
    ShareService service = new ShareService();
    try {
      service.shareToGroup(fromUser, group, grant.getGrantFor(), grant.getGrantType());
      if (exception != null) {
        Assert.fail(msg + ": No exception was thrown");
      }
    } catch (ImejiException e) {
      if (!e.getClass().equals(exception)) {
        Assert.fail(msg + ": " + e.getMessage());
      }
    }
    try {
      UserGroup retGroup = (new UserGroupService()).retrieve(group.getId().toString(), sysadmin);
      if (exception == null) {
        Assert.assertTrue(msg + ": User should have grant",
            retGroup.getGrants().contains(grant.toGrantString()));
      } else {
        Assert.assertFalse(msg + ": User should not have grant",
            retGroup.getGrants().contains(grant.toGrantString()));
      }
    } catch (ImejiException e) {
      Assert.fail(e.getMessage());
    }
  }

}
