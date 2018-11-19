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
	 * Index an object.
	 *
	 * @param obj
	 */
	public void index(Object obj);

	public void index(String name, Object obj);
	/**
	 * Index a list of Object. This method might be faster for multiple objects,
	 * than using the index method for single objects. <br/>
	 * The spaceId will be the same for all objects. <br/>
	 * The spaceId can be null.
	 *
	 * @param l
	 */
	public void indexBatch(List<?> l);

	public void indexBatch(String name, List<?> l);

	/**
	 * Update a list of Object. This method might be faster for multiple objects,
	 * than using the index method for single objects. <br/>
	 * The spaceId will be the same for all objects. <br/>
	 * The spaceId can be null.
	 *
	 * @param l
	 */
	public void updateIndexBatch(List<?> l);

	public void updateIndexBatch(String name, List<?> l);

	/**
	 * Delete an object from the Index
	 *
	 * @param obj
	 */
	public void delete(Object obj);

	public void delete(String name, Object obj);

	/**
	 * Delete many objects from the index
	 *
	 * @param l
	 */
	public void deleteBatch(List<?> l);

	public void deleteBatch(String name, List<?> l);

	/**
	 * Do a partial update of the object
	 *
	 * @param obj
	 */
	public void updatePartial(String id, Object obj);

	public void updatePartial(String name, String id, Object obj);

}
