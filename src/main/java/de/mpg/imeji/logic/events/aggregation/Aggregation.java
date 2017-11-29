package de.mpg.imeji.logic.events.aggregation;

/**
 * An Aggregation process messages from the message queue. Aggregation are called via the
 * Aggregation service
 * 
 * @author saquet
 *
 */
public interface Aggregation {

  public enum Period {
    NIGHTLY;
  }

  public Period getPeriod();

  /**
   * The aggregation is call
   */
  public void aggregate();

}
