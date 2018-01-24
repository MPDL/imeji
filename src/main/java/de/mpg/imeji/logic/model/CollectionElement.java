package de.mpg.imeji.logic.model;

import de.mpg.imeji.logic.util.ObjectHelper.ObjectType;

/**
 * 
 * Interface for all object which belongs to a collection
 * 
 * @author saquet
 *
 */
public interface CollectionElement {

  /**
   * Name of the object
   * 
   * @return
   */
  public String getName();

  /**
   * The id of the object
   * 
   * @return
   */
  public String getUri();

  /**
   * The type of the object
   * 
   * @return
   */
  public ObjectType getType();
}

