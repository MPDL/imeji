package de.mpg.imeji.logic.core.facade;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import de.mpg.imeji.exceptions.AuthenticationError;
import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.exceptions.NotAllowedError;
import de.mpg.imeji.exceptions.NotFoundException;
import de.mpg.imeji.exceptions.UnprocessableError;
import de.mpg.imeji.logic.concurrency.Locks;
import de.mpg.imeji.logic.core.item.ItemService;
import de.mpg.imeji.logic.hierarchy.HierarchyService;
import de.mpg.imeji.logic.model.CollectionImeji;
import de.mpg.imeji.logic.model.Item;
import de.mpg.imeji.logic.model.License;
import de.mpg.imeji.logic.model.User;
import de.mpg.imeji.logic.search.SearchQueryParser;
import de.mpg.imeji.logic.search.elasticsearch.ElasticIndexer;
import de.mpg.imeji.logic.search.elasticsearch.ElasticService;
import de.mpg.imeji.logic.search.elasticsearch.ElasticService.ElasticTypes;
import de.mpg.imeji.logic.search.jenasearch.ImejiSPARQL;
import de.mpg.imeji.logic.search.jenasearch.JenaCustomQueries;
import de.mpg.imeji.logic.security.authorization.Authorization;
import de.mpg.imeji.logic.workflow.WorkflowValidator;

/**
 * Facade to Release or Withdraw collections
 * 
 * @author saquet
 *
 */
public class WorkflowFacade implements Serializable {
  private static final long serialVersionUID = 3966446108673573909L;
  private final Authorization authorization = new Authorization();
  private final WorkflowValidator workflowValidator = new WorkflowValidator();
  private final ElasticIndexer collectionIndexer =
      new ElasticIndexer(ElasticService.DATA_ALIAS, ElasticTypes.folders, ElasticService.ANALYSER);

  public void release(CollectionImeji c, User user, License defaultLicense) throws ImejiException {
    preValidateCollection(c, user, defaultLicense);
    final List<String> itemIds = getItemIds(c, user);
    preValidateItems(itemIds, user);
    // Create a list with the collectionId, all the subcollectionids and all itemIds
    List<String> ids =
        new ArrayList<>(new HierarchyService().findAllSubcollections(c.getId().toString()));
    ids.add(c.getId().toString());
    ids.addAll(itemIds);
    String sparql = ids.stream().map(id -> JenaCustomQueries.updateReleaseObject(id))
        .collect(Collectors.joining("; "));
    System.out.println(sparql);
    ImejiSPARQL.execUpdate(sparql);
    /*
     * ImejiSPARQL.execUpdate(JenaCustomQueries.updateCollectionParent(collection.getId().toString()
     * , collection.getCollection() != null ? collection.getCollection().toString() : null,
     * parent.getId().toString())); collectionIndexer.updatePartial(collection.getId().toString(),
     * new ElasticForlderPartObject(parent.getId().toString()));
     */
  }

  private void releaseItem(String itemId) {

  }

  /**
   * Perform prevalidation on the collection to check if the user can proceed to the workflow
   * operation
   * 
   * @param collection
   * @param user
   * @param defaultLicense
   * @throws ImejiException
   */
  private void preValidateCollection(CollectionImeji collection, User user, License defaultLicense)
      throws ImejiException {
    if (user == null) {
      throw new AuthenticationError(AuthenticationError.USER_MUST_BE_LOGGED_IN);
    }
    if (!authorization.administrate(user, collection)) {
      throw new NotAllowedError(NotAllowedError.NOT_ALLOWED);
    }
    if (collection == null) {
      throw new NotFoundException("collection object does not exists");
    }
    workflowValidator.isReleaseAllowed(collection);
  }

  /**
   * Perform prevalidation on the collection items to check if the user can proceed to the workflow
   * operation
   * 
   * @param itemIds
   * @param user
   * @throws ImejiException
   */
  private void preValidateItems(List<String> itemIds, User user) throws ImejiException {
    if (hasImageLocked(itemIds, user)) {
      throw new UnprocessableError("Collection has locked items: can not be released");
    }
    if (itemIds.isEmpty()) {
      throw new UnprocessableError("An empty collection can not be released!");
    }
  }

  /**
   * Return the ids of all items of the collection and its subcollection
   * 
   * @param c
   * @param user
   * @return
   * @throws UnprocessableError
   */
  private List<String> getItemIds(CollectionImeji c, User user) throws UnprocessableError {
    return new ItemService()
        .search(c.getId(), SearchQueryParser.parsedecoded("*"), null, user, -1, 0).getResults();
  }


  /**
   * True if at least one {@link Item} is locked by another {@link User}
   *
   * @param uris
   * @param user
   * @return
   */
  protected boolean hasImageLocked(List<String> uris, User user) {
    for (final String uri : uris) {
      if (Locks.isLocked(uri.toString(), user.getEmail())) {
        return true;
      }
    }
    return false;
  }

}
