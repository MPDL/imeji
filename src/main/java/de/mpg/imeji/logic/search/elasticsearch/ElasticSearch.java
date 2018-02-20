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
  private static final int SEARCH_MAX_SIZE = 500;
  private static final int ELASTIC_FROM_SIZE_LIMIT = 10000;


  /**
   * Construct an Elastic Search Query for on data type. If type is null, search for all types
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
    return search(query, sortCri, user, folderUri, from, size, false);
  }

  @Override
  public SearchResult searchWithFacets(SearchQuery query, SortCriterion sortCri, User user,
      String folderUri, int from, int size) {
    return search(query, sortCri, user, folderUri, from, size, true);
  }

  private SearchResult search(SearchQuery query, SortCriterion sortCri, User user, String folderUri,
      int from, int size, boolean addFacets) {
    size = size == -1 ? size = SEARCH_MAX_SIZE : size;
    if (size < SEARCH_MAX_SIZE && from + size < ELASTIC_FROM_SIZE_LIMIT) {
      return searchSinglePage(query, sortCri, user, folderUri, from, size, addFacets);
    } else {
      return searchWithScroll(query, sortCri, user, folderUri, from, size, addFacets);
    }
  }

  /**
   * A Search returning a single document. Faster, but limited to not too big search result list
   * (max is SEARCH_MAX_SIZE)
   *
   * @param query
   * @param sortCri
   * @param user
   * @param folderUri
   * @param from
   * @param size
   * @return
   */
  private SearchResult searchSinglePage(SearchQuery query, SortCriterion sortCri, User user,
      String folderUri, int from, int size, boolean addFacets) {
    final ElasticQueryFactory factory =
        new ElasticQueryFactory(query, type).folderUri(folderUri).user(user);
    final QueryBuilder q = factory.build();
    final QueryBuilder f = factory.buildBaseQuery();
    SearchRequestBuilder request = ElasticService.getClient()
        .prepareSearch(ElasticService.DATA_ALIAS).setNoFields().setTypes(getTypes()).setSize(size)
        .setFrom(from).addSort("_type", SortOrder.ASC).addSort(ElasticSortFactory.build(sortCri));
    if (f != null) {
      request.setQuery(f).setPostFilter(q);
    } else {
      request.setQuery(q);
    }
    if (addFacets) {
      request = addAggregations(request, folderUri);
    }
    final SearchResponse resp = request.execute().actionGet();

    return toSearchResult(resp, query);
  }

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
   * A Search via the scroll api. Allow to search for many documents
   *
   * @param query
   * @param sortCri
   * @param user
   * @param folderUri
   * @param from
   * @param size
   * @return
   */
  private SearchResult searchWithScroll(SearchQuery query, SortCriterion sortCri, User user,
      String folderUri, int from, int size, boolean addFacets) {
    final ElasticQueryFactory factory =
        new ElasticQueryFactory(query, type).folderUri(folderUri).user(user);
    final QueryBuilder q = factory.build();
    final QueryBuilder f = factory.buildBaseQuery();
    SearchRequestBuilder request = ElasticService.getClient()
        .prepareSearch(ElasticService.DATA_ALIAS).setScroll(new TimeValue(60000)).setNoFields()
        .setTypes(getTypes()).setSize(SEARCH_MAX_SIZE).setFrom(from).addSort("_type", SortOrder.ASC)
        .addSort(ElasticSortFactory.build(sortCri));
    if (f != null) {
      request.setQuery(f).setPostFilter(q);
    } else {
      request.setQuery(q);
    }
    if (addFacets) {
      request = addAggregations(request, folderUri);
    }

    SearchResponse resp = request.execute().actionGet();
    SearchResult result = toSearchResult(resp, query);
    while (true) {
      resp = ElasticService.getClient().prepareSearchScroll(resp.getScrollId())
          .setScroll(new TimeValue(60000)).execute().actionGet();
      if (resp.getHits().getHits().length == 0) {
        break;
      }
      result = addSearchResult(result, resp);
    }
    ElasticService.getClient().prepareClearScroll().addScrollId(resp.getScrollId()).execute()
        .actionGet();
    return result;
  }

  @Override
  public SearchResult search(SearchQuery query, SortCriterion sortCri, User user,
      List<String> uris) {
    // Not needed for Elasticsearch. This method is used for sparql search
    return null;
  }

  @Override
  public SearchResult searchString(String query, SortCriterion sort, User user, int from,
      int size) {
    final QueryBuilder q = QueryBuilders.queryStringQuery(query);
    final SearchResponse resp = ElasticService.getClient().prepareSearch(ElasticService.DATA_ALIAS)
        .setNoFields().setTypes(getTypes()).setQuery(q).setSize(size).setFrom(from)
        .addSort(ElasticSortFactory.build(sort)).execute().actionGet();
    return toSearchResult(resp, null);
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
   * Transform a {@link SearchResponse} to a {@link SearchResult}
   *
   * @param resp
   * @return
   */
  private SearchResult toSearchResult(SearchResponse resp, SearchQuery query) {
    final List<String> ids = new ArrayList<>(Math.toIntExact(resp.getHits().getTotalHits()));
    for (final SearchHit hit : resp.getHits()) {
      ids.add(hit.getId());
    }
    List<FacetResult> facets = AggregationsParser.parse(resp);
    return new SearchResult(ids, getTotalNumberOfRecords(resp, facets),
        getNumberOfItems(resp, facets), getNumberOfItemsOfCollection(resp, facets),
        getNumberOfSubcollections(resp, facets), facets);
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
   * Add the resp to an existing searchresult
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
