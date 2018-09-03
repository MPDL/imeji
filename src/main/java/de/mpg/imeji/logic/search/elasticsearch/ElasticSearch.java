package de.mpg.imeji.logic.search.elasticsearch;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.aggregations.AbstractAggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.sort.SortOrder;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.model.User;
import de.mpg.imeji.logic.search.Search;
import de.mpg.imeji.logic.search.SearchIndexer;
import de.mpg.imeji.logic.search.elasticsearch.ElasticService.ElasticTypes;
import de.mpg.imeji.logic.search.elasticsearch.factory.ElasticAggregationFactory;
import de.mpg.imeji.logic.search.elasticsearch.factory.ElasticQueryFactory;
import de.mpg.imeji.logic.search.elasticsearch.factory.ElasticSortFactory;
import de.mpg.imeji.logic.search.elasticsearch.factory.util.AggregationsParser;
import de.mpg.imeji.logic.search.elasticsearch.factory.util.ElasticSearchFactoryUtil;
import de.mpg.imeji.logic.search.facet.model.Facet;
import de.mpg.imeji.logic.search.facet.model.FacetResult;
import de.mpg.imeji.logic.search.model.SearchQuery;
import de.mpg.imeji.logic.search.model.SearchResult;
import de.mpg.imeji.logic.search.model.SortCriterion;


/**
 * {@link Search} implementation for ElasticSearch
 *
 * @author bastiens
 *
 */
public class ElasticSearch implements Search {

  private ElasticTypes[] type = null;
  private ElasticIndexer indexer = null;
  private static final int SEARCH_INTERVALL_MAX_SIZE = 500;
  private static final int SEARCH_SCROLL_INTERVALL = SEARCH_INTERVALL_MAX_SIZE;
  private static final int SEARCH_TO_INDEX_LIMIT = 10000;
  public static final int  SIZE_GET_ALL_RESULTS= -1000;  // use this constant to state that you want to retrieve all existing search results 
  														 // of a query without a size limit
  public static final int SEARCH_FROM_START = 0;
  private static final int SCROLL_TIMEOUT_MSEC = 60000; 

  /**
   * Construct an Elastic Search Query for one or more data types. 
   * If type is null, search for all existing types
   *
   * @param type
   * @throws ImejiException
   */
  public ElasticSearch(SearchObjectTypes... types) {
    this.type = Stream.of(types).map(t -> ElasticTypes.toElasticTypes(t))
        .collect(Collectors.toList()).toArray(new ElasticTypes[types.length]);
    this.indexer =
        new ElasticIndexer(ElasticService.DATA_ALIAS, this.type[0], ElasticService.ANALYSER);
  }


  @Override
  public SearchIndexer getIndexer() {
    return indexer;
  }

  
  
  @Override
  public SearchResult search(SearchQuery query, SortCriterion sortCri, User user, String folderUri,
      int from, int size) {
	  
	  List<SortCriterion> sortCriteria = new ArrayList<SortCriterion>(1);
	  sortCriteria.add(sortCri);	  
	  return searchElasticSearch(query, sortCriteria , user, folderUri, from, size, false);
  }
  
  
  @Override
  public SearchResult searchWithMultiLevelSorting(SearchQuery query, List<SortCriterion> sortCriteria, User user,
  		String folderUri, int from, int size) {
        return searchElasticSearch(query, sortCriteria , user, folderUri, from, size, false);
  }
  
  
  
