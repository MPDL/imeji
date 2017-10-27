package de.mpg.imeji.logic.core.facade;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.core.collection.CollectionService;
import de.mpg.imeji.logic.core.item.ItemService;
import de.mpg.imeji.logic.model.CollectionImeji;
import de.mpg.imeji.logic.model.CollectionObject;
import de.mpg.imeji.logic.model.Item;
import de.mpg.imeji.logic.model.User;
import de.mpg.imeji.logic.model.factory.ImejiFactory;
import de.mpg.imeji.logic.search.Search;
import de.mpg.imeji.logic.search.Search.SearchObjectTypes;
import de.mpg.imeji.logic.search.elasticsearch.ElasticSearch;
import de.mpg.imeji.logic.search.model.SearchQuery;
import de.mpg.imeji.logic.search.model.SearchResult;
import de.mpg.imeji.logic.search.model.SortCriterion;
import de.mpg.imeji.logic.util.ObjectHelper;

/**
 * Facade to search and retrieve {@link Item} and {@link Collection} in one request
 * 
 * @author saquet
 *
 */
public class SearchAndRetrieveFacade implements Serializable {
  private static final long serialVersionUID = 105851155975646142L;
  private final Search search =
      new ElasticSearch(SearchObjectTypes.ITEM, SearchObjectTypes.COLLECTION);
  private final ItemService itemService = new ItemService();
  private final CollectionService collectionService = new CollectionService();


  /**
   * Search for all objects within the collection. Items and Sub-Collections are mixed
   * 
   * @param q
   * @param collection
   * @param user
   * @param sortCri
   * @param size
   * @param offset
   * @return
   */
  public SearchResult search(SearchQuery q, CollectionImeji collection, User user,
      SortCriterion sortCri, int size, int offset) {
    return search.searchWithFacets(q, sortCri, user, collection.getId().toString(), offset, size);
  }

  /**
   * Retrieve all objects of the uris list, which can be a mixed of items and collections
   * 
   * @param uris
   * @param user
   * @return
   * @throws ImejiException
   */
  public List<CollectionObject> retrieveItemsAndCollections(List<String> uris, User user)
      throws ImejiException {
    // Split into 2 lists: one list of collection, one of items
    final Map<Boolean, List<String>> resultMap =
        uris.stream().collect(Collectors.partitioningBy(s -> isCollectionUri(s)));
    // Retrieve objects of both list
    final List<CollectionImeji> subCollections =
        collectionService.retrieve(resultMap.get(true), user);
    final List<Item> items = itemService.retrieve(resultMap.get(false), user);
    // Merge the list
    return mergeAndOrder(uris, subCollections, items);
  }

  /**
   * Retrieve all objects of the uris list, which can be a mixed of items and collections. Return
   * the result as a list of item by transforming subcollections to pseudo items
   * 
   * @param uris
   * @param user
   * @return
   * @throws ImejiException
   */
  public List<Item> retrieveItemsAndCollectionsAsItems(List<String> uris, User user)
      throws ImejiException {
    // Split into 2 lists: one list of collection, one of items
    final Map<Boolean, List<String>> resultMap =
        uris.stream().collect(Collectors.partitioningBy(s -> isCollectionUri(s)));
    // Retrieve objects of both list
    final List<CollectionImeji> subCollections =
        collectionService.retrieve(resultMap.get(true), user);
    final List<Item> items = itemService.retrieve(resultMap.get(false), user);
    // Merge the list
    return mergeAndOrderAsItem(uris, subCollections, items);
  }


  /**
   * Merge the subcollection with the items and order them according to the order of uris
   * 
   * @param uris
   * @param subCollections
   * @param items
   * @return
   */
  private List<CollectionObject> mergeAndOrder(List<String> uris,
      List<CollectionImeji> subCollections, List<Item> items) {
    List<CollectionObject> l = new ArrayList<>(items);
    l.addAll(subCollections);
    Collections.sort(l, new Comparator<CollectionObject>() {
      @Override
      public int compare(CollectionObject o1, CollectionObject o2) {
        return Integer.compare(uris.indexOf(o1.getUri()), uris.indexOf(o2.getUri()));
      }
    });
    return l;
  }

  /**
   * Merge the subcollections with the items and order them according to the order of uris. return
   * the result as a list of item by transforming subcollections to pseudo items
   * 
   * @param uris
   * @param subCollections
   * @param items
   * @return
   */
  private List<Item> mergeAndOrderAsItem(List<String> uris, List<CollectionImeji> subCollections,
      List<Item> items) {
    items.addAll(toItemList(subCollections));
    Collections.sort(items, new Comparator<Item>() {
      @Override
      public int compare(Item o1, Item o2) {
        return Integer.compare(uris.indexOf(o1.getUri()), uris.indexOf(o2.getUri()));
      }
    });
    return items;
  }

  private List<Item> toItemList(List<CollectionImeji> collections) {
    return collections.stream().map(c -> toItem(c)).collect(Collectors.toList());
  }

  private Item toItem(CollectionImeji c) {
    return ImejiFactory.newItem().setId(c.getId().toString())
        .setCollection(ObjectHelper.getId(c.getCollection())).setFilename(c.getTitle()).build();
  }

  /**
   * True if the uri is a collection uri
   * 
   * @param s
   * @return
   */
  private boolean isCollectionUri(String s) {
    return s.contains("/collection/");
  }

}
