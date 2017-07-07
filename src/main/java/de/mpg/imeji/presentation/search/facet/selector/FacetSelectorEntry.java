package de.mpg.imeji.presentation.search.facet.selector;

import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.faces.context.FacesContext;

import org.apache.commons.lang3.math.NumberUtils;
import org.apache.log4j.Logger;

import de.mpg.imeji.exceptions.UnprocessableError;
import de.mpg.imeji.logic.search.SearchQueryParser;
import de.mpg.imeji.logic.search.facet.FacetService;
import de.mpg.imeji.logic.search.facet.model.Facet;
import de.mpg.imeji.logic.search.facet.model.FacetResult;
import de.mpg.imeji.logic.search.facet.model.FacetResultValue;
import de.mpg.imeji.logic.search.factory.SearchFactory;
import de.mpg.imeji.logic.search.model.SearchFields;
import de.mpg.imeji.logic.search.model.SearchMetadata;
import de.mpg.imeji.logic.search.model.SearchMetadataFields;
import de.mpg.imeji.logic.search.model.SearchQuery;
import de.mpg.imeji.logic.util.StringHelper;
import de.mpg.imeji.logic.vo.ImejiLicenses;
import de.mpg.imeji.logic.vo.StatementType;
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

  public FacetSelectorEntry(FacetResult facetResult, SearchQuery facetsQuery, int countAll) {
    FacetService facetService = new FacetService();
    this.facetsQuery = facetsQuery;
    this.facet = facetService.retrieveByIndexFromCache(facetResult.getIndex());
    this.values = facetResult.getValues().stream()
        .map(v -> new FacetSelectorEntryValue(v, facet, facetsQuery)).collect(Collectors.toList());
    addAnyLicense(facetResult, facet, facetsQuery, countAll);
    cleanFileTypeFacet(facet);
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

  public void search(String url, String q) throws IOException {
    String fq = buildFromToQuery();
    if (fq != null) {
      FacesContext.getCurrentInstance().getExternalContext()
          .redirect(url + "?q=" + q + "&fq=" + fq);
    }
  }

  private String buildFromToQuery() {
    if (isValidInput()) {
      try {
        SearchMetadata sm = new SearchMetadata(getIndex(), getSearchMetadataFieldsForFacet(),
            getIntervalSearchValue());
        return SearchQueryParser
            .transform2UTF8URL(new SearchFactory(facetsQuery).and(Arrays.asList(sm)).build());
      } catch (UnprocessableError e) {
        LOGGER.error("Error searching for interval", e);
        return "";
      }
    }
    return null;
  }

  private String getIndex() {
    return facet.getIndex().replace("md.", "").split("\\.")[0];
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

  private String getIntervalSearchValue() {
    return (StringHelper.isNullOrEmptyTrim(from) ? "" : "from " + from)
        + (StringHelper.isNullOrEmptyTrim(to) ? "" : " to " + to);
  }

  /**
   * REturn the {@link SearchMetadataFields} needed for the current facet
   * 
   * @return
   */
  private SearchMetadataFields getSearchMetadataFieldsForFacet() {
    switch (StatementType.valueOf(facet.getType())) {
      case NUMBER:
        return SearchMetadataFields.number;
      case DATE:
        return SearchMetadataFields.date;
      default:
        return null;
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

}
