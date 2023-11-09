package de.mpg.imeji.logic.search.elasticsearch.script;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import co.elastic.clients.elasticsearch._types.Time;
import co.elastic.clients.elasticsearch._types.query_dsl.TermQuery;
import co.elastic.clients.elasticsearch.core.*;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation;
import co.elastic.clients.elasticsearch.core.bulk.UpdateOperation;

import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.TrackHits;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import org.elasticsearch.client.RequestOptions;

import de.mpg.imeji.exceptions.SearchIndexBulkFailureException;
import de.mpg.imeji.logic.model.CollectionImeji;
import de.mpg.imeji.logic.search.elasticsearch.ElasticIndexer;
import de.mpg.imeji.logic.search.elasticsearch.ElasticService;
import de.mpg.imeji.logic.search.elasticsearch.ElasticService.ElasticIndices;
import de.mpg.imeji.logic.search.elasticsearch.model.ElasticFields;


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
    final BulkRequest.Builder bulkRequestBuilder = new BulkRequest.Builder();
    //inal XContentBuilder json = new CollectionFields(c).toXContentBuilder();
    for (final String id : ids) {

      //final UpdateRequest req = new UpdateRequest();
      //req.index(index).type("_doc").id(id).doc(json);
      bulkRequestBuilder.operations(BulkOperation.of(bo -> bo.update(ur -> ur.index(index).id(id).action(act -> act.doc(c)))));
    }

    final BulkRequest bulkRequest = bulkRequestBuilder.build();
    if (bulkRequest.operations().size() > 0) {
      BulkResponse bulkResponse = ElasticService.getClient().bulk(bulkRequest);

      if (bulkResponse.errors()) {
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

    TermQuery q = TermQuery.of(tq -> tq.field(ElasticFields.FOLDER.field()).value(collection.getId().toString()));
    SearchRequest searchRequest = SearchRequest.of(sr -> sr.trackTotalHits(TrackHits.of(th -> th.enabled(true))).query(q._toQuery())
        .index(ElasticIndices.items.name()).scroll(Time.of(t -> t.time("1m"))));
    /*
    SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
    searchSourceBuilder.trackTotalHits(true);
    searchSourceBuilder.query(q);
    searchRequest.indices(ElasticIndices.items.name()).source(searchSourceBuilder).scroll(TimeValue.timeValueMinutes(1));
    */
    List<String> ids = new ArrayList<String>(0);


    String lastScrollId;

    SearchResponse<Object> resp = ElasticService.getClient().search(searchRequest, Object.class);
    ids = new ArrayList<>(Math.toIntExact(resp.hits().total().value()));
    lastScrollId = resp.scrollId();
    for (Hit hit : resp.hits().hits()) {
      ids.add(hit.id());
    }

    while (true) {
      String scrollId = resp.scrollId();
      ScrollRequest scrollRequest = ScrollRequest.of(sr -> sr.scrollId(scrollId).scroll(Time.of(t -> t.time("1m"))));
      /*
      SearchRequest scrollRequest = new SearchScrollRequest(scrollId);
      scrollRequest.scroll(TimeValue.timeValueSeconds(60));
      */
      ScrollResponse<Object> scrollResp = ElasticService.getClient().scroll(scrollRequest, Object.class);
      lastScrollId = scrollResp.scrollId();
      if (resp.hits().hits().size() == 0) {
        break;
      }
      for (final Hit hit : scrollResp.hits().hits()) {
        ids.add(hit.id());
      }
    }

    String finalLastScrollId = lastScrollId;
    ClearScrollRequest clearScrollRequest = ClearScrollRequest.of(csr -> csr.scrollId(finalLastScrollId));
    ClearScrollResponse response = ElasticService.getClient().clearScroll(clearScrollRequest);

    return ids;
  }

}
