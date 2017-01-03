/**
 * License: src/main/resources/license/escidoc.license
 */
package de.mpg.imeji.presentation.search;

import de.mpg.imeji.logic.search.model.SearchElement;
import de.mpg.imeji.logic.search.model.SearchElement.SEARCH_ELEMENTS;
import de.mpg.imeji.logic.search.model.SearchGroup;
import de.mpg.imeji.logic.search.model.SearchIndex;
import de.mpg.imeji.logic.search.model.SearchPair;

/**
 * Helper for searchFormular
 *
 * @author bastiens
 *
 */
public class SearchFormularHelper {

  private SearchFormularHelper() {
    // avoid construction
  }

  public static String getCollectionId(SearchGroup searchGroup) {
    String id = null;
    for (final SearchElement se : searchGroup.getElements()) {
      if (se.getType().equals(SEARCH_ELEMENTS.PAIR)
          && SearchIndex.SearchFields.col == ((SearchPair) se).getField()) {
        return ((SearchPair) se).getValue();
      } else if (se.getType().equals(SEARCH_ELEMENTS.GROUP)) {
        id = getCollectionId((SearchGroup) se);
        if (id != null) {
          return id;
        }
      }
    }
    return id;
  }
}
