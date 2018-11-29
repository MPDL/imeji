package de.mpg.imeji.logic.db.reader;

import java.util.List;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.model.User;

/**
 * Object reader interface for imeji. Important: {@link Reader} doens't check Authorization. Please
 * use {@link ReaderFacade} instead.
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public interface Reader {
  /**
   * Read a single object by its uri
   *
   * @param uri
   * @param user
   * @param o
   * @return
   * @throws Exception
   */
  public Object read(String uri, User user, Object o) throws ImejiException;

  /**
   * Read a single Object by its uri (Lazy means don't load Lazy list within the object)
   *
   * @param uri
   * @param user
   * @param o
   * @return
   * @throws Exception
   */
  public Object readLazy(String uri, User user, Object o) throws ImejiException;

  /**
   * Read a List of objects. All objects must have an uri
   *
   * @param objects
   * @param user
   * @return
   * @throws Exception
   */
  public List<Object> read(List<Object> objects, User user) throws ImejiException;

  /**
   * Read a List of objects. All objects must have an uri. (Lazy means don't load Lazy list within
   * the object)
   *
   * @param objects
   * @param user
   * @return
   * @throws Exception
   */
  public List<Object> readLazy(List<Object> objects, User user) throws ImejiException;
}
