package de.mpg.imeji.testimpl.logic.controller;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.Imeji;
import de.mpg.imeji.logic.controller.CollectionController;
import de.mpg.imeji.logic.controller.ItemController;
import de.mpg.imeji.logic.controller.ProfileController;
import de.mpg.imeji.logic.controller.ShareController;
import de.mpg.imeji.logic.controller.ShareController.ShareRoles;
import de.mpg.imeji.logic.controller.UserController;
import de.mpg.imeji.logic.vo.MetadataProfile;
import de.mpg.imeji.logic.vo.User;
import de.mpg.imeji.test.logic.controller.ControllerTest;
import util.JenaUtil;

/**
 * Test the {@link ShareController}
 * 
 * @author saquet
 *
 */
public class ShareControllerTestClass extends ControllerTest {

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
    UserController userController = new UserController(Imeji.adminUser);
    User user = userController.retrieve(JenaUtil.TEST_USER_EMAIL);
    User user2 = userController.retrieve(JenaUtil.TEST_USER_EMAIL_2);
    ShareController controller = new ShareController();
    controller.shareToUser(user, user2, profile.getId().toString(),
        (List<String>) ShareController.rolesAsList(ShareRoles.EDIT));
    ProfileController profileController = new ProfileController();

    try {
      profileController.update(profile, user2);
      profileController.retrieve(profile.getId(), user2);
    } catch (Exception e) {
      Assert.fail(e.getMessage());
    }
    try {
      CollectionController collectionController = new CollectionController();
      profileController.delete(profile, user2);
      Assert.fail("User shouldn't be abble to delete the profile");
      profileController.release(profile, user2);
      Assert.fail("User shouldn't be abble to release the profile");
      collectionController.delete(collection, user2);
      Assert.fail("User shouldn't be abble to delete the collection");
      collectionController.release(collection, user2);
      Assert.fail("User shouldn't be abble to release the collection");
      profileController.update(profile, user2);
    } catch (Exception e) {
      // OK
    }
  }

  @Test
  public void shareReadCollection() throws ImejiException {
    UserController userController = new UserController(Imeji.adminUser);
    User user = userController.retrieve(JenaUtil.TEST_USER_EMAIL);
    User user2 = userController.retrieve(JenaUtil.TEST_USER_EMAIL_2);
    ShareController controller = new ShareController();
    controller.shareToUser(user, user2, collection.getId().toString(),
        (List<String>) ShareController.rolesAsList(ShareRoles.READ));
    ProfileController profileController = new ProfileController();
    CollectionController collectionController = new CollectionController();
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
      collectionController.release(collection, user2);
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
    UserController userController = new UserController(Imeji.adminUser);
    User user = userController.retrieve(JenaUtil.TEST_USER_EMAIL);
    User user2 = userController.retrieve(JenaUtil.TEST_USER_EMAIL_2);
    ShareController controller = new ShareController();
    controller.shareToUser(user, user2, collection.getId().toString(),
        (List<String>) ShareController.rolesAsList(ShareRoles.EDIT));
    ProfileController profileController = new ProfileController();
    CollectionController collectionController = new CollectionController();
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
      collectionController.release(collection, user2);
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
    ShareController shareController = new ShareController();
    shareController.shareToUser(JenaUtil.testUser, JenaUtil.testUser2, item.getId().toString(),
        ShareController.rolesAsList(ShareRoles.READ));
    ItemController itemController = new ItemController();
    try {
      itemController.retrieve(item.getId(), JenaUtil.testUser2);
    } catch (Exception e) {
      Assert.fail(e.getMessage());
    }
  }

  @Test
  public void unshareCollection() throws ImejiException {
    ShareController shareController = new ShareController();
    // First share...
    shareController.shareToUser(JenaUtil.testUser, JenaUtil.testUser2,
        collection.getId().toString(), ShareController.rolesAsList(ShareRoles.READ));
    // ... then unshare
    shareController.shareToUser(JenaUtil.testUser, JenaUtil.testUser2,
        collection.getId().toString(), new ArrayList<String>());
    CollectionController collectionController = new CollectionController();
    try {
      collectionController.retrieve(collection.getId(), JenaUtil.testUser2);
      Assert.fail("Unshare of collection not working");
    } catch (Exception e) {
      // good
    }
  }

}
