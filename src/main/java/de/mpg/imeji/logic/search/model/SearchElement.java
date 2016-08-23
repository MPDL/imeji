package de.mpg.imeji.logic.search.model;

import java.util.List;

import de.mpg.imeji.exceptions.UnprocessableError;
import de.mpg.imeji.logic.search.model.SearchLogicalRelation.LOGICAL_RELATIONS;

/**
 * Element of a {@link SearchQuery}
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public abstract class SearchElement {
  public enum SEARCH_ELEMENTS {
    LOGICAL_RELATIONS, PAIR, GROUP, QUERY, METADATA, SIMPLE_METADATA;
  }

  public abstract SEARCH_ELEMENTS getType();

  public abstract List<SearchElement> getElements();

  /**
   * Add a {@link LOGICAL_RELATIONS} after a {@link SearchElement}
   *
   * @param lr
   * @throws UnprocessableError
   */
  public void addLogicalRelation(LOGICAL_RELATIONS lr) throws UnprocessableError {
    if (!hasElements()) {
      throw new UnprocessableError("Operation not allowed for " + getType());
    }
    if (!isEmpty() && !SEARCH_ELEMENTS.LOGICAL_RELATIONS.equals(getTypeOfLastElement())) {
      getElements().add(new SearchLogicalRelation(lr));
    } else if (SEARCH_ELEMENTS.LOGICAL_RELATIONS.equals(getTypeOfLastElement())) {
      throw new UnprocessableError(
          "Wrong search query: Logical relations can not be added after a logical relation");
    }
  }

  /**
   * Add a {@link SearchPair} after a {@link SearchElement}
   *
   * @param pair
   * @throws UnprocessableError
   */
  public void addPair(SearchPair pair) throws UnprocessableError {
    if (!hasElements()) {
      throw new UnprocessableError("Operation not allowed for " + getType());
    }
    if (isEmpty() || SEARCH_ELEMENTS.LOGICAL_RELATIONS.equals(getTypeOfLastElement())) {
      getElements().add(pair);
    } else {
      throw new UnprocessableError(
          "Wrong search query. A pair should be added after a logical relation or a the begining of the query!");
    }
  }

  /**
   * Add a {@link SearchGroup} after a {@link SearchElement}
   *
   * @param group
   * @throws UnprocessableError
   */
  public void addGroup(SearchGroup group) throws UnprocessableError {
    if (!hasElements()) {
      throw new UnprocessableError("Operation not allowed for " + getType());
    }
    if (isEmpty() || SEARCH_ELEMENTS.LOGICAL_RELATIONS.equals(getTypeOfLastElement())) {
      getElements().add(group);
    } else {
      throw new UnprocessableError(
          "Wrong search query. A group should be added after a logical relation or a the begining of the query!");
    }
  }

  /**
   * Get the {@link SEARCH_ELEMENTS} of the last element of a {@link SearchElement} (if it is a
   * {@link SearchGroup} or a {@link SearchQuery})
   *
   * @return
   * @throws UnprocessableError
   */
  public SEARCH_ELEMENTS getTypeOfLastElement() throws UnprocessableError {
    SearchElement se = getLastElement();
    if (se == null) {
      return null;
    }
    return se.getType();
  }

  private SearchElement getLastElement() throws UnprocessableError {
    if (!hasElements()) {
      throw new UnprocessableError("Operation not allowed for " + getType());
    }
    if (!isEmpty()) {
      return getElements().get(getElements().size() - 1);
    }
    return null;
  }

  public boolean isEmpty() {
    if (!hasElements()) {
      return true;
    }
    return getElements().size() == 0;
  }

  /**
   * True if 2 elements are equals
   * 
   * @param element
   * @return
   */
  public abstract boolean isSame(SearchElement element);

  private boolean hasElements() {
    return getType().equals(SEARCH_ELEMENTS.QUERY) || getType().equals(SEARCH_ELEMENTS.GROUP);
  }
}
