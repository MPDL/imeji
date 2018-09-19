package de.mpg.imeji.presentation.item.move;

import static de.mpg.imeji.logic.search.model.SearchLogicalRelation.LOGICAL_RELATIONS.AND;
import static de.mpg.imeji.logic.search.model.SearchLogicalRelation.LOGICAL_RELATIONS.OR;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.ViewScoped;

import org.apache.log4j.Logger;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.config.Imeji;
import de.mpg.imeji.logic.core.collection.CollectionService;
import de.mpg.imeji.logic.core.item.ItemService;
import de.mpg.imeji.logic.hierarchy.HierarchyService;
import de.mpg.imeji.logic.model.CollectionImeji;
import de.mpg.imeji.logic.model.Grant.GrantType;
import de.mpg.imeji.logic.model.Item;
import de.mpg.imeji.logic.model.SearchFields;
import de.mpg.imeji.logic.model.factory.ImejiFactory;
import de.mpg.imeji.logic.search.Search;
import de.mpg.imeji.logic.search.factory.SearchFactory;
import de.mpg.imeji.logic.search.model.SearchPair;
import de.mpg.imeji.logic.search.model.SortCriterion;
import de.mpg.imeji.logic.search.model.SortCriterion.SortOrder;
import de.mpg.imeji.logic.security.authorization.Authorization;
import de.mpg.imeji.logic.util.ObjectHelper;
import de.mpg.imeji.logic.util.ObjectHelper.ObjectType;
import de.mpg.imeji.logic.util.StringHelper;
import de.mpg.imeji.presentation.beans.SuperBean;
import de.mpg.imeji.presentation.collection.tree.Node;
import de.mpg.imeji.presentation.collection.tree.Tree;
import de.mpg.imeji.presentation.item.license.LicenseEditor;
import de.mpg.imeji.presentation.session.BeanHelper;
import de.mpg.imeji.presentation.session.SessionBean;

@ManagedBean(name = "MoveItemsBean")
@ViewScoped
public class MoveItemsBean extends SuperBean {
  private static final long serialVersionUID = 2230148128355260199L;
  private static final Logger LOGGER = Logger.getLogger(MoveItemsBean.class);
  private List<CollectionImeji> collectionsForMove = new ArrayList<>();
  private Tree tree;
  @ManagedProperty(value = "#{SessionBean}")
  private SessionBean sessionBean;
  private String query = "";
  private LicenseEditor licenseEditor;
  private final ItemService itemService = new ItemService();
  private final HierarchyService hierarchyService = new HierarchyService();
  private String newSubcollectionName;
  private CollectionImeji target;


  private boolean toNewSubCollection = false;

  /**
   * Load all collections for which the user has at least edit role
   * 
   * @throws ImejiException
   */
  public void searchCollectionsForMove(String collectionSrcId) throws ImejiException {
    licenseEditor = new LicenseEditor(getLocale(), false);
    SearchFactory factory = new SearchFactory();
    factory.addElement(new SearchPair(SearchFields.title, query + "*"), OR);
    if (!new Authorization().isSysAdmin(getSessionUser())) {
      factory.addElement(new SearchPair(SearchFields.role, GrantType.EDIT.name().toLowerCase()),
          AND);
    }
    collectionsForMove = new CollectionService()
        .searchAndRetrieve(factory.build(),
            new SortCriterion(SearchFields.modified, SortOrder.DESCENDING), getSessionUser(), Search.GET_ALL_RESULTS, Search.SEARCH_FROM_START_INDEX)
        .stream().collect(Collectors.toList());
    tree = new Tree(collectionsForMove);
  }

  /**
   * Move to the the destinationId
   * 
   * @param id
   * @throws IOException
   * @throws ImejiException
   */
  public void moveTo(String id) throws IOException, ImejiException {
    if (toNewSubCollection) {
      moveToNewSubCollection(id, newSubcollectionName, target);
    } else {
      moveTo(id, target);
    }
  }

  /**
   * Move an object (Item of Collection) to the target collection. If Id is null, move currently
   * selected Items
   * 
   * @param collectionId
   * @param id
   * @throws IOException
   */
  public void moveTo(String id, CollectionImeji c) throws IOException {
    if (StringHelper.isNullOrEmptyTrim(id)) {
      moveSelectedTo(c);
    } else {
      switch (ObjectHelper.getObjectType(URI.create(id))) {
        case ITEM:
          moveItem(id, c);
          break;
        case COLLECTION:
          moveCollection(id, c);
          break;
        default:
          break;
      }
    }
  }

  /**
   * ove an object (Item of Collection) to a new Subcollection. If Id is null, move currently
   * selected Items
   * 
   * @param id
   * @throws IOException
   * @throws ImejiException
   */
  public void moveToNew(String id) throws IOException, ImejiException {
    moveToNewSubCollection(id, newSubcollectionName, target);
  }

  /**
   * Move an object (Item of Collection) to a new Subcollection. If Id is null, move currently
   * selected Items
   * 
   * @param id
   * @param name
   * @param parentId
   * @throws IOException
   * @throws ImejiException
   */
  public void moveToNewSubCollection(String id, String name, CollectionImeji c)
      throws IOException, ImejiException {
    CollectionImeji subcollection = new CollectionService()
        .create(ImejiFactory.newCollection().setTitle(name).setPerson(getSessionUser().getPerson())
            .setCollection(c.getId().toString()).build(), getSessionUser());
    collectionsForMove.add(subcollection);
    moveTo(id, subcollection);
  }