  @Override
  public SearchResult searchWithFacetsAndMultiLevelSorting(SearchQuery query, List<SortCriterion> sortCriteria, User user, String folderUri,
	      int from, int size) {	  
	  return searchElasticSearch(query, sortCriteria, user, folderUri, from, size, true);
  }

  
  @Override
  public SearchResult searchWithFacets(SearchQuery query, SortCriterion sortCri, User user,
      String folderUri, int from, int size) {
    
	  List<SortCriterion> sortCriteria = new ArrayList<SortCriterion>(1);
	  sortCriteria.add(sortCri);	  
	  return searchElasticSearch(query, sortCriteria, user, folderUri, from, size, true);
  }

  
  private SearchResult searchElasticSearch(SearchQuery query, List<SortCriterion> sortCriteria, User user, String folderUri,
      int from, int size, boolean addFacets) {
    
	// magic number "-1" for size is spread all over the code
    // semantic meaning: I don't know how much results I want, give me all?
	if(size < 0) { size = SEARCH_INTERVALL_MAX_SIZE; }
	from = from < 0 ? 0 : from;
       
    if (size != SIZE_GET_ALL_RESULTS && 
    		size < SEARCH_INTERVALL_MAX_SIZE && 
    		from + size < SEARCH_TO_INDEX_LIMIT) {
      return searchSinglePage(query, sortCriteria, user, folderUri, from, size, addFacets);
    } else {
      return searchWithScroll(query, sortCriteria, user, folderUri, from, size, addFacets);
    }
  }

  
  /**
   * A Search returning a single document. Faster, but limited to not too big search result list
   * (max is SEARCH_MAX_SIZE)
   *
   * @param query
   * @param sortCriteria
   * @param user
   * @param folderUri
   * @param from
   * @param size
   * @return
   */
  private SearchResult searchSinglePage(SearchQuery query, List<SortCriterion> sortCriteria, User user,
      String folderUri, int from, int size, boolean addFacets) {
    final ElasticQueryFactory factory =
        new ElasticQueryFactory(query, type).folderUri(folderUri).user(user);
    final QueryBuilder q = factory.build();
    final QueryBuilder f = factory.buildBaseQuery();
    SearchRequestBuilder request = ElasticService.getClient()
        .prepareSearch(ElasticService.DATA_ALIAS).setNoFields().setTypes(getTypes()).setSize(size)
        .setFrom(from).addSort("_type", SortOrder.ASC);
    request = ElasticSearchFactoryUtil.addSorting(request, sortCriteria);
    if (f != null) {
      request.setQuery(f).setPostFilter(q);
    } else {
      request.setQuery(q);
    }
    if (addFacets) {
    	request = addAggregations(request, folderUri);
    }

    final SearchResponse resp = request.execute().actionGet();
    SearchResult elasticSearchResult = getSearchResultFromElasticSearchResponse(resp, query);
    return elasticSearchResult;
  }

  /**
   * Add ElasticSearch Aggregations to a SearchRequestBuilder
   * @param request
   * @param folderUri
   * @return
   */
  
  private SearchRequestBuilder addAggregations(SearchRequestBuilder request, String folderUri) {
    final List<AbstractAggregationBuilder> aggregations = ElasticAggregationFactory.build();
    if (folderUri != null) {
      aggregations
          .add(AggregationBuilders.filters(Facet.COLLECTION_ITEMS).filter(Facet.COLLECTION_ITEMS,
              QueryBuilders.boolQuery().queryName(Facet.COLLECTION_ITEMS)
                  .must(QueryBuilders.termQuery("folder", folderUri)).must(
                      QueryBuilders.typeQuery(ElasticService.ElasticTypes.items.name()))));
    }
    for (AbstractAggregationBuilder agg : aggregations) {
      request.addAggregation(agg);
    }
    return request;
  }

  
  /**
   * Search via ElasticSearch Scroll Search. 
   * This allows to search for a greater number of documents or all documents
   * that exist within ElasticSeach database for a given query
   *
   * @param query 			ElasticSearch search query
   * @param sortCriteria	Criteria by which the results are be sorted
   * @param user			User
   * @param folderUri
   * @param from			Search from this result number (within all results for the given query)
   * @param size            Retrieve this amount of hits/documents
   * @return	
   */
  private SearchResult searchWithScroll(SearchQuery query, List<SortCriterion> sortCriteria, User user,
      String folderUri, int from, int size, boolean addFacets) {

    final ElasticQueryFactory factory =
        new ElasticQueryFactory(query, type).folderUri(folderUri).user(user);
    final QueryBuilder q = factory.build();
    final QueryBuilder f = factory.buildBaseQuery();
    // scroll api conducts search from beginning, "search from index" must be handled additionally
    SearchRequestBuilder request = ElasticService.getClient()
        .prepareSearch(ElasticService.DATA_ALIAS).setScroll(new TimeValue(SCROLL_TIMEOUT_MSEC)).setNoFields()
        .setTypes(getTypes()).setSize(SEARCH_SCROLL_INTERVALL).addSort("_type", SortOrder.ASC);
    request = ElasticSearchFactoryUtil.addSorting(request, sortCriteria);
    if (f != null) {
      request.setQuery(f).setPostFilter(q);
    } else {
      request.setQuery(q);
    }
    if (addFacets) {
      request = addAggregations(request, folderUri);
    }
    SearchResponse resp = request.execute().actionGet();
    // initialize SearchResult (with first interval of retrieved results)
    SearchResult result = getSearchResultFromElasticSearchResponse(resp, query);

    List<String> retrievedIDs = new ArrayList<String>();
    // scroll all results starting at a given index
    if(size == SIZE_GET_ALL_RESULTS) {    	
    	retrievedIDs = scrollAllResults(resp, from); 
    }
    // scroll to a size limit starting at a given index
    else {
    	retrievedIDs = scrollWithSizeUpperBound(resp, from, size);
    }
   
    result.setResults(retrievedIDs);
    return result;
  }
  
