/**
 * License: src/main/resources/license/escidoc.license
 */
package de.mpg.imeji.presentation.collection;

import java.util.ArrayList;
import java.util.List;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;

import de.mpg.imeji.logic.vo.CollectionImeji;
import de.mpg.imeji.logic.vo.Metadata;

/**
 * Session with objects related to {@link CollectionImeji}
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
@ManagedBean(name = "CollectionSessionBean")
@SessionScoped
public class CollectionSessionBean {
  private String selectedMenu = "SORTING";
  private String filter = "all";
  private List<Metadata> metadataTypes = null;

  /**
   * Constructor
   */
  public CollectionSessionBean() {
    try {
      init();
    } catch (final Exception e) {
      throw new RuntimeException("Error initializing collection session:", e);
    }
  }

  /**
   * Initialize the session objects
   *
   * @throws Exception
   */
  public void init() {
    metadataTypes = new ArrayList<Metadata>();
  }


  /**
   * @return the selectedMenu
   */
  public String getSelectedMenu() {
    return selectedMenu;
  }

  /**
   * @param selectedMenu the selectedMenu to set
   */
  public void setSelectedMenu(String selectedMenu) {
    this.selectedMenu = selectedMenu;
  }

  /**
   * getter
   *
   * @return
   */
  public String getFilter() {
    return filter;
  }

  /**
   * setter
   *
   * @param filter
   */
  public void setFilter(String filter) {
    this.filter = filter;
  }

  /**
   * setter
   *
   * @param metadataTypes
   */
  public void setMetadataTypes(List<Metadata> metadataTypes) {
    this.metadataTypes = metadataTypes;
  }

  /**
   * getter
   *
   * @return
   */
  public List<Metadata> getMetadataTypes() {
    return metadataTypes;
  }
}
