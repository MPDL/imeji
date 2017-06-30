package de.mpg.imeji.logic.search.model;

import java.util.ArrayList;
import java.util.List;

import de.mpg.imeji.logic.vo.Metadata;
import de.mpg.imeji.logic.vo.factory.StatementFactory;

/**
 * Specific {@link SearchPair} for {@link Metadata} search
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public class SearchMetadata extends SearchPair {
  private static final long serialVersionUID = -7422025148855635937L;
  private final String index;
  private final SearchMetadataFields metadataField;

  /**
   * Search for a particular SearchField of a metadata (for instance familyName)
   * 
   * @param index
   * @param f
   * @param value
   */
  public SearchMetadata(String index, SearchMetadataFields f, String value) {
    super(null, value);
    this.index = new StatementFactory().setIndex(index).build().getIndexUrlEncoded();
    this.metadataField = f;
  }

  /**
   * Search for a {@link Metadata} for a particular field (for instance family)
   * 
   * @param index
   * @param operator
   * @param value
   * @param not
   */
  public SearchMetadata(String index, SearchMetadataFields f, SearchOperators operator,
      String value, boolean not) {
    super(null, operator, value, not);
    this.index = index;
    this.metadataField = f;
  }

  @Override
  public SEARCH_ELEMENTS getType() {
    return SEARCH_ELEMENTS.METADATA;
  }

  @Override
  public List<SearchElement> getElements() {
    return new ArrayList<>();
  }

  /**
   * @return the index
   */
  public String getIndex() {
    return index;
  }

  @Override
  public boolean isSame(SearchElement element) {
    if (SEARCH_ELEMENTS.METADATA == element.getType()) {
      SearchMetadata smd = (SearchMetadata) element;
      return smd.getIndex().equals(index) && smd.getMetadataField().equals(getMetadataField())
          && smd.getValue().equals(getValue()) && smd.getOperator().equals(getOperator());
    }
    return false;
  }

  /**
   * @return the metadataField
   */
  public SearchMetadataFields getMetadataField() {
    return metadataField;
  }

}
