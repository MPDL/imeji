package de.mpg.imeji.logic.model.aspects;

/**
 * To be implemented by model classes. Given a data model object, create a new empty data model
 * object of the same class and initialize it with the URI of the original data model object.
 * 
 * @author breddin
 *
 */
public interface CloneURI {

  public Object cloneURI();
}
