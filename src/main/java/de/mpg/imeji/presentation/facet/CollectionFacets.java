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
import de.mpg.imeji.logic.Imeji;
import de.mpg.imeji.logic.controller.resource.ItemController;
import de.mpg.imeji.logic.controller.resource.ProfileController;
import de.mpg.imeji.logic.search.model.SearchIndex.SearchFields;
import de.mpg.imeji.logic.search.model.SearchLogicalRelation.LOGICAL_RELATIONS;
import de.mpg.imeji.logic.search.model.SearchOperators;
import de.mpg.imeji.logic.search.model.SearchPair;
import de.mpg.imeji.logic.search.model.SearchQuery;
import de.mpg.imeji.logic.search.model.SearchResult;
import de.mpg.imeji.logic.util.ObjectHelper;
import de.mpg.imeji.logic.vo.CollectionImeji;
import de.mpg.imeji.logic.vo.Item;
import de.mpg.imeji.logic.vo.MetadataProfile;
import de.mpg.imeji.logic.vo.Statement;
import de.mpg.imeji.logic.vo.User;
import de.mpg.imeji.presentation.beans.MetadataLabels;
import de.mpg.imeji.presentation.facet.Facet.FacetType;

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
  private MetadataProfile profile;
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
    this.profile = new ProfileController().retrieve(col.getProfile(), Imeji.adminUser);
    this.locale = locale;
  }

  /*
   * (non-Javadoc)
   *
   * @see de.mpg.imeji.presentation.facet.Facets#init()
   */
  @Override
  public void init() {
    // Use relativ url instead of aboslut, due to issues with space url
    // String baseURI = nav.getCollectionUrl() + ObjectHelper.getId(colURI)
    // + "/" + nav.getBrowsePath() + "?q=";
    String baseURI = "?q=";
    FacetURIFactory uriFactory = new FacetURIFactory(searchQuery);
    int count = 0;
    int sizeAllImages = allImages.getNumberOfRecords();
    HashSet<String> set = new HashSet<String>(allImages.getResults());
    if (profile != null) {
      MetadataLabels metadataLabels = new MetadataLabels(profile, locale);
      try {
        for (Statement st : profile.getStatements()) {
          List<Facet> group = new ArrayList<Facet>();
          if (st.isPreview()) {
            SearchPair pair = new SearchPair(SearchFields.statement, SearchOperators.EQUALS,
                st.getId().toString(), false);
            count = getCount(searchQuery, pair, set);

            group.add(new Facet(
                uriFactory.createFacetURI(baseURI, pair, getName(st.getId()), FacetType.COLLECTION),
                metadataLabels.getInternationalizedLabels().get(st.getId()), count,
                FacetType.COLLECTION, st.getId(), locale, metadataLabels));

            // create this facet only if there are no
            if (count <= sizeAllImages) {
              pair.setNot(true);
              group.add(new Facet(
                  uriFactory.createFacetURI(baseURI, pair, "No " + getName(st.getId()),
                      FacetType.COLLECTION),
                  "No " + getName(st.getId()), sizeAllImages - count, FacetType.COLLECTION,
                  st.getId(), locale, metadataLabels));
            }
          }
          facets.add(group);
        }
      } catch (Exception e) {
        LOGGER.error("Error in collection facets", e);
      }
    }
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
    ItemController ic = new ItemController();
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
