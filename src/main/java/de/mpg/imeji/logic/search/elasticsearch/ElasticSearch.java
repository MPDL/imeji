package de.mpg.imeji.logic.search.elasticsearch;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchScrollRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.AbstractAggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.filter.FiltersAggregator;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortBuilder;
import org.elasticsearch.search.sort.SortOrder;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.core.facade.SearchAndRetrieveFacade;
import de.mpg.imeji.logic.model.User;
import de.mpg.imeji.logic.search.Search;
import de.mpg.imeji.logic.search.SearchIndexer;
import de.mpg.imeji.logic.search.elasticsearch.ElasticService.ElasticIndices;
import de.mpg.imeji.logic.search.elasticsearch.factory.ElasticAggregationFactory;
import de.mpg.imeji.logic.search.elasticsearch.factory.ElasticQueryFactory;
import de.mpg.imeji.logic.search.elasticsearch.factory.ElasticSortFactory;
import de.mpg.imeji.logic.search.elasticsearch.factory.util.AggregationsParser;
import de.mpg.imeji.logic.search.elasticsearch.factory.util.ElasticSearchFactoryUtil;
import de.mpg.imeji.logic.search.elasticsearch.model.ElasticFields;
import de.mpg.imeji.logic.search.facet.model.Facet;
import de.mpg.imeji.logic.search.facet.model.FacetResult;
import de.mpg.imeji.logic.search.model.SearchQuery;
import de.mpg.imeji.logic.search.model.SearchResult;
import de.mpg.imeji.logic.search.model.SortCriterion;
import de.mpg.imeji.logic.security.user.UserService;

/**
 * {@link Search} implementation for ElasticSearch
 *
 * @author bastiens
 *
 */
public class ElasticSearch implements Search {

  private SearchObjectTypes[] types = null;
  private ElasticIndices[] indices = null;
  private String[] indicesNames = null;
  private ElasticIndexer indexer = null;
  private static final int SEARCH_INTERVALL_MAX_SIZE = 500;
  public static final int SEARCH_SCROLL_INTERVALL = SEARCH_INTERVALL_MAX_SIZE;
  private static final int SEARCH_TO_INDEX_LIMIT = 10000;
  public static final int SCROLL_TIMEOUT_MSEC = 60000;
  private final static org.apache.logging.log4j.Logger LOGGER = LogManager.getLogger(ElasticSearch.class);

  /**
   * Construct an Elastic Search Query for one or more data types. If type is null, search for all
   * existing types
   *
   * @param type
   * @throws ImejiException
   */
  public ElasticSearch(SearchObjectTypes... types) {
    this.types = types;
    this.indices = Stream.of(types).map(t -> ElasticIndices.toElasticIndex(t)).toArray(ElasticIndices[]::new);
    this.indicesNames = Stream.of(indices).map(i -> i.name()).toArray(String[]::new);
    this.indexer = new ElasticIndexer(indices[0].name());
  }

  @Override
  public SearchIndexer getIndexer() {
    return indexer;
  }

  @Override
  public SearchResult search(SearchQuery query, SortCriterion sortCri, User user, String folderUri, int from, int size) {

    List<SortCriterion> sortCriteria = new ArrayList<SortCriterion>(1);
    sortCriteria.add(sortCri);
    return searchElasticSearch(query, sortCriteria, user, folderUri, from, size, false);
  }

  @Override
  public SearchResult searchWithMultiLevelSorting(SearchQuery query, List<SortCriterion> sortCriteria, User user, String folderUri,
      int from, int size) {
    return searchElasticSearch(query, sortCriteria, user, folderUri, from, size, false);
  }

  @Override
  public SearchResult searchWithFacetsAndMultiLevelSorting(SearchQuery query, List<SortCriterion> sortCriteria, User user, String folderUri,
      int from, int size) {
    return searchElasticSearch(query, sortCriteria, user, folderUri, from, size, true);
  }

