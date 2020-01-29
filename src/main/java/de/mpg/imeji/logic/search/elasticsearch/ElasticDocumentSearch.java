package de.mpg.imeji.logic.search.elasticsearch;

import java.io.IOException;
import java.net.URI;

import org.apache.logging.log4j.LogManager;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthRequest;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.ClusterClient;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.cluster.health.ClusterHealthStatus;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.IdsQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;

import org.elasticsearch.search.builder.SearchSourceBuilder;
import de.mpg.imeji.logic.search.elasticsearch.ElasticService.ElasticIndices;
import org.apache.logging.log4j.Logger;

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

    return ElasticService.getClient().ping(RequestOptions.DEFAULT);
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

    ClusterHealthRequest clusterHealthRequest = new ClusterHealthRequest();
    clusterHealthRequest.timeout(TimeValue.timeValueSeconds(TIME_OUT_FOR_CLUSTER_HEALTH_IN_SEC));
    clusterHealthRequest.waitForStatus(ClusterHealthStatus.YELLOW);

    ClusterClient clusterClient = ElasticService.getClient().cluster();
    ClusterHealthResponse response = clusterClient.health(clusterHealthRequest, RequestOptions.DEFAULT);
    if (response.getStatus() == ClusterHealthStatus.YELLOW || response.getStatus() == ClusterHealthStatus.GREEN) {
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

    SearchRequest searchRequest = new SearchRequest();
    SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
    searchSourceBuilder.version(true);
    searchSourceBuilder.query(getIdQuery(documentId));
    searchRequest.indices(index.name());
    searchRequest.source(searchSourceBuilder);

    return getDocumentVersion(searchRequest, documentId);

  }

  /**
   * Creates an id query for Elastic Search.
   * 
   * @param uri
   * @return
   */
  private static IdsQueryBuilder getIdQuery(URI uri) {

    IdsQueryBuilder idQuery = QueryBuilders.idsQuery();
    idQuery.addIds(uri.toString());
    return idQuery;
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

    SearchResponse response = ElasticService.getClient().search(request, RequestOptions.DEFAULT);
    SearchHit[] hits = response.getHits().getHits();
    if (hits.length == 1) {
      SearchHit firstHit = hits[0];
      version = firstHit.getVersion();
    } else {
      LOGGER.error("Could not find document with uri " + uri.toString());
    }
    return version;

  }



}
