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
    private final String child;

    public Node(String child, String parent) {
      this.child = child;
      this.parent = parent;
    }

    /**
     * The uri of the parent
     * 
     * @return
     */
    public String getParent() {
      return parent;
    }

    /**
     * The uri of the Child
     * 
     * @return
     */
    public String getChild() {
      return child;
    }
  }

  /**
   * Load the complete Hierarchy
   */
  public void init() {
    final List<String> l = ImejiSPARQL.exec(JenaCustomQueries.selectAllSubcollections(), Imeji.collectionModel);
    final List<Node> nodeList = l.stream().map(s -> new Node(s.split("\\|")[0], s.split("\\|")[1])).collect(Collectors.toList());
    nodes = nodeList.stream().collect(Collectors.toMap(Node::getChild, Function.identity()));
    tree = nodeList.stream().collect(Collectors.groupingBy(Node::getParent, Collectors.mapping(Node::getChild, Collectors.toList())));
  }

  /**
   * Return the Tree of the hierarchy:
   * <li>key: the uri of a collection which has child(s)</li>
   * <li>value: The list of all childs of the collection
   * 
   * @return
   */
  public Map<String, List<String>> getTree() {
    return tree;
  }

  /**
   * Map of all nodes (i.e. parent <-> child relation):
   * <li>key: the uri of the child</li>
   * <li>value: the Node</li>
   * 
   * @return
   */
  public Map<String, Node> getNodes() {
    return nodes;
  }

}
