package de.mpg.imeji.logic.db.indexretry.model;

import co.elastic.clients.elasticsearch.core.bulk.OperationType;
import de.mpg.imeji.logic.model.aspects.ResourceLastModified;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.Serializable;
import java.net.URI;
import java.util.Calendar;

/**
 * Base class of retry requests. Stores basic information about objects that could not be written to
 * or deleted from search index, i.e. the object's URI and the time of failure.
 * 
 * 
 * @author breddin
 *
 */

public abstract class RetryBaseRequest implements Serializable {

  /**
   * 
   */
  private static final long serialVersionUID = 3904763389591479583L;

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

  public void setObjectUri(URI objectUri) {
    this.objectUri = objectUri;
  }


  public Calendar getFailTime() {
    return this.failTime;
  }

  public void setFailTime(Calendar calendar) {
    this.failTime = calendar;
  }

  /**
   * Set/save the time when writing an object to search index failed.
   * 
   * @param object
   */
  public void setFailTime(Object object) {

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
  public static RetryBaseRequest getRetryBaseRequest(Object object, OperationType elasticSearchResponse) {

    if (elasticSearchResponse.equals(OperationType.Index) || elasticSearchResponse.equals(OperationType.Update)) {
      RetryBaseRequest retryIndexRequest = RetryIndexRequest.getRetryIndexRequest(object);
      return retryIndexRequest;
    } else if (elasticSearchResponse.equals(OperationType.Delete)) {
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
