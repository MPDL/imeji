/**
 * License: src/main/resources/license/escidoc.license
 */
package de.mpg.imeji.logic.reader;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import com.hp.hpl.jena.Jena;
import com.hp.hpl.jena.rdf.model.Model;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.j2j.helper.J2JHelper;
import de.mpg.imeji.j2j.transaction.CRUDTransaction;
import de.mpg.imeji.j2j.transaction.Transaction;
import de.mpg.imeji.j2j.transaction.CRUDTransaction.CRUDTransactionType;
import de.mpg.imeji.logic.Imeji;
import de.mpg.imeji.logic.vo.User;
import de.mpg.imeji.logic.writer.JenaWriter;

/**
 * imeji READ operations in {@link Jena} <br/>
 * - Use {@link CRUDTransaction} to load objects <br/>
 * - Implements lazy loading ({@link List} contained in objects are then no loaded), for faster load
 * <br/>
 * - For WRITE operations, uses {@link JenaWriter}
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public class JenaReader implements Reader {
  private String modelURI;
  private boolean lazy = false;

  /**
   * imeji object loader for one {@link Model}
   *
   * @param modelURI
   */
  public JenaReader(String modelURI) {
    this.modelURI = modelURI;
  }

  /**
   * Load lazy one {@link Object} according to its uri <br/>
   * Faster than load method, but contained {@link List} are skipped for loading
   *
   * @param uri
   * @param user
   * @param o
   * @return
   * @throws Exception
   */
  @Override
  public Object readLazy(String uri, User user, Object o) throws ImejiException {
    return read(uri, user, o, true);
  }

  /**
   * Load a object from {@link Jena} within one {@link CRUDTransaction}
   *
   * @param uri
   * @param user
   * @param o
   * @return
   * @throws Exception
   */
  @Override
  public Object read(String uri, User user, Object o) throws ImejiException {
    return read(uri, user, o, false);
  }

  /**
   * Load a list of objects within one {@link CRUDTransaction}
   *
   * @param objects
   * @param user
   * @return
   * @throws Exception
   */
  @Override
  public List<Object> read(List<Object> objects, User user) throws ImejiException {
    return read(objects, user, false);
  }

  /**
   * Load a {@link List} of {@link Object} within one {@link CRUDTransaction} <br/>
   * Faster than load method, but contained {@link List} are skipped for loading
   *
   * @param objects
   * @param user
   * @return
   * @throws Exception
   */
  @Override
  public List<Object> readLazy(List<Object> objects, User user) throws ImejiException {
    return read(objects, user, true);
  }

  private Object read(String uri, User user, Object o, boolean lazy) throws ImejiException {
    J2JHelper.setId(o, URI.create(uri));
    List<Object> objects = new ArrayList<Object>();
    objects.add(o);
    List<Object> l = read(objects, user, lazy);
    if (l.size() > 0) {
      return l.get(0);
    }
    return null;
  }

  private List<Object> read(List<Object> objects, User user, boolean lazy) throws ImejiException {
    Transaction t = new CRUDTransaction(objects, CRUDTransactionType.READ, modelURI, lazy);
    t.start(Imeji.dataset);
    t.throwException();
    return objects;
  }
}
