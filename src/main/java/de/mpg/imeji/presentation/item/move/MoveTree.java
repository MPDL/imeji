package de.mpg.imeji.presentation.item.move;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import de.mpg.imeji.logic.hierarchy.HierarchyService;
import de.mpg.imeji.logic.model.CollectionImeji;

public class MoveTree implements Serializable {
  private static final long serialVersionUID = 3293078595888014645L;
  private List<MoveTreeNode> nodes;
  private final Map<String, CollectionImeji> map;

  public MoveTree(List<CollectionImeji> collections) {
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
  private List<MoveTreeNode> addChildren(List<MoveTreeNode> nodes) {
    List<MoveTreeNode> nodeWithChildren = new ArrayList<>();
    for (MoveTreeNode node : nodes) {
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
  private List<MoveTreeNode> initTopNodes(List<CollectionImeji> collections) {
    return collections.stream().filter(c -> !c.isSubCollection()).map(c -> new MoveTreeNode(c, map))
        .collect(Collectors.toList());
  }

  public void showChildren(MoveTreeNode node) {
    int index = nodes.indexOf(node);
    nodes.addAll(index,
        new HierarchyService().getFullHierarchy().getTree()
            .get(node.getCollection().getId().toString()).stream()
            .map(id -> new MoveTreeNode(map.get(id), map)).collect(Collectors.toList()));
  }

  public List<MoveTreeNode> getNodes() {
    return nodes;
  }


}
