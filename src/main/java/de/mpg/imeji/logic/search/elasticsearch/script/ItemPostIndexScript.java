package de.mpg.imeji.logic.search.elasticsearch.script;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.update.UpdateRequestBuilder;
import org.elasticsearch.common.xcontent.XContentBuilder;

import de.mpg.imeji.logic.search.elasticsearch.ElasticService;
import de.mpg.imeji.logic.search.elasticsearch.ElasticService.ElasticTypes;
import de.mpg.imeji.logic.search.elasticsearch.model.ElasticFields;
import de.mpg.imeji.logic.search.elasticsearch.script.misc.CollectionFields;
import de.mpg.imeji.logic.vo.Item;

/**
 * Script running before an item is indexed
 * 
 * @author saquet
 *
 */
public class ItemPostIndexScript {
  private static final Logger LOGGER = Logger.getLogger(ItemPostIndexScript.class);


  public static void run(List<?> list, String index) {
    List<Item> items =
        (List<Item>) list.stream().filter(o -> o instanceof Item).collect(Collectors.toList());
    final BulkRequestBuilder bulkRequest = ElasticService.getClient().prepareBulk();

    for (final Item item : items) {
      try {
        CollectionFields fields = retrieveCollectionFields(item, index);
        if (fields != null) {
          final XContentBuilder json = fields.toXContentBuilder();
          final UpdateRequestBuilder req = ElasticService.getClient()
              .prepareUpdate(index, ElasticTypes.items.name(), item.getId().toString()).setDoc(json)
              .setParent(item.getCollection().toString());
          bulkRequest.add(req);
        }
      } catch (Exception e) {
        LOGGER.error("Error indexing item with collection fields", e);
      }

    }
    if (bulkRequest.numberOfActions() > 0) {
      BulkResponse resp = bulkRequest.get();
      if (resp.hasFailures()) {
        LOGGER.error(resp.buildFailureMessage());
      }
    }
  }

  private static CollectionFields retrieveCollectionFields(Item item, String index) {
    GetResponse resp = ElasticService.getClient()
        .prepareGet(index, ElasticTypes.folders.name(), item.getCollection().toString())
        .setFields(ElasticFields.AUTHOR_COMPLETENAME.field(),
            ElasticFields.AUTHOR_ORGANIZATION.field(), ElasticFields.ID.field(),
            ElasticFields.NAME.field())
        .get();
    if (resp.isExists()) {
      return new CollectionFields(resp.getField(ElasticFields.AUTHOR_COMPLETENAME.field()),
          resp.getField(ElasticFields.AUTHOR_ORGANIZATION.field()),
          resp.getField(ElasticFields.ID.field()), resp.getField(ElasticFields.NAME.field()));
    }
    return null;
  }

}
