/**
 * License: src/main/resources/license/escidoc.license
 */
package de.mpg.imeji.presentation.facet;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.log4j.Logger;

import de.mpg.imeji.exceptions.UnprocessableError;
import de.mpg.imeji.logic.Imeji;
import de.mpg.imeji.logic.controller.resource.ItemController;
import de.mpg.imeji.logic.search.model.SearchIndex.SearchFields;
import de.mpg.imeji.logic.search.model.SearchLogicalRelation.LOGICAL_RELATIONS;
import de.mpg.imeji.logic.search.model.SearchOperators;
import de.mpg.imeji.logic.search.model.SearchPair;
import de.mpg.imeji.logic.search.model.SearchQuery;
import de.mpg.imeji.logic.vo.Properties.Status;
import de.mpg.imeji.logic.vo.User;
import de.mpg.imeji.logic.vo.predefinedMetadata.Metadata;
import de.mpg.imeji.presentation.beans.Navigation;
import de.mpg.imeji.presentation.facet.Facet.FacetType;
import de.mpg.imeji.presentation.util.BeanHelper;

/**
 * Initiliaze all technical {@link Facet}
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public class TechnicalFacets extends FacetsAbstract {
  private static final Logger LOGGER = Logger.getLogger(TechnicalFacets.class);
  private FacetFiltersSession fs = (FacetFiltersSession) BeanHelper.getSessionBean(FacetFiltersSession.class);
  private List<List<Facet>> facets = new ArrayList<List<Facet>>();
  private SearchQuery searchQuery;
  private String baseURI =
      Imeji.PROPERTIES.getApplicationURL() + Navigation.BROWSE.getPath() + "?q=";
  private Locale locale;
  private User user;
  private String space;

  /**
   * Constructor
   *
   * @param searchQuery
   */
  public TechnicalFacets(SearchQuery searchQuery, User user, Locale locale, String space) {
    this.searchQuery = searchQuery;
    this.locale = locale;
    this.user = user;
    this.locale = locale;
    this.space = space;
  }

  /*
   * (non-Javadoc)
   *
   * @see de.mpg.imeji.presentation.facet.Facets#init()
   */
  @Override
  public void init() {
    FacetURIFactory uriFactory = new FacetURIFactory(searchQuery);
    List<Facet> techFacets = new ArrayList<Facet>();
    try {
      int count = 0;
      if (user != null) {
        if (!fs.isFilter("pending_images") && !fs.isNoResultFilter("pending_images")) {
          SearchPair privatePair = new SearchPair(SearchFields.status, SearchOperators.EQUALS,
              Status.PENDING.getUriString(), false);
          count = getCount(searchQuery, privatePair);
          if (count > 0) {
            techFacets.add(new Facet(
                uriFactory.createFacetURI(baseURI, privatePair, "pending_images",
                    FacetType.TECHNICAL),
                "pending_images", count, FacetType.TECHNICAL, null, locale, null));
          }
        }
        if (!fs.isFilter("released_images") && !fs.isNoResultFilter("released_images")) {
          SearchPair publicPair = new SearchPair(SearchFields.status, SearchOperators.EQUALS,
              Status.RELEASED.getUriString().toString(), false);
          count = getCount(searchQuery, publicPair);
          if (count > 0) {
            techFacets.add(new Facet(
                uriFactory.createFacetURI(baseURI, publicPair, "released_images",
                    FacetType.TECHNICAL),
                "released_images", count, FacetType.TECHNICAL, null, locale, null));
          }
        }
      }

      boolean showFacet = false;
      for (Metadata.Types t : Metadata.Types.values()) {
        showFacet = (Metadata.Types.GEOLOCATION.name().equals(t.name())
            || Metadata.Types.LICENSE.name().equals(t.name())
            || Metadata.Types.PUBLICATION.name().equals(t.name()));
        if (!fs.isFilter(t.name()) && !fs.isNoResultFilter(t.name()) && showFacet) {
          SearchPair pair = new SearchPair(SearchFields.metadatatype, SearchOperators.EQUALS,
              t.getClazzNamespace(), false);
          count = getCount(searchQuery, pair);
          if (count > 0) {
            techFacets.add(
                new Facet(uriFactory.createFacetURI(baseURI, pair, t.name(), FacetType.TECHNICAL),
                    t.toString(), count, FacetType.TECHNICAL, null, locale, null));
          } else {
            fs.getNoResultsFilters()
                .add(new FacetFilter(t.toString(), "", 0, FacetType.TECHNICAL, null, locale, null));
          }
          count = 0;
        }
      }
      facets.add(techFacets);
    } catch (

    UnsupportedEncodingException e)

    {
      Logger.getLogger(TechnicalFacets.class)
          .error("There had been some issues with the technical facets", e);
    }

  }

  /**
   * Count the number of item for a facet with one {@link SearchPair}
   *
   * @param searchQuery
   * @param pair
   * @param allImages
   * @return
   */
  public int getCount(SearchQuery searchQuery, SearchPair pair) {
    ItemController ic = new ItemController();
    SearchQuery q = searchQuery.copy();

    try {
      q.addLogicalRelation(LOGICAL_RELATIONS.AND);
      q.addPair(pair);
    } catch (UnprocessableError e) {
      LOGGER.error("Error creating query to get Facet count", e);
    }
    return ic.search(null, q, null, user, space, -1, 0).getNumberOfRecords();
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

  public void setFacets(List<List<Facet>> facets) {
    this.facets = facets;
  }
}
