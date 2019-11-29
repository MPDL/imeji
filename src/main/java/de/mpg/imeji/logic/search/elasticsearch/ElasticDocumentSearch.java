package de.mpg.imeji.logic.search.elasticsearch;

import java.io.IOException;
import java.net.URI;

import org.apache.logging.log4j.LogManager;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;

import de.mpg.imeji.exceptions.UnprocessableError;
import de.mpg.imeji.logic.model.SearchFields;
import de.mpg.imeji.logic.search.elasticsearch.ElasticService.ElasticIndices;
import de.mpg.imeji.logic.search.elasticsearch.factory.ElasticQueryFactory;
import de.mpg.imeji.logic.search.elasticsearch.model.ElasticFields;
import de.mpg.imeji.logic.search.model.SearchPair;
import de.mpg.imeji.logic.search.model.SearchQuery;

import org.apache.logging.log4j.Logger;

/**
 * Given the
 * 
 * @author breddin
 *
 */
public class ElasticDocumentSearch {


  private final static Logger LOGGER = LogManager.getLogger(ElasticDocumentSearch.class);
  public static final long VERSION_NOT_FOUND = -1;


  /**
   * Currently checking whether Elastic Search is up and running Could be extended to check for
   * cluster health with indices
   * 
   * @return
   */
  public static boolean pingElasticSearch() {

    try {
      return ElasticService.getClient().ping(RequestOptions.DEFAULT);
    } catch (IOException e) {
      return false;
    }

  }


  /**
   * Given the uri of a document, get the version (time stamp) of that document.
   * 
   * @param documentId
   * @return documents version or VERSION_NOT_FOUND (If the document is not found or document's
   *         version cannot be obtained)
   */
  public static long searchVersionOfDocument(URI documentId) {


    LOGGER.error("searchVersionOfDocument");
    // get index from uri
    ElasticIndices index = ElasticIndices.uriToElasticIndex(documentId);

    SearchRequest searchRequest = new SearchRequest();
    SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
    searchSourceBuilder.version(true);
    searchSourceBuilder.query(getElasticSearchQuery(documentId));
    searchRequest.indices(index.name());
    searchRequest.source(searchSourceBuilder);

    return getDocumentVersion(searchRequest, documentId);

  }


  /**
   * 
   * 
   * @param uri
   * @param index
   */
  private static BoolQueryBuilder getElasticSearchQuery(URI uri) {

    final BoolQueryBuilder booleanQuery = QueryBuilders.boolQuery();
    TermQueryBuilder term = QueryBuilders.termQuery(ElasticFields.ID.field(), uri.toString());
    booleanQuery.must(term);
    return booleanQuery;

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
  private static long getDocumentVersion(SearchRequest request, URI uri) {

    // send request to ElasticSearch
    LOGGER.debug(request.source().toString());
    long version = VERSION_NOT_FOUND;
    try {

      SearchResponse response = ElasticService.getClient().search(request, RequestOptions.DEFAULT);
      SearchHit[] hits = response.getHits().getHits();
      if (hits.length == 1) {
        SearchHit firstHit = hits[0];
        version = firstHit.getVersion();
      } else {
        LOGGER.error("could not find document with uri " + uri.toString());
      }

    } catch (IOException e) {
      LOGGER.error("error getting search response", e);
    }
    return version;

  }



}
