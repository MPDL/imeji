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

  /**
   * Constructor with default value for operator (REGEX) and not (false)
   * 
   * @param index
   * @param value
   */
  public SearchMetadata(String index, String value) {
    super(null, value);
    this.index = index;
  }

  /**
   * Search for a particular SearchField of a metadata (for instance familyName)
   * 
   * @param index
   * @param f
   * @param value
   */
  public SearchMetadata(String index, SearchFields f, String value) {
    super(f, value);
    this.index = new StatementFactory().setIndex(index).build().getIndexUrlEncoded();;
  }

  /**
   * Search for a {@link Metadata} for a particular field (for instance family)
   * 
   * @param index
   * @param operator
   * @param value
   * @param not
   */
  public SearchMetadata(String index, SearchFields f, SearchOperators operator, String value,
      boolean not) {
    super(f, operator, value, not);
    this.index = index;
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

}
