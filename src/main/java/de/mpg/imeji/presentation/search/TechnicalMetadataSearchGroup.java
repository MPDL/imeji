package de.mpg.imeji.presentation.search;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.faces.model.SelectItem;

import de.mpg.imeji.exceptions.UnprocessableError;
import de.mpg.imeji.logic.Imeji;
import de.mpg.imeji.logic.search.model.SearchGroup;
import de.mpg.imeji.logic.search.model.SearchIndex.SearchFields;
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
public class TechnicalMetadataSearchGroup implements Serializable {
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
    list = new ArrayList<>();
    list.add(0, new TechnicalMetadataElement(getDefaultMetadata(), ""));
  }

  private void initMenu() {
    menu = new ArrayList<>();
    for (String index : Imeji.CONFIG.getTechnicalMetadata().split(",")) {
      menu.add(new SelectItem(index));
    }
  }

  private String getDefaultMetadata() {
    return (String) menu.get(0).getValue();
  }

  /**
   * Return the technical metadata group as a {@link SearchGroup}
   * 
   * @return
   * @throws UnprocessableError
   */
  public SearchGroup asSearchGroup() throws UnprocessableError {
    SearchGroup group = new SearchGroup();
    for (TechnicalMetadataElement el : list) {
      if (!StringHelper.isNullOrEmptyTrim(el.value)) {
        if (!group.isEmpty()) {
          group.addLogicalRelation(el.getRelation());
        }
        group.addPair(new SearchTechnicalMetadata(SearchFields.technical, SearchOperators.REGEX,
            el.getValue(), el.getIndex(), false));
      }
    }
    return group;
  }

  /**
   * True if no search is set for the technical metadata
   * 
   * @return
   */
  public boolean isEmpty() {
    for (TechnicalMetadataElement el : list) {
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
