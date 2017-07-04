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
import de.mpg.imeji.logic.search.factory.SearchFactory;
import de.mpg.imeji.logic.search.model.SearchMetadata;
import de.mpg.imeji.logic.search.model.SearchMetadataFields;
import de.mpg.imeji.logic.search.model.SearchQuery;
import de.mpg.imeji.logic.util.StringHelper;
import de.mpg.imeji.logic.vo.StatementType;

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
  private final List<FacetSelectorEntryValue> values;
  private final SearchQuery facetsQuery;
  private String from;
  private String to;

  public FacetSelectorEntry(FacetResult facetResult, SearchQuery facetsQuery) {
    FacetService facetService = new FacetService();
    this.facetsQuery = facetsQuery;
    this.facet = facetService.retrieveByIndexFromCache(facetResult.getIndex());
    this.values = facetResult.getValues().stream()
        .map(v -> new FacetSelectorEntryValue(v, facet, facetsQuery)).collect(Collectors.toList());
  }

  public void search(String url, String q) throws IOException {
    String fq = buildFromToQuery();
    if (q != null) {
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

  private boolean isValidInput() {
    return NumberUtils.isNumber(from) && NumberUtils.isNumber(to);
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

}
