package de.mpg.imeji.presentation.search.facet.selector;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import org.apache.log4j.Logger;

import de.mpg.imeji.exceptions.UnprocessableError;
import de.mpg.imeji.logic.search.SearchQueryParser;
import de.mpg.imeji.logic.search.model.SearchQuery;
import de.mpg.imeji.logic.search.model.SearchResult;
import de.mpg.imeji.logic.util.UrlHelper;
import de.mpg.imeji.presentation.beans.SuperBean;

/**
 * Bean for the FacetSelector
 * 
 * @author saquet
 *
 */
@ManagedBean(name = "FacetSelectorBean")
@ViewScoped
public class FacetSelectorBean extends SuperBean {
  private static final long serialVersionUID = -1071527421059585519L;
  private List<FacetSelectorEntry> entries = new ArrayList<>();
  private SearchQuery facetQuery = new SearchQuery();
  private static final Logger LOGGER = Logger.getLogger(FacetSelectorBean.class);

  public FacetSelectorBean() {
    try {
      facetQuery = SearchQueryParser.parseStringQuery(UrlHelper.getParameterValue("fq"));
    } catch (UnprocessableError e) {
      LOGGER.error("Error parsing facet query " + UrlHelper.getParameterValue("fq"), e);
    }
  }

  public String init(SearchResult result) {
    entries = result.getFacets().stream()
        .map(r -> new FacetSelectorEntry(r, facetQuery, result.getNumberOfRecords()))
        .collect(Collectors.toList());
    return "";
  }

  /**
   * @return the entries
   */
  public List<FacetSelectorEntry> getEntries() {
    return entries;
  }
}
