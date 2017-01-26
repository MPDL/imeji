package de.mpg.imeji.presentation.facet;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;

import org.apache.log4j.Logger;

import de.mpg.imeji.exceptions.UnprocessableError;
import de.mpg.imeji.logic.search.SearchQueryParser;
import de.mpg.imeji.logic.search.model.SearchLogicalRelation.LOGICAL_RELATIONS;
import de.mpg.imeji.logic.search.model.SearchPair;
import de.mpg.imeji.logic.search.model.SearchQuery;
import de.mpg.imeji.presentation.facet.Facet.FacetType;

/**
 * Factory for URI used in Facets and Filters
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public class FacetURIFactory {
  private static final Logger LOGGER = Logger.getLogger(FacetURIFactory.class);
  private SearchQuery searchQuery = new SearchQuery();

  /**
   * Constructor for one {@link SearchQuery}
   *
   * @param searchQuery
   */
  public FacetURIFactory(SearchQuery searchQuery) {
    this.searchQuery = searchQuery;
  }

  /**
   * Create an {@link URI} attached to a {@link Facet} (for the link on which the user click to see
   * the facet)
   *
   * @param baseURI
   * @param pair
   * @param facetName
   * @param type
   * @return
   * @throws UnsupportedEncodingException
   */
  public URI createFacetURI(String baseURI, SearchPair pair, String facetName, FacetType type)
      throws UnsupportedEncodingException {
    final SearchQuery sq = new SearchQuery(searchQuery.getElements());
    try {
      sq.addLogicalRelation(LOGICAL_RELATIONS.AND);
      sq.addPair(pair);
    } catch (final UnprocessableError e) {
      LOGGER.error("Error creating facet URI", e);
    }

    final String uri = baseURI + getCommonURI(sq, facetName, type);
    return URI.create(uri);
  }

  /**
   * Return parameters part of {@link URI} for this {@link Facet}
   *
   * @param sq
   * @param facetName
   * @param type
   * @return
   * @throws UnsupportedEncodingException
   */
  private String getCommonURI(SearchQuery sq, String facetName, FacetType type)
      throws UnsupportedEncodingException {
    return SearchQueryParser.transform2UTF8URL(sq) + "&f=" + URLEncoder.encode(facetName, "UTF-8")
        + "&t=" + URLEncoder.encode(type.name().toLowerCase(), "UTF-8") + "&page=1";
  }
}
