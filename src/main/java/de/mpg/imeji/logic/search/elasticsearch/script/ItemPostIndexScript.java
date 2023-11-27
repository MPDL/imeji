package de.mpg.imeji.logic.search.elasticsearch.script;

import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.GetRequest;
import co.elastic.clients.elasticsearch.core.GetResponse;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation;
import com.fasterxml.jackson.databind.node.ObjectNode;
import de.mpg.imeji.exceptions.SearchIndexBulkFailureException;
import de.mpg.imeji.logic.model.Item;
import de.mpg.imeji.logic.search.elasticsearch.ElasticIndexer;
import de.mpg.imeji.logic.search.elasticsearch.ElasticService;
import de.mpg.imeji.logic.search.elasticsearch.ElasticService.ElasticIndices;
import de.mpg.imeji.logic.search.elasticsearch.model.ElasticFields;
import de.mpg.imeji.logic.search.elasticsearch.script.misc.CollectionFields;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

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
    final BulkRequest.Builder bulkRequestBuilder = new BulkRequest.Builder();

    boolean fieldsFound = false;
    for (final Item item : items) {
      CollectionFields fields = retrieveCollectionFields(item, index);
      if (fields != null) {
        fieldsFound = true;
        final ObjectNode json = fields.toJsonNode();
        bulkRequestBuilder.operations(BulkOperation
            .of(bo -> bo.update(ur -> ur.index(ElasticIndices.items.name()).id(item.getId().toString()).action(act -> act.doc(json)))));

      }
    }

    if (fieldsFound) {
      final BulkRequest bulkRequest = bulkRequestBuilder.build();
      BulkResponse bulkResponse = ElasticService.getClient().bulk(bulkRequest);

      if (bulkResponse.errors()) {
        throw ElasticIndexer.getSearchIndexBulkFailureException(bulkResponse);
      }
    }

  }

  private static CollectionFields retrieveCollectionFields(Item item, String index) {

    GetRequest.Builder getRequest = new GetRequest.Builder();
    String[] includes = new String[] {ElasticFields.AUTHOR_COMPLETENAME.field(), ElasticFields.AUTHOR_ORGANIZATION.field(),
        ElasticFields.ID.field(), ElasticFields.NAME.field()};
    //FetchSourceContext source_ctx = new FetchSourceContext(true, includes, null);
    getRequest.index(ElasticIndices.folders.name()).id(item.getCollection().toString()).sourceIncludes(Arrays.asList(includes));
    GetResponse<ObjectNode> resp;
    try {
      resp = ElasticService.getClient().get(getRequest.build(), ObjectNode.class);
      if (resp.found()) {
        /*
         * return new
         * CollectionFields(resp.getField(ElasticFields.AUTHOR_COMPLETENAME.field()),
         * resp.getField(ElasticFields.AUTHOR_ORGANIZATION.field()),
         * resp.getField(ElasticFields.ID.field()),
         * resp.getField(ElasticFields.NAME.field()));
         */
        return new CollectionFields(resp.source());
      }
    } catch (IOException e) {
      LOGGER.error("Could not retrieve collection fields");
    }
    return null;
  }

}
