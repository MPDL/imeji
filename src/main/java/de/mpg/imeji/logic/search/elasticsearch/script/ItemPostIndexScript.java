package de.mpg.imeji.logic.search.elasticsearch.script;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.search.fetch.subphase.FetchSourceContext;

import de.mpg.imeji.exceptions.SearchIndexBulkFailureException;
import de.mpg.imeji.logic.model.Item;
import de.mpg.imeji.logic.search.elasticsearch.ElasticIndexer;
import de.mpg.imeji.logic.search.elasticsearch.ElasticService;
import de.mpg.imeji.logic.search.elasticsearch.ElasticService.ElasticIndices;
import de.mpg.imeji.logic.search.elasticsearch.model.ElasticFields;
import de.mpg.imeji.logic.search.elasticsearch.script.misc.CollectionFields;

/**
 * Script running after an item is indexed
 * 
 * @author saquet
 *
 */
public class ItemPostIndexScript {

  private static final Logger LOGGER = LogManager.getLogger(ItemPostIndexScript.class);

  public static void run(List<?> list, String index) throws IOException, SearchIndexBulkFailureException {

    List<Item> items = (List<Item>) list.stream().filter(o -> o instanceof Item).collect(Collectors.toList());
    final BulkRequest bulkRequest = new BulkRequest();

    for (final Item item : items) {
      CollectionFields fields = retrieveCollectionFields(item, index);
      if (fields != null) {
        final XContentBuilder json = fields.toXContentBuilder();
        UpdateRequest updateRequest = new UpdateRequest();
        updateRequest.index(ElasticIndices.items.name()).type("_doc").id(item.getId().toString()).doc(json);
        bulkRequest.add(updateRequest);
      }
    }
    if (bulkRequest.numberOfActions() > 0) {
      BulkResponse resp = ElasticService.getClient().bulk(bulkRequest, RequestOptions.DEFAULT);
      if (resp.hasFailures()) {
        throw ElasticIndexer.getSearchIndexBulkFailureException(resp);
      }
    }
  }

  private static CollectionFields retrieveCollectionFields(Item item, String index) {

    GetRequest getRequest = new GetRequest();
    String[] includes = new String[] {ElasticFields.AUTHOR_COMPLETENAME.field(), ElasticFields.AUTHOR_ORGANIZATION.field(),
        ElasticFields.ID.field(), ElasticFields.NAME.field()};
    FetchSourceContext source_ctx = new FetchSourceContext(true, includes, null);
    getRequest.index(ElasticIndices.folders.name()).id(item.getCollection().toString()).fetchSourceContext(source_ctx);
    GetResponse resp;
    try {
      resp = ElasticService.getClient().get(getRequest, RequestOptions.DEFAULT);
      if (resp.isExists()) {
        /*
         * return new
         * CollectionFields(resp.getField(ElasticFields.AUTHOR_COMPLETENAME.field()),
         * resp.getField(ElasticFields.AUTHOR_ORGANIZATION.field()),
         * resp.getField(ElasticFields.ID.field()),
         * resp.getField(ElasticFields.NAME.field()));
         */
        return new CollectionFields(resp.getSourceAsBytes());
      }
    } catch (IOException e) {
      LOGGER.error("Could not retrieve collection fields");
    }
    return null;
  }

}
