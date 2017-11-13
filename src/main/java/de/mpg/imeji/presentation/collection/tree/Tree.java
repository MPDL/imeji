package de.mpg.imeji.presentation.collection.tree;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import de.mpg.imeji.logic.hierarchy.HierarchyService;
import de.mpg.imeji.logic.model.CollectionImeji;

public class Tree implements Serializable {
  private static final long serialVersionUID = 3293078595888014645L;
  private List<Node> nodes;
  private final Map<String, CollectionImeji> map;

  public Tree(List<CollectionImeji> collections) {
    map = collections.stream()
        .collect(Collectors.toMap(c -> c.getId().toString(), Function.identity()));
    nodes = addChildren(initTopNodes(collections));
  }

  /**
   * Add to the nodes their respective children
   * 
   * @param nodes
   * @return
   */
  private List<Node> addChildren(List<Node> nodes) {
    List<Node> nodeWithChildren = new ArrayList<>();
    for (Node node : nodes) {
      nodeWithChildren.add(node);
      if (!node.getChildren().isEmpty()) {
        nodeWithChildren.addAll(addChildren(node.getChildren()));
      }
    }
    return nodeWithChildren;
  }

  /**
   * Return all the nodes which haven't a parent
   * 
   * @param collections
   * @return
   */
  private List<Node> initTopNodes(List<CollectionImeji> collections) {
    return collections.stream().filter(c -> !c.isSubCollection()).map(c -> new Node(c, map))
        .collect(Collectors.toList());
  }

  public void showChildren(Node node) {
    int index = nodes.indexOf(node);
    nodes.addAll(index,
        new HierarchyService().getFullHierarchy().getTree()
            .get(node.getCollection().getId().toString()).stream().filter(id -> map.get(id) != null)
            .map(id -> new Node(map.get(id), map)).collect(Collectors.toList()));
  }

  public List<Node> getNodes() {
    return nodes;
  }


}
