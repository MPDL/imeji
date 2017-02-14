package de.mpg.imeji.presentation.search.advanced.group;

import java.io.Serializable;
import java.util.Arrays;

import org.apache.log4j.Logger;

import de.mpg.imeji.exceptions.UnprocessableError;
import de.mpg.imeji.logic.search.factory.SearchFactory;
import de.mpg.imeji.logic.search.model.SearchElement;
import de.mpg.imeji.logic.search.model.SearchFields;
import de.mpg.imeji.logic.search.model.SearchPair;

/**
 * Search Group of the advanced search
 * 
 * @author saquet
 *
 */
public class TextSearchGroup extends AbstractAdvancedSearchFormGroup implements Serializable {
  private static Logger LOGGER = Logger.getLogger(TextSearchGroup.class);
  private static final long serialVersionUID = -6588304080472017223L;
  private String query;
  private boolean includeFulltext = true;

  @Override
  public SearchElement toSearchElement() {
    if (query != null && !query.isEmpty()) {
      SearchPair fulltext = new SearchPair(SearchFields.fulltext, query);
      SearchPair all = new SearchPair(SearchFields.all, query);
      if (includeFulltext) {
        try {
          return new SearchFactory().or(Arrays.asList(all, fulltext)).buildAsGroup();
        } catch (UnprocessableError e) {
          LOGGER.error("Error adding searchpairs to factory", e);
        }
      } else {
        return all;
      }
    }
    return new SearchPair();
  }

  @Override
  public void validate() {
    // TODO Auto-generated method stub

  }


  /**
   * @return the query
   */
  public String getQuery() {
    return query;
  }

  /**
   * @param query the query to set
   */
  public void setQuery(String query) {
    this.query = query;
  }

  /**
   * @return the includeFulltext
   */
  public boolean isIncludeFulltext() {
    return includeFulltext;
  }

  /**
   * @param includeFulltext the includeFulltext to set
   */
  public void setIncludeFulltext(boolean includeFulltext) {
    this.includeFulltext = includeFulltext;
  }

}
