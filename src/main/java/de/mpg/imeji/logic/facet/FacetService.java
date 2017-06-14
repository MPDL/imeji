package de.mpg.imeji.logic.facet;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.exceptions.NotFoundException;
import de.mpg.imeji.logic.config.Imeji;
import de.mpg.imeji.logic.facet.controller.FacetController;
import de.mpg.imeji.logic.facet.model.Facet;
import de.mpg.imeji.logic.search.Search.SearchObjectTypes;
import de.mpg.imeji.logic.search.jenasearch.ImejiSPARQL;
import de.mpg.imeji.logic.search.jenasearch.JenaCustomQueries;
import de.mpg.imeji.logic.search.model.SearchQuery;
import de.mpg.imeji.logic.search.model.SearchResult;
import de.mpg.imeji.logic.search.model.SortCriterion;
import de.mpg.imeji.logic.service.SearchServiceAbstract;
import de.mpg.imeji.logic.util.ObjectHelper;
import de.mpg.imeji.logic.vo.User;

/**
 * Service for the Facets
 * 
 * @author saquet
 *
 */
public class FacetService extends SearchServiceAbstract<Facet> {

  private static List<Facet> cachedFacets = null;
  private static Map<String, Facet> cachedFacetsMapByIndex = new HashMap<>();
  private static Map<String, Facet> cachedFacetsMapByName = new HashMap<>();
  private static final Logger LOGGER = Logger.getLogger(FacetService.class);

  public FacetService() {
    super(SearchObjectTypes.ALL);
  }

  private final FacetController controller = new FacetController();

  public Facet create(Facet facet, User user) throws ImejiException {
    facet = controller.create(facet, user);
    retrieveAll();// renew Cache
    return facet;
  }

  public Facet update(Facet facet, User user) throws ImejiException {
    facet = controller.update(facet, user);
    retrieveAll();// renew Cache
    return facet;
  }

  public Facet read(String id) throws ImejiException {
    if (id == null) {
      throw new NotFoundException("Id is null");
    }
    return controller.retrieve(ObjectHelper.getURI(Facet.class, id).toString(), Imeji.adminUser);
  }

  public void delete(Facet facet, User user) throws ImejiException {
    controller.delete(facet, user);
    retrieveAll();// renew Cache
  }

  @Override
  public SearchResult search(SearchQuery searchQuery, SortCriterion sortCri, User user, int size,
      int offset) {
    // Not implemented
    return null;
  }

  @Override
  public List<Facet> retrieve(List<String> ids, User user) throws ImejiException {
    return controller.retrieveBatch(ids, Imeji.adminUser);
  }

  @Override
  public List<Facet> retrieveAll() throws ImejiException {
    final List<String> uris =
        ImejiSPARQL.exec(JenaCustomQueries.selectFacetAll(), Imeji.facetModel);
    resetCache(retrieve(uris, Imeji.adminUser));
    return retrieveAllFromCache();
  }

  private void resetCache(List<Facet> facets) {
    cachedFacets = facets;
    cachedFacetsMapByIndex =
        cachedFacets.stream().collect(Collectors.toMap(Facet::getIndex, Function.identity()));
    cachedFacetsMapByName =
        cachedFacets.stream().collect(Collectors.toMap(Facet::getName, Function.identity()));
  }

  /**
   * Return the Facets as currently cache
   * 
   * @return
   */
  public List<Facet> retrieveAllFromCache() {
    if (cachedFacets == null) {
      try {
        retrieveAll();
      } catch (ImejiException e) {
        LOGGER.error("Error retrieving facets", e);
        return new ArrayList<>();
      }
    }
    return cachedFacets;
  }

  /**
   * Return a Facet by its index
   * 
   * @param index
   * @return
   */
  public Facet retrieveByIndeyFromCache(String index) {
    return cachedFacetsMapByIndex.get(index);
  }

  /**
   * Retrieve a Facet by its name
   * 
   * @param name
   * @return
   */
  public Facet retrieveByNameFromCache(String name) {
    return cachedFacetsMapByName.get(name);
  }
}
