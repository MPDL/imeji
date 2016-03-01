/**
 * License: src/main/resources/license/escidoc.license
 */
package de.mpg.imeji.presentation.facet;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import de.mpg.imeji.logic.controller.ItemController;
import de.mpg.imeji.logic.search.SearchResult;
import de.mpg.imeji.logic.search.model.SearchIndex.SearchFields;
import de.mpg.imeji.logic.search.model.SearchLogicalRelation.LOGICAL_RELATIONS;
import de.mpg.imeji.logic.search.model.SearchOperators;
import de.mpg.imeji.logic.search.model.SearchPair;
import de.mpg.imeji.logic.search.model.SearchQuery;
import de.mpg.imeji.logic.vo.Item;
import de.mpg.imeji.logic.vo.Metadata;
import de.mpg.imeji.logic.vo.Properties.Status;
import de.mpg.imeji.presentation.beans.Navigation;
import de.mpg.imeji.presentation.facet.Facet.FacetType;
import de.mpg.imeji.presentation.filter.Filter;
import de.mpg.imeji.presentation.filter.FiltersSession;
import de.mpg.imeji.presentation.image.ItemsBean;
import de.mpg.imeji.presentation.session.SessionBean;
import de.mpg.imeji.presentation.util.BeanHelper;

/**
 * Initiliaze all technical {@link Facet}
 * 
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public class TechnicalFacets extends Facets {
  private SessionBean sb = (SessionBean) BeanHelper.getSessionBean(SessionBean.class);
  private FiltersSession fs = (FiltersSession) BeanHelper.getSessionBean(FiltersSession.class);
  private List<List<Facet>> facets = new ArrayList<List<Facet>>();
  private SearchQuery searchQuery;
  private SearchResult allImages =
      ((ItemsBean) BeanHelper.getSessionBean(ItemsBean.class)).getSearchResult();
  private String baseURI =
      ((Navigation) BeanHelper.getApplicationBean(Navigation.class)).getBrowseUrl() + "?q=";

  /**
   * Constructor
   * 
   * @param searchQuery
   */
  public TechnicalFacets(SearchQuery searchQuery, SearchResult allImages) {
    this.searchQuery = searchQuery;
    this.allImages = allImages;
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
      int sizeAllImages = allImages.getNumberOfRecords();
      if (sizeAllImages > 0) {
        if (sb.getUser() != null) {
          if (!fs.isFilter("my_images") && !fs.isNoResultFilter("my_images")) {
            // NOT WORKING AND NOT USEFULL SO FAR
            /*
             * SearchPair myImageSearchPair = new
             * SearchPair(SPARQLSearch.getIndex(SearchIndex.IndexNames.user),
             * SearchOperators.EQUALS, sb.getUser().getEmail()); count = getCount(searchQuery,
             * myImageSearchPair, allImages.getResults()); if (count > 0) { techFacets.add(new
             * Facet(uriFactory.createFacetURI(baseURI, myImageSearchPair, "my_images",
             * FacetType.TECHNICAL), "my_images", count, FacetType.TECHNICAL, null)); } else {
             * fs.getNoResultsFilters().add(new Filter("My images", "", 0, FacetType.TECHNICAL,
             * null)); }
             */
          }
          if (!fs.isFilter("pending_images") && !fs.isNoResultFilter("pending_images")) {
            SearchPair privatePair = new SearchPair(SearchFields.status, SearchOperators.EQUALS,
                Status.PENDING.getUriString(), false);
            count = getCount(searchQuery, privatePair, allImages.getResults());
            if (count > 0) {
              techFacets
                  .add(new Facet(
                      uriFactory.createFacetURI(baseURI, privatePair, "pending_images",
                          FacetType.TECHNICAL),
                      "pending_images", count, FacetType.TECHNICAL, null));
            }
          }
          if (!fs.isFilter("released_images") && !fs.isNoResultFilter("released_images")) {
            SearchPair publicPair = new SearchPair(SearchFields.status, SearchOperators.EQUALS,
                Status.RELEASED.getUriString().toString(), false);
            count = getCount(searchQuery, publicPair, allImages.getResults());
            if (count > 0) {
              techFacets
                  .add(new Facet(
                      uriFactory.createFacetURI(baseURI, publicPair, "released_images",
                          FacetType.TECHNICAL),
                      "released_images", count, FacetType.TECHNICAL, null));
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
            count = getCount(searchQuery, pair, allImages.getResults());
            if (count > 0 && count < allImages.getNumberOfRecords()) {
              techFacets.add(
                  new Facet(uriFactory.createFacetURI(baseURI, pair, t.name(), FacetType.TECHNICAL),
                      t.toString(), count, FacetType.TECHNICAL, null));
            } else {
              fs.getNoResultsFilters()
                  .add(new Filter(t.toString(), "", 0, FacetType.TECHNICAL, null));
            }
            count = 0;
          }
        }
      }
      facets.add(techFacets);
    } catch (UnsupportedEncodingException e) {
      Logger.getLogger(TechnicalFacets.class)
          .error("There had been some issues with the technical facets", e);
    }
  }

  /**
   * Retrieve all {@link Item} currently browsed
   * 
   * @param searchQuery
   * @return
   */
  public SearchResult retrieveAllImages(SearchQuery searchQuery) {
    return ((ItemsBean) BeanHelper.getSessionBean(ItemsBean.class)).getSearchResult();
  }

  /**
   * Count the number of item for a facet with one {@link SearchPair}
   * 
   * @param searchQuery
   * @param pair
   * @param allImages
   * @return
   */
  public int getCount(SearchQuery searchQuery, SearchPair pair, List<String> allImages) {
    ItemController ic = new ItemController();
    SearchQuery q = searchQuery.copy();
    q.addLogicalRelation(LOGICAL_RELATIONS.AND);
    q.addPair(pair);
    return ic.search(null, q, null, sb.getUser(), sb.getSelectedSpaceString(), -1, 0)
        .getNumberOfRecords();
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
