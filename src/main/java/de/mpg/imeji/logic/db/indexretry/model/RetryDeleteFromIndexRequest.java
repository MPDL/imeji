package de.mpg.imeji.logic.db.indexretry.model;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.mpg.imeji.logic.db.writer.WriterFacade;

/**
 * In case deleting an object from index has failed: Create a RetryDeleteFromIndexRequest object to
 * try again later on. Class stores information of objects that could not be deleted from index.
 * 
 * @author breddin
 *
 */
public class RetryDeleteFromIndexRequest extends RetryBaseRequest {

  /**
   * The object that shall be deleted
   */
  private Object objectToDelete;

  private static final Logger LOGGER = LogManager.getLogger(RetryDeleteFromIndexRequest.class);

  /**
   * Constructor for a request to delete an object from index
   * 
   * @param objectUri
   * @param objectToDelete
   * @param failTime
   * @param action
   */
  public RetryDeleteFromIndexRequest(URI objectUri, Object objectToDelete) {
    super(objectUri, objectToDelete);
    this.objectToDelete = objectToDelete;
  }


  public Object getObjectToDelete() {
    return this.objectToDelete;
  }


  /**
   * Create a list of retry delete requests from a list of objects.
   * 
   * @param objects
   * @return
   */
  public static List<RetryBaseRequest> getRetryDeleteFromIndexRequests(List<Object> objects) {

    List<RetryBaseRequest> requestList = new ArrayList<RetryBaseRequest>(objects.size());
    for (Object object : objects) {
      RetryBaseRequest request = getRetryDeleteFromIndexRequest(object);
      if (request != null) {
        requestList.add(request);
      }
    }
    return requestList;

  }

  /**
   * Create a retry delete request from an object.
   * 
   * @param object
   * @return
   */
  public static RetryBaseRequest getRetryDeleteFromIndexRequest(Object object) {

    URI uri = WriterFacade.extractID(object);
    if (uri != null) {
      RetryDeleteFromIndexRequest request = new RetryDeleteFromIndexRequest(uri, object);
      return request;
    } else {
      LOGGER.error(
          "Could not extract id from object that was to be deleted from search index. Could not construct RetryDeleteFromIndexRequest");
      return null;
    }
  }


}
