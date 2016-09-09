package de.mpg.imeji.testimpl.logic.auth;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.collaboration.share.ShareBusinessController;
import de.mpg.imeji.logic.collaboration.share.ShareBusinessController.ShareRoles;
import de.mpg.imeji.logic.controller.resource.CollectionController;
import de.mpg.imeji.logic.controller.resource.ItemController;
import de.mpg.imeji.presentation.beans.ConfigurationBean;
import de.mpg.imeji.presentation.storage.StorageUtil;
import de.mpg.imeji.test.logic.controller.ControllerTest;
import util.JenaUtil;

/**
 * Regression Test for File Authorization
 * 
 * @author bastiens
 *
 */
public class FileAuthorizationTest extends ControllerTest {

  @After
  public void reset() throws IOException, URISyntaxException, ImejiException {
    disabledPrivateMode();
  }

  @Test
  public void notLoggedInReadPrivateItem() throws ImejiException {
    createCollection();
    createItemWithFile();
    Assert.assertFalse(StorageUtil.isAllowedToViewFile(item.getFullImageLink(), null));
    Assert.assertFalse(StorageUtil.isAllowedToViewFile(item.getThumbnailImageLink(), null));
    Assert.assertFalse(StorageUtil.isAllowedToViewFile(item.getWebImageLink(), null));
  }

  @Test
  public void notLoggedInReadPublicItemOfReleasedCollection() throws ImejiException {
    createCollection();
    createItemWithFile();
    releaseCollection();
    Assert.assertTrue(StorageUtil.isAllowedToViewFile(item.getFullImageLink(), null));
    Assert.assertTrue(StorageUtil.isAllowedToViewFile(item.getThumbnailImageLink(), null));
    Assert.assertTrue(StorageUtil.isAllowedToViewFile(item.getWebImageLink(), null));

  }

  @Test
  public void notLoggedInReadPublicItemOfPrivateCollection() throws ImejiException {
    createCollection();
    createItemWithFile();
    releaseItem();
    Assert.assertTrue(StorageUtil.isAllowedToViewFile(item.getFullImageLink(), null));
    Assert.assertTrue(StorageUtil.isAllowedToViewFile(item.getThumbnailImageLink(), null));
    Assert.assertTrue(StorageUtil.isAllowedToViewFile(item.getWebImageLink(), null));
  }

  @Test
  public void loggedInReadPrivateItemOfOwnCollection() throws ImejiException {
    createCollection();
    createItemWithFile();
    Assert.assertTrue(StorageUtil.isAllowedToViewFile(item.getFullImageLink(), JenaUtil.testUser));
    Assert
        .assertTrue(StorageUtil.isAllowedToViewFile(item.getThumbnailImageLink(), JenaUtil.testUser));
    Assert.assertTrue(StorageUtil.isAllowedToViewFile(item.getWebImageLink(), JenaUtil.testUser));
  }

  @Test
  public void loggedInReadPrivateItemOfForbiddenCollection() throws ImejiException {
    createCollection();
    createItemWithFile();
    Assert.assertFalse(StorageUtil.isAllowedToViewFile(item.getFullImageLink(), JenaUtil.testUser2));
    Assert.assertFalse(
        StorageUtil.isAllowedToViewFile(item.getThumbnailImageLink(), JenaUtil.testUser2));
    Assert.assertFalse(StorageUtil.isAllowedToViewFile(item.getWebImageLink(), JenaUtil.testUser2));
  }

  @Test
  public void loggedInReadPrivateItemOfsharedCollection() throws ImejiException {
    createCollection();
    createItemWithFile();
    ShareBusinessController c = new ShareBusinessController();
    c.shareToUser(JenaUtil.testUser, JenaUtil.testUser2, collection.getId().toString(),
        ShareBusinessController.rolesAsList(ShareRoles.READ));
    Assert.assertTrue(StorageUtil.isAllowedToViewFile(item.getFullImageLink(), JenaUtil.testUser2));
    Assert
        .assertTrue(StorageUtil.isAllowedToViewFile(item.getThumbnailImageLink(), JenaUtil.testUser2));
    Assert.assertTrue(StorageUtil.isAllowedToViewFile(item.getWebImageLink(), JenaUtil.testUser2));
  }

  @Test
  public void loggedInReadPrivateItemOfsharedItem() throws ImejiException {
    createCollection();
    createItemWithFile();
    ShareBusinessController c = new ShareBusinessController();
    c.shareToUser(JenaUtil.testUser, JenaUtil.testUser2, item.getId().toString(),
        ShareBusinessController.rolesAsList(ShareRoles.READ));
    Assert.assertTrue(StorageUtil.isAllowedToViewFile(item.getFullImageLink(), JenaUtil.testUser2));
    Assert
        .assertTrue(StorageUtil.isAllowedToViewFile(item.getThumbnailImageLink(), JenaUtil.testUser2));
    Assert.assertTrue(StorageUtil.isAllowedToViewFile(item.getWebImageLink(), JenaUtil.testUser2));
  }

  @Test
  public void loggedInReadPublicItemOfPrivateCollectionInPrivateMode()
      throws IOException, URISyntaxException, ImejiException {
    createCollection();
    createItemWithFile();
    releaseItem();
    enablePrivateMode();
    Assert.assertTrue(StorageUtil.isAllowedToViewFile(item.getFullImageLink(), JenaUtil.testUser2));
    Assert
        .assertTrue(StorageUtil.isAllowedToViewFile(item.getThumbnailImageLink(), JenaUtil.testUser2));
    Assert.assertTrue(StorageUtil.isAllowedToViewFile(item.getWebImageLink(), JenaUtil.testUser2));
  }

  @Test
  public void loggedInReadPublicItemOfReleasedCollectionInPrivateMode()
      throws ImejiException, IOException, URISyntaxException {
    createCollection();
    createItemWithFile();
    releaseCollection();
    enablePrivateMode();
    Assert.assertTrue(StorageUtil.isAllowedToViewFile(item.getFullImageLink(), JenaUtil.testUser2));
    Assert
        .assertTrue(StorageUtil.isAllowedToViewFile(item.getThumbnailImageLink(), JenaUtil.testUser2));
    Assert.assertTrue(StorageUtil.isAllowedToViewFile(item.getWebImageLink(), JenaUtil.testUser2));
  }

  private void releaseCollection() throws ImejiException {
    CollectionController c = new CollectionController();
    c.release(collection, JenaUtil.testUser);
  }

  private void releaseItem() throws ImejiException {
    ItemController c = new ItemController();
    c.release(Arrays.asList(item), JenaUtil.testUser);
  }

  private void enablePrivateMode() throws IOException, URISyntaxException, ImejiException {
    ConfigurationBean configurationBean = new ConfigurationBean();
    configurationBean.setPrivateModus(true);
  }

  private void disabledPrivateMode() throws IOException, URISyntaxException, ImejiException {
    ConfigurationBean configurationBean = new ConfigurationBean();
    configurationBean.setPrivateModus(false);
  }
}
