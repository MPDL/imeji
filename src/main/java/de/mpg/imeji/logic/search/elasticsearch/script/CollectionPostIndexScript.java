package de.mpg.imeji.logic.search.elasticsearch.script;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.search.ClearScrollRequest;
import org.elasticsearch.action.search.ClearScrollResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchScrollRequest;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.RequestOptions;

import org.elasticsearch.core.TimeValue;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;

import de.mpg.imeji.exceptions.SearchIndexBulkFailureException;
import de.mpg.imeji.logic.model.CollectionImeji;
import de.mpg.imeji.logic.search.elasticsearch.ElasticIndexer;
import de.mpg.imeji.logic.search.elasticsearch.ElasticService;
import de.mpg.imeji.logic.search.elasticsearch.ElasticService.ElasticIndices;
import de.mpg.imeji.logic.search.elasticsearch.model.ElasticFields;
import de.mpg.imeji.logic.search.elasticsearch.script.misc.CollectionFields;
import org.elasticsearch.xcontent.XContentBuilder;

/**
 * Script runned after a collection has been indexed
 * 
 * @author saquet
 *
 */
public class CollectionPostIndexScript {
  private static final Logger LOGGER = LogManager.getLogger(CollectionPostIndexScript.class);

  /**
   * Run the script
   * 
   * @param collection
   * @throws ExecutionException
   * @throws InterruptedException
   * @throws IOException
   * @throws SearchIndexBulkFailureException
   */
  public static void run(CollectionImeji collection, String indexName)
      throws IOException, InterruptedException, ExecutionException, SearchIndexBulkFailureException {
    updateCollectionItemsWithAuthorAndOrganization(collection, indexName);
  }

  /**
   * Update all items of the collection with the author(s) of the collection and the organization(s)
   * of these authors
   * 
   * @param c
   * @throws ExecutionException
   * @throws InterruptedException
   * @throws SearchIndexBulkFailureException
   */
  private static void updateCollectionItemsWithAuthorAndOrganization(CollectionImeji c, String index)
      throws IOException, InterruptedException, ExecutionException, SearchIndexBulkFailureException {

    List<String> ids = getCollectionItemIds(c);
    if (ids.isEmpty()) {
      return;
    }
    final BulkRequest bulkRequest = new BulkRequest();
    final XContentBuilder json = new CollectionFields(c).toXContentBuilder();
    for (final String id : ids) {
      final UpdateRequest req = new UpdateRequest();
      req.index(index).type("_doc").id(id).doc(json);
      bulkRequest.add(req);
    }
    if (bulkRequest.numberOfActions() > 0) {
      BulkResponse bulkResponse = ElasticService.getClient().bulk(bulkRequest, RequestOptions.DEFAULT);
      if (bulkResponse.hasFailures()) {
        throw ElasticIndexer.getSearchIndexBulkFailureException(bulkResponse);
      }
    }
  }

  /**
   * Return all items of the collection
   * 
   * @param collection
   * @return
   * @throws InterruptedException
   * @throws ExecutionException
   * @throws IOException
   */
  private static List<String> getCollectionItemIds(CollectionImeji collection)
      throws InterruptedException, ExecutionException, IOException {

    TermQueryBuilder q = QueryBuilders.termQuery(ElasticFields.FOLDER.field(), collection.getId().toString());
    SearchRequest searchRequest = new SearchRequest();
    SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
    searchSourceBuilder.trackTotalHits(true);
    searchSourceBuilder.query(q);
    searchRequest.indices(ElasticIndices.items.name()).source(searchSourceBuilder).scroll(TimeValue.timeValueMinutes(1));
    List<String> ids = new ArrayList<String>(0);

    SearchResponse resp = ElasticService.getClient().search(searchRequest, RequestOptions.DEFAULT);
    ids = new ArrayList<>(Math.toIntExact(resp.getHits().getTotalHits().value));
    for (final SearchHit hit : resp.getHits()) {
      ids.add(hit.getId());
    }

    while (true) {
      String scrollId = resp.getScrollId();
      SearchScrollRequest scrollRequest = new SearchScrollRequest(scrollId);
      scrollRequest.scroll(TimeValue.timeValueSeconds(60));
      resp = ElasticService.getClient().scroll(scrollRequest, RequestOptions.DEFAULT);
      if (resp.getHits().getHits().length == 0) {
        break;
      }
      for (final SearchHit hit : resp.getHits()) {
        ids.add(hit.getId());
      }
    }

    ClearScrollRequest clearScrollRequest = new ClearScrollRequest();
    clearScrollRequest.addScrollId(resp.getScrollId());
    ClearScrollResponse response = ElasticService.getClient().clearScroll(clearScrollRequest, RequestOptions.DEFAULT);

    return ids;
  }

}
