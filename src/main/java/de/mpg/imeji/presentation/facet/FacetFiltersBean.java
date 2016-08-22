/**
 * License: src/main/resources/license/escidoc.license
 */
package de.mpg.imeji.presentation.facet;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import org.apache.log4j.Logger;

import de.mpg.imeji.logic.search.SearchQueryParser;
import de.mpg.imeji.logic.search.model.SearchQuery;
import de.mpg.imeji.logic.util.ObjectHelper;
import de.mpg.imeji.logic.util.UrlHelper;
import de.mpg.imeji.logic.vo.Statement;
import de.mpg.imeji.presentation.beans.MetadataLabels;
import de.mpg.imeji.presentation.facet.Facet.FacetType;
import de.mpg.imeji.presentation.util.BeanHelper;

/**
 * Java Bean for the {@link FacetFilter}
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
@ManagedBean(name = "FacetFiltersBean")
@ViewScoped
public class FacetFiltersBean {
  private FacetFiltersSession fs =
      (FacetFiltersSession) BeanHelper.getSessionBean(FacetFiltersSession.class);
  private int count = 0;
  private static final Logger LOGGER = Logger.getLogger(FacetFiltersBean.class);

  /**
   * Default constructor
   */
  public FacetFiltersBean() {
    // construct...
  }

  /**
   * Constructor with one string query and one count (total number of elements on the current page)
   *
   * @param query
   * @param count
   */
  public FacetFiltersBean(SearchQuery sq, int count, Locale locale, MetadataLabels metadataLabels) {
    try {
      this.count = count;
      String q = SearchQueryParser.transform2URL(sq);
      String n = UrlHelper.getParameterValue("f");
      String t = UrlHelper.getParameterValue("t");
      URI metadataURI = null;
      if (n != null) {
        if (n.startsWith("No")) {
          metadataURI = ObjectHelper.getURI(Statement.class, n.substring(3));
        } else {
          metadataURI = ObjectHelper.getURI(Statement.class, n);
        }
      }
      if (t == null) {
        t = FacetType.SEARCH.name();
        n = FacetType.SEARCH.name().toLowerCase();
      }
      if (q != null) {
        List<FacetFilter> filters =
            parseQueryAndSetFilters(q, n, t, metadataURI, locale, metadataLabels);
        resetFiltersSession(q, filters);
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Reset elements in the filters session
   *
   * @param q
   * @param filters
   */
  private void resetFiltersSession(String q, List<FacetFilter> filters) {
    fs.setFilters(filters);
    if (!q.contains(fs.getWholeQuery()) || "".equals(q)) {
      fs.getNoResultsFilters().clear();
    }
    fs.setWholeQuery(q);
  }

  /**
   * Parse the query and set the Filters
   *
   * @param q
   * @param n
   * @param t
   * @return
   * @throws IOException
   */
  private List<FacetFilter> parseQueryAndSetFilters(String q, String n, String t, URI metadataURI,
      Locale locale, MetadataLabels metadataLabels) throws IOException {
    List<FacetFilter> filters = findAlreadyDefinedFilters(q, n, t);
    String newQuery = removeFiltersQueryFromQuery(q, filters);
    FacetFilter newFilter = createNewFilter(newQuery, n, t, metadataURI, locale, metadataLabels);
    if (newFilter != null) {
      filters.add(newFilter);
    }
    resetQueriesToRemoveFilters(q, filters);
    return filters;
  }

  /**
   * Define the query as new filter. If the query has been already parsed and cleaned from previous
   * filters, then the created filter is equals to the filter clicked by the user.
   *
   * @param q
   * @param n
   * @param t
   * @return
   * @throws IOException
   */
  private FacetFilter createNewFilter(String q, String n, String t, URI metadataURI, Locale locale,
      MetadataLabels metadataLabels) throws IOException {
    if (q != null && !"".equals(q.trim())) {
      return new FacetFilter(n, q, count, FacetType.valueOf(t.toUpperCase()), metadataURI, locale,
          metadataLabels);
    }
    return null;
  }

  /**
   * Find the filters which were already defined (in previous queries)
   *
   * @param q
   * @param n
   * @param t
   * @return
   */
  private List<FacetFilter> findAlreadyDefinedFilters(String q, String n, String t) {
    List<FacetFilter> filters = new ArrayList<FacetFilter>();
    for (FacetFilter f : fs.getFilters()) {
      if (q != null && q.contains(f.getQuery())) {
        filters.add(f);
      }
    }
    return filters;
  }

  /**
   * Reset the queries to remove the filters (since the complete query has been change with the new
   * filter)
   *
   * @param q
   * @param filters
   * @return
   * @throws UnsupportedEncodingException
   */
  private List<FacetFilter> resetQueriesToRemoveFilters(String q, List<FacetFilter> filters)
      throws UnsupportedEncodingException {
    for (FacetFilter f : filters) {
      f.setRemoveQuery(createQueryToRemoveFilter(f, q));
    }
    return filters;
  }

  /**
   * Remove the filters from a query
   *
   * @param q
   * @param filters
   * @return
   */
  private String removeFiltersQueryFromQuery(String q, List<FacetFilter> filters) {
    for (FacetFilter f : filters) {
      q = removeFilterQueryFromQuery(q, f);
    }
    return q;
  }

  /**
   * Remove one filter from a query
   *
   * @param q
   * @param filter
   * @return
   */
  private String removeFilterQueryFromQuery(String q, FacetFilter filter) {
    if (!q.contains(filter.getQuery())) {
      LOGGER.error("Query: " + q + " . Error: non removable filter: " + filter.getQuery());
    }
    return q.replace(filter.getQuery(), "").replace("  ", " ").trim();
  }

  /**
   * If q is the complete query, create a query with all information to remove the filter
   *
   * @param f
   * @param q
   * @return
   * @throws UnsupportedEncodingException
   */
  public String createQueryToRemoveFilter(FacetFilter f, String q)
      throws UnsupportedEncodingException {
    return URLEncoder.encode(removeFilterQueryFromQuery(q, f), "UTF-8") + "&f=" + f.getLabel();
  }

  public FacetFiltersSession getSession() {
    return fs;
  }
}
