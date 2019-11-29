package de.mpg.imeji.logic.db.indexretry.model;

import java.net.URI;
import java.util.Calendar;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.action.DocWriteResponse;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.update.UpdateResponse;

import de.mpg.imeji.logic.model.aspects.ResourceLastModified;

/**
 * Base class of retry requests. Stores basic information about objects that could not be written to
 * or deleted from search index, i.e. the object's URI and the time of failure.
 * 
 * 
 * @author breddin
 *
 */
public abstract class RetryBaseRequest {


  /**
   * The uri of the resource that shall be copied to the index
   */
  private URI objectUri;

  /**
   * Time when indexing the object did not work
   */
  private Calendar failTime;

  private static final Logger LOGGER = LogManager.getLogger(RetryBaseRequest.class);


  /**
   * Constructor for RetryBaseRequest
   * 
   * @param objectUri
   * @param failTime
   */
  public RetryBaseRequest(URI objectUri, Object object) {
    this.objectUri = objectUri;
    setFailTime(object);
  }


  public URI getUri() {
    return this.objectUri;
  }



  public Calendar getFailTime() {
    return this.failTime;
  }

  /**
   * Set/save the time when writing an object to search index failed.
   * 
   * @param object
   */
  private void setFailTime(Object object) {

    this.failTime = Calendar.getInstance();
    if (object instanceof ResourceLastModified) {
      this.failTime = ((ResourceLastModified) object).getModified();
    } else {
      this.failTime = Calendar.getInstance();
    }

  }

  /**
   * Given an object to index and a response from ElasticSearch that indicates a failure, constructs
   * a retry request.
   * 
   * @param object
   * @param elasticSearchResponse
   * @return
   */
  public static RetryBaseRequest getRetryBaseRequest(Object object, DocWriteResponse elasticSearchResponse) {

    if (elasticSearchResponse instanceof IndexResponse || elasticSearchResponse instanceof UpdateResponse) {
      RetryBaseRequest retryIndexRequest = RetryIndexRequest.getRetryIndexRequest(object);
      return retryIndexRequest;
    } else if (elasticSearchResponse instanceof DeleteResponse) {
      RetryBaseRequest retryDeleteRequest = RetryDeleteFromIndexRequest.getRetryDeleteFromIndexRequest(object);
      return retryDeleteRequest;
    }

    else {
      LOGGER.error("Unknown operation response type from Elastic Search. Expected  IndexResponse, UpdateResponse or DeleteResponse, got "
          + elasticSearchResponse);
    }

    return null;
  }


}
