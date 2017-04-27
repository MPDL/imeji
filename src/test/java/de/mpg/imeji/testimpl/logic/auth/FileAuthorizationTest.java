package de.mpg.imeji.testimpl.logic.auth;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.collection.CollectionService;
import de.mpg.imeji.logic.content.ContentService;
import de.mpg.imeji.logic.item.ItemService;
import de.mpg.imeji.logic.share.ShareService;
import de.mpg.imeji.logic.share.ShareService.ShareRoles;
import de.mpg.imeji.logic.vo.ContentVO;
import de.mpg.imeji.logic.vo.Item;
import de.mpg.imeji.presentation.admin.ConfigurationBean;
import de.mpg.imeji.presentation.storage.StorageUtil;
import de.mpg.imeji.test.logic.service.SuperServiceTest;
import de.mpg.imeji.util.JenaUtil;

/**
 * Regression Test for File Authorization
 * 
 * @author bastiens
 *
 */
public class FileAuthorizationTest extends SuperServiceTest {

  @After
  public void reset() throws IOException, URISyntaxException, ImejiException {
    disabledPrivateMode();
  }

  @Test
  public void notLoggedInReadPrivateItem() throws ImejiException {
    createCollection();
    createItemWithFile();
    Assert.assertFalse(StorageUtil.isAllowedToViewFile(getContent(item).getFull(), null));
    Assert.assertFalse(StorageUtil.isAllowedToViewFile(getContent(item).getThumbnail(), null));
    Assert.assertFalse(StorageUtil.isAllowedToViewFile(getContent(item).getPreview(), null));
    Assert.assertFalse(StorageUtil.isAllowedToViewFile(getContent(item).getOriginal(), null));
  }

  @Test
  public void notLoggedInReadPublicItemOfReleasedCollection() throws ImejiException {
    createCollection();
    createItemWithFile();
    releaseCollection();
    Assert.assertTrue(StorageUtil.isAllowedToViewFile(getContent(item).getFull(), null));
    Assert.assertTrue(StorageUtil.isAllowedToViewFile(getContent(item).getThumbnail(), null));
    Assert.assertTrue(StorageUtil.isAllowedToViewFile(getContent(item).getPreview(), null));
    Assert.assertFalse(StorageUtil.isAllowedToViewFile(getContent(item).getOriginal(), null));

  }

  @Test
  public void notLoggedInReadPublicItemOfPrivateCollection() throws ImejiException {
    createCollection();
    createItemWithFile();
    releaseItem();
    Assert.assertTrue(StorageUtil.isAllowedToViewFile(getContent(item).getFull(), null));
    Assert.assertTrue(StorageUtil.isAllowedToViewFile(getContent(item).getThumbnail(), null));
    Assert.assertTrue(StorageUtil.isAllowedToViewFile(getContent(item).getPreview(), null));
    Assert.assertFalse(StorageUtil.isAllowedToViewFile(getContent(item).getOriginal(), null));
  }

  @Test
  public void loggedInReadPrivateItemOfOwnCollection() throws ImejiException {
    createCollection();
    createItemWithFile();
    Assert
        .assertTrue(StorageUtil.isAllowedToViewFile(getContent(item).getFull(), JenaUtil.testUser));
    Assert.assertTrue(
        StorageUtil.isAllowedToViewFile(getContent(item).getThumbnail(), JenaUtil.testUser));
    Assert.assertTrue(
        StorageUtil.isAllowedToViewFile(getContent(item).getPreview(), JenaUtil.testUser));
  }

  @Test
  public void loggedInReadPrivateItemOfForbiddenCollection() throws ImejiException {
    createCollection();
    createItemWithFile();
    Assert.assertFalse(
        StorageUtil.isAllowedToViewFile(getContent(item).getFull(), JenaUtil.testUser2));
    Assert.assertFalse(
        StorageUtil.isAllowedToViewFile(getContent(item).getThumbnail(), JenaUtil.testUser2));
    Assert.assertFalse(
        StorageUtil.isAllowedToViewFile(getContent(item).getPreview(), JenaUtil.testUser2));
  }

