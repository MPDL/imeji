package de.mpg.imeji.presentation.search.facet.selector;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import java.util.stream.Collectors;

import javax.faces.context.FacesContext;

import org.apache.commons.lang3.math.NumberUtils;
import org.apache.log4j.Logger;

import de.mpg.imeji.exceptions.UnprocessableError;
import de.mpg.imeji.logic.model.ImejiLicenses;
import de.mpg.imeji.logic.model.SearchFields;
import de.mpg.imeji.logic.model.StatementType;
import de.mpg.imeji.logic.search.SearchQueryParser;
import de.mpg.imeji.logic.search.facet.FacetService;
import de.mpg.imeji.logic.search.facet.model.Facet;
import de.mpg.imeji.logic.search.facet.model.FacetResult;
import de.mpg.imeji.logic.search.facet.model.FacetResultValue;
import de.mpg.imeji.logic.search.factory.SearchFactory;
import de.mpg.imeji.logic.search.model.SearchQuery;
import de.mpg.imeji.presentation.navigation.history.HistoryPage;
import de.mpg.imeji.util.DateHelper;

/**
 * Entry of the {@link FacetSelectorBean}
 * 
 * @author saquet
 *
 */
public class FacetSelectorEntry implements Serializable {
  private static final long serialVersionUID = -982329261816788783L;
  private static final Logger LOGGER = Logger.getLogger(FacetSelectorEntry.class);
  private final Facet facet;
  private List<FacetSelectorEntryValue> values;
  private final SearchQuery facetsQuery;
  private String from;
  private String to;
  private boolean showMore = false;
  private final long count;

  public FacetSelectorEntry(FacetResult facetResult, SearchQuery facetsQuery, int countAll) {
    FacetService facetService = new FacetService();
    this.facetsQuery = facetsQuery;
    this.facet = facetService.retrieveByIndexFromCache(facetResult.getIndex());
    this.values = facetResult.getValues().stream()
        .map(v -> new FacetSelectorEntryValue(v, facet, facetsQuery)).collect(Collectors.toList());
    addAnyLicense(facetResult, facet, facetsQuery, countAll);
    cleanFileTypeFacet(facet);
    this.count = values.stream().collect(Collectors.summingLong(v -> v.getCount()));
    if (count > 0) {
      this.from = values.get(0).getMin();
      this.to = values.get(0).getMax();
    }
  }

  /**
   * Using the facetsresults,calculate the facet for any licenses
   * 
   * @param facetResult
   * @param facet
   * @param facetsQuery
   * @param countAll
   */
  private void addAnyLicense(FacetResult facetResult, Facet facet, SearchQuery facetsQuery,
      int countAll) {
    if (facet.getIndex().equals(SearchFields.license.getIndex())) {
      long countNone = facetResult.getValues().stream()
          .filter(r -> r.getLabel().equals(ImejiLicenses.NO_LICENSE)).map(r -> r.getCount())
          .findFirst().orElse((long) countAll);
      long countAny = countAll - countNone;
      if (countAny > 0) {
        FacetResultValue v = new FacetResultValue("Any", countAll - countNone);
        values.add(new FacetSelectorEntryValue(v, facet, facetsQuery));
        values = values.stream().sorted((v1, v2) -> Long.compare(v2.getCount(), v1.getCount()))
            .collect(Collectors.toList());
      }
    }
  }

  private void cleanFileTypeFacet(Facet facet) {
    if (facet.getIndex().equals(SearchFields.filetype.getIndex())) {
      values = values.stream().filter(v -> v.getCount() > 0)
          .sorted((v1, v2) -> Long.compare(v2.getCount(), v1.getCount()))
          .collect(Collectors.toList());
    }
  }

  public void search(HistoryPage page, String q) throws IOException {
    String fq = buildFromToQuery();
    if (fq != null) {
      FacesContext.getCurrentInstance().getExternalContext()
          .redirect(page.setParamValue("fq", fq).getCompleteUrl());
    }
  }

  private String buildFromToQuery() {
    if (isValidInput()) {
      try {
        FacetSelectorEntryValue selectorValue = values.get(0);
        FacetResultValue resultValue = new FacetResultValue(facet.getName(), 0);
        resultValue.setMax(to);
        resultValue.setMin(from);
        FacetSelectorEntryValue selectorValueNew = new FacetSelectorEntryValue(resultValue, facet,
            new SearchFactory(facetsQuery).clone().remove(selectorValue.getEntryQuery()).build());
        return SearchQueryParser.transform2URL(new SearchFactory(facetsQuery)
            .remove(selectorValue.getEntryQuery()).and(selectorValueNew.getEntryQuery()).build());
      } catch (UnprocessableError e) {
        LOGGER.error("Error creating searchquery for range query", e);
      }
    }
    return null;
  }

  /**
   * True if the input is valid
   * 
   * @return
   */
  private boolean isValidInput() {
    switch (StatementType.valueOf(facet.getType())) {
      case NUMBER:
        return NumberUtils.isNumber(from) || NumberUtils.isNumber(to);
      case DATE:
        return DateHelper.isValidDate(from) || DateHelper.isValidDate(to);
      default:
        return false;
    }
  }

  /**
   * @return the facet
   */
  public Facet getFacet() {
    return facet;
  }

  /**
   * @return the values
   */
  public List<FacetSelectorEntryValue> getValues() {
    return values;
  }

  /**
   * @return the from
   */
  public String getFrom() {
    return from;
  }

  /**
   * @param from the from to set
   */
  public void setFrom(String from) {
    this.from = from;
  }

  /**
   * @return the to
   */
  public String getTo() {
    return to;
  }

  /**
   * @param to the to to set
   */
  public void setTo(String to) {
    this.to = to;
  }

  public boolean isShowMore() {
    return showMore;
  }

  public void toggleShowMore() {
    showMore = showMore ? false : true;
  }



  public long getCount() {
    return count;
  }

}
