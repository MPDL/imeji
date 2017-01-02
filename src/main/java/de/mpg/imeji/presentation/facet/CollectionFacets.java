/**
 * License: src/main/resources/license/escidoc.license
 */
package de.mpg.imeji.presentation.facet;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

import org.apache.log4j.Logger;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.exceptions.UnprocessableError;
import de.mpg.imeji.logic.search.model.SearchLogicalRelation.LOGICAL_RELATIONS;
import de.mpg.imeji.logic.item.ItemService;
import de.mpg.imeji.logic.search.model.SearchPair;
import de.mpg.imeji.logic.search.model.SearchQuery;
import de.mpg.imeji.logic.search.model.SearchResult;
import de.mpg.imeji.logic.util.ObjectHelper;
import de.mpg.imeji.logic.vo.CollectionImeji;
import de.mpg.imeji.logic.vo.Item;
import de.mpg.imeji.logic.vo.User;

/**
 * Facets for the item browsed within a collection
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public class CollectionFacets extends FacetsAbstract {
  private List<List<Facet>> facets = new ArrayList<List<Facet>>();
  private URI colURI = null;
  private SearchQuery searchQuery;
  private SearchResult allImages;
  private Locale locale;
  private User user;
  private static final Logger LOGGER = Logger.getLogger(CollectionFacets.class);

  /**
   * Constructor for the {@link Facet}s of one {@link CollectionImeji} with one {@link SearchQuery}
   *
   * @param col
   * @param searchQuery
   * @throws ImejiException
   */
  public CollectionFacets(CollectionImeji col, SearchQuery searchQuery, SearchResult r, User user,
      Locale locale) throws ImejiException {
    if (col == null) {
      return;
    }
    allImages = r;
    this.colURI = col.getId();
    this.searchQuery = searchQuery;
    this.user = user;
    this.locale = locale;
  }

  /*
   * (non-Javadoc)
   *
   * @see de.mpg.imeji.presentation.facet.Facets#init()
   */
  @Override
  public void init() {

  }

  /**
   * Get
   *
   * @param uri
   * @return
   */
  public String getName(URI uri) {
    return ObjectHelper.getId(uri);
  }

  /**
   * Count {@link Item} for one facet
   *
   * @param searchQuery
   * @param pair
   * @param collectionImages
   * @return
   */
  public int getCount(SearchQuery searchQuery, SearchPair pair, HashSet<String> collectionImages) {
    int counter = 0;
    ItemService ic = new ItemService();
    SearchQuery sq = new SearchQuery();
    if (pair != null) {
      try {
        sq.addLogicalRelation(LOGICAL_RELATIONS.AND);
        sq.addPair(pair);
      } catch (UnprocessableError e) {
        LOGGER.error("Error creating query to count facet size", e);
      }

    }
    SearchResult res = ic.search(colURI, sq, null, user, null, -1, 0);
    for (String record : res.getResults()) {
      if (collectionImages.contains(record)) {
        counter++;
      }
    }
    return counter;
  }

  /*
   * (non-Javadoc)
   *
   * @see de.mpg.imeji.presentation.facet.Facets#getFacets()
   */
  @Override
  public List<List<Facet>> getFacets() {
    return facets;
  }
}