  @Test
  public void loggedInReadPrivateItemOfsharedCollection() throws ImejiException {
    createCollection();
    createItemWithFile();
    ShareService c = new ShareService();
    c.shareToUser(JenaUtil.testUser, JenaUtil.testUser2, collectionBasic.getId().toString(),
        ShareRoles.READ.name());
    Assert.assertTrue(
        StorageUtil.isAllowedToViewFile(getContent(item).getFull(), JenaUtil.testUser2));
    Assert.assertTrue(
        StorageUtil.isAllowedToViewFile(getContent(item).getThumbnail(), JenaUtil.testUser2));
    Assert.assertTrue(
        StorageUtil.isAllowedToViewFile(getContent(item).getPreview(), JenaUtil.testUser2));
  }

  @Test
  public void loggedInReadPrivateItemOfsharedItem() throws ImejiException {
    createCollection();
    createItemWithFile();
    ShareService c = new ShareService();
    c.shareToUser(JenaUtil.testUser, JenaUtil.testUser2, getContent(item).getId().toString(),
        ShareRoles.READ.name());
    Assert.assertTrue(
        StorageUtil.isAllowedToViewFile(getContent(item).getFull(), JenaUtil.testUser2));
    Assert.assertTrue(
        StorageUtil.isAllowedToViewFile(getContent(item).getThumbnail(), JenaUtil.testUser2));
    Assert.assertTrue(
        StorageUtil.isAllowedToViewFile(getContent(item).getPreview(), JenaUtil.testUser2));
  }

  @Test
  public void loggedInReadPublicItemOfPrivateCollectionInPrivateMode()
      throws IOException, URISyntaxException, ImejiException {
    createCollection();
    createItemWithFile();
    releaseItem();
    enablePrivateMode();
    Assert.assertTrue(
        StorageUtil.isAllowedToViewFile(getContent(item).getFull(), JenaUtil.testUser2));
    Assert.assertTrue(
        StorageUtil.isAllowedToViewFile(getContent(item).getThumbnail(), JenaUtil.testUser2));
    Assert.assertTrue(
        StorageUtil.isAllowedToViewFile(getContent(item).getPreview(), JenaUtil.testUser2));
  }

  @Test
  public void loggedInReadPublicItemOfReleasedCollectionInPrivateMode()
      throws ImejiException, IOException, URISyntaxException {
    createCollection();
    createItemWithFile();
    releaseCollection();
    enablePrivateMode();
    Assert.assertTrue(
        StorageUtil.isAllowedToViewFile(getContent(item).getFull(), JenaUtil.testUser2));
    Assert.assertTrue(
        StorageUtil.isAllowedToViewFile(getContent(item).getThumbnail(), JenaUtil.testUser2));
    Assert.assertTrue(
        StorageUtil.isAllowedToViewFile(getContent(item).getPreview(), JenaUtil.testUser2));
  }

  private void releaseCollection() throws ImejiException {
    CollectionService c = new CollectionService();
    c.releaseWithDefaultLicense(collectionBasic, JenaUtil.testUser);
  }

  private void releaseItem() throws ImejiException {
    ItemService c = new ItemService();
    c.releaseWithDefaultLicense(Arrays.asList(item), JenaUtil.testUser);
  }

  private void enablePrivateMode() throws IOException, URISyntaxException, ImejiException {
    ConfigurationBean configurationBean = new ConfigurationBean();
    configurationBean.setPrivateModus(true);
  }

  private void disabledPrivateMode() throws IOException, URISyntaxException, ImejiException {
    ConfigurationBean configurationBean = new ConfigurationBean();
    configurationBean.setPrivateModus(false);
  }

  private ContentVO getContent(Item i) throws ImejiException {
    ContentService service = new ContentService();
    String id = service.findContentId(i.getId().toString());
    return service.retrieve(id);
  }
}
