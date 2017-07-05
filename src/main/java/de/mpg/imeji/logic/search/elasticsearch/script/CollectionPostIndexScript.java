package de.mpg.imeji.logic.search.elasticsearch.script;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.apache.log4j.Logger;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.update.UpdateRequestBuilder;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.search.SearchHit;

import de.mpg.imeji.logic.search.elasticsearch.ElasticService;
import de.mpg.imeji.logic.search.elasticsearch.ElasticService.ElasticTypes;
import de.mpg.imeji.logic.search.elasticsearch.model.ElasticFields;
import de.mpg.imeji.logic.search.elasticsearch.script.misc.CollectionFields;
import de.mpg.imeji.logic.vo.CollectionImeji;

/**
 * Script runned after a collection has been indexed
 * 
 * @author saquet
 *
 */
public class CollectionPostIndexScript {
  private static final Logger LOGGER = Logger.getLogger(CollectionPostIndexScript.class);

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
  private static void updateCollectionItemsWithAuthorAndOrganization(CollectionImeji c,
      String index) throws Exception {
    List<String> ids = getCollectionItemIds(c);
    if (ids.isEmpty()) {
      return;
    }
    final BulkRequestBuilder bulkRequest = ElasticService.getClient().prepareBulk();
    final XContentBuilder json = new CollectionFields(c).toXContentBuilder();
    for (final String id : ids) {
      final UpdateRequestBuilder req =
          ElasticService.getClient().prepareUpdate(index, ElasticTypes.items.name(), id)
              .setDoc(json).setParent(c.getId().toString());
      bulkRequest.add(req);
    }
    if (bulkRequest.numberOfActions() > 0) {
      BulkResponse resp = bulkRequest.get();
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
  private static List<String> getCollectionItemIds(CollectionImeji c)
      throws InterruptedException, ExecutionException {
    TermQueryBuilder q =
        QueryBuilders.termQuery(ElasticFields.FOLDER.field(), c.getId().toString());
    SearchResponse resp = ElasticService.getClient().prepareSearch(ElasticService.DATA_ALIAS)
        .setNoFields().setQuery(q).setTypes(ElasticTypes.items.name())
        .setScroll(new TimeValue(60000)).execute().get();
    final List<String> ids = new ArrayList<>(Math.toIntExact(resp.getHits().getTotalHits()));
    for (final SearchHit hit : resp.getHits()) {
      ids.add(hit.getId());
    }
    while (true) {
      resp = ElasticService.getClient().prepareSearchScroll(resp.getScrollId())
          .setScroll(new TimeValue(60000)).execute().actionGet();
      if (resp.getHits().getHits().length == 0) {
        break;
      }
      for (final SearchHit hit : resp.getHits()) {
        ids.add(hit.getId());
      }
    }
    ElasticService.getClient().prepareClearScroll().addScrollId(resp.getScrollId()).execute()
        .actionGet();
    return ids;
  }

}
