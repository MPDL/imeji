package de.mpg.imeji.rest.api;

import java.util.List;

import javax.ws.rs.NotSupportedException;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.model.User;
import de.mpg.imeji.rest.to.SearchResultTO;

/**
 * The generic Interface defining all methods (i.e verbs) available for the imeji objects (Item,
 * Collection, etc.). <br/>
 * <br/>
 * If a method doesn't make sense for one object type, then should return a
 * {@link NotSupportedException} <br/>
 * <br/>
 * Authorization is always done via the {@link User} which is passed in every method
 *
 * @author saquet
 *
 * @param <V>
 */
public interface APIService<V> {

  /**
   * Create an Object in imeji
   *
   * @param o
   * @param u
   * @return
   */
  public V create(V o, User u) throws ImejiException;

  /**
   * Read an object from imneji with its id
   *
   * @param id
   * @param u
   * @return
   */
  public V read(String id, User u) throws ImejiException;

  /**
   * Update an object in imeji
   *
   * @param o
   * @param u
   * @return
   */
  public V update(V o, User u) throws ImejiException;

  /**
   * Delete an object in imeji
   *
   * @param o
   * @param u
   * @return
   */
  public boolean delete(String i, User u) throws ImejiException;

  /**
   * Release an object
   *
   * @param i
   * @param u
   */
  public V release(String i, User u) throws ImejiException;

  /**
   * Withdraw an object
   *
   * @param i
   * @param u
   */
  public V withdraw(String i, User u, String discardComment) throws ImejiException;

  /**
   * Share an object defined by id to a user defined by userid. The roles to added are passed into a
   * List of String
   *
   * @param id
   * @param userId
   * @param roles
   * @param u
   */
  public void share(String id, String userId, List<String> roles, User u) throws ImejiException;

  /**
   * Unshare an object defined by id from a user defined by userid. The roles to be removed are
   * passed into a List of String
   *
   * @param id
   * @param userId
   * @param roles
   * @param u
   */
  public void unshare(String id, String userId, List<String> roles, User u) throws ImejiException;

  /**
   * Search for an object according to a query
   *
   * @param q
   * @param u
   * @return
   */
  public SearchResultTO<V> search(String q, int offset, int size, User u) throws Exception;

}