  /**
   * Triggerd when the user click on "+" to move the object to a new subcollection
   * 
   * @param parentId
   */
  public void initMoveToNewSubcollection(CollectionImeji c) {
    target = c;
    toNewSubCollection = true;
  }

  /**
   * Move the selected items to the collection
   * 
   * @param collectionId
   * @throws IOException
   */
  private void moveSelectedTo(CollectionImeji c) throws IOException {
    moveItems(c, sessionBean.getSelected());
    sessionBean.getSelected().clear();
    redirect(getCurrentPage().getCompleteUrl());
  }

  /**
   * Move the items (defined by a list of their ids) to the collection
   * 
   * @param collectionId
   * @param ids
   */
  private void moveItems(CollectionImeji col, List<String> ids) {
    try {
      List<Item> items = (List<Item>) itemService.retrieveBatchLazy(ids, Search.GET_ALL_RESULTS, Search.SEARCH_FROM_START_INDEX, getSessionUser());
      List<Item> moved =
          itemService.moveItems(items, col, getSessionUser(), licenseEditor.getLicense());
      if (moved.size() > 0) {
        BeanHelper.addMessage(moved.size() + " "
            + (moved.size() > 1 ? Imeji.RESOURCE_BUNDLE.getLabel("items", getLocale())
                : Imeji.RESOURCE_BUNDLE.getLabel("item", getLocale()))
            + " " + Imeji.RESOURCE_BUNDLE.getLabel("moved", getLocale()));
      }
      int notMoved = ids.size() - moved.size();
      if (notMoved > 0) {
        BeanHelper.error(notMoved + " "
            + (notMoved > 1 ? Imeji.RESOURCE_BUNDLE.getLabel("items", getLocale())
                : Imeji.RESOURCE_BUNDLE.getLabel("item", getLocale()))
            + " " + Imeji.RESOURCE_BUNDLE.getLabel("moved_error", getLocale()));
      }

    } catch (Exception e) {
      BeanHelper.error("Error moving items " + e.getMessage());
      LOGGER.error("Error moving items ", e);
    }
  }

  /**
   * Move a single Item to the collection
   * 
   * @param targetId
   * @param id
   * @throws IOException
   */
  public void moveItem(String id, CollectionImeji c) throws IOException {
    moveItems(c, Arrays.asList(id));
    redirect(getNavigation().getCollectionUrl() + ObjectHelper.getId(c.getId()) + "/item/"
        + ObjectHelper.getId(URI.create(id)));
  }

  /**
   * Move the collection as child of the target collection
   * 
   * @param collectionId
   * @param targetCollectionId
   */
  private void moveCollection(String collectionId, CollectionImeji target) {
    CollectionImeji collection = collectionsForMove.stream()
        .filter(c -> c.getId().toString().equals(collectionId)).findAny().get();
    try {
      new CollectionService().moveCollection(collection, target, getSessionUser(),
          getLicenseEditor().getLicense());
      BeanHelper.info("Collection moved.");
    } catch (ImejiException e) {
      BeanHelper.error("Error moving collection: " + e.getMessage());
      LOGGER.error("Error moving collection", e);
    }
  }

  /**
   * True if the node is a child of collection id
   * 
   * @param node
   * @param collectionId
   * @return
   */
  public boolean isChildOf(Node node, String collectionId) {
    return node.hasParent()
        && hierarchyService.isChildOf(node.getCollection().getId().toString(), collectionId);
  }

  /**
   * True if the object being moved is a collection
   * 
   * @param objectId
   * @return
   */
  public boolean isCollection(String objectId) {
    return objectId != null
        && ObjectHelper.getObjectType(URI.create(objectId)) == ObjectType.COLLECTION;
  }

  /**
   * List of all Collection where the user could move items into
   * 
   * @return
   */
  public List<CollectionImeji> getCollectionsForMove() {
    return collectionsForMove;
  }

  public SessionBean getSessionBean() {
    return sessionBean;
  }

  public void setSessionBean(SessionBean sessionBean) {
    this.sessionBean = sessionBean;
  }

  /**
   * @return the query
   */
  public String getQuery() {
    return query;
  }

  /**
   * @param query the query to set
   */
  public void setQuery(String query) {
    this.query = query;
  }

  /**
   * @return the licenseEditor
   */
  public LicenseEditor getLicenseEditor() {
    return licenseEditor;
  }

  /**
   * @param licenseEditor the licenseEditor to set
   */
  public void setLicenseEditor(LicenseEditor licenseEditor) {
    this.licenseEditor = licenseEditor;
  }

  public Tree getTree() {
    return tree;
  }

  /**
   * @return the newSubcollectionName
   */
  public String getNewSubcollectionName() {
    return newSubcollectionName;
  }

  /**
   * @param newSubcollectionName the newSubcollectionName to set
   */
  public void setNewSubcollectionName(String newSubcollectionName) {
    this.newSubcollectionName = newSubcollectionName;
  }

  /**
   * @return the toNewCollection
   */
  public boolean isToNewSubCollection() {
    return toNewSubCollection;
  }

  /**
   * @param toNewCollection the toNewCollection to set
   */
  public void setToNewSubCollection(boolean toNewCollection) {
    this.toNewSubCollection = toNewCollection;
  }

  /**
   * @return the target
   */
  public CollectionImeji getTarget() {
    return target;
  }

  /**
   * @param target the target to set
   */
  public void setTarget(CollectionImeji target) {
    this.target = target;
  }

}
