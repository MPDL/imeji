package de.mpg.imeji.presentation.collection.tree;

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
public class Node {
  private final CollectionImeji collection;
  private int depth = 0;
  private List<Node> children = new ArrayList<>();
  private final Map<String, CollectionImeji> collectionsMap;

  public Node(CollectionImeji c, Map<String, CollectionImeji> map) {
    this.collection = c;
    this.collectionsMap = map;
    initChildren();
  }

  public Node(Node parent, CollectionImeji c, Map<String, CollectionImeji> map) {
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
          .map(id -> new Node(this, collectionsMap.get(id), collectionsMap))
          .collect(Collectors.toList());
    }
  }

  public List<Node> getChildren() {
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
