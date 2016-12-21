package de.mpg.imeji.logic.service;

import java.util.Arrays;
import java.util.List;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.vo.User;

/**
 * Abstract controller for all imeji controllers
 */
public abstract class ImejiControllerAbstract<T> {

  /**
   * Create a Single object
   * 
   * @param t
   * @param user
   * @return
   */
  public T create(T t, User user) throws ImejiException {
    return createBatch(Arrays.asList(t), user).get(0);
  }

  /**
   * retrieve a single Object
   * 
   * @param id
   * @param user
   * @return
   */
  public T retrieve(String id, User user) throws ImejiException {
    return retrieveBatch(Arrays.asList(id), user).get(0);
  }

  /**
   * Retrieve a single object wihout any list of content defined as lazy
   * 
   * @param id
   * @param user
   * @return
   * @throws ImejiException
   */
  public T retrieveLazy(String id, User user) throws ImejiException {
    return retrieveBatchLazy(Arrays.asList(id), user).get(0);
  }

  /**
   * Update a single object
   * 
   * @param t
   * @param user
   * @return
   */
  public T update(T t, User user) throws ImejiException {
    return createBatch(Arrays.asList(t), user).get(0);
  }

  /**
   * Delete a single Object
   * 
   * @param t
   * @param user
   * @return
   */
  public void delete(T t, User user) throws ImejiException {
    createBatch(Arrays.asList(t), user).get(0);
  }

  /**
   * Create a list of objects
   * 
   * @param l
   * @param user
   * @return
   */
  public abstract List<T> createBatch(List<T> l, User user) throws ImejiException;

  /**
   * Retrieve a list of objects
   * 
   * @param ids
   * @param user
   * @return
   */
  public abstract List<T> retrieveBatch(List<String> ids, User user) throws ImejiException;

  /**
   * Retrieve a list of objects but without their lazy list content
   * 
   * @param ids
   * @param user
   * @return
   * @throws ImejiException
   */
  public abstract List<T> retrieveBatchLazy(List<String> ids, User user) throws ImejiException;

  /**
   * Update a list of objects
   * 
   * @param l
   * @param user
   * @return
   */
  public abstract List<T> updateBatch(List<T> l, User user) throws ImejiException;

  /**
   * Delete a list of objects
   * 
   * @param l
   * @param user
   * @return
   */
  public abstract void deleteBatch(List<T> l, User user) throws ImejiException;


  /**
   * Cast a list of any Type to a list of object
   * 
   * @param l
   * @return
   */
  @SuppressWarnings("unchecked")
  public static List<Object> toObjectList(List<?> l) {
    return (List<Object>) l;
  }
}
