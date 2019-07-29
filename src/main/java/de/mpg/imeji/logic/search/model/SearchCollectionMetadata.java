package de.mpg.imeji.logic.search.model;

import java.util.ArrayList;
import java.util.List;

import de.mpg.imeji.logic.model.Metadata;
import de.mpg.imeji.logic.model.SearchMetadataFields;
import de.mpg.imeji.logic.model.factory.StatementFactory;

/**
 * Specific {@link SearchPair} for {@link Metadata} search
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public class SearchCollectionMetadata extends SearchPair {
  private static final long serialVersionUID = -7422025148855635937L;
  private final String label;
  private final String index;

  /**
   * Search for a particular SearchField of a metadata (for instance familyName)
   * 
   * @param index
   * @param f
   * @param value
   */
  public SearchCollectionMetadata(String index, String value) {
    super(null, value);
    this.index = index;
    this.label = indexToLabel(index);
  }

  /**
   * Search for a {@link Metadata} for a particular field (for instance family)
   * 
   * @param index
   * @param operator
   * @param value
   * @param not
   */
  public SearchCollectionMetadata(String index, SearchOperators operator, String value, boolean not) {
    super(null, operator, value, not);
    this.index = index;
    this.label = indexToLabel(index);
  }

  @Override
  public SEARCH_ELEMENTS getType() {
    return SEARCH_ELEMENTS.SIMPLE_METADATA;
  }

  @Override
  public List<SearchElement> getElements() {
    return new ArrayList<>();
  }



  @Override
  public boolean isSame(SearchElement element) {
    if (SEARCH_ELEMENTS.SIMPLE_METADATA == element.getType()) {
      SearchCollectionMetadata smd = (SearchCollectionMetadata) element;
      return smd.getLabel().equals(label) && smd.getValue().equals(getValue()) && smd.getOperator().equals(getOperator());
    }
    return false;
  }

  public String getLabel() {
    return label;
  }


  public static String labelToIndex(String label) {
    return "collection.md." + label.replaceAll(" ", "_");
  }

  public static String indexToLabel(String index) {

    //collection.md. has 14 characters
    index = index.substring(14, index.length());
    return index.replaceAll("_", " ");


  }


}
