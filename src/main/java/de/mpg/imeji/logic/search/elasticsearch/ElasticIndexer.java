package de.mpg.imeji.logic.search.elasticsearch;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.action.DocWriteResponse;
import org.elasticsearch.action.admin.indices.mapping.put.PutMappingRequest;
import org.elasticsearch.action.admin.indices.refresh.RefreshRequest;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.VersionType;
import org.elasticsearch.rest.RestStatus;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.mpg.imeji.exceptions.SearchIndexBulkFailureException;
import de.mpg.imeji.exceptions.SearchIndexFailureException;
import de.mpg.imeji.exceptions.UnprocessableError;
import de.mpg.imeji.logic.model.CollectionImeji;
import de.mpg.imeji.logic.model.ContentVO;
import de.mpg.imeji.logic.model.Item;
import de.mpg.imeji.logic.model.Properties;
import de.mpg.imeji.logic.model.Subscription;
import de.mpg.imeji.logic.model.User;
import de.mpg.imeji.logic.model.UserGroup;
import de.mpg.imeji.logic.model.aspects.ResourceLastModified;
import de.mpg.imeji.logic.search.SearchIndexer;
import de.mpg.imeji.logic.search.elasticsearch.model.ElasticContent;
import de.mpg.imeji.logic.search.elasticsearch.model.ElasticFolder;
import de.mpg.imeji.logic.search.elasticsearch.model.ElasticItem;
import de.mpg.imeji.logic.search.elasticsearch.model.ElasticJoinField;
import de.mpg.imeji.logic.search.elasticsearch.model.ElasticJoinableContent;
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
  private final String indexName;
  private final String dataType = "_doc";
  // private final ElasticAnalysers analyser;
  private String mappingFile = "elasticsearch/Elastic_TYPE_Mapping.json";

  /**
   * Create an instance for writing data to the ElasticSearch server
   * 
   * @param indexName the name for the index under which data shall be stored index is an
   *        ElasticSearch concept, a name or number under which data can be stored collectively
   * @param dataType the type under which data shall be stored type is an ElasticSearch concept, a
   *        "category" under which data of an index can be stored
   * @param analyser
   */

  public ElasticIndexer(String indexName) {
    this.indexName = indexName;
    // this.dataType = dataType.name();
    // this.analyser = analyser;
    this.mappingFile = mappingFile.replace("_TYPE_", StringUtils.capitalize(this.indexName));
  }

  /**
   * Add object to the index of this ElasticIndexer instance index is an ElasticSearch concept, a
   * name or number under which data can be stored For each index in ElasticSearch an original
   * version plus a configurable number of replicas is created
   * 
   * @param obj the object that will be added to the index
   * @throws UnprocessableError
   * @throws IOException
   * @throws SearchIndexFailureException
   */
  @Override
  public void index(Object obj) throws UnprocessableError, IOException, SearchIndexFailureException {

    if (obj instanceof ResourceLastModified) {
      long timestamp = ((ResourceLastModified) obj).getModified().getTimeInMillis();
      indexJSON(getId(obj), toJson(obj, dataType, indexName), getParent(obj), timestamp);
    } else {
      indexJSON(getId(obj), toJson(obj, dataType, indexName), getParent(obj));
    }
    commit();

  }

  @Override
  public void indexBatch(List<?> l)
      throws UnprocessableError, IOException, SearchIndexBulkFailureException, InterruptedException, ExecutionException {
    updateIndexBatch(l);
  }

  @Override
  public void updateIndexBatch(List<?> objectList)
      throws UnprocessableError, IOException, SearchIndexBulkFailureException, InterruptedException, ExecutionException {
    if (objectList.isEmpty()) {
      return;
    }

    final BulkRequest bulkRequest = new BulkRequest();

    for (final Object obj : objectList) {

      LOGGER.info("+++ index request " + indexName + "  " + getId(obj));
      final IndexRequest indexRequest;
      if (obj instanceof ResourceLastModified) {
        long timestamp = ((ResourceLastModified) obj).getModified().getTimeInMillis();
        indexRequest = getIndexRequest(getId(obj), toJson(obj, dataType, indexName), getParent(obj), dataType, timestamp);
      } else {
        indexRequest = getIndexRequest(getId(obj), toJson(obj, dataType, indexName), getParent(obj), dataType);
      }
      bulkRequest.add(indexRequest);
    }

    if (bulkRequest.numberOfActions() > 0) {
      BulkResponse bulkResponse = ElasticService.getClient().bulk(bulkRequest, RequestOptions.DEFAULT);
      if (bulkResponse.hasFailures()) {
        throw getSearchIndexBulkFailureException(bulkResponse);
      }
    }

    if (!(objectList.get(0) instanceof ContentVO)) {
      commit();
    }
    updateIndexBatchPostProcessing(objectList);
  }


  /**
   * 
   * @param objectList
   * @throws ExecutionException
   * @throws InterruptedException
   * @throws IOException
   * @throws SearchIndexBulkFailureException
   */
  private void updateIndexBatchPostProcessing(List<?> objectList)
      throws SearchIndexBulkFailureException, IOException, InterruptedException, ExecutionException {
    if (objectList.isEmpty()) {
      return;
    }
    ItemPostIndexScript.run(objectList, indexName);
    for (Object o : objectList) {
      if (o instanceof CollectionImeji) {
        CollectionPostIndexScript.run((CollectionImeji) o, "items");
      }
    }
  }


  @Override
  public void delete(Object obj) throws IOException, SearchIndexFailureException {
    final String id = getId(obj);
    if (id != null) {
      DeleteRequest deleteRequest = getDeleteRequest(id, getParent(obj));
      DeleteResponse deleteResponse = ElasticService.getClient().delete(deleteRequest, RequestOptions.DEFAULT);
      checkIfOperationWasSuccessful(deleteResponse);
      commit();
    }
  }

  @Override
  public void deleteBatch(List<?> l) throws IOException, SearchIndexBulkFailureException {
    if (l.isEmpty()) {
      return;
    }
    final BulkRequest bulkRequest = new BulkRequest();
    for (final Object obj : l) {
      final String id = getId(obj);
      if (id != null) {
        bulkRequest.add(getDeleteRequest(id, getParent(obj)));
      }
    }
    if (bulkRequest.numberOfActions() > 0) {
      BulkResponse bulkResponse = ElasticService.getClient().bulk(bulkRequest, RequestOptions.DEFAULT);
      if (bulkResponse.hasFailures()) {
        throw getSearchIndexBulkFailureException(bulkResponse);
      }
    }
    if (!(l.get(0) instanceof ContentVO)) {
      commit();
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
  private IndexRequest getIndexRequest(String id, String json, String parent, String type, long timestamp) {

    final IndexRequest indexRequest = new IndexRequest();
    indexRequest.index(indexName).id(id).source(json, XContentType.JSON);

    // Add version information (See ticket #1122)
    // We can choose here:
    // - VersionType.EXTERNAL: Any request to index a document 
    //                         that has THE SAME timestamp as the existing
    //                         document will be rejected
    // - VersionType.EXTERNAL_GTE: Means that any request to index a document 
    //                             that has a timestamp BEFORE the existing document
    //                             will be rejected

    indexRequest.versionType(VersionType.EXTERNAL_GTE).version(timestamp);
    if (parent != null) {
      // indexRequest.parent(parent);
      indexRequest.routing(parent);
    }
    if (type != null) {
      indexRequest.type(type);
    }
    LOGGER.info("+++ the request object: " + indexRequest.index() + "   " + indexRequest.type() + "   " + indexRequest.id());
    return indexRequest;
  }


  /**
   * Return the index Request
   *
   * @param id
   * @param json
   * @param parent
   * @return
   */
  private IndexRequest getIndexRequest(String id, String json, String parent, String type) {
    final IndexRequest indexRequest = new IndexRequest();
    indexRequest.index(indexName).id(id).source(json, XContentType.JSON);
    if (parent != null) {
      // indexRequest.parent(parent);
      indexRequest.routing(parent);
    }
    if (type != null) {
      indexRequest.type(type);
    }
    LOGGER.info("+++ the request object: " + indexRequest.index() + "   " + indexRequest.type() + "   " + indexRequest.id());
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
  private DeleteRequest getDeleteRequest(String id, String parent) {
    final DeleteRequest deleteRequest = new DeleteRequest();
    deleteRequest.index(indexName).id(id).type(dataType);
    if (parent != null) {
      deleteRequest.routing(parent);
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
      // mapper.configure(DeserializationFeature..UNWRAP_ROOT_VALUE, true);
      /*
      if (obj instanceof ContentVO) {
      mapper.configure(SerializationFeature.WRAP_ROOT_VALUE, true);
      }
      */
      return mapper.setSerializationInclusion(Include.NON_NULL).writeValueAsString(toESEntity(obj, dataType, index));
    } catch (final JsonProcessingException e) {
      throw new UnprocessableError("Error serializing object to json", e);
    }
  }

  /**
   * Index in Elasticsearch the passed json with the given id
   *
   * @param id
   * @param json
   * @throws IOException
   * @throws SearchIndexFailureException
   */
  public void indexJSON(String id, String json, String parent, long timestamp) throws IOException, SearchIndexFailureException {
    if (id != null) {
      IndexRequest req = getIndexRequest(id, json, parent, dataType, timestamp);
      IndexResponse indexResponse = ElasticService.getClient().index(req, RequestOptions.DEFAULT);
      checkIfOperationWasSuccessful(indexResponse);
    }
  }

  /**
   * Index in Elasticsearch the passed json with the given id
   *
   * @param id
   * @param json
   * @throws IOException
   * @throws SearchIndexFailureException
   */
  public void indexJSON(String id, String json, String parent) throws IOException, SearchIndexFailureException {
    if (id != null) {
      IndexRequest req = getIndexRequest(id, json, parent, dataType);
      IndexResponse indexResponse = ElasticService.getClient().index(req, RequestOptions.DEFAULT);
      checkIfOperationWasSuccessful(indexResponse);
    }
  }



  /**
   * Make all changes done searchable. Kind of a commit. Might be important if data needs to be
   * immediately available for other tasks
   */

  public void commit() {
    // Check if refresh is needed: cost is very high
    RefreshRequest rr = new RefreshRequest(indexName);
    try {
      ElasticService.getClient().indices().refresh(rr, RequestOptions.DEFAULT);
    } catch (Exception e) {
      LOGGER.error("error refreshing index ", e);
    }
  }



  /**
   * If one or more requests (indexing/updating/deleting a document) have failed in a bulk request
   * to Elastic Search, process the response object and create a SearchIndexBulkFailureException.
   * 
   * @param bulkResponse
   * @return
   */
  public static SearchIndexBulkFailureException getSearchIndexBulkFailureException(BulkResponse bulkResponse) {

    SearchIndexBulkFailureException failureException = new SearchIndexBulkFailureException();
    for (BulkItemResponse bulkItemResponse : bulkResponse.getItems()) {

      if (bulkItemResponse.isFailed()) {

        BulkItemResponse.Failure failure = bulkItemResponse.getFailure();
        String idOfDocument = failure.getId();
        Exception rootProblem = failure.getCause();
        RestStatus status = failure.getStatus();
        DocWriteResponse itemResponse = bulkItemResponse.getResponse();

        // Elastic Search has already indicated that it sees the operation as failed
        // additionally we do our own check (i.e. a delete operation that could not find the document is not a problem)
        if (!elasticSearchResponseReportsSuccess(itemResponse)) {
          failureException.addFailure(idOfDocument, itemResponse, status, rootProblem);
        }
      }
    }
    return failureException;
  }


  /**
   * Checks whether an operation (indexing/updating/deleting a document) in Elastic Search has been
   * successful. Throws Exception if status codes indicate error/failure of the operation.
   * 
   * @param response Response object (DocWrite)
   * @throws SearchIndexFailureException
   */
  private void checkIfOperationWasSuccessful(DocWriteResponse response) throws SearchIndexFailureException {

    if (!elasticSearchResponseReportsSuccess(response)) {
      throw new SearchIndexFailureException(response);
    }

  }


  private static boolean elasticSearchResponseReportsSuccess(DocWriteResponse response) {

    if (response == null) {
      return false;
    }
    RestStatus restStatus = response.status();
    DocWriteResponse.Result operationResult = response.getResult();

    if (restStatus == RestStatus.OK) {
      return true;
    } else {
      if (response instanceof IndexResponse) {
        // in case of index we can get created ins
        if (restStatus == RestStatus.CREATED) {
          return true;
        }
      } else if (response instanceof DeleteResponse) {
        // if we want to delete a resource that does not exist, this is not a problem
        if (operationResult == DocWriteResponse.Result.NOT_FOUND) {
          return true;
        }
      }
      return false;
    }
  }



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
      ElasticContent cvo = new ElasticContent((ContentVO) obj);
      ElasticJoinField ejf = new ElasticJoinField();
      ejf.setName("content");
      ejf.setParent(((ContentVO) obj).getItemId().toString());
      ElasticJoinableContent ejc = new ElasticJoinableContent();
      ejc.setContent(cvo);
      ejc.setJoinField(ejf);
      return ejc;
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
   * Get the Id of an Object that is of type Properties, User, UserGroup, ContentVO or other with
   * function getId
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
      final String jsonMapping =
          new String(Files.readAllBytes(Paths.get(ElasticIndexer.class.getClassLoader().getResource(mappingFile).toURI())), "UTF-8");
      PutMappingRequest request = new PutMappingRequest(this.indexName);
      request.type(dataType);
      request.source(jsonMapping, XContentType.JSON);
      AcknowledgedResponse putMappingResponse = ElasticService.getClient().indices().putMapping(request, RequestOptions.DEFAULT);
    } catch (final Exception e) {
      LOGGER.error("Error initializing the Elastic Search Mapping " + mappingFile, e);
    }
  }

}
