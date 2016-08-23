package de.mpg.imeji.presentation.filter;

import java.util.List;

import javax.faces.model.SelectItem;

import org.apache.log4j.Logger;

import de.mpg.imeji.exceptions.UnprocessableError;
import de.mpg.imeji.logic.search.SearchQueryParser;
import de.mpg.imeji.logic.search.model.SearchElement;
import de.mpg.imeji.logic.search.model.SearchElement.SEARCH_ELEMENTS;
import de.mpg.imeji.logic.search.model.SearchLogicalRelation.LOGICAL_RELATIONS;
import de.mpg.imeji.logic.search.model.SearchQuery;
import de.mpg.imeji.logic.util.UrlHelper;
import de.mpg.imeji.presentation.beans.SuperBean;
import de.mpg.imeji.presentation.history.HistoryPage;
import de.mpg.imeji.presentation.util.BeanHelper;

/**
 * Super Bean for Filter menu implementation.
 * 
 * @author saquet
 *
 */
public class SuperFilterMenuBean extends SuperBean {
  private static final long serialVersionUID = 5211495478085868441L;
  private static final Logger LOGGER = Logger.getLogger(SuperFilterMenuBean.class);
  private List<SelectItem> menu;
  private String selected;

  public void init(List<SelectItem> menu) {
    this.menu = menu;
    try {
      this.selected =
          findSelected(SearchQueryParser.parseStringQuery(UrlHelper.getParameterValue("q")));
    } catch (UnprocessableError e) {
      BeanHelper.error("Error parsing query in the URL");
      LOGGER.error("Error parsing query in the URL", e);
    }
  }

  /**
   * Find the selected Filter from the url. If the Filter is not found, return null
   * 
   * @param query
   * @return
   * @throws UnprocessableError
   */
  private String findSelected(SearchQuery query) throws UnprocessableError {
    for (SearchElement element : query.getElements()) {
      for (SelectItem selectItem : menu) {
        SearchQuery filterQuery =
            SearchQueryParser.parseStringQuery(selectItem.getValue().toString());
        if (filterQuery.isSame(element)) {
          return selectItem.getValue().toString();
        }
      }
    }
    return null;
  }

  /**
   * Get the URL for this filter
   * 
   * @param filter
   * @return
   * @throws UnprocessableError
   */
  public String getFilterUrl(String filter) throws UnprocessableError {
    SearchQuery filterQuery = SearchQueryParser.parseStringQuery(filter);
    SearchQuery currentQuery = getCurrentQueryWihoutFilter();
    currentQuery = mergeQueries(currentQuery, filterQuery);
    HistoryPage page = getHistory().getCurrentPage();
    page.setParamValue("q", SearchQueryParser.transform2URL(currentQuery));
    return page.getCompleteUrl();
  }

  /**
   * Return the url to remove the current filter
   * 
   * @return
   * @throws UnprocessableError
   */
  public String getRemoveFilterUrl() throws UnprocessableError {
    String query = SearchQueryParser.transform2URL(getCurrentQueryWihoutFilter());
    HistoryPage page = getHistory().getCurrentPage();
    page.setParamValue("q", query);
    return page.getCompleteUrl();
  }

  /**
   * Read the current Query and remove all possible filter (defined in the current menu) from it
   * 
   * @return
   * @throws UnprocessableError
   */
  private SearchQuery getCurrentQueryWihoutFilter() throws UnprocessableError {
    SearchQuery currentQuery = SearchQueryParser.parseStringQuery(UrlHelper.getParameterValue("q"));
    for (SelectItem item : menu) {
      currentQuery = soustractToQuery(currentQuery,
          SearchQueryParser.parseStringQuery((String) item.getValue()));
    }
    return currentQuery;
  }

  /**
   * Merge q1 to q2 as "q1 AND q2"
   * 
   * @param q1
   * @param q2
   * @return
   * @throws UnprocessableError
   */
  private SearchQuery mergeQueries(SearchQuery q1, SearchQuery q2) throws UnprocessableError {
    if (q1.isEmpty()) {
      return q2;
    } else if (q2.isEmpty()) {
      return q1;
    } else {
      q1.addLogicalRelation(LOGICAL_RELATIONS.AND);
      q1.getElements().addAll(q2.getElements());
      return q1;
    }
  }

  /**
   * Soustract a SearchQuery from a SearchQuery: q1 - q2
   * 
   * @param q1
   * @param q2
   * @return
   * @throws UnprocessableError
   */
  private SearchQuery soustractToQuery(SearchQuery q1, SearchQuery q2) throws UnprocessableError {
    if (!q1.isEmpty() && !q2.isEmpty()) {
      for (int i = 0; i < q1.getElements().size(); i++) {
        if (q1.getElements().get(i).isSame(q2)) {
          q1.getElements().remove(i);
          // if (i - 1 > 0
          // && q1.getElements().get(i - 1).getType() == SEARCH_ELEMENTS.LOGICAL_RELATIONS) {
          // q1.getElements().remove(i - 1);
          // }
        }
      }
    }
    return trimLogicalOperation(q1);

  }

  /**
   * Remove all Search Logical Operation which are at the beginning or at the end (and therefore
   * useless)
   * 
   * @param q
   * @return
   * @throws UnprocessableError
   */
  private SearchQuery trimLogicalOperation(SearchQuery q) throws UnprocessableError {
    if (q.isEmpty()) {
      return q;
    }
    while (q.getElements().get(0).getType() == SEARCH_ELEMENTS.LOGICAL_RELATIONS) {
      q.getElements().remove(0);
    }
    while (q.getTypeOfLastElement() == SEARCH_ELEMENTS.LOGICAL_RELATIONS) {
      q.getElements().remove(q.getElements().size() - 1);
    }
    return q;
  }

  public List<SelectItem> getMenu() {
    return menu;
  }

  public void setMenu(List<SelectItem> menu) {
    this.menu = menu;
  }

  public String getSelected() {
    return selected;
  }

  public void setSelected(String selected) {
    this.selected = selected;
  }

}
