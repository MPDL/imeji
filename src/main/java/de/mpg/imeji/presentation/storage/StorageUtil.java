package de.mpg.imeji.presentation.storage;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.collection.CollectionService;
import de.mpg.imeji.logic.item.ItemService;
import de.mpg.imeji.logic.storage.StorageController;
import de.mpg.imeji.logic.util.ObjectHelper;
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
    return StorageUtil.isAllowedToViewItemOfFile(fileUrl, user)
        || isAllowedToViewCollectionOfFile(fileUrl, user);
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
      new ItemService().retrieveLazyForFile(fileUrl, user);
      return true;
    } catch (final ImejiException e) {
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
      final String collectionId = STORAGE_CONTROLLER.getCollectionId(fileUrl);
      new CollectionService().retrieve(ObjectHelper.getURI(CollectionImeji.class, collectionId),
          user);
      return true;
    } catch (final Exception e) {
      return false;
    }
  }
}
