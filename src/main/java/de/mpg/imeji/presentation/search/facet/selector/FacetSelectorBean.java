package de.mpg.imeji.presentation.search.facet.selector;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import org.apache.log4j.Logger;

import de.mpg.imeji.exceptions.UnprocessableError;
import de.mpg.imeji.logic.search.SearchQueryParser;
import de.mpg.imeji.logic.search.factory.SearchFactory;
import de.mpg.imeji.logic.search.model.SearchElement.SEARCH_ELEMENTS;
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
  private List<String> selectedValueQueries = new ArrayList<>();
  private SearchQuery facetQuery = new SearchQuery();
  private static final Logger LOGGER = Logger.getLogger(FacetSelectorBean.class);

  public FacetSelectorBean() {
    try {
      facetQuery = SearchQueryParser.parseStringQuery(UrlHelper.getParameterValue("fq"));
      parseSelectedValueQueries();
    } catch (UnprocessableError e) {
      LOGGER.error("Error parsing facet query " + UrlHelper.getParameterValue("fq"), e);
    }
  }

  private void parseSelectedValueQueries() {
    selectedValueQueries = facetQuery.getElements().stream()
        .filter(e -> e.getType() != SEARCH_ELEMENTS.LOGICAL_RELATIONS)
        .map(e -> SearchQueryParser.transform2UTF8URL(new SearchQuery(Arrays.asList(e))))
        .collect(Collectors.toList());
  }

  public String init(SearchResult result) {
    entries = result.getFacets().stream()
        .map(r -> new FacetSelectorEntry(r, facetQuery, result.getNumberOfRecords()))
        .collect(Collectors.toList());
    setAddQuery();
    setRemoveQuery();
    setSelectedEntries();
    return "";
  }

  /**
   * Set the addQuery to all values of all entries
   */
  private void setAddQuery() {
    entries.stream().flatMap(e -> e.getValues().stream())
        .forEach(v -> v.setAddQuery(createAddQuery(v)));
  }

  /**
   * Create the add query for a FacetSelectorEntryValue according to the current FacetQuery
   * 
   * @param entryValue
   * @return
   */
  private String createAddQuery(FacetSelectorEntryValue entryValue) {
    try {
      return SearchQueryParser.transform2UTF8URL(
          new SearchFactory(facetQuery).and(entryValue.getEntryQuery().getElements()).build());
    } catch (UnprocessableError e) {
      LOGGER.error("Error building add query for facet " + entryValue.getLabel(), e);
      return "";
    }
  }

  private void setRemoveQuery() {
    entries.stream().flatMap(e -> e.getValues().stream())
        .forEach(v -> v.setRemoveQuery(createRemoveQuery(v)));
  }

  private String createRemoveQuery(FacetSelectorEntryValue entryValue) {
    return selectedValueQueries.stream().sorted()
        .filter(v -> !v.equals(SearchQueryParser.transform2UTF8URL(entryValue.getEntryQuery())))
        .collect(Collectors.joining(" AND "));
  }

  /**
   * Set if a FacetSelectorEntryValue is selected or not
   */
  private void setSelectedEntries() {
    entries.stream().flatMap(e -> e.getValues().stream())
        .forEach(v -> v.setSelected(isSelected(v)));
  }


  private boolean isSelected(FacetSelectorEntryValue entryValue) {
    return selectedValueQueries.stream()
        .anyMatch(s -> s.equals(SearchQueryParser.transform2UTF8URL(entryValue.getEntryQuery())));
  }

  /**
   * @return the entries
   */
  public List<FacetSelectorEntry> getEntries() {
    return entries;
  }
}
