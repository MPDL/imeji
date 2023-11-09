package de.mpg.imeji.logic.search.elasticsearch;

import java.io.IOException;
import java.util.*;
import java.util.stream.Stream;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.Time;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.query_dsl.*;
import co.elastic.clients.elasticsearch.core.*;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.HitsMetadata;
import co.elastic.clients.elasticsearch.core.search.ResponseBody;
import co.elastic.clients.elasticsearch.core.search.TrackHits;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.logging.log4j.LogManager;

import org.elasticsearch.client.RequestOptions;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.model.User;
import de.mpg.imeji.logic.search.Search;
import de.mpg.imeji.logic.search.SearchIndexer;
import de.mpg.imeji.logic.search.elasticsearch.ElasticService.ElasticIndices;
import de.mpg.imeji.logic.search.elasticsearch.factory.ElasticAggregationFactory;
import de.mpg.imeji.logic.search.elasticsearch.factory.ElasticQueryFactory;
import de.mpg.imeji.logic.search.elasticsearch.factory.ElasticSortFactory;
import de.mpg.imeji.logic.search.elasticsearch.factory.util.AggregationsParser;
import de.mpg.imeji.logic.search.elasticsearch.model.ElasticFields;
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
    return searchElasticSearch(query, sortCriteria, user, folderUri, from, size, false, true);
  }

  @Override
  public SearchResult searchWithMultiLevelSorting(SearchQuery query, List<SortCriterion> sortCriteria, User user, String folderUri,
      int from, int size) {
    return searchElasticSearch(query, sortCriteria, user, folderUri, from, size, false, true);
  }

  @Override
  public SearchResult searchWithFacetsAndMultiLevelSorting(SearchQuery query, List<SortCriterion> sortCriteria, User user, String folderUri,
      int from, int size, boolean includeSubcollections) {
    return searchElasticSearch(query, sortCriteria, user, folderUri, from, size, true, includeSubcollections);
  }

  @Override
  public SearchResult searchWithFacets(SearchQuery query, SortCriterion sortCri, User user, String folderUri, int from, int size) {

    List<SortCriterion> sortCriteria = new ArrayList<SortCriterion>(1);
    sortCriteria.add(sortCri);
    return searchElasticSearch(query, sortCriteria, user, folderUri, from, size, true, true);
  }

  private SearchResult searchElasticSearch(SearchQuery query, List<SortCriterion> sortCriteria, User user, String folderUri, int from,
      int size, boolean addFacets, boolean includeSubcollections) {

    // magic number "-1" for unlimited size is spread all over the code:
    if (size != GET_ALL_RESULTS && size < 0) {
      size = SEARCH_INTERVALL_MAX_SIZE;
    }
    from = from < 0 ? 0 : from;

    // construct request
    final ElasticQueryFactory factory = new ElasticQueryFactory(query, this.indices).folderUri(folderUri).user(user);
    factory.setIncludeSubcollections(includeSubcollections);
    final Query q = factory.build();
    final Query f = factory.buildBaseQuery();
    /*
     * SearchRequestBuilder request =
     * ElasticService.getClient().prepareSearch(ElasticService.DATA_ALIAS).
     * setNoFields() .setTypes(getTypes()).addSort("_type", SortOrder.ASC); request
     * = ElasticSearchFactoryUtil.addSorting(request, sortCriteria);
     */
    SearchRequest.Builder searchRequestBuilder = new SearchRequest.Builder();
    //SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
    searchRequestBuilder.trackTotalHits(th -> th.enabled(true));
    //searchSourceBuilder.trackTotalHits(true);
    if (f != null) {
      searchRequestBuilder.query(f).postFilter(q);
    } else {
      searchRequestBuilder.query(q);
    }
    if (addFacets) {
      searchRequestBuilder = addAggregations(searchRequestBuilder, folderUri);
    }

    // single page or scroll search
    if (size != GET_ALL_RESULTS && size < SEARCH_INTERVALL_MAX_SIZE && from + size < SEARCH_TO_INDEX_LIMIT) {
      searchRequestBuilder.from(from).size(size);
      for (SortOptions sb : ElasticSortFactory.build(sortCriteria)) {
        if (sb != null) {
          searchRequestBuilder.sort(sb);
        }
      }
      searchRequestBuilder.index(Arrays.asList(this.indicesNames));
      //LOGGER.info(searchSourceBuilder.toString());
      return searchSinglePage(searchRequestBuilder.build(), query);
    } else {
      searchRequestBuilder.size(SEARCH_SCROLL_INTERVALL);
      for (SortOptions sb : ElasticSortFactory.build(sortCriteria)) {
        if (sb != null) {
          searchRequestBuilder.sort(sb);
        }
      }
      searchRequestBuilder.index(Arrays.asList(this.indicesNames)).scroll(Time.of(t -> t.time("30s")));

      return searchWithScroll(searchRequestBuilder.build(), query, from, size);
    }
  }

  /**
   * Add ElasticSearch Aggregations to a SearchRequestBuilder
   * 
   * @param request
   * @param folderUri
   * @return
   */

  private SearchRequest.Builder addAggregations(SearchRequest.Builder request, String folderUri) {
    final Map<String, Aggregation> aggregations = ElasticAggregationFactory.build(this.types);
    if (folderUri != null) {
      /*
      aggregations.add(AggregationBuilders.filters(Facet.COLLECTION_ITEMS,
        new FiltersAggregator.KeyedFilter(Facet.COLLECTION_ITEMS,
            QueryBuilders.boolQuery().queryName(Facet.COLLECTION_ITEMS).must(QueryBuilders.termQuery("folder", folderUri))
                .must(QueryBuilders.typeQuery(ElasticService.ElasticIndices.items.name())))));
                */

      aggregations.put(Facet.COLLECTION_ITEMS, Aggregation.of(agg -> agg.filters(
          filterAgg -> filterAgg.keyed(true).filters(filter -> filter.keyed(Collections.singletonMap(Facet.COLLECTION_ITEMS, BoolQuery
              .of(bq -> bq.queryName(Facet.COLLECTION_ITEMS).must(q -> q.term(tq -> tq.field("folder").value(folderUri))))._toQuery()))))));


      /*
      aggregations.add(AggregationBuilders.filters(Facet.COLLECTION_ITEMS, new FiltersAggregator.KeyedFilter(Facet.COLLECTION_ITEMS,
          QueryBuilders.boolQuery().queryName(Facet.COLLECTION_ITEMS).must(QueryBuilders.termQuery("folder", folderUri)))));
      */

      aggregations
          .put(Facet.COLLECTION_ROOT_ITEMS,
              Aggregation
                  .of(agg -> agg
                      .filters(
                          filterAgg -> filterAgg
                              .keyed(
                                  true)
                              .filters(filter -> filter.keyed(Collections.singletonMap(Facet.COLLECTION_ROOT_ITEMS,
                                  BoolQuery
                                      .of(bq -> bq.queryName(Facet.COLLECTION_ROOT_ITEMS)
                                          .must(q -> q.term(tq -> tq.field("folder").value(folderUri)))
                                          .must(q -> q.term(tq -> tq.field(ElasticFields.JOIN_FIELD.field()).value("item"))))
                                      ._toQuery()))))));
      /*
      aggregations.add(AggregationBuilders.filters(Facet.COLLECTION_ROOT_ITEMS,
          new FiltersAggregator.KeyedFilter(Facet.COLLECTION_ROOT_ITEMS,
              QueryBuilders.boolQuery().queryName(Facet.COLLECTION_ROOT_ITEMS).must(QueryBuilders.termQuery("folder", folderUri))
                  .queryName(Facet.COLLECTION_ROOT_ITEMS).must(QueryBuilders.termQuery(ElasticFields.JOIN_FIELD.field(), "item")))));
      
      
       */
    }



    request.aggregations(aggregations);
    /*
    for (Map.Entry<String, Aggregation> agg : aggregations.entrySet()) {
     request.aggregation(agg);
    }
    */
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
    LOGGER.debug(request.source().toString());
    SearchResponse<ObjectNode> resp;
    try {
      resp = ElasticService.getClient().search(request, ObjectNode.class);
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
   * @param sortCriteria Criteria by which the results are sorted
   * @param user User
   * @param folderUri
   * @param from Search from this result number (within all results for the given query)
   * @param amountOfDocumentsToRetrieve Retrieve this amount of hits/documents
   * @return
   */
  private SearchResult searchWithScroll(SearchRequest request, SearchQuery query, int from, int amountOfDocumentsToRetrieve) {
    // add scroll search
    // request = request.setScroll(new TimeValue(SCROLL_TIMEOUT_MSEC));

    LOGGER.debug(request.source().toString());
    SearchResponse<ObjectNode> searchResponse;
    try {
      searchResponse = ElasticService.getClient().search(request, ObjectNode.class);
      if (searchResponse.shards().failures().size() > 0) {
        LOGGER.error("Error during search: " + searchResponse.shards().failures().toString());
      }
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

      //Clear scroll
      if (searchResponse.scrollId() != null) {
        ClearScrollRequest csr = ClearScrollRequest.of(cs -> cs.scrollId(searchResponse.scrollId()));
        //csr.addScrollId(searchResponse.getScrollId());
        ClearScrollResponse clearScrollResp = ElasticService.getClient().clearScroll(csr);
      }


      return result;
    } catch (IOException e) {
      LOGGER.error("Error during search: ", e);
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
  private List<String> scrollWithSizeUpperBound(ResponseBody<ObjectNode> searchResponse, int from, int amountOfResults) {

    // get results of first query
    List<String> hitIds = new ArrayList<>(getIDsFromScrollSearchResponse(searchResponse));

    // search until results with index range [0, from+ amountOfResults] are
    // retrieved
    while (hitIds.size() < from + amountOfResults) {
      String scrollId = searchResponse.scrollId();
      ScrollRequest scrollRequest = ScrollRequest.of(sr -> sr.scrollId(scrollId).scroll(Time.of(t -> t.time("30s"))));
      //SearchScrollRequest scrollRequest = new SearchScrollRequest(scrollId);
      //scrollRequest.scroll(TimeValue.timeValueSeconds(30));
      try {
        searchResponse = ElasticService.getClient().scroll(scrollRequest, ObjectNode.class);
      } catch (IOException e) {
        LOGGER.error("Error during search: ", e);
      }
      if (searchResponse.hits().hits().size() == 0) {
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
  private List<String> scrollAllResults(ResponseBody<ObjectNode> searchResponse, int from) {

    List<String> retrievedIDs = new ArrayList<>(getIDsFromScrollSearchResponse(searchResponse));
    do {
      /*
       * searchResponse =
       * ElasticService.getClient().prepareSearchScroll(searchResponse.getScrollId())
       * .setScroll(new TimeValue(SCROLL_TIMEOUT_MSEC)).execute().actionGet();
       */
      String scrollId = searchResponse.scrollId();
      ScrollRequest scrollRequest = ScrollRequest.of(sr -> sr.scrollId(scrollId).scroll(Time.of(t -> t.time("30s"))));
      //SearchScrollRequest scrollRequest = new SearchScrollRequest(scrollId);
      //scrollRequest.scroll(TimeValue.timeValueSeconds(30));
      try {
        searchResponse = ElasticService.getClient().scroll(scrollRequest, ObjectNode.class);
      } catch (IOException e) {
        LOGGER.error("Error during search: ", e);
      }
      List<String> retrievedIDsOfScroll = getIDsFromScrollSearchResponse(searchResponse);
      retrievedIDs.addAll(retrievedIDsOfScroll);
    } while (searchResponse.hits().hits().size() > 0);

    // cut indices at beginning [0,from]
    if (retrievedIDs.size() > from) {
      return retrievedIDs.subList(from, retrievedIDs.size());
    } else {
      return new ArrayList<>();
    }
  }

  private ArrayList<String> getIDsFromScrollSearchResponse(ResponseBody<ObjectNode> searchResponse) {

    final ArrayList<String> ids = new ArrayList<>(SEARCH_SCROLL_INTERVALL);
    for (final Hit<ObjectNode> hit : searchResponse.hits().hits()) {
      ids.add(hit.id());
    }
    return ids;
  }

  @Override
  public SearchResult searchString(String query, SortCriterion sort, User user, int from, int size) {
    final Query q = QueryStringQuery.of(qs -> qs.query(query))._toQuery();
    SearchRequest.Builder searchRequestBuilder = new SearchRequest.Builder();
    searchRequestBuilder.trackTotalHits(th -> th.enabled(true));
    //SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
    //searchSourceBuilder.trackTotalHits(true);

    // single page or scroll search
    if (size != GET_ALL_RESULTS && size < SEARCH_INTERVALL_MAX_SIZE && from + size < SEARCH_TO_INDEX_LIMIT) {
      searchRequestBuilder.query(q).size(size).from(from).sort(ElasticSortFactory.build(sort));
      searchRequestBuilder.index(Arrays.asList(this.indicesNames));
      return searchSinglePage(searchRequestBuilder.build(), null);
    } else {
      searchRequestBuilder.query(q).size(SEARCH_SCROLL_INTERVALL).from(from).sort(ElasticSortFactory.build(sort));
      searchRequestBuilder.index(Arrays.asList(this.indicesNames));
      return searchWithScroll(searchRequestBuilder.build(), null, from, size);
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
    final Query q = QueryStringQuery.of(qs -> qs.query(query))._toQuery();
    SearchRequest.Builder searchRequestBuilder = new SearchRequest.Builder();
    searchRequestBuilder.trackTotalHits(th -> th.enabled(true));
    //SearchRequest searchRequest = new SearchRequest();
    //SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
    //searchSourceBuilder.trackTotalHits(true);

    // single page search
    if (size != GET_ALL_RESULTS && size < SEARCH_INTERVALL_MAX_SIZE && from + size < SEARCH_TO_INDEX_LIMIT) {
      searchRequestBuilder.docvalueFields(FieldAndFormat.of(fv -> fv.field(field))).query(q).size(size).from(from)
          .sort(ElasticSortFactory.build(sort));
      searchRequestBuilder.index(Arrays.asList(indexName));
      SearchResponse<ObjectNode> singlePageSearchResponse;
      try {
        singlePageSearchResponse = ElasticService.getClient().search(searchRequestBuilder.build(), ObjectNode.class);
        fieldValues = getFieldValuesOfSearchResponse(singlePageSearchResponse.hits(), field);
      } catch (IOException e) {
        LOGGER.error("Error while searching", e);
      }
    }
    // scroll search
    else {
      searchRequestBuilder.docvalueFields(FieldAndFormat.of(fv -> fv.field(field))).query(q).size(SEARCH_INTERVALL_MAX_SIZE).from(from)
          .sort(ElasticSortFactory.build(sort));
      searchRequestBuilder.index(Arrays.asList(indexName)).scroll(t -> t.time("30s"));
      ResponseBody<ObjectNode> scrollSearchResponse = null;
      try {
        scrollSearchResponse = ElasticService.getClient().search(searchRequestBuilder.build(), ObjectNode.class);
      } catch (IOException e1) {
        LOGGER.error("Error while searching", e1);
      }
      // scroll until no more hits are returned. Zero hits marks the end of the scroll
      // and the while loop
      do {
        List<String> fieldValuesOfScrollSearch = getFieldValuesOfSearchResponse(scrollSearchResponse.hits(), field);
        fieldValues.addAll(fieldValuesOfScrollSearch);
        String scrollId = scrollSearchResponse.scrollId();
        ScrollRequest scrollRequest = ScrollRequest.of(sr -> sr.scrollId(scrollId).scroll(t -> t.time("30s")));
        //scrollRequest.scroll(TimeValue.timeValueSeconds(30));
        try {
          scrollSearchResponse = ElasticService.getClient().scroll(scrollRequest, ObjectNode.class);
        } catch (IOException e) {
          LOGGER.error("Error while searching", e);
        }
      } while (scrollSearchResponse.hits().hits().size() != 0);

      //Clear scroll
      if (scrollSearchResponse.scrollId() != null) {
        String scrollId = scrollSearchResponse.scrollId();
        try {
          ClearScrollRequest csr = ClearScrollRequest.of(cs -> cs.scrollId(scrollId));
          //csr.addScrollId(scrollSearchResponse.getScrollId());
          ClearScrollResponse clearScrollResp = ElasticService.getClient().clearScroll(csr);
        } catch (IOException e) {
          LOGGER.error("Error while clearing scroll request", e);
        }
      }

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

  private static List<String> getFieldValuesOfSearchResponse(HitsMetadata<ObjectNode> searchHits, String field) {

    List<String> fieldValues = new ArrayList<>();
    for (final Hit<ObjectNode> hit : searchHits.hits()) {
      if (field.equals(ElasticFields.ID.field())) {
        fieldValues.add(hit.id());
      } else {
        fieldValues.add(hit.fields().get(field).toString());
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
  private SearchResult getSearchResultFromElasticSearchResponse(ResponseBody<ObjectNode> searchResponse, SearchQuery query) {

    final int totalNumberOfHitsForSearchQuery = Math.toIntExact(searchResponse.hits().total().value());
    final List<String> ids = new ArrayList<String>(totalNumberOfHitsForSearchQuery);
    for (final Hit hit : searchResponse.hits().hits()) {
      ids.add(hit.id());
    }

    List<FacetResult> facets = AggregationsParser.parse(searchResponse, this.types);
    SearchResult searchResult = new SearchResult(ids, getTotalNumberOfRecords(searchResponse, facets),
        getNumberOfItems(searchResponse, facets), getNumberOfItemsOfCollection(searchResponse, facets),
        getNumberOfRootItemsOfCollection(searchResponse, facets), getNumberOfSubcollections(searchResponse, facets), facets);
    return searchResult;
  }

  private long getTotalNumberOfRecords(ResponseBody resp, List<FacetResult> facets) {
    return facets.stream().filter(f -> f.getName().equals("all")).findAny().map(f -> f.getValues().get(0).getCount())
        .orElse(resp.hits().total().value());
  }

  private long getNumberOfItems(ResponseBody resp, List<FacetResult> facets) {
    return facets.stream().filter(f -> f.getName().equals(Facet.ITEMS)).findAny().map(f -> f.getValues().get(0).getCount())
        .orElse(resp.hits().total().value());
  }

  private long getNumberOfItemsOfCollection(ResponseBody resp, List<FacetResult> facets) {
    return facets.stream().filter(f -> f.getName().equals(Facet.COLLECTION_ITEMS)).findAny().map(f -> f.getValues().get(0).getCount())
        .orElse(resp.hits().total().value());
  }

  private long getNumberOfRootItemsOfCollection(ResponseBody resp, List<FacetResult> facets) {
    return facets.stream().filter(f -> f.getName().equals(Facet.COLLECTION_ROOT_ITEMS)).findAny().map(f -> f.getValues().get(0).getCount())
        .orElse((long) 0);
  }

  private long getNumberOfSubcollections(ResponseBody resp, List<FacetResult> facets) {
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
