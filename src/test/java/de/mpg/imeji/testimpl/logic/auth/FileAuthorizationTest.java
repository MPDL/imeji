package de.mpg.imeji.testimpl.logic.auth;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.auth.util.AuthUtil;
import de.mpg.imeji.logic.controller.CollectionController;
import de.mpg.imeji.logic.controller.ItemController;
import de.mpg.imeji.logic.controller.ShareController;
import de.mpg.imeji.logic.controller.ShareController.ShareRoles;
import de.mpg.imeji.presentation.beans.ConfigurationBean;
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
    Assert.assertFalse(AuthUtil.isAllowedToViewFile(item.getFullImageLink(), null));
    Assert.assertFalse(AuthUtil.isAllowedToViewFile(item.getThumbnailImageLink(), null));
    Assert.assertFalse(AuthUtil.isAllowedToViewFile(item.getWebImageLink(), null));
  }

  @Test
  public void notLoggedInReadPublicItemOfReleasedCollection() throws ImejiException {
    createCollection();
    createItemWithFile();
    releaseCollection();
    Assert.assertTrue(AuthUtil.isAllowedToViewFile(item.getFullImageLink(), null));
    Assert.assertTrue(AuthUtil.isAllowedToViewFile(item.getThumbnailImageLink(), null));
    Assert.assertTrue(AuthUtil.isAllowedToViewFile(item.getWebImageLink(), null));

  }

  @Test
  public void notLoggedInReadPublicItemOfPrivateCollection() throws ImejiException {
    createCollection();
    createItemWithFile();
    releaseItem();
    Assert.assertTrue(AuthUtil.isAllowedToViewFile(item.getFullImageLink(), null));
    Assert.assertTrue(AuthUtil.isAllowedToViewFile(item.getThumbnailImageLink(), null));
    Assert.assertTrue(AuthUtil.isAllowedToViewFile(item.getWebImageLink(), null));
  }

  @Test
  public void loggedInReadPrivateItemOfOwnCollection() throws ImejiException {
    createCollection();
    createItemWithFile();
    Assert.assertTrue(AuthUtil.isAllowedToViewFile(item.getFullImageLink(), JenaUtil.testUser));
    Assert
        .assertTrue(AuthUtil.isAllowedToViewFile(item.getThumbnailImageLink(), JenaUtil.testUser));
    Assert.assertTrue(AuthUtil.isAllowedToViewFile(item.getWebImageLink(), JenaUtil.testUser));
  }

  @Test
  public void loggedInReadPrivateItemOfForbiddenCollection() throws ImejiException {
    createCollection();
    createItemWithFile();
    Assert.assertFalse(AuthUtil.isAllowedToViewFile(item.getFullImageLink(), JenaUtil.testUser2));
    Assert.assertFalse(
        AuthUtil.isAllowedToViewFile(item.getThumbnailImageLink(), JenaUtil.testUser2));
    Assert.assertFalse(AuthUtil.isAllowedToViewFile(item.getWebImageLink(), JenaUtil.testUser2));
  }

  @Test
  public void loggedInReadPrivateItemOfsharedCollection() throws ImejiException {
    createCollection();
    createItemWithFile();
    ShareController c = new ShareController();
    c.shareToUser(JenaUtil.testUser, JenaUtil.testUser2, collection.getId().toString(),
        ShareController.rolesAsList(ShareRoles.READ));
    Assert.assertTrue(AuthUtil.isAllowedToViewFile(item.getFullImageLink(), JenaUtil.testUser2));
    Assert
        .assertTrue(AuthUtil.isAllowedToViewFile(item.getThumbnailImageLink(), JenaUtil.testUser2));
    Assert.assertTrue(AuthUtil.isAllowedToViewFile(item.getWebImageLink(), JenaUtil.testUser2));
  }

  @Test
  public void loggedInReadPrivateItemOfsharedItem() throws ImejiException {
    createCollection();
    createItemWithFile();
    ShareController c = new ShareController();
    c.shareToUser(JenaUtil.testUser, JenaUtil.testUser2, item.getId().toString(),
        ShareController.rolesAsList(ShareRoles.READ));
    Assert.assertTrue(AuthUtil.isAllowedToViewFile(item.getFullImageLink(), JenaUtil.testUser2));
    Assert
        .assertTrue(AuthUtil.isAllowedToViewFile(item.getThumbnailImageLink(), JenaUtil.testUser2));
    Assert.assertTrue(AuthUtil.isAllowedToViewFile(item.getWebImageLink(), JenaUtil.testUser2));
  }

  @Test
  public void loggedInReadPublicItemOfPrivateCollectionInPrivateMode()
      throws IOException, URISyntaxException, ImejiException {
    createCollection();
    createItemWithFile();
    releaseItem();
    enablePrivateMode();
    Assert.assertTrue(AuthUtil.isAllowedToViewFile(item.getFullImageLink(), JenaUtil.testUser2));
    Assert
        .assertTrue(AuthUtil.isAllowedToViewFile(item.getThumbnailImageLink(), JenaUtil.testUser2));
    Assert.assertTrue(AuthUtil.isAllowedToViewFile(item.getWebImageLink(), JenaUtil.testUser2));
  }

  @Test
  public void loggedInReadPublicItemOfReleasedCollectionInPrivateMode()
      throws ImejiException, IOException, URISyntaxException {
    createCollection();
    createItemWithFile();
    releaseCollection();
    enablePrivateMode();
    Assert.assertTrue(AuthUtil.isAllowedToViewFile(item.getFullImageLink(), JenaUtil.testUser2));
    Assert
        .assertTrue(AuthUtil.isAllowedToViewFile(item.getThumbnailImageLink(), JenaUtil.testUser2));
    Assert.assertTrue(AuthUtil.isAllowedToViewFile(item.getWebImageLink(), JenaUtil.testUser2));
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
