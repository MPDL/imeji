package de.mpg.imeji.logic.search.model;

import java.util.ArrayList;
import java.util.List;

/**
 * SearchElement grouping {@link SearchElement}
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public class SearchGroup extends SearchElement {
  private List<SearchElement> group;
  private boolean not;

  public SearchGroup() {
    group = new ArrayList<SearchElement>();
  }

  public void setGroup(List<SearchElement> group) {
    this.group = group;
  }

  public List<SearchElement> getGroup() {
    return group;
  }

  @Override
  public SEARCH_ELEMENTS getType() {
    return SEARCH_ELEMENTS.GROUP;
  }

  @Override
  public List<SearchElement> getElements() {
    return group;
  }

  public void setNot(boolean not) {
    this.not = not;
  }

  public boolean isNot() {
    return not;
  }

  @Override
  public boolean isSame(SearchElement element) {
    SearchGroup g = toGroup(element);
    if (g != null) {
      if (g.group.size() != group.size()) {
        return false;
      } else {
        for (int i = 0; i < group.size(); i++) {
          if (!g.group.get(i).isSame(group.get(i))) {
            return false;
          }
        }
      }
      return true;
    }
    return false;
  }

  public SearchGroup toGroup(SearchElement element) {
    if (element.getType() == SEARCH_ELEMENTS.GROUP) {
      return (SearchGroup) element;
    } else if (element.getType() == SEARCH_ELEMENTS.QUERY
        && ((SearchQuery) element).getElements().size() == 1) {
      return toGroup(((SearchQuery) element).getElements().get(0));
    } else if (element.getType() == SEARCH_ELEMENTS.GROUP
        && ((SearchGroup) element).getElements().size() == 1) {
      return toGroup(((SearchGroup) element).getElements().get(0));
    }
    return null;
  }
}
