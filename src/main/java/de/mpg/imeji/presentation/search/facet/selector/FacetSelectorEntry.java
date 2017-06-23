package de.mpg.imeji.presentation.search.facet.selector;

import static de.mpg.imeji.logic.search.model.SearchLogicalRelation.LOGICAL_RELATIONS.AND;

import java.io.Serializable;

import org.apache.log4j.Logger;

import de.mpg.imeji.exceptions.UnprocessableError;
import de.mpg.imeji.logic.search.SearchQueryParser;
import de.mpg.imeji.logic.search.facet.model.Facet;
import de.mpg.imeji.logic.search.facet.model.FacetResultValue;
import de.mpg.imeji.logic.search.factory.SearchFactory;
import de.mpg.imeji.logic.search.model.SearchFields;
import de.mpg.imeji.logic.search.model.SearchMetadata;
import de.mpg.imeji.logic.search.model.SearchOperators;
import de.mpg.imeji.logic.search.model.SearchPair;
import de.mpg.imeji.logic.search.model.SearchQuery;
import de.mpg.imeji.logic.vo.StatementType;

public class FacetSelectorEntry implements Serializable {
  private static final long serialVersionUID = -5562614379983226471L;
  private static final Logger LOGGER = Logger.getLogger(FacetSelectorEntry.class);
  private final String label;
  private final long count;
  private final SearchQuery query;
  private final String type;

  public FacetSelectorEntry(FacetResultValue resultValue, Facet facet) {
    this.label = resultValue.getLabel();
    this.count = resultValue.getCount();
    this.type = facet.getType();
    this.query = buildQuery(facet, resultValue.getLabel());
  }

  private SearchQuery buildQuery(Facet facet, String value) {
    boolean isMetadataFacet = facet.getIndex().startsWith("md.");
    return isMetadataFacet ? buildMetadataQuery(facet, value) : buildSystemQuery(facet, value);

  }

  /**
   * Build the SearchQuery for the user metadata
   * 
   * @param facet
   * @param value
   * @return
   */
  private SearchQuery buildMetadataQuery(Facet facet, String value) {
    try {
      switch (StatementType.valueOf(facet.getType())) {
        case TEXT:
          return buildMetadataTextQuery(facet, value);
        case NUMBER:
        case DATE:
          return buildMetadataDateQuery(facet, value);
        case PERSON:
          return new SearchQuery();
        case URL:
          return new SearchQuery();
        case GEOLOCATION:
          return new SearchQuery();

      }
    } catch (Exception e) {
      LOGGER.error("Error building facet metadata query", e);
    }
    return new SearchQuery();
  }

  private SearchQuery buildMetadataTextQuery(Facet facet, String value) throws UnprocessableError {
    SearchMetadata smd = new SearchMetadata(facet.getIndex().replace("md.", ""), value);
    return new SearchFactory().addElement(smd, AND).build();
  }

  private SearchQuery buildMetadataDateQuery(Facet facet, String value) throws UnprocessableError {
    String dateValue = value.replace("Before", "").replace("After", "").trim();
    SearchOperators operator = SearchOperators.EQUALS;
    if (value.contains("Before")) {
      operator = SearchOperators.LESSER;
    } else if (value.contains("After")) {
      operator = SearchOperators.GREATER;
    }
    SearchMetadata smd = new SearchMetadata(facet.getIndex().replace("md.", ""), SearchFields.date,
        operator, dateValue, false);
    return new SearchFactory().addElement(smd, AND).build();
  }

  /**
   * Build the SearchQuery for system metadata
   * 
   * @param facet
   * @param value
   * @return
   * @throws UnprocessableError
   */
  private SearchQuery buildSystemQuery(Facet facet, String value) {
    try {
      return new SearchFactory()
          .addElement(new SearchPair(SearchFields.valueOf(facet.getIndex()), value), AND).build();
    } catch (UnprocessableError e) {
      LOGGER.error("Error building facet system query", e);
      return new SearchQuery();
    }
  }

  public String addQuery(SearchQuery fq) throws UnprocessableError {
    return SearchQueryParser
        .transform2UTF8URL(new SearchFactory(fq).and(query.getElements()).build());
  }

  /**
   * @return the label
   */
  public String getLabel() {
    return label;
  }

  /**
   * @return the count
   */
  public long getCount() {
    return count;
  }

  /**
   * @return the type
   */
  public String getType() {
    return type;
  }

}
