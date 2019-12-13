package de.mpg.imeji.logic.db.indexretry.model;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.mpg.imeji.logic.db.writer.WriterFacade;



/**
 * Request to retry to index an object. Stored information about objects that could not be written
 * into search index due to failure.
 * 
 * @author breddin
 *
 */
public class RetryIndexRequest extends RetryBaseRequest {

  /**
   * 
   */
  private static final long serialVersionUID = 1451561364128569344L;

  /**
   * The class of the resource that shall be copied to the index
   */
  private Class objectClass;

  private static final Logger LOGGER = LogManager.getLogger(RetryIndexRequest.class);


  /**
   * Constructor for a request to retry to index an object
   * 
   * @param objectUri
   * @param objectClass
   * @param failTime
   * @param action
   */
  public RetryIndexRequest(URI objectUri, Object object) {

    super(objectUri, object);
    this.objectClass = object.getClass();

  }


  public Class getObjectClass() {
    return this.objectClass;
  }


  /**
   * Create a list of retry index requests from a list of objects.
   * 
   * @param objects
   * @param action
   * @return
   */
  public static List<RetryBaseRequest> getRetryIndexRequests(List<Object> objects) {

    List<RetryBaseRequest> requestList = new ArrayList<RetryBaseRequest>(objects.size());
    for (Object object : objects) {
      RetryBaseRequest request = getRetryIndexRequest(object);
      if (request != null) {
        requestList.add(request);
      }
    }
    return requestList;
  }

  /**
   * Create a retry index request from an object
   * 
   * @param object
   * @return
   */
  public static RetryBaseRequest getRetryIndexRequest(Object object) {

    URI uri = WriterFacade.extractID(object);
    if (uri != null) {
      RetryIndexRequest request = new RetryIndexRequest(uri, object);
      return request;
    } else {
      LOGGER.error("Could not extract id from object that was to be indexed in search index. Could not construct RetryIndexRequest");
      return null;
    }
  }

}
