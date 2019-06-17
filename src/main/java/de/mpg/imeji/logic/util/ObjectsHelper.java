package de.mpg.imeji.logic.util;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import de.mpg.imeji.logic.model.CollectionImeji;
import de.mpg.imeji.logic.model.ContentVO;
import de.mpg.imeji.logic.model.Item;
import de.mpg.imeji.logic.model.Statement;
import de.mpg.imeji.logic.model.Subscription;
import de.mpg.imeji.logic.model.User;
import de.mpg.imeji.logic.model.UserGroup;
import de.mpg.imeji.logic.search.facet.model.Facet;

/**
 * Provides utility functions for handling lists of imeji data objects.
 * 
 * @author breddin
 *
 */
public class ObjectsHelper {

  /**
   * Given a list of imeji data objects of various types (i.e. items, collections, statements, etc.), create a map that 
   * groups the data objects by their classes and provides access to the objects via their class. 
   * 
   * Map's key is object class, maps's value is list of all given data objects of that class.
   * 
   * @param dataObjects unsorted list of imeji data objects
   * @return map<data object class, list of data objects with that class>
   */
  public static Map<Class<?>, List<Object>> createTypedObjectMap(List<Object> dataObjects) {

    Map<Class<?>, List<Object>> typedDataObjectList = new HashMap<Class<?>, List<Object>>();

    for (Object dataObject : dataObjects) {

      if (dataObject instanceof Item) {
        addToMap(Item.class, dataObject, typedDataObjectList);
      } else if (dataObject instanceof CollectionImeji) {
        addToMap(CollectionImeji.class, dataObject, typedDataObjectList);
      } else if (dataObject instanceof User) {
        addToMap(User.class, dataObject, typedDataObjectList);
      } else if (dataObject instanceof UserGroup) {
        addToMap(UserGroup.class, dataObject, typedDataObjectList);
      } else if (dataObject instanceof ContentVO) {
        addToMap(ContentVO.class, dataObject, typedDataObjectList);
      } else if (dataObject instanceof Subscription) {
        addToMap(Subscription.class, dataObject, typedDataObjectList);
      } else if (dataObject instanceof Statement) {
        addToMap(Statement.class, dataObject, typedDataObjectList);
      } else if (dataObject instanceof Facet) {
        addToMap(Facet.class, dataObject, typedDataObjectList);
      } else {
        // throw error
      }
    }
    return typedDataObjectList;
  }


  private static void addToMap(Class<?> objectType, Object dataObject, Map<Class<?>, List<Object>> map) {

    if (map.containsKey(objectType)) {
      List<Object> values = map.get(objectType);
      values.add(dataObject);
      map.put(objectType, values);
    } else {
      List<Object> values = new LinkedList<Object>();
      values.add(dataObject);
      map.put(objectType, values);

    }


  }

}
