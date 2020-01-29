package de.mpg.imeji.logic.model.aspects;

/**
 * Interface can be implemented by model classes. Creates a new object of the same type as the given
 * object. Copies the URI of the given object to the new object. Useful when reading data from Jena.
 * 
 * @author breddin
 *
 */
public interface CloneURI {

  /**
   * "Clone" the given object: Return a new object of the same type as the given object with all
   * fields empty/default except the URI field. URI of cloned object is set to URI of the given
   * object.
   * 
   * @return clone
   */
  public Object cloneURI();
}
