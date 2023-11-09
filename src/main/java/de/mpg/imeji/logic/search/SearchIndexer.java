package de.mpg.imeji.logic.search;

import java.net.URI;
import java.util.List;

/**
 * Index data for the {@link Search}
 *
 * @author bastiens
 *
 */
public interface SearchIndexer {



  /**
   * Index a list of Object. This method might be faster for multiple objects, than using the index
   * method for single objects. <br/>
   * The spaceId will be the same for all objects. <br/>
   * The spaceId can be null.
   *
   * @param l
   */
  public void indexBatch(List<?> l) throws Exception;

  /**
   * Update a list of Object. This method might be faster for multiple objects, than using the index
   * method for single objects. <br/>
   * The spaceId will be the same for all objects. <br/>
   * The spaceId can be null.
   *
   * @param l
   */
  public void updateIndexBatch(List<?> l) throws Exception;


  /**
   * Delete many objects from the index
   *
   * @param l
   */
  public void deleteBatch(List<?> l) throws Exception;


}
