package de.mpg.imeji.logic.db.indexretry;


import java.util.List;

import de.mpg.imeji.logic.db.indexretry.model.RetryBaseRequest;


/**
 * Interface can be implemented by tasks that write to search index. In case writing to index fails
 * the objects-to-be-written are transformed into index requests and stored in a queue (RetryQueue).
 * Writing objects to index is then attempted again later on.
 * 
 * @author breddin
 *
 */
public interface RetryIndex {

  /**
   * Returns a list of index requests
   * 
   * @return
   */
  public List<RetryBaseRequest> getRetryRequests();


}
