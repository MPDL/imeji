package de.mpg.imeji.logic.search.elasticsearch;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.mapping.put.PutMappingRequest;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.delete.DeleteRequestBuilder;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.common.xcontent.XContentType;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.mpg.imeji.exceptions.UnprocessableError;
import de.mpg.imeji.logic.model.CollectionImeji;
import de.mpg.imeji.logic.model.ContentVO;
import de.mpg.imeji.logic.model.Item;
import de.mpg.imeji.logic.model.Properties;
import de.mpg.imeji.logic.model.Subscription;
import de.mpg.imeji.logic.model.User;
import de.mpg.imeji.logic.model.UserGroup;
import de.mpg.imeji.logic.search.SearchIndexer;
import de.mpg.imeji.logic.search.elasticsearch.ElasticService.ElasticAnalysers;
import de.mpg.imeji.logic.search.elasticsearch.ElasticService.ElasticIndices;
import de.mpg.imeji.logic.search.elasticsearch.model.ElasticContent;
import de.mpg.imeji.logic.search.elasticsearch.model.ElasticFolder;
import de.mpg.imeji.logic.search.elasticsearch.model.ElasticItem;
import de.mpg.imeji.logic.search.elasticsearch.model.ElasticUser;
import de.mpg.imeji.logic.search.elasticsearch.model.ElasticUserGroup;
import de.mpg.imeji.logic.search.elasticsearch.script.CollectionPostIndexScript;
import de.mpg.imeji.logic.search.elasticsearch.script.ItemPostIndexScript;
import de.mpg.imeji.logic.util.StringHelper;

/**
 * Indexer for ElasticSearch
 *
 * @author bastiens
 *
 */
public class ElasticIndexer implements SearchIndexer {

	private static final Logger LOGGER = LogManager.getLogger(ElasticIndexer.class);
	private static final ObjectMapper mapper = new ObjectMapper();
	private final String index;
	private final String dataType = "_doc";
	// private final ElasticAnalysers analyser;
	private String mappingFile = "elasticsearch/Elastic_TYPE_Mapping.json";

	/**
	 * Create an instance for writing data to the ElasticSearch server
	 * 
	 * @param indexName
	 *            the name for the index under which data shall be stored index is
	 *            an ElasticSearch concept, a name or number under which data can be
	 *            stored collectively
	 * @param dataType
	 *            the type under which data shall be stored type is an ElasticSearch
	 *            concept, a "category" under which data of an index can be stored
	 * @param analyser
	 */

	public ElasticIndexer(String indexName) {
		this.index = indexName;
		// this.dataType = dataType.name();
		// this.analyser = analyser;
		this.mappingFile = mappingFile.replace("_TYPE_", StringUtils.capitalize(this.index));
	}

	/**
	 * Add object to the index of this ElasticIndexer instance index is an
	 * ElasticSearch concept, a name or number under which data can be stored For
	 * each index in ElasticSearch an original version plus a configurable number of
	 * replicas is created
	 * 
	 * @param obj
	 *            the object that will be added to the index
	 */
	@Override
	public void index(String indexName, Object obj) {
		try {
			indexJSON(indexName, getId(obj), toJson(obj, dataType, index), getParent(obj));
			// commit();
		} catch (final Exception e) {
			LOGGER.error("Error indexing object ", e);
		}
	}

	@Override
	public void indexBatch(String indexName, List<?> l) {
		updateIndexBatch(indexName, l);
	}

	@Override
	public void updateIndexBatch(String indexName, List<?> l) {
		if (l.isEmpty()) {
			return;
		}
		try {
			final BulkRequest bulkRequest = new BulkRequest();
			for (final Object obj : l) {
				LOGGER.info("+++ index request " + index + "  " + getId(obj));
				bulkRequest.add(
						getIndexRequest(indexName, getId(obj), toJson(obj, dataType, index), getParent(obj), dataType));
			}
			if (bulkRequest.numberOfActions() > 0) {
				BulkResponse resp = ElasticService.getClient().bulk(bulkRequest, RequestOptions.DEFAULT);
			}
		} catch (final Exception e) {
			LOGGER.error("error indexing object ", e);
		}
		if (!(l.get(0) instanceof ContentVO)) {
			// commit();
		}
		updateIndexBatchPostProcessing(l);
	}

