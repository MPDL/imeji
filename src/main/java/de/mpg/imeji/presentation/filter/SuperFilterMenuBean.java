package de.mpg.imeji.presentation.filter;

import java.util.List;

import javax.faces.model.SelectItem;

import org.apache.log4j.Logger;

import de.mpg.imeji.exceptions.UnprocessableError;
import de.mpg.imeji.logic.search.SearchQueryParser;
import de.mpg.imeji.logic.search.model.SearchElement;
import de.mpg.imeji.logic.search.model.SearchElement.SEARCH_ELEMENTS;
import de.mpg.imeji.logic.search.model.SearchGroup;
import de.mpg.imeji.logic.search.model.SearchLogicalRelation.LOGICAL_RELATIONS;
import de.mpg.imeji.logic.search.model.SearchPair;
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
    String selected = null;
    for (SearchElement element : query.getElements()) {
      SearchQuery q = transformSearchElementToQuery(element);
      for (SelectItem selectItem : menu) {
        if (isSameFilter(q, selectItem)) {
          return selected;
        }
      }
    }
    return null;
  }

  /**
   * True if the query and the selectItem are actually a same query
   * 
   * @param q
   * @param selectItem
   * @return
   */
  private boolean isSameFilter(SearchQuery q, SelectItem selectItem) {
    return q != null && !q.isEmpty() && SearchQueryParser.transform2URL(q).equals(selectItem);
  }

  /**
   * Transform a searchelement to a searchquery
   * 
   * @param element
   * @return
   * @throws UnprocessableError
   */
  protected SearchQuery transformSearchElementToQuery(SearchElement element)
      throws UnprocessableError {
    SearchQuery query = new SearchQuery();
    if (element.getType() == SEARCH_ELEMENTS.PAIR) {
      query.addPair((SearchPair) element);
    } else if (element.getType() == SEARCH_ELEMENTS.GROUP) {
      query.addGroup((SearchGroup) element);
    }
    return query;
  }

  public String getFilterUrl(String filter) throws UnprocessableError {
    SearchQuery filterQuery = SearchQueryParser.parseStringQuery(filter);
    SearchQuery currentQuery = SearchQueryParser.parseStringQuery(UrlHelper.getParameterValue("q"));
    currentQuery = mergeQueries(currentQuery, filterQuery);
    HistoryPage page = getHistory().getCurrentPage();
    page.setParamValue("q", SearchQueryParser.transform2URL(currentQuery));
    return page.getCompleteUrl();
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
    }
    return q1;
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
