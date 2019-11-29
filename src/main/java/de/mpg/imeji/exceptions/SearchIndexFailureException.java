package de.mpg.imeji.exceptions;

import java.net.URI;
import java.util.Calendar;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.action.DocWriteResponse;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.update.UpdateResponse;

import de.mpg.imeji.logic.db.indexretry.model.RetryBaseRequest;
import de.mpg.imeji.logic.db.indexretry.model.RetryDeleteFromIndexRequest;
import de.mpg.imeji.logic.db.indexretry.model.RetryIndexRequest;
import de.mpg.imeji.logic.db.writer.WriterFacade;
import de.mpg.imeji.logic.model.aspects.ResourceLastModified;


/**
 * 
 * @author breddin
 *
 */
public class SearchIndexFailureException extends ImejiException {

  /**
   * 
   */
  private static final long serialVersionUID = 3714623912575707648L;

  /**
   * In case a failure is reported back from Elastic Search when trying to index, update or delete a
   * document, save the response to the exception
   */
  private DocWriteResponse responseOfFailedOperation;


  private static final Logger LOGGER = LogManager.getLogger(SearchIndexFailureException.class);


  public SearchIndexFailureException(DocWriteResponse response) {

    super("An indexing/updating/deleting operation failed in elastic search.");
    this.responseOfFailedOperation = response;
  }


  public DocWriteResponse getFailedResponse() {
    return this.responseOfFailedOperation;
  }


  public RetryBaseRequest getRetryRequest(Object objectToIndexOrDelete) {

    return RetryBaseRequest.getRetryBaseRequest(objectToIndexOrDelete, this.responseOfFailedOperation);

  }

}
