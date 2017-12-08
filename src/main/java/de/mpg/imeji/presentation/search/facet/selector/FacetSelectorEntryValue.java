package de.mpg.imeji.presentation.search.facet.selector;

import static de.mpg.imeji.logic.search.model.SearchLogicalRelation.LOGICAL_RELATIONS.AND;

import java.io.Serializable;
import java.util.List;

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
  private String label;
  private String index;
  private long count;
  private String type;
  private String addQuery;
  private String removeQuery;
  private SearchQuery entryQuery;
  private boolean selected = false;
  private String max;
  private String min;


  public FacetSelectorEntryValue(FacetResultValue resultValue, Facet facet,
      SearchQuery facetsQuery) {
    this.index = facet.getIndex();
    this.type = facet.getType();
    this.count = resultValue.getCount();
    if (facet.getType().equals(StatementType.DATE.name())
        || facet.getType().equals(StatementType.NUMBER.name())) {
      initRangeEntry(resultValue, facet, facetsQuery);
    } else {
      initEntry(resultValue, facet, facetsQuery);
    }
  }

  private void initEntry(FacetResultValue resultValue, Facet facet, SearchQuery facetsQuery) {
    this.label = readLabel(resultValue, facet);
    this.entryQuery = buildEntryQuery(facet, toQueryValue(resultValue, facet, facetsQuery));
  }

  private void initRangeEntry(FacetResultValue resultValue, Facet facet, SearchQuery facetsQuery) {
    this.label = readQueryValue(facet, facetsQuery);
    this.min = facet.getType().equals(StatementType.DATE.name())
        ? DateFormatter.format(resultValue.getMin()) : resultValue.getMin();
    this.max = facet.getType().equals(StatementType.DATE.name())
        ? DateFormatter.format(resultValue.getMax()) : resultValue.getMax();
    if (label == null) {
      this.label = "from " + min + " to " + max;
    }
    this.entryQuery = buildMetadataQuery(facet, label);
  }

  /**
   * 
   * @param resultValue
   * @param facet
   * @return
   */
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

  /**
   * Read the value of the facet according to the query (and not from the facet result value)
   * 
   * @param resultValue
   * @param facet
   * @param facetsQuery
   * @return
   */
  private String readQueryValue(Facet facet, SearchQuery facetsQuery) {
    String index = facet.getIndex().replace("md.", "").split("\\.")[0];
    List<SearchMetadata> mds = new SearchFactory(facetsQuery).getElementsWithIndex(index);
    if (mds.size() > 0) {
      return mds.get(0).getValue();
    }
    return null;
  }

  /**
   * TRansforma the value from the facetResultvalue to a searchquery value
   * 
   * @param resultValue
   * @param facet
   * @param facetsQuery
   * @return
   */
  private String toQueryValue(FacetResultValue resultValue, Facet facet, SearchQuery facetsQuery) {
    if (facet.getIndex().equals(SearchFields.collection.getIndex())) {
      return resultValue.getLabel().split(" ", 2)[0];
    }
    if (facet.getIndex().equals(SearchFields.license.getIndex())) {
      if ("Any".equalsIgnoreCase(resultValue.getLabel())) {
        return "*";
      } else {
        return resultValue.getLabel();
      }
    }
    return label;
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
          return buildMetadataNumberQuery(facet, value);
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
    String mdIndex = facet.getIndex().replace("md.", "").split("\\.")[0];
    SearchMetadata smd = new SearchMetadata(mdIndex, SearchMetadataFields.date,
        SearchOperators.EQUALS, value, false);
    return new SearchFactory().and(smd).build();
  }

  private SearchQuery buildMetadataNumberQuery(Facet facet, String value)
      throws UnprocessableError {
    String mdIndex = facet.getIndex().replace("md.", "").split("\\.")[0];
    SearchMetadata smd = new SearchMetadata(mdIndex, SearchMetadataFields.number,
        SearchOperators.EQUALS, value, false);
    return new SearchFactory().and(smd).build();
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
