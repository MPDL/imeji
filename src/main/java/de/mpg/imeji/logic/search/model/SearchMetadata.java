package de.mpg.imeji.logic.search.model;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import de.mpg.imeji.logic.search.model.SearchIndex.SearchFields;
import de.mpg.imeji.logic.vo.predefinedMetadata.Metadata;

/**
 * Specific {@link SearchPair} for {@link Metadata} search
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public class SearchMetadata extends SearchPair {
  private final URI statement;

  /**
   * Default Constructor
   */
  public SearchMetadata() {
    this.statement = null;
  }

  public SearchMetadata(SearchFields field, SearchOperators operator, String value, URI statement,
      boolean not) {
    super(field, operator, value, not);
    this.statement = statement;
  }

  @Override
  public SEARCH_ELEMENTS getType() {
    return SEARCH_ELEMENTS.METADATA;
  }

  @Override
  public List<SearchElement> getElements() {
    return new ArrayList<>();
  }

  public URI getStatement() {
    return statement;
  }
}
