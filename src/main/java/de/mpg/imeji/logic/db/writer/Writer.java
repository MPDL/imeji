package de.mpg.imeji.logic.db.writer;

import java.util.List;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.model.User;

/**
 * Write imeji objects in the persistence layer. Important: {@link Writer} doens't check
 * Authorization. Please use {@link WriterFacade} instead.
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public interface Writer {
  /**
   * Create a list of objects
   *
   * @param objects
   * @param user
   * @throws ImejiException
   */
  public void create(List<Object> objects, User user) throws ImejiException;

  /**
   * Delete a list of objects
   *
   * @param objects
   * @param user
   * @throws ImejiException
   */
  public void delete(List<Object> objects, User user) throws ImejiException;

  /**
   * Update a list of objects
   *
   * @param objects
   * @param user
   * @throws ImejiException
   */
  public void update(List<Object> objects, User user) throws ImejiException;

  /**
   * Lazy Update a list of objects (don't update lazy list)
   *
   * @param objects
   * @param user
   * @throws ImejiException
   */
  public void updateLazy(List<Object> objects, User user) throws ImejiException;
}