  /**
   *  Use scroll search for retrieving documents/results
   *  from index "from" to index "from + amountOfResults"
   * @param searchResponse
   * @param from
   * @param amountOfResults
   * @return
   */
  private List<String> scrollWithSizeUpperBound(SearchResponse searchResponse, int from, int amountOfResults) {
    
	// get results of first query
	List<String> hitIds = new ArrayList<>(getIDsFromScrollSearchResponse(searchResponse));
    
	// search until results with index range [0, from+ amountOfResults] are retrieved
	while (hitIds.size() < from + amountOfResults) {
      searchResponse = ElasticService.getClient().prepareSearchScroll(searchResponse.getScrollId())
          .setScroll(new TimeValue(SCROLL_TIMEOUT_MSEC)).execute().actionGet();
      if (searchResponse.getHits().getHits().length == 0) {
        break;
      }
      hitIds.addAll(getIDsFromScrollSearchResponse(searchResponse));
    }
    
	if(hitIds.size() > from) {
		// cut indices at beginning [0,from] and end [from + amountOfResults, end]
		if(from + amountOfResults < hitIds.size()) {
			return hitIds.subList(from, from + amountOfResults);
		}
		else {
			return hitIds.subList(from, hitIds.size());
		}
	}
	else {
		return new ArrayList<>();
	}

  }

  /**
   * Use ElasticSearch scroll search to scroll all results starting at a given index
   * @param searchResponse  ElasticSearch SearchResponse object
   * @param from	        start index (relative to all results that exist in ElasticSeach for the given query)
   * @return				list with UIDs of retrieved objects 
   */
  private List<String> scrollAllResults(SearchResponse searchResponse, int from){
	  
	  List<String> retrievedIDs = new ArrayList<>(getIDsFromScrollSearchResponse(searchResponse));
	  do {		  
		  searchResponse = ElasticService.getClient().prepareSearchScroll(searchResponse.getScrollId())
		          .setScroll(new TimeValue(SCROLL_TIMEOUT_MSEC)).execute().actionGet();
		  List<String> retrievedIDsOfScroll = getIDsFromScrollSearchResponse(searchResponse);
		  retrievedIDs.addAll(retrievedIDsOfScroll);
		  
	  }while(searchResponse.getHits().getHits().length > 0);
	  	
	  
	  // cut indices at beginning [0,from] 
	  if(retrievedIDs.size() > from) {
				return retrievedIDs.subList(from, retrievedIDs.size());
	  }
	  else {
			return new ArrayList<>();
	  }

  }

  
  private ArrayList<String> getIDsFromScrollSearchResponse(SearchResponse searchResponse) {
	    
	  final ArrayList<String> ids = new ArrayList<>(SEARCH_SCROLL_INTERVALL);
	  for (final SearchHit hit : searchResponse.getHits()) {
	      ids.add(hit.getId());
	  }
	  return ids;
  }
  
  
  @Override
  public SearchResult search(SearchQuery query, SortCriterion sortCri, User user,
      List<String> uris) {
    // Not needed for Elasticsearch. 
	// This method is used for sparql search
    return null;
  }

