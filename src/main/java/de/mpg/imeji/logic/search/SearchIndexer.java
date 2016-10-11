package de.mpg.imeji.logic.search;

import java.util.List;

/**
 * Index data for the {@link Search}
 *
 * @author bastiens
 *
 */
public interface SearchIndexer {

  /**
   * Index an object. SpaceId can be null.
   *
   * @param obj
   * @param spaceId
   */
  public void index(Object obj);

  /**
   * Index a list of Object. This method might be faster for multiple objects, than using the index
   * method for single objects. <br/>
   * The spaceId will be the same for all objects. <br/>
   * The spaceId can be null.
   *
   * @param l
   */
  public void indexBatch(List<?> l);

  /**
   * Delete an object from the Index
   *
   * @param obj
   */
  public void delete(Object obj);

  /**
   * Delete many objects from the index
   *
   * @param l
   */
  public void deleteBatch(List<?> l);

  /**
   * Do a partial update of the object
   * 
   * @param obj
   */
  public void updatePartial(String id, Object obj);
}
