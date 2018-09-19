package de.mpg.imeji.logic.export.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.core.collection.CollectionService;
import de.mpg.imeji.logic.core.content.ContentService;
import de.mpg.imeji.logic.core.item.ItemService;
import de.mpg.imeji.logic.hierarchy.HierarchyService;
import de.mpg.imeji.logic.model.CollectionImeji;
import de.mpg.imeji.logic.model.ContentVO;
import de.mpg.imeji.logic.model.Item;
import de.mpg.imeji.logic.model.User;
import de.mpg.imeji.logic.search.Search;

/**
 * Utility class for export
 * 
 * @author saquet
 *
 */
public class ExportUtil {

  /**
   * Retrieve the items as a Map [itemId,Item]
   * 
   * @param result
   * @return
   * @throws ImejiException
   */
  public static Map<String, Item> retrieveItems(List<String> ids, User user) throws ImejiException {
    final List<Item> items = (List<Item>) new ItemService().retrieveBatchLazy(ids, Search.GET_ALL_RESULTS, Search.SEARCH_FROM_START_INDEX, user);
    Map<String, Item> map = new HashMap<>(items.size());
    for (Item item : items) {
      map.put(item.getId().toString(), item);
    }
    return map;
  }

  /**
   * Retrieve the contents as a map [ContentId, ContentVO]
   * 
   * @param items
   * @return
   * @throws ImejiException
   */
  public static List<ContentVO> retrieveContents(Collection<Item> items) throws ImejiException {
    List<ContentVO> l = new ArrayList<>();
    ContentService service = new ContentService();
    for (Item item : items) {
      ContentVO content = service.retrieveLazy(service.findContentId(item.getId().toString()));
      l.add(content);
    }
    return l;
  }


  /**
   * Return the Path of a collection
   * 
   * @param topParent
   * @param current
   * @param user
   * @return
   * @throws ImejiException
   */
  public static String getPath(CollectionImeji topParent, CollectionImeji current, User user)
      throws ImejiException {
    List<String> parentUris = new HierarchyService().findAllParents(current);
    List<CollectionImeji> parents = new CollectionService().retrieve(parentUris, user);
    String path = "";
    for (CollectionImeji c : parents) {
      path += c.getName() + "/";
    }
    return path + current.getName();
  }

  /**
   * Retrieve the Path of an Item
   * 
   * @param topParent
   * @param item
   * @param user
   * @return
   * @throws ImejiException
   */
  public static String getPath(CollectionImeji topParent, Item item, User user)
      throws ImejiException {
    final CollectionImeji c = new CollectionService().retrieveLazy(item.getCollection(), user);
    return getPath(topParent, c, user) + "/" + item.getFilename();
  }
}