  @Override
  public SearchResult searchString(String query, SortCriterion sort, User user, int from,
      int size) {
    final QueryBuilder q = QueryBuilders.queryStringQuery(query);
    final SearchResponse resp = ElasticService.getClient().prepareSearch(ElasticService.DATA_ALIAS)
        .setNoFields().setTypes(getTypes()).setQuery(q).setSize(size).setFrom(from)
        .addSort(ElasticSortFactory.build(sort)).execute().actionGet();
    return getSearchResultFromElasticSearchResponse(resp, null);
  }


  /**
   * Get the datatype to search for
   *
   * @return
   */
  private String[] getTypes() {
    if (type == null || type.length == 0) {
      return Arrays.stream(ElasticTypes.values()).map(ElasticTypes::name).toArray(String[]::new);
    }
    return Stream.of(type).map(ElasticTypes::name).toArray(String[]::new);
  }

  /**
   * Transform an ElasticSearch {@link SearchResponse} to an Imeji {@link SearchResult} object
   *
   * @param searchResponse
   * @return SearchResult
   */
  private SearchResult getSearchResultFromElasticSearchResponse(SearchResponse searchResponse, SearchQuery query) {
    
	final int totalNumberOfHitsForSearchQuery =  Math.toIntExact(searchResponse.getHits().getTotalHits());
	final List<String> ids = new ArrayList<String>(totalNumberOfHitsForSearchQuery);
    for (final SearchHit hit : searchResponse.getHits()) {
      ids.add(hit.getId());
    }
    
    List<FacetResult> facets = AggregationsParser.parse(searchResponse);    
    SearchResult searchResult = new SearchResult(ids, getTotalNumberOfRecords(searchResponse, facets),
            getNumberOfItems(searchResponse, facets), getNumberOfItemsOfCollection(searchResponse, facets),
            getNumberOfSubcollections(searchResponse, facets), facets);
    return searchResult;
  }

  
  private long getTotalNumberOfRecords(SearchResponse resp, List<FacetResult> facets) {
    return facets.stream().filter(f -> f.getName().equals("all")).findAny()
        .map(f -> f.getValues().get(0).getCount()).orElse(resp.getHits().getTotalHits());
  }

  private long getNumberOfItems(SearchResponse resp, List<FacetResult> facets) {
    return facets.stream().filter(f -> f.getName().equals(Facet.ITEMS)).findAny()
        .map(f -> f.getValues().get(0).getCount()).orElse(resp.getHits().getTotalHits());
  }

  private long getNumberOfItemsOfCollection(SearchResponse resp, List<FacetResult> facets) {
    return facets.stream().filter(f -> f.getName().equals(Facet.COLLECTION_ITEMS)).findAny()
        .map(f -> f.getValues().get(0).getCount()).orElse(resp.getHits().getTotalHits());
  }


  private long getNumberOfSubcollections(SearchResponse resp, List<FacetResult> facets) {
    return facets.stream().filter(f -> f.getName().equals(Facet.SUBCOLLECTIONS)).findAny()
        .map(f -> f.getValues().get(0).getCount()).orElse((long) 0);
  }

  /**
   * Add the resp to an existing search result
   *
   * @param resp
   * @return
   */
  private SearchResult addSearchResult(SearchResult result, SearchResponse resp) {
    final List<String> ids = new ArrayList<>(Math.toIntExact(resp.getHits().getTotalHits()));
    for (final SearchHit hit : resp.getHits()) {
      ids.add(hit.getId());
    }
    result.getResults().addAll(ids);
    return result;
  }

  



}
