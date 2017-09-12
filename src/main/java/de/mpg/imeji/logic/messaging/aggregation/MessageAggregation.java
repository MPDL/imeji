package de.mpg.imeji.logic.messaging.aggregation;

/**
 * Interface for Message aggregation.
 * 
 * @author saquet
 *
 */
public interface MessageAggregation {

  /**
   * The aggregation is call
   */
  public void aggregate();

  /**
   * Method called when all aggregation are done. Can be used to delete messages which are not
   * relevant anymore
   */
  public void postProcessing();

}
