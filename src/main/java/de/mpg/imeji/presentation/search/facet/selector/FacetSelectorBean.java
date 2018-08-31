package de.mpg.imeji.presentation.search.facet.selector;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import org.apache.log4j.Logger;

import de.mpg.imeji.exceptions.UnprocessableError;
import de.mpg.imeji.logic.search.SearchQueryParser;
import de.mpg.imeji.logic.search.facet.model.Facet;
import de.mpg.imeji.logic.search.factory.SearchFactory;
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
  
  private static final long serialVersionUID = 4953953758406265116L;
  private static final Logger LOGGER = Logger.getLogger(FacetSelectorBean.class);
  private List<FacetSelectorEntry> entries = new ArrayList<>();
  private SearchQuery facetQuery = new SearchQuery();
  private SearchFactory factory;

  @PostConstruct
  private void init() {
    try {
      facetQuery = SearchQueryParser.parseStringQuery(UrlHelper.getParameterValue("fq"));
      factory = new SearchFactory(facetQuery);
    } catch (Exception e) {
      LOGGER.error("Error parsing facet query " + UrlHelper.getParameterValue("fq"), e);
    }
  }

  /**
   * Init the facet selector with a SearchResult
   * 
   * @param result
   * @return
   */
  public String init(SearchResult result) {
    if (result != null) {
      this.entries = result.getFacets().stream()
          .filter(f -> !f.getName().equals(Facet.ITEMS) && !f.getName().equals(Facet.SUBCOLLECTIONS)
              && !f.getName().equals(Facet.COLLECTION_ITEMS))
          .map(r -> new FacetSelectorEntry(r, facetQuery, result.getNumberOfRecords(), this.getLocale()))
          .sorted(
              (f1, f2) -> Integer.compare(f1.getFacet().getPosition(), f2.getFacet().getPosition()))
          .collect(Collectors.toList());      
      setAddQuery();
      setRemoveQuery();
      setSelectedEntries();
    }
    return "";
  }

  public List<FacetSelectorEntryValue> getSelectedValues() {
    return entries.stream().flatMap(e -> e.getValues().stream()).filter(v -> v.isSelected())
        .collect(Collectors.toList());
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
      return getCurrentPage().copy()
          .setParamValue("fq",
              SearchQueryParser
                  .transform2URL(factory.clone().and(entryValue.getEntryQuery()).build()))
          .getCompleteUrl();
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
    return getCurrentPage().copy()
        .setParamValue("fq",
            SearchQueryParser
                .transform2URL(factory.clone().remove(entryValue.getEntryQuery()).build()))
        .getCompleteUrl();
  }

  /**
   * Set if a FacetSelectorEntryValue is selected or not
   */
  private void setSelectedEntries() {
    entries.stream().flatMap(e -> e.getValues().stream())
        .forEach(v -> v.setSelected(isSelected(v)));
  }

  private boolean isSelected(FacetSelectorEntryValue entryValue) {
    return factory.contains(entryValue.getEntryQuery());
  }

  /**
   * @return the entries
   */
  public List<FacetSelectorEntry> getEntries() {
    return entries;
  }

  public void setEntries(List<FacetSelectorEntry> entries) {
    this.entries = entries;
  }
}
