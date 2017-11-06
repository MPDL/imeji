package de.mpg.imeji.presentation.item.move;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import de.mpg.imeji.logic.hierarchy.HierarchyService;
import de.mpg.imeji.logic.model.CollectionImeji;

/**
 * A Node of the Move Tree
 * 
 * @author saquet
 *
 */
public class MoveTreeNode {
  private final CollectionImeji collection;
  private int depth = 0;
  private List<MoveTreeNode> children = new ArrayList<>();
  private final Map<String, CollectionImeji> collectionsMap;

  public MoveTreeNode(CollectionImeji c, Map<String, CollectionImeji> map) {
    this.collection = c;
    this.collectionsMap = map;
    initChildren();
  }

  public MoveTreeNode(MoveTreeNode parent, CollectionImeji c, Map<String, CollectionImeji> map) {
    this.depth = parent.depth + 1;
    this.collection = c;
    this.collectionsMap = map;
    initChildren();
  }

  /**
   * Initialize the children of this node
   */
  public void initChildren() {
    List<String> childrenIds =
        new HierarchyService().getFullHierarchy().getTree().get(collection.getId().toString());
    if (childrenIds != null) {
      children = childrenIds.stream().filter(id -> collectionsMap.get(id) != null)
          .map(id -> new MoveTreeNode(this, collectionsMap.get(id), collectionsMap))
          .collect(Collectors.toList());
    }
  }

  public List<MoveTreeNode> getChildren() {
    return children;
  }


  public CollectionImeji getCollection() {
    return collection;
  }

  public int getDepth() {
    return depth;
  }

  public boolean hasParent() {
    return collection.getCollection() != null;
  }
}
