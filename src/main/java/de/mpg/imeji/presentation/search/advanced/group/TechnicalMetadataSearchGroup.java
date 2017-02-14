package de.mpg.imeji.presentation.search.advanced.group;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.faces.model.SelectItem;

import org.apache.log4j.Logger;

import de.mpg.imeji.exceptions.UnprocessableError;
import de.mpg.imeji.logic.config.Imeji;
import de.mpg.imeji.logic.search.factory.SearchFactory;
import de.mpg.imeji.logic.search.model.SearchElement;
import de.mpg.imeji.logic.search.model.SearchLogicalRelation.LOGICAL_RELATIONS;
import de.mpg.imeji.logic.search.model.SearchOperators;
import de.mpg.imeji.logic.search.model.SearchTechnicalMetadata;
import de.mpg.imeji.logic.util.StringHelper;

/**
 * Search Group for technical metadata
 *
 * @author saquet
 *
 */
public class TechnicalMetadataSearchGroup extends AbstractAdvancedSearchFormGroup
    implements Serializable {
  private static final Logger LOGGER = Logger.getLogger(TechnicalMetadataSearchGroup.class);
  private static final long serialVersionUID = -8685239131773216610L;
  private List<SelectItem> menu;
  private List<TechnicalMetadataElement> list = new ArrayList<>();

  /**
   * Technical metadata Search Element for the advanced search form
   *
   * @author saquet
   *
   */
  public class TechnicalMetadataElement implements Serializable {

    private static final long serialVersionUID = 1956745139187896361L;
    private String index;
    private String value;
    private LOGICAL_RELATIONS relation = LOGICAL_RELATIONS.OR;

    public TechnicalMetadataElement(String index, String value) {
      this.index = index;
      this.value = value;
    }

    /**
     * Return the {@link TechnicalMetadataElement} as {@link SearchTechnicalMetadata}
     * 
     * @return
     */
    public SearchTechnicalMetadata toSearchTechnicalMetadata() {
      return new SearchTechnicalMetadata(SearchOperators.EQUALS, value, index, false);
    }

    /**
     * @return the index
     */
    public String getIndex() {
      return index;
    }

    /**
     * @param index the index to set
     */
    public void setIndex(String index) {
      this.index = index;
    }

    /**
     * @return the value
     */
    public String getValue() {
      return value;
    }

    /**
     * @param value the value to set
     */
    public void setValue(String value) {
      this.value = value;
    }

    /**
     * @return the relation
     */
    public LOGICAL_RELATIONS getRelation() {
      return relation;
    }

    /**
     * @param relation the relation to set
     */
    public void setRelation(LOGICAL_RELATIONS relation) {
      this.relation = relation;
    }
  }

  public TechnicalMetadataSearchGroup() {
    initMenu();
    if (!menu.isEmpty()) {
      list = new ArrayList<>();
      list.add(0, new TechnicalMetadataElement(getDefaultMetadata(), ""));
    }
  }

  private void initMenu() {
    menu = new ArrayList<>();
    for (final String index : Imeji.CONFIG.getTechnicalMetadata().split(",")) {
      if (!StringHelper.isNullOrEmptyTrim(index)) {
        menu.add(new SelectItem(index));
      }
    }
  }

  private String getDefaultMetadata() {
    return (String) menu.get(0).getValue();
  }


  @Override
  public SearchElement toSearchElement() {
    SearchFactory factory = new SearchFactory();
    for (final TechnicalMetadataElement el : list) {
      try {
        factory.addElement(el.toSearchTechnicalMetadata(), el.getRelation());
      } catch (UnprocessableError e) {
        LOGGER.error("Error adding SearchTechnicalMetadata to factory", e);
      }
    }
    return factory.buildAsGroup();
  }

  @Override
  public void validate() {
    // TODO Auto-generated method stub

  }

  /**
   * True if no search is set for the technical metadata
   *
   * @return
   */
  public boolean isEmpty() {
    for (final TechnicalMetadataElement el : list) {
      if (!StringHelper.isNullOrEmptyTrim(el)) {
        return false;
      }
    }
    return true;
  }

  public void add(int pos) {
    list.add(pos + 1, new TechnicalMetadataElement(getDefaultMetadata(), ""));
  }

  public void remove(int pos) {
    list.remove(pos);
  }

  /**
   * @return the metadata
   */
  public List<TechnicalMetadataElement> getList() {
    return list;
  }

  /**
   * @param metadata the metadata to set
   */
  public void setList(List<TechnicalMetadataElement> l) {
    this.list = l;
  }

  public List<SelectItem> getMenu() {
    return menu;
  }

  public void setMenu(List<SelectItem> menu) {
    this.menu = menu;
  }

}
