package de.mpg.imeji.presentation.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.log4j.Logger;

import de.mpg.imeji.logic.vo.Album;
import de.mpg.imeji.logic.vo.CollectionImeji;
import de.mpg.imeji.logic.vo.Item;
import de.mpg.imeji.logic.vo.User;
import de.mpg.imeji.presentation.album.AlbumBean;
import de.mpg.imeji.presentation.collection.CollectionListItem;
import de.mpg.imeji.presentation.item.ThumbnailBean;

/**
 * Utility class for List
 *
 * @author bastiens
 *
 */
public class ListUtils {
  private static final Logger LOGGER = Logger.getLogger(ListUtils.class);

  private ListUtils() {
    // private Constructor
  }

  /**
   * Compare 2 arrays ignoring order, i.e. both arrays are compared in their natural order
   *
   * @param l1
   * @param l2
   */
  public static boolean equalsIgnoreOrder(List<String> l1, List<String> l2) {
    l1.sort(null);
    l2.sort(null);
    return l1.equals(l2);
  }

  /**
   * Transform a {@link List} of {@link Item} to a {@link List} of {@link ThumbnailBean}
   *
   * @param itemList
   * @return
   */
  public static List<ThumbnailBean> itemListToThumbList(Collection<Item> itemList) {
    List<ThumbnailBean> beanList = new ArrayList<ThumbnailBean>();
    for (Item img : itemList) {
      try {
        beanList.add(new ThumbnailBean(img, true));
      } catch (Exception e) {
        LOGGER.error("Error creating ThumbnailBean list", e);
      }
    }
    return beanList;
  }

  /**
   * Transform a {@link List} of {@link Album} to a {@link List} of {@link AlbumBean}
   *
   * @param albumList
   * @return
   * @throws Exception
   */
  public static List<AlbumBean> albumListToAlbumBeanList(Collection<Album> albumList, User user,
      Album activeAlbum) throws Exception {
    List<AlbumBean> beanList = new ArrayList<AlbumBean>();
    for (Album album : albumList) {
      beanList.add(new AlbumBean(album, user, activeAlbum));
    }
    return beanList;
  }

  /**
   * Transform a {@link List} of {@link CollectionImeji} to a {@link List} of
   * {@link CollectionListItem}
   *
   * @param collList
   * @param user
   * @return
   */
  public static List<CollectionListItem> collectionListToListItem(
      Collection<CollectionImeji> collList, User user) {
    List<CollectionListItem> l = new ArrayList<CollectionListItem>();
    if (collList != null) {
      for (CollectionImeji c : collList) {
        l.add(new CollectionListItem(c, user));
      }
    }
    return l;
  }
}
