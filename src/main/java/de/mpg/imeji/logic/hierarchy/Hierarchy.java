package de.mpg.imeji.logic.hierarchy;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import de.mpg.imeji.logic.config.Imeji;
import de.mpg.imeji.logic.search.jenasearch.ImejiSPARQL;
import de.mpg.imeji.logic.search.jenasearch.JenaCustomQueries;

/**
 * The complete Hierarchy of imeji
 * 
 * @author saquet
 *
 */
public class Hierarchy {
  private Map<String, List<String>> tree = new HashMap<>();
  private Map<String, Node> nodes = new HashMap<>();

  /**
   * A node of the Hierarchy
   * 
   * @author saquet
   *
   */
  class Node {
    private final String parent;
    private final String id;

    public Node(String id, String parent) {
      this.id = id;
      this.parent = parent;
    }

    public String getParent() {
      return parent;
    }

    public String getId() {
      return id;
    }
  }

  /**
   * Load the complete Hierarchy
   */
  public void init() {
    final List<String> l =
        ImejiSPARQL.exec(JenaCustomQueries.selectAllSubcollections(), Imeji.collectionModel);
    final List<Node> nodeList = l.stream().map(s -> new Node(s.split("\\|")[0], s.split("\\|")[1]))
        .collect(Collectors.toList());
    nodes = nodeList.stream().collect(Collectors.toMap(Node::getId, Function.identity()));
    tree = nodeList.stream().collect(Collectors.groupingBy(Node::getParent,
        Collectors.mapping(Node::getId, Collectors.toList())));
  }

  public Map<String, List<String>> getTree() {
    return tree;
  }

  public Map<String, Node> getNodes() {
    return nodes;
  }
}
