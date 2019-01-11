package de.mpg.imeji.logic.search.elasticsearch.script;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.search.ClearScrollRequest;
import org.elasticsearch.action.search.ClearScrollResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchScrollRequest;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.action.update.UpdateRequestBuilder;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;

import de.mpg.imeji.logic.model.CollectionImeji;
import de.mpg.imeji.logic.search.elasticsearch.ElasticService;
import de.mpg.imeji.logic.search.elasticsearch.ElasticService.ElasticIndices;
import de.mpg.imeji.logic.search.elasticsearch.model.ElasticFields;
import de.mpg.imeji.logic.search.elasticsearch.script.misc.CollectionFields;

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
   * @param c
   */
  public static void run(CollectionImeji c, String index) {
    try {
      updateCollectionItemsWithAuthorAndOrganization(c, index);
    } catch (Exception e) {
      LOGGER.error("Error running IndexCollectionPostProcessingScript ", e);
    }
  }

  /**
   * Update all items of the collection with the author(s) of the collection and the organization(s)
   * of these authors
   * 
   * @param c
   * @throws ExecutionException
   * @throws InterruptedException
   */
  private static void updateCollectionItemsWithAuthorAndOrganization(CollectionImeji c, String index) throws Exception {
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
      BulkResponse resp = ElasticService.getClient().bulk(bulkRequest, RequestOptions.DEFAULT);
      if (resp.hasFailures()) {
        LOGGER.error(resp.buildFailureMessage());
      }
    }
  }

  /**
   * Return all items of the collection
   * 
   * @param c
   * @return
   * @throws InterruptedException
   * @throws ExecutionException
   */
  private static List<String> getCollectionItemIds(CollectionImeji c) throws InterruptedException, ExecutionException {
    TermQueryBuilder q = QueryBuilders.termQuery(ElasticFields.FOLDER.field(), c.getId().toString());
    SearchRequest searchRequest = new SearchRequest();
    SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
    searchSourceBuilder.query(q);
    searchRequest.indices(ElasticIndices.items.name()).source(searchSourceBuilder).scroll(TimeValue.timeValueMinutes(1));
    SearchResponse resp = null;
    List<String> ids = null;
    try {
      resp = ElasticService.getClient().search(searchRequest, RequestOptions.DEFAULT);
      ids = new ArrayList<>(Math.toIntExact(resp.getHits().getTotalHits()));
      for (final SearchHit hit : resp.getHits()) {
        ids.add(hit.getId());
      }
    } catch (Exception e1) {
      LOGGER.error("error during search", e1);
    }
    while (true) {
      String scrollId = resp.getScrollId();
      SearchScrollRequest scrollRequest = new SearchScrollRequest(scrollId);
      scrollRequest.scroll(TimeValue.timeValueSeconds(60));
      try {
        resp = ElasticService.getClient().scroll(scrollRequest, RequestOptions.DEFAULT);
        if (resp.getHits().getHits().length == 0) {
          break;
        }
        for (final SearchHit hit : resp.getHits()) {
          ids.add(hit.getId());
        }
      } catch (IOException e) {
        LOGGER.error("error durin scroll", e);
      }
    }
    ClearScrollRequest clearScrollRequest = new ClearScrollRequest();
    clearScrollRequest.addScrollId(resp.getScrollId());
    try {
      ClearScrollResponse response = ElasticService.getClient().clearScroll(clearScrollRequest, RequestOptions.DEFAULT);
    } catch (IOException e) {
      LOGGER.error("error during clear scroll", e);
    }
    return ids;
  }

}
