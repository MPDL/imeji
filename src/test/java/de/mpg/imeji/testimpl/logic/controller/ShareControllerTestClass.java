package de.mpg.imeji.testimpl.logic.controller;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.collection.CollectionService;
import de.mpg.imeji.logic.config.Imeji;
import de.mpg.imeji.logic.controller.resource.ProfileController;
import de.mpg.imeji.logic.item.ItemService;
import de.mpg.imeji.logic.share.ShareService;
import de.mpg.imeji.logic.share.ShareService.ShareRoles;
import de.mpg.imeji.logic.user.UserService;
import de.mpg.imeji.logic.vo.MetadataProfile;
import de.mpg.imeji.logic.vo.User;
import de.mpg.imeji.test.logic.service.SuperServiceTest;
import util.JenaUtil;

/**
 * Test the {@link ShareService}
 * 
 * @author saquet
 *
 */
public class ShareControllerTestClass extends SuperServiceTest {

  private static final Logger LOGGER = Logger.getLogger(ShareControllerTestClass.class);

  @BeforeClass
  public static void specificSetup() {
    try {
      createProfile();
      createCollection();
      createItem();
    } catch (ImejiException e) {
      LOGGER.error("Error initializing collection or item", e);
    }

  }

  @Test
  public void shareEditProfile() throws ImejiException {
    UserService userController = new UserService();
    User user = userController.retrieve(JenaUtil.TEST_USER_EMAIL, Imeji.adminUser);
    User user2 = userController.retrieve(JenaUtil.TEST_USER_EMAIL_2, Imeji.adminUser);
    ShareService controller = new ShareService();
    controller.shareToUser(user, user2, profile.getId().toString(),
        (List<String>) ShareService.rolesAsList(ShareRoles.EDIT));
    ProfileController profileController = new ProfileController();

    try {
      profileController.update(profile, user2);
      profileController.retrieve(profile.getId(), user2);
    } catch (Exception e) {
      Assert.fail(e.getMessage());
    }
    try {
      CollectionService collectionController = new CollectionService();
      profileController.delete(profile, user2);
      Assert.fail("User shouldn't be abble to delete the profile");
      profileController.release(profile, user2);
      Assert.fail("User shouldn't be abble to release the profile");
      collectionController.delete(collection, user2);
      Assert.fail("User shouldn't be abble to delete the collection");
      collectionController.releaseWithDefaultLicense(collection, user2);
      Assert.fail("User shouldn't be abble to release the collection");
      profileController.update(profile, user2);
    } catch (Exception e) {
      // OK
    }
  }

  @Test
  public void shareReadCollection() throws ImejiException {
    UserService userController = new UserService();
    User user = userController.retrieve(JenaUtil.TEST_USER_EMAIL, Imeji.adminUser);
    User user2 = userController.retrieve(JenaUtil.TEST_USER_EMAIL_2, Imeji.adminUser);
    ShareService controller = new ShareService();
    controller.shareToUser(user, user2, collection.getId().toString(),
        (List<String>) ShareService.rolesAsList(ShareRoles.READ));
    ProfileController profileController = new ProfileController();
    CollectionService collectionController = new CollectionService();
    MetadataProfile collectionProfile = null;
    try {
      collectionController.retrieve(collection.getId(), user2);
      collectionProfile = profileController.retrieve(collection.getProfile(), user2);
    } catch (Exception e) {
      Assert.fail(e.getMessage());
    }
    try {
      collectionController.update(collection, user2);
      Assert.fail("User shouldn't be abble to update the collection");
    } catch (Exception e) {
      // OK
    }
    try {
      collectionController.delete(collection, user2);
      Assert.fail("User shouldn't be abble to delete the collection");
    } catch (Exception e) {
      // OK
    }
    try {
      collectionController.releaseWithDefaultLicense(collection, user2);
      Assert.fail("User shouldn't be abble to release the collection");
    } catch (Exception e) {
      // OK
    }
    try {
      profileController.update(collectionProfile, user2);
      Assert.fail("User shouldn't be abble to edit the profile");
    } catch (Exception e) {
      // OK
    }
    try {
      profileController.delete(collectionProfile, user2);
      Assert.fail("User shouldn't be abble to delete the profile");
    } catch (Exception e) {
      // OK
    }
    try {
      profileController.release(collectionProfile, user2);
      Assert.fail("User shouldn't be abble to release the profile");
    } catch (Exception e) {
      // OK
    }
  }

  @Test
  public void shareEditCollectionMetadata() throws ImejiException {
    UserService userController = new UserService();
    User user = userController.retrieve(JenaUtil.TEST_USER_EMAIL, Imeji.adminUser);
    User user2 = userController.retrieve(JenaUtil.TEST_USER_EMAIL_2, Imeji.adminUser);
    ShareService controller = new ShareService();
    controller.shareToUser(user, user2, collection.getId().toString(),
        (List<String>) ShareService.rolesAsList(ShareRoles.EDIT));
    ProfileController profileController = new ProfileController();
    CollectionService collectionController = new CollectionService();
    MetadataProfile collectionProfile = null;
    try {
      collectionController.retrieve(collection.getId(), user2);
      collectionController.update(collection, user2);
      collectionProfile = profileController.retrieve(collection.getProfile(), user2);
    } catch (Exception e) {
      Assert.fail(e.getMessage());
    }
    try {
      collectionController.delete(collection, user2);
      Assert.fail("User shouldn't be abble to delete the collection");
    } catch (Exception e) {
      // OK
    }
    try {
      collectionController.releaseWithDefaultLicense(collection, user2);
      Assert.fail("User shouldn't be abble to release the collection");
    } catch (Exception e) {
      // OK
    }
    try {
      profileController.update(collectionProfile, user2);
      Assert.fail("User shouldn't be abble to edit the profile");
    } catch (Exception e) {
      // OK
    }
    try {
      profileController.delete(collectionProfile, user2);
      Assert.fail("User shouldn't be abble to delete the profile");
    } catch (Exception e) {
      // OK
    }
    try {
      profileController.release(collectionProfile, user2);
      Assert.fail("User shouldn't be abble to release the profile");
    } catch (Exception e) {
      // OK
    }
  }

  @Test
  public void shareItem() throws ImejiException {
    ShareService shareController = new ShareService();
    shareController.shareToUser(JenaUtil.testUser, JenaUtil.testUser2, item.getId().toString(),
        ShareService.rolesAsList(ShareRoles.READ));
    ItemService itemController = new ItemService();
    try {
      itemController.retrieve(item.getId(), JenaUtil.testUser2);
    } catch (Exception e) {
      Assert.fail(e.getMessage());
    }
  }

  @Test
  public void unshareCollection() throws ImejiException {
    ShareService shareController = new ShareService();
    // First share...
    shareController.shareToUser(JenaUtil.testUser, JenaUtil.testUser2,
        collection.getId().toString(), ShareService.rolesAsList(ShareRoles.READ));
    // ... then unshare
    shareController.shareToUser(JenaUtil.testUser, JenaUtil.testUser2,
        collection.getId().toString(), new ArrayList<String>());
    CollectionService collectionController = new CollectionService();
    try {
      collectionController.retrieve(collection.getId(), JenaUtil.testUser2);
      Assert.fail("Unshare of collection not working");
    } catch (Exception e) {
      // good
    }
  }

}
