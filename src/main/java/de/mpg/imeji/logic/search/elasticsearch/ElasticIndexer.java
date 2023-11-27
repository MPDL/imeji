package de.mpg.imeji.logic.search.elasticsearch;

import co.elastic.clients.elasticsearch._types.ErrorCause;
import co.elastic.clients.elasticsearch._types.VersionType;
import co.elastic.clients.elasticsearch.core.*;
import co.elastic.clients.elasticsearch.core.bulk.BulkResponseItem;
import co.elastic.clients.elasticsearch.core.bulk.DeleteOperation;
import co.elastic.clients.elasticsearch.core.bulk.IndexOperation;
import co.elastic.clients.elasticsearch.core.bulk.OperationType;
import co.elastic.clients.elasticsearch.indices.PutMappingRequest;
import co.elastic.clients.elasticsearch.indices.PutMappingResponse;
import co.elastic.clients.elasticsearch.indices.RefreshRequest;
import co.elastic.clients.util.BinaryData;
import co.elastic.clients.util.ContentType;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.mpg.imeji.exceptions.SearchIndexBulkFailureException;
import de.mpg.imeji.exceptions.UnprocessableError;
import de.mpg.imeji.logic.model.*;
import de.mpg.imeji.logic.model.aspects.ResourceLastModified;
import de.mpg.imeji.logic.search.SearchIndexer;
import de.mpg.imeji.logic.search.elasticsearch.model.*;
import de.mpg.imeji.logic.search.elasticsearch.script.CollectionPostIndexScript;
import de.mpg.imeji.logic.search.elasticsearch.script.ItemPostIndexScript;
import de.mpg.imeji.logic.util.StringHelper;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.ExecutionException;

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

    final BulkRequest.Builder bulkRequestBuilder = new BulkRequest.Builder();

    for (final Object obj : objectList) {

      try {
        LOGGER.info("+++ index request " + indexName + "  " + getId(obj));
        final IndexOperation indexOperation;
        if (obj instanceof ResourceLastModified && ((ResourceLastModified) obj).getModified() != null) {
          long timestamp = ((ResourceLastModified) obj).getModified().getTimeInMillis();
          indexOperation = getIndexOperation(getId(obj), toJson(obj, dataType, indexName), getParent(obj), dataType, timestamp);
        } else {
          indexOperation = getIndexOperation(getId(obj), toJson(obj, dataType, indexName), getParent(obj), dataType, null);
        }
        bulkRequestBuilder.operations(op -> op.index(indexOperation));
      } catch (Exception e) {
        LOGGER.error("Error adding object to bulk index list", e);
      }
    }

    BulkRequest bulkRequest = bulkRequestBuilder.build();
    if (bulkRequest.operations().size() > 0) {
      //ElasticService.getClient().bulk(bulkRequest);
      BulkResponse bulkResponse = ElasticService.getClient().bulk(bulkRequest);
      if (bulkResponse.errors()) {
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
  public void deleteBatch(List<?> l) throws IOException, SearchIndexBulkFailureException {
    if (l.isEmpty()) {
      return;
    }
    final BulkRequest.Builder bulkRequestBuilder = new BulkRequest.Builder();
    for (final Object obj : l) {
      final String id = getId(obj);
      if (id != null) {
        bulkRequestBuilder.operations(op -> op.delete(getDeleteOperation(id, getParent(obj))));
      }
    }
    BulkRequest bulkRequest = bulkRequestBuilder.build();
    if (bulkRequest.operations().size() > 0) {
      BulkResponse bulkResponse = ElasticService.getClient().bulk(bulkRequest);
      if (bulkResponse.errors()) {
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
  private IndexRequest getIndexRequest(String id, String json, String parent, String type, Long timestamp) {

    final IndexRequest.Builder indexRequestBuilder = new IndexRequest.Builder();
    indexRequestBuilder.index(indexName).id(id).withJson(new StringReader(json));

    // Add version information (See ticket #1122)
    // We can choose here:
    // - VersionType.EXTERNAL: Any request to index a document 
    //                         that has THE SAME timestamp as the existing
    //                         document will be rejected
    // - VersionType.EXTERNAL_GTE: Means that any request to index a document 
    //                             that has a timestamp BEFORE the existing document
    //                             will be rejected

    if (timestamp != null) {
      indexRequestBuilder.versionType(VersionType.ExternalGte).version(timestamp);
    }

    if (parent != null) {
      // indexRequest.parent(parent);
      indexRequestBuilder.routing(parent);
    }
    /*
    if (type != null) {
      indexRequest.type(type);
    }
     */
    IndexRequest indexRequest = indexRequestBuilder.build();
    LOGGER.info("+++ the request object: " + indexRequest.index() /*+ "   " + indexRequest.type() */ + "   " + indexRequest.id());
    return indexRequest;
  }

  private IndexOperation getIndexOperation(String id, byte[] json, String parent, String type, Long timestamp) {

    final IndexOperation.Builder indexOperationBuilder = new IndexOperation.Builder();
    indexOperationBuilder.document(BinaryData.of(json, ContentType.APPLICATION_JSON)).index(indexName).id(id);

    // Add version information (See ticket #1122)
    // We can choose here:
    // - VersionType.EXTERNAL: Any request to index a document
    //                         that has THE SAME timestamp as the existing
    //                         document will be rejected
    // - VersionType.EXTERNAL_GTE: Means that any request to index a document
    //                             that has a timestamp BEFORE the existing document
    //                             will be rejected

    if (timestamp != null) {
      indexOperationBuilder.versionType(VersionType.ExternalGte).version(timestamp);
    }

    if (parent != null) {
      // indexRequest.parent(parent);
      indexOperationBuilder.routing(parent);
    }
    IndexOperation indexOperation = indexOperationBuilder.build();
    LOGGER.info("+++ the request object: " + indexOperation.index() /*+ "   " + indexRequest.type() */ + "   " + indexOperation.id());
    return indexOperation;
  }


  /**
   * Return the index Request
   *
   * @param id
   * @param json
   * @param parent
   * @return
   */
  /*
  private IndexRequest getIndexRequest(String id, String json, String parent, String type) {
    final IndexRequest.Builder indexRequestBuilder = new IndexRequest.Builder();
    indexRequestBuilder.index(indexName).id(id).withJson(new StringReader(json));
    if (parent != null) {
      // indexRequest.parent(parent);
      indexRequestBuilder.routing(parent);
    }
  
    IndexRequest indexRequest = indexRequestBuilder.build();
    LOGGER.info("+++ the request object: " + indexRequest.index() + "   " + indexRequest.id());
    return indexRequest;
  }
  */


  /**
   * Return the Delete Request
   *
   * @param id
   * @param json
   * @param parent
   * @return
   */
  private DeleteRequest getDeleteRequest(String id, String parent) {
    final DeleteRequest.Builder deleteRequestBuilder = new DeleteRequest.Builder();
    deleteRequestBuilder.index(indexName).id(id);
    if (parent != null) {
      deleteRequestBuilder.routing(parent);
    }
    return deleteRequestBuilder.build();
  }

  private DeleteOperation getDeleteOperation(String id, String parent) {
    final DeleteOperation.Builder deleteRequestBuilder = new DeleteOperation.Builder();
    deleteRequestBuilder.index(indexName).id(id);
    if (parent != null) {
      deleteRequestBuilder.routing(parent);
    }
    return deleteRequestBuilder.build();
  }

  /**
   * Transform an object to a json
   *
   * @param obj
   * @return
   * @throws UnprocessableError
   */
  public static byte[] toJson(Object obj, String dataType, String index) throws UnprocessableError {
    try {
      // mapper.configure(DeserializationFeature..UNWRAP_ROOT_VALUE, true);
      /*
      if (obj instanceof ContentVO) {
      mapper.configure(SerializationFeature.WRAP_ROOT_VALUE, true);
      }
      */
      return mapper.setSerializationInclusion(Include.NON_NULL).writeValueAsBytes(toESEntity(obj, dataType, index));
    } catch (final JsonProcessingException e) {
      throw new UnprocessableError("Error serializing object to json", e);
    }
  }



  /**
   * Make all changes done searchable. Kind of a commit. Might be important if data needs to be
   * immediately available for other tasks
   */

  public void commit() {
    // Check if refresh is needed: cost is very high

    RefreshRequest.Builder rrb = new RefreshRequest.Builder();
    rrb.index(indexName);
    try {
      ElasticService.getClient().indices().refresh(rrb.build());
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
    for (BulkResponseItem bulkItemResponse : bulkResponse.items()) {

      if (bulkItemResponse.error() != null) {


        ErrorCause errorCause = bulkItemResponse.error();
        String idOfDocument = bulkItemResponse.id();
        int status = bulkItemResponse.status();
        ErrorCause rootProblem = bulkItemResponse.error();

        // Elastic Search has already indicated that it sees the operation as failed
        // additionally we do our own check (i.e. a delete operation that could not find the document is not a problem)
        if ((bulkItemResponse.operationType() == OperationType.Delete && status != HttpStatus.SC_NOT_FOUND)
            || bulkItemResponse.operationType() == OperationType.Index || bulkItemResponse.operationType() == OperationType.Create
            || bulkItemResponse.operationType() == OperationType.Update)

          failureException.addFailure(idOfDocument, bulkItemResponse.operationType(), bulkItemResponse.status(), bulkItemResponse.error());
      }
    }
    return failureException;
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

      PutMappingRequest pmr = PutMappingRequest.of(pm -> pm.index(this.indexName).withJson(new StringReader(jsonMapping)));

      PutMappingResponse response = ElasticService.getClient().indices().putMapping(pmr);

      /*
      PutMappingRequest request = new PutMappingRequest(this.indexName);
      request.type(dataType);
      request.source(jsonMapping, XContentType.JSON);
      AcknowledgedResponse putMappingResponse = ElasticService.getClient().indices().putMapping(request, RequestOptions.DEFAULT);
      
       */
    } catch (final Exception e) {
      LOGGER.error("Error initializing the Elastic Search Mapping " + mappingFile, e);
    }
  }

}
