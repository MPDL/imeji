package de.mpg.imeji.logic.search.factory;


import de.mpg.imeji.logic.search.Search;
import de.mpg.imeji.logic.search.Search.SearchObjectTypes;
import de.mpg.imeji.logic.search.elasticsearch.ElasticSearch;
import de.mpg.imeji.logic.search.jenasearch.JenaSearch;

/**
 * Factory for {@link Search}
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public class SearchFactory {

  private static SEARCH_IMPLEMENTATIONS defaultSearch = SEARCH_IMPLEMENTATIONS.JENA;

  public enum SEARCH_IMPLEMENTATIONS {
    JENA, ELASTIC;
  }

  /**
   * Create a new {@link Search}
   *
   * @return
   */
  public static Search create() {
    return create(defaultSearch);
  }

  /**
   * Create A new {@link Search}
   *
   * @param impl
   * @return
   */
  public static Search create(SEARCH_IMPLEMENTATIONS impl) {
    return create(SearchObjectTypes.ALL, impl);
  }

  /**
   * Create a new {@link Search}
   *
   * @param type
   * @param impl TODO
   * @return
   */
  public static Search create(SearchObjectTypes type, SEARCH_IMPLEMENTATIONS impl) {
    switch (impl) {
      case JENA:
        return new JenaSearch(type, null);
      case ELASTIC:
        return new ElasticSearch(type);
    }
    return null;
  }

  /**
   * Create a new {@link Search} !!! Only for JENA Search !!!
   *
   * @param type
   * @param containerUri
   * @return
   */
  public static Search create(SearchObjectTypes type, String containerUri) {
    return new JenaSearch(type, containerUri);
  }
}