	/**
	 * 
	 * @param l
	 */
	private void updateIndexBatchPostProcessing(List<?> l) {
		if (l.isEmpty()) {
			return;
		}
		ItemPostIndexScript.run(l, index);
		for (Object o : l) {
			if (o instanceof CollectionImeji) {
				CollectionPostIndexScript.run((CollectionImeji) o, index);
			}
		}
	}

	@Override
	public void delete(String indexName, Object obj) {
		final String id = getId(obj);
		if (id != null) {
			DeleteRequest deleteRequest = new DeleteRequest();
			deleteRequest.index(index).id(id);
			try {
				DeleteResponse resp = ElasticService.getClient().delete(deleteRequest, RequestOptions.DEFAULT);
			} catch (IOException e) {
				LOGGER.error("error deleting " + id, e);
			}
			// commit();
		}
	}

	@Override
	public void deleteBatch(String indexName, List<?> l) {
		if (l.isEmpty()) {
			return;
		}
		final BulkRequest bulkRequest = new BulkRequest();
		for (final Object obj : l) {
			final String id = getId(obj);
			if (id != null) {
				bulkRequest.add(getDeleteRequest(indexName, id, getParent(obj)));
			}
		}
		if (bulkRequest.numberOfActions() > 0) {
			try {
				BulkResponse resp = ElasticService.getClient().bulk(bulkRequest, RequestOptions.DEFAULT);
			} catch (IOException e) {
				LOGGER.error("ERROR during bulk delete", e);
			}
		}
		if (!(l.get(0) instanceof ContentVO)) {
			// commit();
		}
	}

	/**
	 * Return the index Request
	 *
	 * @param id
	 * @param json
	 * @param parent
	 * @return
	 */
	private IndexRequest getIndexRequest(String indexName, String id, String json, String parent, String type) {
		final IndexRequest indexRequest = new IndexRequest();
		indexRequest.index(index).id(id).source(json, XContentType.JSON);
		if (parent != null) {
			indexRequest.parent(parent);
		}
		if (type != null) {
			indexRequest.type(type);
		}
		LOGGER.info("+++ the request object: " + indexRequest.index() + "   " + indexRequest.type() + "   "
				+ indexRequest.id());
		return indexRequest;
	}

	/**
	 * Return the Delete Request
	 *
	 * @param id
	 * @param json
	 * @param parent
	 * @return
	 */
	private DeleteRequest getDeleteRequest(String index, String id, String parent) {
		final DeleteRequest deleteRequest = new DeleteRequest();
		deleteRequest.index(index).id(id);
		if (parent != null) {
			deleteRequest.parent(parent);
		}
		return deleteRequest;
	}

	/**
	 * Transform an object to a json
	 *
	 * @param obj
	 * @return
	 * @throws UnprocessableError
	 */
	public static String toJson(Object obj, String dataType, String index) throws UnprocessableError {
		try {
			return mapper.setSerializationInclusion(Include.NON_NULL)
					.writeValueAsString(toESEntity(obj, dataType, index));
		} catch (final JsonProcessingException e) {
			throw new UnprocessableError("Error serializing object to json", e);
		}
	}

	/**
	 * Index in Elasticsearch the passed json with the given id
	 *
	 * @param id
	 * @param json
	 */
	public void indexJSON(String indexName, String id, String json, String parent) {
		if (id != null) {
			IndexRequest req = getIndexRequest(indexName, id, json, parent, dataType);
			try {
				IndexResponse resp = ElasticService.getClient().index(req, RequestOptions.DEFAULT);
			} catch (IOException e) {
				LOGGER.error("error indexing " + id, e);
			}
		}
	}

	/**
	 * Make all changes done searchable. Kind of a commit. Might be important if
	 * data needs to be immediately available for other tasks
	 */
	/*
	 * public void commit() { // Check if refresh is needed: cost is very high
	 * ElasticService.getClient().admin().indices().prepareRefresh(index).execute().
	 * actionGet(); }
	 */

	/**
	 * Remove all indexed data
	 */
	/*
	 * public static void clear(Client client, String index) { final
	 * DeleteIndexResponse delete = client.admin().indices().delete(new
	 * DeleteIndexRequest(index)).actionGet(); if (!delete.isAcknowledged()) { //
	 * Error } }
	 */

