package de.mpg.imeji.logic.messaging.aggregation;

/**
 * An Aggregation process messages from the message queue. Aggregation are called via the
 * Aggregation service
 * 
 * @author saquet
 *
 */
public interface Aggregation {

  /**
   * The aggregation is call
   */
  public void aggregate();

}
