package de.mpg.imeji.presentation.filter;

import java.util.List;
import java.util.stream.Collectors;

import javax.faces.model.SelectItem;

import org.apache.log4j.Logger;

import de.mpg.imeji.exceptions.UnprocessableError;
import de.mpg.imeji.logic.search.SearchQueryParser;
import de.mpg.imeji.logic.search.model.SearchElement.SEARCH_ELEMENTS;
import de.mpg.imeji.logic.search.model.SearchQuery;
import de.mpg.imeji.logic.util.UrlHelper;
import de.mpg.imeji.presentation.beans.SuperBean;
import de.mpg.imeji.presentation.navigation.history.HistoryPage;
import de.mpg.imeji.presentation.session.BeanHelper;

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
  private final SearchQuery filterQuery;
  private final String filterQueryString;
  private SearchQuery filterQueryWithoutCurrentFilter;
  private String selectedQuery;

  public SuperFilterMenuBean() throws UnprocessableError {
    this.filterQueryString =
        UrlHelper.getParameterValue("filter") == null ? "" : UrlHelper.getParameterValue("filter");
    this.filterQuery = SearchQueryParser.parseStringQuery(filterQueryString);
  }

  public void init(List<SelectItem> menu) {
    try {
      this.filterQueryWithoutCurrentFilter = filterQueryWithoutCurrentFilters(filterQuery, menu);
      this.menu = menu.stream()
          .map(i -> new SelectItem(getFilterLink((SearchQuery) i.getValue()), i.getLabel()))
          .collect(Collectors.toList());
    } catch (final Exception e) {
      BeanHelper.error("Error parsing query in the URL");
      LOGGER.error("Error parsing query in the URL", e);
    }
  }

  protected String getFilterLink(SearchQuery q) {
    String qs = SearchQueryParser.transform2URL(q);
    try {
      if (filterQueryString.contains(qs)) {
        selectedQuery = initRemoveFilterLink();
        return selectedQuery;
      } else {
        return initAddFilterLink(q);
      }
    } catch (Exception e) {
      LOGGER.error("Error building filter query", e);
      return "";
    }
  }

  private String initRemoveFilterLink() throws UnprocessableError {
    return buildPageUrl(SearchQueryParser.transform2URL(filterQueryWithoutCurrentFilter));
  }

  private String initAddFilterLink(SearchQuery q) throws UnprocessableError {
    if (filterQueryWithoutCurrentFilter.isEmpty()) {
      return buildPageUrl(SearchQueryParser.transform2URL(q));
    } else {
      return buildPageUrl(SearchQueryParser.transform2URL(filterQueryWithoutCurrentFilter) + " AND "
          + SearchQueryParser.transform2URL(q));
    }
  }

  /**
   * Build the page url with the passed filter query
   * 
   * @param filterQuery
   * @return
   */
  private String buildPageUrl(String filterQuery) {
    final HistoryPage page = getCurrentPage().copy();
    page.setParamValue("filter", filterQuery);
    return page.getCompleteUrl();
  }

  /**
   * Read the current Query and remove all possible filter (defined in the current menu) from it
   *
   * @return
   * @throws UnprocessableError
   */
  private SearchQuery filterQueryWithoutCurrentFilters(SearchQuery filterQuery,
      List<SelectItem> menu) throws UnprocessableError {
    SearchQuery currentQuery = filterQuery.copy();
    for (final SelectItem item : menu) {
      currentQuery = soustractToQuery(currentQuery, (SearchQuery) item.getValue());
    }
    return currentQuery;
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
          if (i - 1 > 0
              && q1.getElements().get(i - 1).getType() == SEARCH_ELEMENTS.LOGICAL_RELATIONS) {
            q1.getElements().remove(i - 1);
          }
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

  public String getSelectedQuery() {
    return selectedQuery;
  }
}
