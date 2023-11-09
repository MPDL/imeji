package de.mpg.imeji.logic.search.elasticsearch;

import co.elastic.clients.elasticsearch._types.HealthStatus;
import co.elastic.clients.elasticsearch._types.Time;
import co.elastic.clients.elasticsearch._types.query_dsl.IdsQuery;
import co.elastic.clients.elasticsearch.cluster.HealthRequest;
import co.elastic.clients.elasticsearch.cluster.HealthResponse;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import de.mpg.imeji.logic.search.elasticsearch.ElasticService.ElasticIndices;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.URI;
import java.util.List;

/**
 * Collection of functions for checking status of Elastic Search and searching documents.
 * 
 * @author breddin
 *
 */
public class ElasticDocumentSearch {


  private final static Logger LOGGER = LogManager.getLogger(ElasticDocumentSearch.class);
  public static final long VERSION_NOT_FOUND = -1;

  private static final int TIME_OUT_FOR_CLUSTER_HEALTH_IN_SEC = 50;


  /**
   * Ping Elastic Search.
   * 
   * @return whether ES has answered to ping or not
   * @throws IOException
   */
  public static boolean pingElasticSearch() throws IOException {
    return ElasticService.getClient().ping().value();
  }


  /**
   * Checks if cluster health has yellow state, i.e. Elastic Search is ready for operations.
   * 
   * @return
   * @throws IOException
   */
  public static boolean getClusterHealthYellow() throws IOException {

    /*
     * Synchronous calls may throw an IOException in case of either failing 
     * to parse the REST response in the high-level REST client, 
     * the request times out or similar cases where there is no response coming back from the server.
     * In cases where the server returns a 4xx or 5xx error code, the high-level client tries to parse 
     * the response body error details instead and then throws a generic ElasticsearchException
     * (RuntimeException) and adds the original ResponseException as a suppressed exception to it.
     */

    ElasticService.getClient().cluster().health();
    HealthRequest healthRequest =
        HealthRequest.of(hr -> hr.timeout(Time.of(t -> t.offset(TIME_OUT_FOR_CLUSTER_HEALTH_IN_SEC))).waitForStatus(HealthStatus.Yellow));

    HealthResponse healthResponse = ElasticService.getClient().cluster().health(healthRequest);

    if (healthResponse.status() == HealthStatus.Yellow || healthResponse.status() == HealthStatus.Green) {
      return true;
    }
    return false;

  }


  /**
   * Given the uri of a document, get the version (time stamp) of that document.
   * 
   * @param documentId
   * @return documents version or VERSION_NOT_FOUND (If the document is not found or document's
   *         version cannot be obtained)
   * @throws IOException
   */
  public static long searchVersionOfDocument(URI documentId) throws IOException {

    // get index from uri
    ElasticIndices index = ElasticIndices.uriToElasticIndex(documentId);

    SearchRequest searchRequest = SearchRequest.of(sr -> sr.version(true).query(getIdQuery(documentId)._toQuery()).index(index.name()));

    /*
    SearchRequest searchRequest = new SearchRequest();
    SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
    searchSourceBuilder.version(true);
    searchSourceBuilder.query(getIdQuery(documentId));
    searchRequest.indices(index.name());
    searchRequest.source(searchSourceBuilder);
    */

    return getDocumentVersion(searchRequest, documentId);

  }

  /**
   * Creates an id query for Elastic Search.
   * 
   * @param uri
   * @return
   */
  private static IdsQuery getIdQuery(URI uri) {
    return IdsQuery.of(idq -> idq.values(uri.toString()));
    //IdsQueryBuilder idQuery = QueryBuilders.idsQuery();
    //idQuery.addIds(uri.toString());
    //return idQuery;
  }

  /**
   * Connect to Elastic Search and retrieve the current version of the document with given id (URI).
   * 
   * @param request Elastic Saerch SearchRequest
   * @param uri id of the document
   * @return version of document or -1 if version could not be retrieved for document
   * @throws IOException
   */
  private static long getDocumentVersion(SearchRequest request, URI uri) throws IOException {

    long version = VERSION_NOT_FOUND;

    SearchResponse response = ElasticService.getClient().search(request, Object.class);
    List<Hit> searchHits = response.hits().hits();
    //SearchHit[] hits = response.getHits().getHits();
    if (searchHits.size() == 1) {
      Hit firstHit = searchHits.get(0);
      version = firstHit.version();
    } else {
      LOGGER.error("Could not find document with uri " + uri.toString());
    }
    return version;

  }



}