  @Override
  public SearchResult searchWithFacets(SearchQuery query, SortCriterion sortCri, User user, String folderUri, int from, int size) {

    List<SortCriterion> sortCriteria = new ArrayList<SortCriterion>(1);
    sortCriteria.add(sortCri);
    return searchElasticSearch(query, sortCriteria, user, folderUri, from, size, true);
  }

  private SearchResult searchElasticSearch(SearchQuery query, List<SortCriterion> sortCriteria, User user, String folderUri, int from,
      int size, boolean addFacets) {

    // magic number "-1" for unlimited size is spread all over the code:
    if (size != GET_ALL_RESULTS && size < 0) {
      size = SEARCH_INTERVALL_MAX_SIZE;
    }
    from = from < 0 ? 0 : from;

    // construct request
    final ElasticQueryFactory factory = new ElasticQueryFactory(query, this.indices).folderUri(folderUri).user(user);
    final QueryBuilder q = factory.build();
    final QueryBuilder f = factory.buildBaseQuery();
    /*
     * SearchRequestBuilder request =
     * ElasticService.getClient().prepareSearch(ElasticService.DATA_ALIAS).
     * setNoFields() .setTypes(getTypes()).addSort("_type", SortOrder.ASC); request
     * = ElasticSearchFactoryUtil.addSorting(request, sortCriteria);
     */
    SearchRequest searchRequest = new SearchRequest();
    SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
    if (f != null) {
      searchSourceBuilder.query(f).postFilter(q);
    } else {
      searchSourceBuilder.query(q);
    }
    if (addFacets) {
      searchSourceBuilder = addAggregations(searchSourceBuilder, folderUri);
    }

    // single page or scroll search
    if (size != GET_ALL_RESULTS && size < SEARCH_INTERVALL_MAX_SIZE && from + size < SEARCH_TO_INDEX_LIMIT) {
      searchSourceBuilder.from(from).size(size);
      for (SortBuilder sb : ElasticSortFactory.build(sortCriteria)) {
        if (sb != null) {
          searchSourceBuilder.sort(sb);
        }
      }
      searchRequest.indices(this.indicesNames).source(searchSourceBuilder);
      return searchSinglePage(searchRequest, query);
    } else {
      searchSourceBuilder.size(SEARCH_SCROLL_INTERVALL);
      for (SortBuilder sb : ElasticSortFactory.build(sortCriteria)) {
        if (sb != null) {
          searchSourceBuilder.sort(sb);
        }
      }
      searchRequest.indices(this.indicesNames).source(searchSourceBuilder).scroll(TimeValue.timeValueSeconds(30));
      return searchWithScroll(searchRequest, query, from, size);
    }
  }

  /**
   * Add ElasticSearch Aggregations to a SearchRequestBuilder
   * 
   * @param request
   * @param folderUri
   * @return
   */

