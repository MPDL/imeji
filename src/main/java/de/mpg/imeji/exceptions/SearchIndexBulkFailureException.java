package de.mpg.imeji.exceptions;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.action.DocWriteResponse;
import org.elasticsearch.rest.RestStatus;

import de.mpg.imeji.logic.db.indexretry.model.RetryBaseRequest;
import de.mpg.imeji.logic.db.writer.WriterFacade;

/**
 * Exception is thrown when one or more requests (delete/index) of a bulk request to elastic search
 * have failed. Holds information about operation types, failure causes and document ids.
 * 
 * @author breddin
 *
 */
public class SearchIndexBulkFailureException extends ImejiException {

  /**
   * 
   */
  private static final long serialVersionUID = -7591848317278525642L;

  private HashMap<String, OperationFailureInformation> failures;

  private static final Logger LOGGER = LogManager.getLogger(SearchIndexBulkFailureException.class);

  /**
   * 
   */
  public SearchIndexBulkFailureException() {

    this.failures = new HashMap<String, OperationFailureInformation>();
  }



  public void addFailure(String idOfDocument, DocWriteResponse operationType, RestStatus status, Exception operationException) {

    OperationFailureInformation failure = new OperationFailureInformation(operationType, status, operationException);
    this.failures.put(idOfDocument, failure);
  }



  public List<RetryBaseRequest> getRetryRequests(List<Object> objectsToIndexOrDelete) {

    List<RetryBaseRequest> retryRequests = new ArrayList<RetryBaseRequest>();

    for (Object objectToIndexOrDelete : objectsToIndexOrDelete) {
      URI uri = WriterFacade.extractID(objectToIndexOrDelete);

      if (uri != null && failures.containsKey(uri.toString())) {

        OperationFailureInformation failure = this.failures.get(uri.toString());

        if (failure.operationException != null) {
          LOGGER.error("Request to ElasticSearch failed for document " + uri.toString() + " cause: Rest Code " + failure.status
              + " exception: " + failure.operationException);
        } else {
          LOGGER.error("Request to ElasticSearch failed for document " + uri.toString() + " cause: Rest Code " + failure.status);
        }

        RetryBaseRequest retryRequest = RetryBaseRequest.getRetryBaseRequest(objectToIndexOrDelete, failure.operationType);
        if (retryRequest != null) {
          retryRequests.add(retryRequest);
        }
      } else if (uri == null) {
        LOGGER.error(
            "Could not extract id from object that was to be indexed/deleted from search index. Can not determine if operation succeeded. ");
      }
    }
    return retryRequests;
  }


  /**
   * 
   * @author breddin
   *
   */
  private class OperationFailureInformation {

    DocWriteResponse operationType;
    RestStatus status;
    Exception operationException;


    public OperationFailureInformation(DocWriteResponse operationType, RestStatus status, Exception operationException) {

      this.operationType = operationType;
      this.status = status;
      this.operationException = operationException;
    }

  }



}
