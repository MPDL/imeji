package de.mpg.imeji.presentation.navigation;

import java.io.Serializable;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import org.apache.log4j.Logger;

import de.mpg.imeji.logic.hierarchy.HierarchyService;
import de.mpg.imeji.logic.model.CollectionImeji;
import de.mpg.imeji.logic.model.Item;
import de.mpg.imeji.logic.util.ObjectHelper;
import de.mpg.imeji.presentation.beans.SuperBean;

/**
 * JSF Bean for the imeji breadcrumb
 * 
 * @author saquet
 *
 */
@ManagedBean(name = "BreadcrumbBean")
@ViewScoped
public class BreadcrumbBean extends SuperBean {
  private static final long serialVersionUID = -5061850823347395653L;
  private static final Logger LOGGER = Logger.getLogger(BreadcrumbBean.class);
  private List<Entry> entries = new ArrayList<>();
  private HierarchyService hierarchyService = new HierarchyService();

  /**
   * Initialize the Breadcrumb for an object (Item or Collection)
   * 
   * @param o
   */
  public void init(Object o) {
    HierarchyService.reloadHierarchy();
    try {
      entries = hierarchyService.findAllParentsWithNames(getObjectCollectionUri(o), false).stream()
          .map(w -> new Entry(w.getName(), getLinkToCollection(w.getUri())))
          .collect(Collectors.toList());
    } catch (Exception e) {
      LOGGER.error("Error retrieving parent collections", e);
    }
  }

  private String getObjectCollectionUri(Object o) {
    if (o instanceof Item) {
      return ((Item) o).getCollection().toString();
    }
    if (o instanceof CollectionImeji) {
      return ((CollectionImeji) o).getId().toString();
    }
    return null;
  }

  /**
   * Return the link to the collection
   * 
   * @param collectionUri
   * @return
   */
  private String getLinkToCollection(String collectionUri) {
    return getNavigation().getCollectionUrl() + ObjectHelper.getId(URI.create(collectionUri));
  }


  /**
   * Entry of the Breadcrumb
   * 
   * @author saquet
   *
   */
  public class Entry implements Serializable {
    private static final long serialVersionUID = -6639393671167064138L;
    private final String label;
    private final String link;

    public Entry(final String label, final String link) {
      this.label = label;
      this.link = link;
    }

    /**
     * @return the link
     */
    public String getLink() {
      return link;
    }

    /**
     * @return the label
     */
    public String getLabel() {
      return label;
    }
  }

  /**
   * @return the entries
   */
  public List<Entry> getEntries() {
    return entries;
  }
}
