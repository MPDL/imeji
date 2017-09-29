package de.mpg.imeji.presentation.search.facet.selector;

import static de.mpg.imeji.logic.search.model.SearchLogicalRelation.LOGICAL_RELATIONS.AND;

import java.io.Serializable;

import org.apache.log4j.Logger;

import de.mpg.imeji.exceptions.UnprocessableError;
import de.mpg.imeji.logic.model.ImejiLicenses;
import de.mpg.imeji.logic.model.SearchFields;
import de.mpg.imeji.logic.model.SearchMetadataFields;
import de.mpg.imeji.logic.model.StatementType;
import de.mpg.imeji.logic.search.facet.model.Facet;
import de.mpg.imeji.logic.search.facet.model.FacetResultValue;
import de.mpg.imeji.logic.search.factory.SearchFactory;
import de.mpg.imeji.logic.search.model.SearchMetadata;
import de.mpg.imeji.logic.search.model.SearchOperators;
import de.mpg.imeji.logic.search.model.SearchPair;
import de.mpg.imeji.logic.search.model.SearchQuery;
import de.mpg.imeji.util.DateFormatter;

/**
 * Value of a {@link FacetSelectorEntry}
 * 
 * @author saquet
 *
 */
public class FacetSelectorEntryValue implements Serializable {
  private static final long serialVersionUID = -5562614379983226471L;
  private static final Logger LOGGER = Logger.getLogger(FacetSelectorEntryValue.class);
  private final String label;
  private final String index;
  private final long count;
  private final String type;
  private String addQuery;
  private String removeQuery;
  private SearchQuery entryQuery;
  private boolean selected = false;
  private final String max;
  private final String min;


  public FacetSelectorEntryValue(FacetResultValue resultValue, Facet facet,
      SearchQuery facetsQuery) {
    this.label = readLabel(resultValue, facet);
    this.count = resultValue.getCount();
    this.index = facet.getIndex();
    this.type = facet.getType();
    this.min = facet.getType().equals(StatementType.DATE.name())
        ? DateFormatter.format(resultValue.getMin()) : resultValue.getMin();
    this.max = facet.getType().equals(StatementType.DATE.name())
        ? DateFormatter.format(resultValue.getMax()) : resultValue.getMax();
    this.entryQuery = buildEntryQuery(facet, readQueryValue(resultValue, facet));
  }

  private String readLabel(FacetResultValue resultValue, Facet facet) {
    if (facet.getIndex().equals(SearchFields.collection.getIndex())) {
      return resultValue.getLabel().split(" ", 2)[1];
    }
    if (facet.getIndex().equals(SearchFields.license.getIndex())
        && resultValue.getLabel().equals(ImejiLicenses.NO_LICENSE)) {
      return "None";
    }
    return resultValue.getLabel();
  }

  private String readQueryValue(FacetResultValue resultValue, Facet facet) {
    if (facet.getIndex().equals(SearchFields.collection.getIndex())) {
      return resultValue.getLabel().split(" ", 2)[0];
    }
    if (facet.getIndex().equals(SearchFields.license.getIndex())
        && "Any".equalsIgnoreCase(resultValue.getLabel())) {
      return "*";
    }
    return resultValue.getLabel();
  }

  private SearchQuery buildEntryQuery(Facet facet, String value) {
    boolean isMetadataFacet = facet.getIndex().startsWith("md.");
    if (!"*".equals(value) && !facet.getIndex().equals(SearchFields.collection.getIndex())) {
      value = "\"" + value + "\"";
    }
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
          return new SearchQuery();
        case DATE:
          return buildMetadataDateQuery(facet, value);
        case PERSON:
          return buildMetadataTextQuery(facet, value);
        case URL:
          return buildMetadataTextQuery(facet, value);
        case GEOLOCATION:
          return new SearchQuery();
      }
    } catch (Exception e) {
      LOGGER.error("Error building facet metadata query", e);
    }
    return new SearchQuery();
  }

  private SearchQuery buildMetadataTextQuery(Facet facet, String value) throws UnprocessableError {
    String mdIndex = facet.getIndex().replace("md.", "").split("\\.")[0];
    SearchMetadata smd = new SearchMetadata(mdIndex, SearchMetadataFields.exact, value);
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
    SearchMetadata smd = new SearchMetadata(facet.getIndex().replace("md.", ""),
        SearchMetadataFields.date, operator, dateValue, false);
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
          .addElement(new SearchPair(SearchFields.valueOfIndex(facet.getIndex()), value), AND)
          .build();
    } catch (UnprocessableError e) {
      LOGGER.error("Error building facet system query", e);
      return new SearchQuery();
    }
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



  /**
   * @return the index
   */
  public String getIndex() {
    return index;
  }

  /**
   * @return the selected
   */
  public boolean isSelected() {
    return selected;
  }

  /**
   * @param selected the selected to set
   */
  public void setSelected(boolean selected) {
    this.selected = selected;
  }

  public SearchQuery getEntryQuery() {
    return entryQuery;
  }

  /**
   * @return the addQuery
   */
  public String getAddQuery() {
    return addQuery;
  }


  public void setAddQuery(String addQuery) {
    this.addQuery = addQuery;
  }

  public String getRemoveQuery() {
    return removeQuery;
  }

  public void setRemoveQuery(String removeQuery) {
    this.removeQuery = removeQuery;
  }

  /**
   * @return the max
   */
  public String getMax() {
    return max;
  }

  /**
   * @return the min
   */
  public String getMin() {
    return min;
  }

}