  private SearchSourceBuilder addAggregations(SearchSourceBuilder request, String folderUri) {
    final List<AbstractAggregationBuilder> aggregations = ElasticAggregationFactory.build();
    if (folderUri != null) {
      aggregations.add(AggregationBuilders.filters(Facet.COLLECTION_ITEMS,
          new FiltersAggregator.KeyedFilter(Facet.COLLECTION_ITEMS,
              QueryBuilders.boolQuery().queryName(Facet.COLLECTION_ITEMS).must(QueryBuilders.termQuery("folder", folderUri))
                  .must(QueryBuilders.typeQuery(ElasticService.ElasticIndices.items.name())))));
    }
    for (AbstractAggregationBuilder agg : aggregations) {
      request.aggregation(agg);
    }
    return request;
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
  private SearchResult searchSinglePage(SearchRequest request, SearchQuery query) {

    // send request to ElasticSearch
    SearchResponse resp;
    try {
      resp = ElasticService.getClient().search(request, RequestOptions.DEFAULT);
      SearchResult elasticSearchResult = getSearchResultFromElasticSearchResponse(resp, query);
      return elasticSearchResult;
    } catch (IOException e) {
      LOGGER.error("error getting search response", e);
    }
    return null;

  }

  /**
   * Search via ElasticSearch Scroll Search. This allows to search for a greater number of documents
   * or all documents that exist within ElasticSeach database for a given query
   *
   * @param query ElasticSearch search query
   * @param sortCriteria Criteria by which the results are be sorted
   * @param user User
   * @param folderUri
   * @param from Search from this result number (within all results for the given query)
   * @param amountOfDocumentsToRetrieve Retrieve this amount of hits/documents
   * @return
   */
  private SearchResult searchWithScroll(SearchRequest request, SearchQuery query, int from, int amountOfDocumentsToRetrieve) {
    // add scroll search
    // request = request.setScroll(new TimeValue(SCROLL_TIMEOUT_MSEC));

    SearchResponse searchResponse;
    try {
      searchResponse = ElasticService.getClient().search(request, RequestOptions.DEFAULT);
      SearchResult result = getSearchResultFromElasticSearchResponse(searchResponse, query);
      List<String> retrievedIDs = new ArrayList<String>();
      // scroll all results starting at a given index
      if (amountOfDocumentsToRetrieve == GET_ALL_RESULTS) {
        retrievedIDs = scrollAllResults(searchResponse, from);
      }
      // scroll to a size limit starting at a given index
      else {
        retrievedIDs = scrollWithSizeUpperBound(searchResponse, from, amountOfDocumentsToRetrieve);
      }
      result.setResults(retrievedIDs);
      return result;
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    return null;
  }

  /**
   * Use scroll search for retrieving documents/results from index "from" to index "from +
   * amountOfResults"
   * 
   * @param searchResponse
   * @param from
   * @param amountOfResults
   * @return
   */
  private List<String> scrollWithSizeUpperBound(SearchResponse searchResponse, int from, int amountOfResults) {

    // get results of first query
    List<String> hitIds = new ArrayList<>(getIDsFromScrollSearchResponse(searchResponse));

    // search until results with index range [0, from+ amountOfResults] are
    // retrieved
    while (hitIds.size() < from + amountOfResults) {
      String scrollId = searchResponse.getScrollId();
      SearchScrollRequest scrollRequest = new SearchScrollRequest(scrollId);
      scrollRequest.scroll(TimeValue.timeValueSeconds(30));
      try {
        searchResponse = ElasticService.getClient().scroll(scrollRequest, RequestOptions.DEFAULT);
      } catch (IOException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
      if (searchResponse.getHits().getHits().length == 0) {
        break;
      }
      hitIds.addAll(getIDsFromScrollSearchResponse(searchResponse));
    }

    if (hitIds.size() > from) {
      // cut indices at beginning [0, from] and end [from + amountOfResults, end]
      if (from + amountOfResults < hitIds.size()) {
        return hitIds.subList(from, from + amountOfResults);
      } else {
        return hitIds.subList(from, hitIds.size());
      }
    } else {
      return new ArrayList<>();
    }

  }

  /**
   * Use ElasticSearch scroll search to scroll all results starting at a given index
   * 
   * @param searchResponse ElasticSearch SearchResponse object
   * @param from start index (relative to all results that exist in ElasticSeach for the given
   *        query)
   * @return list with UIDs of retrieved objects
   */
  private List<String> scrollAllResults(SearchResponse searchResponse, int from) {

    List<String> retrievedIDs = new ArrayList<>(getIDsFromScrollSearchResponse(searchResponse));
    do {
      /*
       * searchResponse =
       * ElasticService.getClient().prepareSearchScroll(searchResponse.getScrollId())
       * .setScroll(new TimeValue(SCROLL_TIMEOUT_MSEC)).execute().actionGet();
       */
      String scrollId = searchResponse.getScrollId();
      SearchScrollRequest scrollRequest = new SearchScrollRequest(scrollId);
      scrollRequest.scroll(TimeValue.timeValueSeconds(30));
      try {
        searchResponse = ElasticService.getClient().scroll(scrollRequest, RequestOptions.DEFAULT);
      } catch (IOException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
      List<String> retrievedIDsOfScroll = getIDsFromScrollSearchResponse(searchResponse);
      retrievedIDs.addAll(retrievedIDsOfScroll);
    } while (searchResponse.getHits().getHits().length > 0);

    // cut indices at beginning [0,from]
    if (retrievedIDs.size() > from) {
      return retrievedIDs.subList(from, retrievedIDs.size());
    } else {
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
  public SearchResult searchString(String query, SortCriterion sort, User user, int from, int size) {
    final QueryBuilder q = QueryBuilders.queryStringQuery(query);
    SearchRequest searchRequest = new SearchRequest();
    SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();

    // single page or scroll search
    if (size != GET_ALL_RESULTS && size < SEARCH_INTERVALL_MAX_SIZE && from + size < SEARCH_TO_INDEX_LIMIT) {
      searchSourceBuilder.query(q).size(size).from(from).sort(ElasticSortFactory.build(sort));
      searchRequest.indices(this.indicesNames).source(searchSourceBuilder);
      return searchSinglePage(searchRequest, null);
    } else {
      searchSourceBuilder.query(q).size(SEARCH_SCROLL_INTERVALL).from(from).sort(ElasticSortFactory.build(sort));
      searchRequest.indices(this.indicesNames).source(searchSourceBuilder);
      return searchWithScroll(searchRequest, null, from, size);
    }

    /*
     * final SearchResponse resp =
     * ElasticService.getClient().prepareSearch(ElasticService.DATA_ALIAS)
     * .setNoFields().setTypes(getTypes()).setQuery(q).setSize(size).setFrom(from)
     * .addSort(ElasticSortFactory.build(sort)).execute().actionGet(); return
     * getSearchResultFromElasticSearchResponse(resp, null);
     */
  }

  /**
   * Search with a String Query (see ElasticSearch Query String Query) and retrieve the value of a
   * specific field. A String Query is a query that takes a search expression (in String format) and
   * uses a query parser to parse its content.
   *
   * @param query search expression, "query_string"
   * @param field return the value of this field if a document matches the query
   * @param sort sort criterion
   * @param from start index (in search results list)
   * @param size amount of results
   * @return
   */
  public static List<String> searchStringAndRetrieveFieldValue(String indexName, String query, String field, SortCriterion sort, int from,
      int size) {

    List<String> fieldValues = new ArrayList<>();
    final QueryBuilder q = QueryBuilders.queryStringQuery(query);
    SearchRequest searchRequest = new SearchRequest();
    SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();

    // single page search
    if (size != GET_ALL_RESULTS && size < SEARCH_INTERVALL_MAX_SIZE && from + size < SEARCH_TO_INDEX_LIMIT) {
      searchSourceBuilder.docValueField(field).query(q).size(size).from(from).sort(ElasticSortFactory.build(sort));
      searchRequest.indices(indexName).source(searchSourceBuilder);
      SearchResponse singlePageSearchResponse;
      try {
        singlePageSearchResponse = ElasticService.getClient().search(searchRequest, RequestOptions.DEFAULT);
        fieldValues = getFieldValuesOfSearchResponse(singlePageSearchResponse.getHits(), field);
      } catch (IOException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }
    // scroll search
    else {
      searchSourceBuilder.docValueField(field).query(q).size(SEARCH_INTERVALL_MAX_SIZE).from(from).sort(ElasticSortFactory.build(sort));
      searchRequest.indices(indexName).scroll(TimeValue.timeValueSeconds(30)).source(searchSourceBuilder);
      SearchResponse scrollSearchResponse = null;
      try {
        scrollSearchResponse = ElasticService.getClient().search(searchRequest, RequestOptions.DEFAULT);
      } catch (IOException e1) {
        // TODO Auto-generated catch block
        e1.printStackTrace();
      }
      // scroll until no more hits are returned. Zero hits marks the end of the scroll
      // and the while loop
      do {
        List<String> fieldValuesOfScrollSearch = getFieldValuesOfSearchResponse(scrollSearchResponse.getHits(), field);
        fieldValues.addAll(fieldValuesOfScrollSearch);
        String scrollId = scrollSearchResponse.getScrollId();
        SearchScrollRequest scrollRequest = new SearchScrollRequest(scrollId);
        scrollRequest.scroll(TimeValue.timeValueSeconds(30));
        try {
          scrollSearchResponse = ElasticService.getClient().scroll(scrollRequest, RequestOptions.DEFAULT);
        } catch (IOException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
      } while (scrollSearchResponse.getHits().getHits().length != 0);

      if (fieldValues.size() > from) {
        // cut indices at beginning [0,from] and end [from + size, end]
        if (size != GET_ALL_RESULTS && from + size < fieldValues.size()) {
          return fieldValues.subList(from, from + size);
        } else {
          return fieldValues.subList(from, fieldValues.size());
        }
      } else {
        return new ArrayList<>();
      }
    }
    return fieldValues;
  }

  private static List<String> getFieldValuesOfSearchResponse(SearchHits searchHits, String field) {

    List<String> fieldValues = new ArrayList<>();
    for (final SearchHit hit : searchHits) {
      if (field.equals(ElasticFields.ID.field())) {
        fieldValues.add(hit.getId());
      } else {
        fieldValues.add(hit.field(field).getValue());
      }
    }
    return fieldValues;
  }

  /**
   * Get the datatype to search for
   *
   * @return
   */
  /*
   * private String[] getTypes() { if (type == null || type.length == 0) { return
   * Arrays.stream(ElasticIndices.values()).map(ElasticIndices::name).toArray(
   * String[]::new); } return
   * Stream.of(type).map(ElasticIndices::name).toArray(String[]::new); }
   */

  /**
   * Transform an ElasticSearch {@link SearchResponse} to an Imeji {@link SearchResult} object
   *
   * @param searchResponse
   * @return SearchResult
   */
  private SearchResult getSearchResultFromElasticSearchResponse(SearchResponse searchResponse, SearchQuery query) {

    final int totalNumberOfHitsForSearchQuery = Math.toIntExact(searchResponse.getHits().getTotalHits());
    final List<String> ids = new ArrayList<String>(totalNumberOfHitsForSearchQuery);
    for (final SearchHit hit : searchResponse.getHits()) {
      ids.add(hit.getId());
    }

    List<FacetResult> facets = AggregationsParser.parse(searchResponse);
    SearchResult searchResult =
        new SearchResult(ids, getTotalNumberOfRecords(searchResponse, facets), getNumberOfItems(searchResponse, facets),
            getNumberOfItemsOfCollection(searchResponse, facets), getNumberOfSubcollections(searchResponse, facets), facets);
    return searchResult;
  }

  private long getTotalNumberOfRecords(SearchResponse resp, List<FacetResult> facets) {
    return facets.stream().filter(f -> f.getName().equals("all")).findAny().map(f -> f.getValues().get(0).getCount())
        .orElse(resp.getHits().getTotalHits());
  }

  private long getNumberOfItems(SearchResponse resp, List<FacetResult> facets) {
    return facets.stream().filter(f -> f.getName().equals(Facet.ITEMS)).findAny().map(f -> f.getValues().get(0).getCount())
        .orElse(resp.getHits().getTotalHits());
  }

  private long getNumberOfItemsOfCollection(SearchResponse resp, List<FacetResult> facets) {
    return facets.stream().filter(f -> f.getName().equals(Facet.COLLECTION_ITEMS)).findAny().map(f -> f.getValues().get(0).getCount())
        .orElse(resp.getHits().getTotalHits());
  }

  private long getNumberOfSubcollections(SearchResponse resp, List<FacetResult> facets) {
    return facets.stream().filter(f -> f.getName().equals(Facet.SUBCOLLECTIONS)).findAny().map(f -> f.getValues().get(0).getCount())
        .orElse((long) 0);
  }

  @Override
  public SearchResult search(SearchQuery query, SortCriterion sortCri, User user, List<String> uris) {
    // Not needed for Elasticsearch.
    // This method is used for sparql search
    return null;
  }

}
