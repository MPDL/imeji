package de.mpg.imeji.logic.search.elasticsearch.script;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.action.update.UpdateRequestBuilder;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.common.xcontent.XContentBuilder;

import de.mpg.imeji.logic.model.Item;
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

	public static void run(List<?> list, String index) {
		List<Item> items = (List<Item>) list.stream().filter(o -> o instanceof Item).collect(Collectors.toList());
		final BulkRequest bulkRequest = new BulkRequest();

		for (final Item item : items) {
			try {
				CollectionFields fields = retrieveCollectionFields(item, index);
				if (fields != null) {
					final XContentBuilder json = fields.toXContentBuilder();
					UpdateRequest updateRequest = new UpdateRequest();
					updateRequest.index(ElasticIndices.items.name()).id(item.getId().toString()).doc(json);
					bulkRequest.add(updateRequest);
				}
			} catch (Exception e) {
				LOGGER.error("Error indexing item with collection fields", e);
			}

		}
		if (bulkRequest.numberOfActions() > 0) {
			BulkResponse resp;
			try {
				resp = ElasticService.getClient().bulk(bulkRequest, RequestOptions.DEFAULT);
			} catch (IOException e) {
				LOGGER.error("error during bulk", e);
			}
		}
	}

	private static CollectionFields retrieveCollectionFields(Item item, String index) {
		GetRequest getRequest = new GetRequest();
		getRequest.index(ElasticIndices.folders.name()).id(item.getCollection().toString()).storedFields(
				ElasticFields.AUTHOR_COMPLETENAME.field(), ElasticFields.AUTHOR_ORGANIZATION.field(),
				ElasticFields.ID.field(), ElasticFields.NAME.field());

		GetResponse resp;
		try {
			resp = ElasticService.getClient().get(getRequest, RequestOptions.DEFAULT);
			if (resp.isExists()) {
				return new CollectionFields(resp.getField(ElasticFields.AUTHOR_COMPLETENAME.field()),
						resp.getField(ElasticFields.AUTHOR_ORGANIZATION.field()),
						resp.getField(ElasticFields.ID.field()), resp.getField(ElasticFields.NAME.field()));
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

}
