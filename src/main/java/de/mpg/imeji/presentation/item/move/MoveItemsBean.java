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
import de.mpg.imeji.logic.search.factory.SearchFactory;
import de.mpg.imeji.logic.search.model.SearchPair;
import de.mpg.imeji.logic.security.authorization.Authorization;
import de.mpg.imeji.logic.util.ObjectHelper;
import de.mpg.imeji.logic.util.ObjectHelper.ObjectType;
import de.mpg.imeji.logic.util.StringHelper;
import de.mpg.imeji.presentation.beans.SuperBean;
import de.mpg.imeji.presentation.license.LicenseEditor;
import de.mpg.imeji.presentation.session.BeanHelper;
import de.mpg.imeji.presentation.session.SessionBean;

@ManagedBean(name = "MoveItemsBean")
@ViewScoped
public class MoveItemsBean extends SuperBean {
  private static final long serialVersionUID = 2230148128355260199L;
  private static final Logger LOGGER = Logger.getLogger(MoveItemsBean.class);
  private List<CollectionImeji> collectionsForMove = new ArrayList<>();
  private MoveTree tree;
  @ManagedProperty(value = "#{SessionBean}")
  private SessionBean sessionBean;
  private String query = "";
  private LicenseEditor licenseEditor;
  private String destinationId;
  private final ItemService itemService = new ItemService();
  private final HierarchyService hierarchyService = new HierarchyService();


  /**
   * Load all the collection for which the user has at least edit role
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
    collectionsForMove =
        new CollectionService().searchAndRetrieve(factory.build(), null, getSessionUser(), -1, 0)
            .stream().collect(Collectors.toList());
    tree = new MoveTree(collectionsForMove);
  }

  /**
   * Move the
   * 
   * @param collectionId
   * @param id
   * @throws IOException
   */
  public void moveTo(String id, String targetId) throws IOException {
    if (StringHelper.isNullOrEmptyTrim(id)) {
      moveSelectedTo(targetId);
    } else {
      switch (ObjectHelper.getObjectType(URI.create(id))) {
        case ITEM:
          moveItem(id, targetId);
          break;
        case COLLECTION:
          moveCollection(id, targetId);
          break;
        default:
          break;
      }
    }
  }

  /**
   * Move to the the destinationId
   * 
   * @param id
   * @throws IOException
   */
  public void moveTo(String id) throws IOException {
    moveTo(id, destinationId);
  }

  /**
   * Move the selected items to the collection
   * 
   * @param collectionId
   * @throws IOException
   */
  private void moveSelectedTo(String collectionId) throws IOException {
    moveItems(collectionId, sessionBean.getSelected());
    sessionBean.getSelected().clear();
    redirect(getCurrentPage().getCompleteUrl());
  }

  /**
   * Move the items (defined by a list of their ids) to the collection
   * 
   * @param collectionId
   * @param ids
   */
  private void moveItems(String collectionId, List<String> ids) {
    try {
      CollectionImeji col = collectionsForMove.stream()
          .filter(c -> c.getId().toString().equals(collectionId)).findAny().get();
      List<Item> items = (List<Item>) itemService.retrieveBatchLazy(ids, 0, -1, getSessionUser());
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
  public void moveItem(String id, String targetId) throws IOException {
    moveItems(targetId, Arrays.asList(id));
    redirect(getNavigation().getCollectionUrl() + ObjectHelper.getId(URI.create(targetId))
        + "/item/" + ObjectHelper.getId(URI.create(id)));
  }

  /**
   * Move the collection as child of the target collection
   * 
   * @param collectionId
   * @param targetCollectionId
   */
  private void moveCollection(String collectionId, String targetCollectionId) {
    CollectionImeji target = collectionsForMove.stream()
        .filter(c -> c.getId().toString().equals(targetCollectionId)).findAny().get();
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
  public boolean isChildOf(MoveTreeNode node, String collectionId) {
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

  public void setDestinationId(String colId) {
    this.destinationId = colId;;
  }

  public String getDestinationId() {
    return destinationId;
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

  public MoveTree getTree() {
    return tree;
  }
}
