package de.mpg.imeji.presentation.storage;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.controller.resource.AlbumController;
import de.mpg.imeji.logic.controller.resource.CollectionController;
import de.mpg.imeji.logic.controller.resource.ItemController;
import de.mpg.imeji.logic.controller.resource.SpaceController;
import de.mpg.imeji.logic.storage.StorageController;
import de.mpg.imeji.logic.util.ObjectHelper;
import de.mpg.imeji.logic.vo.Album;
import de.mpg.imeji.logic.vo.CollectionImeji;
import de.mpg.imeji.logic.vo.Item;
import de.mpg.imeji.logic.vo.User;

/**
 * Utility Class for storage presentation module
 * 
 * @author saquet
 *
 */
public class StorageUtil {

  public static final StorageController STORAGE_CONTROLLER = new StorageController();

  /**
   * True if the user is allowed to view this file
   *
   * @param fileUrl
   * @param user
   * @return
   */
  public static boolean isAllowedToViewFile(String fileUrl, User user) {
    if (StorageUtil.isSpaceUrl(fileUrl)) {
      // For space Logos do not check any security (spaces are always public)
      return true;
    }
    return StorageUtil.isAllowedToViewItemOfFile(fileUrl, user)
        || isAllowedToViewCollectionOfFile(fileUrl, user)
        || isAllowedToViewAlbumOfFile(fileUrl, user);
  }

  /**
   * True if the fileUrl is associated to a {@link Item} which can be read by the user
   *
   * @param fileUrl
   * @param user
   * @return
   */
  private static boolean isAllowedToViewItemOfFile(String fileUrl, User user) {
    try {
      new ItemController()
          .retrieveLazyForFile(STORAGE_CONTROLLER.getStorage().getStorageId(fileUrl), user);
      return true;
    } catch (ImejiException e) {
      return false;
    }
  }

  /**
   * True if the fileurl is associated to {@link CollectionImeji} which can be read by the user
   * (usefull for collection logos)
   *
   * @param fileUrl
   * @param user
   * @return
   */
  private static boolean isAllowedToViewCollectionOfFile(String fileUrl, User user) {
    try {
      String collectionId = STORAGE_CONTROLLER.getCollectionId(fileUrl);
      new CollectionController().retrieve(ObjectHelper.getURI(CollectionImeji.class, collectionId),
          user);
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  /**
   * True if the filerurl is associated an {@link Album} which can be read by the user (usefull for
   * album logos)
   *
   * @param fileUrl
   * @param user
   * @return
   */
  private static boolean isAllowedToViewAlbumOfFile(String fileUrl, User user) {
    String albumId = STORAGE_CONTROLLER.getCollectionId(fileUrl);
    try {
      new AlbumController().retrieve(ObjectHelper.getURI(Album.class, albumId), user);
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  /**
   * True if the file is the logo of a space
   *
   * @param url
   * @return
   */
  public static boolean isSpaceUrl(String url) {
    return new SpaceController().isSpaceLogoURL(url);
  }


}
