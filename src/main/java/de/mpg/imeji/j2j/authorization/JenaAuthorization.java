package de.mpg.imeji.j2j.authorization;

import java.net.URI;
import java.util.LinkedList;
import java.util.List;

import org.apache.jena.query.Dataset;

import de.mpg.imeji.exceptions.NotFoundException;
import de.mpg.imeji.j2j.queries.Queries;
import de.mpg.imeji.logic.init.ImejiInitializer;
import de.mpg.imeji.logic.model.CollectionImeji;
import de.mpg.imeji.logic.model.Item;
import de.mpg.imeji.logic.security.authorization.Authorization;
import de.mpg.imeji.logic.util.ObjectHelper;
import de.mpg.imeji.logic.util.ObjectHelper.ObjectType;


/**
 * (Re-) Checks authorization during Jena transactions. JenaAuthorization extends Authorization and
 * uses the same authorization rules as Authorization.
 * 
 * Only Difference: When searching the top-level parent collection (URI) of Item and CollectionImeji
 * object, JenaAuthorization does this via a direct Jena query while Authorization uses
 * HierarchyService.
 * 
 * JenaAuthorization must only be used from within Jena transactions.
 * 
 * @author breddin
 *
 */

public class JenaAuthorization extends Authorization {

  private static final String X_PATH_FUNCTIONS_DECLARATION = " PREFIX fn: <http://www.w3.org/2005/xpath-functions#> ";
  private Dataset dataset;

  public JenaAuthorization(Dataset dataset) {
    this.dataset = dataset;
  }


  private static final long serialVersionUID = 6107445425666600545L;


  /**
   * Return the URI of the top-level parent collection of an Item or CollectionImeji object. Returns
   * null for all other objects.
   * 
   * @param obj
   * @return URI of top-level collection
   * @throws NotFoundException
   */
  @Override
  protected String getLastParent(Object obj) throws NotFoundException {

    if (obj instanceof Item) {
      Item item = (Item) obj;
      if (item.getId() == null) {
        throw new NotFoundException("Could not find item, item has no id");
      }
      return queryItemsTopLevelParent(item.getId().toString());

    } else if (obj instanceof CollectionImeji) {
      CollectionImeji collection = (CollectionImeji) obj;
      if (collection.getId() == null) {
        throw new NotFoundException("Could not find collection, collection has no id");
      }
      return queryCollectionsTopLevelParent(collection.getId().toString());
    }

    return null;
  }


  /**
   * If the given URI String contains the URI an Item or CollectionImeji object, function returns
   * the URI of the top-level collection of the given item or collection. Returns null otherwise.
   * 
   * @param uriString URI
   * @throws NotFoundException
   */
  @Override
  protected String getLastParent(String uriString) throws NotFoundException {

    ObjectType type = ObjectHelper.getObjectType(URI.create(uriString));
    if (type == ObjectType.ITEM) {
      return queryItemsTopLevelParent(uriString);
    } else if (type == ObjectType.COLLECTION) {
      return queryCollectionsTopLevelParent(uriString);
    }

    return null;
  }

  /**
   * Find the URI of the top-level parent of an item. The top level parent of an item is the top
   * most collection that contains the item. If the item is in a sub collection, then the parent
   * collection of that sub collection and its parent, if there exists one and so on.
   * 
   * @param itemURI
   * @return
   * @throws NotFoundException
   */
  private String queryItemsTopLevelParent(String itemURI) throws NotFoundException {


    String itemModelName = ImejiInitializer.getModelName(Item.class);
    String sparqlQuery = X_PATH_FUNCTIONS_DECLARATION + "SELECT ?s WHERE {<" + itemURI + "> <http://imeji.org/terms/collection> ?s}";

    List<String> itemsParent = Queries.executeSPARQLQueryAndGetResults(sparqlQuery, this.dataset, itemModelName);

    if (itemsParent.size() == 1) {
      String parentURI = itemsParent.get(0);
      // check if item's collection is subcollection and has a top-level parent itself
      String topLevelParentURI = queryCollectionsTopLevelParent(parentURI);
      return topLevelParentURI;
    } else {
      // throw error: item must have parent, item not found or other problem, abort
      throw new NotFoundException("Could not find " + itemURI);
    }
  }

  /**
   * Find the URI of the top-level collection of a (sub-) collection. If the collection is a sub
   * collection function returns its top-most parent collection. If the collection is not a sub
   * collection function returns given collection's URI.
   * 
   * @param collectionURI
   * @return top-level parent URI
   */
  private String queryCollectionsTopLevelParent(String collectionURI) {

    String sparqlQuery = X_PATH_FUNCTIONS_DECLARATION + "SELECT ?s WHERE {<" + collectionURI + "> <http://imeji.org/terms/collection>+ ?s}";
    String collectionModelName = ImejiInitializer.getModelName(CollectionImeji.class);

    LinkedList<String> parentURIs = Queries.executeSPARQLQueryAndGetResults(sparqlQuery, this.dataset, collectionModelName);

    if (parentURIs.size() > 0) {
      // top level parent is at end of list of parents
      String topLevelParentURI = parentURIs.getLast();
      return topLevelParentURI;
    } else {
      // in case no parent collection was found the given collection is no sub collection and  
      // top level parent id is its own id
      return collectionURI;
    }

  }


}