	/**
	 * Transform a model Entity into an Elasticsearch Entity
	 *
	 * @param obj
	 * @return
	 */
	private static Object toESEntity(Object obj, String dataType, String index) {
		if (obj instanceof Item) {
			return new ElasticItem((Item) obj);
		}
		if (obj instanceof CollectionImeji) {
			return new ElasticFolder((CollectionImeji) obj);
		}
		if (obj instanceof User) {
			return new ElasticUser((User) obj);
		}
		if (obj instanceof UserGroup) {
			return new ElasticUserGroup((UserGroup) obj);
		}
		if (obj instanceof ContentVO) {
			return new ElasticContent((ContentVO) obj);
		}
		if (obj instanceof Subscription) {
			return null;
		}
		return obj;
	}

	/**
	 * Get the Parent of the object
	 *
	 * @param obj
	 * @return
	 */
	public static String getParent(Object obj) {
		if (obj instanceof ContentVO) {
			return StringHelper.isNullOrEmptyTrim(((ContentVO) obj).getItemId()) ? null : ((ContentVO) obj).getItemId();
		} else if (obj instanceof Item) {
			// return ((Item) obj).getCollection().toString();
		}
		return null;
	}

	/**
	 * Get the Id of an Object that is of type Properties, User, UserGroup,
	 * ContentVO or other with function getId
	 * 
	 * @param obj
	 * @return
	 */
	private String getId(Object obj) {

		if (obj instanceof Properties) {
			return ((Properties) obj).getId().toString();
		}
		if (obj instanceof User) {
			return ((User) obj).getId().toString();
		}
		if (obj instanceof UserGroup) {
			return ((UserGroup) obj).getId().toString();
		}
		if (obj instanceof ContentVO) {
			return ((ContentVO) obj).getId().toString();
		}
		try {
			return (String) obj.getClass().getMethod("getId").invoke(obj);
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * Add a mapping to the fields (important to have a better search)
	 */
	public void addMapping() {
		try {
			final String jsonMapping = new String(
					Files.readAllBytes(
							Paths.get(ElasticIndexer.class.getClassLoader().getResource(mappingFile).toURI())),
					"UTF-8");
			PutMappingRequest request = new PutMappingRequest(this.index);
			request.type(dataType);
			request.source(jsonMapping, XContentType.JSON);
			AcknowledgedResponse putMappingResponse = ElasticService.getClient().indices().putMapping(request,
					RequestOptions.DEFAULT);
		} catch (final Exception e) {
			LOGGER.error("Error initializing the Elastic Search Mapping " + mappingFile, e);
		}
	}

	@Override
	public void updatePartial(String indexName, String id, Object obj) {
		if (id != null) {
			UpdateRequest updateRequest = new UpdateRequest();
			updateRequest.index(index).id(id).doc(obj);
			try {
				UpdateResponse resp = ElasticService.getClient().update(updateRequest, RequestOptions.DEFAULT);
			} catch (IOException e) {
				LOGGER.error("error updating " + id, e);
			}
		}
	}

	/**
	 * Bulk Partial update
	 * 
	 * @param l
	 */
	public void partialUpdateIndexBatch(String indexName, List<?> l) {
		if (l.isEmpty()) {
			return;
		}
		try {
			final BulkRequest bulkRequest = new BulkRequest();
			for (final Object obj : l) {

				UpdateRequest updateRequest = new UpdateRequest();
				updateRequest.index(index).id(getId(obj)).doc(obj);
				bulkRequest.add(updateRequest);
			}
			BulkResponse resp = ElasticService.getClient().bulk(bulkRequest, RequestOptions.DEFAULT);
		} catch (final Exception e) {
			LOGGER.error("error indexing object ", e);
		}
	}

	@Override
	public void index(Object obj) {
		// TODO Auto-generated method stub

	}

	@Override
	public void indexBatch(List<?> l) {
		// TODO Auto-generated method stub

	}

	@Override
	public void updateIndexBatch(List<?> l) {
		// TODO Auto-generated method stub

	}

	@Override
	public void delete(Object obj) {
		// TODO Auto-generated method stub

	}

	@Override
	public void deleteBatch(List<?> l) {
		// TODO Auto-generated method stub

	}

	@Override
	public void updatePartial(String id, Object obj) {
		// TODO Auto-generated method stub

	}
}
