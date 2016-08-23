package de.mpg.imeji.logic.search.model;

import java.util.List;

/**
 * {@link SearchElement} defining logical operations (and/or) between other {@link SearchElement}
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public class SearchLogicalRelation extends SearchElement {
  public enum LOGICAL_RELATIONS {
    AND, OR;
  }

  private LOGICAL_RELATIONS logicalRelation;

  public SearchLogicalRelation(LOGICAL_RELATIONS lr) {
    setLogicalRelation(lr);
  }

  public void setLogicalRelation(LOGICAL_RELATIONS logicalRelation) {
    this.logicalRelation = logicalRelation;
  }

  public LOGICAL_RELATIONS getLogicalRelation() {
    return logicalRelation;
  }

  @Override
  public SEARCH_ELEMENTS getType() {
    return SEARCH_ELEMENTS.LOGICAL_RELATIONS;
  }

  @Override
  public List<SearchElement> getElements() {
    return null;
  }

  @Override
  public boolean isSame(SearchElement element) {
    if (element.getType() == SEARCH_ELEMENTS.LOGICAL_RELATIONS) {
      return ((SearchLogicalRelation) element).getLogicalRelation() == logicalRelation;
    }
    return false;
  }
}
