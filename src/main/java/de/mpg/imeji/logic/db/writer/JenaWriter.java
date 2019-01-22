package de.mpg.imeji.logic.db.writer;

import java.net.URI;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.jena.Jena;
import org.apache.jena.rdf.model.Model;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.j2j.transaction.CRUDTransaction;
import de.mpg.imeji.j2j.transaction.CRUDTransaction.CRUDTransactionType;
import de.mpg.imeji.j2j.transaction.ThreadedTransaction;
import de.mpg.imeji.j2j.transaction.Transaction;
import de.mpg.imeji.logic.config.Imeji;
import de.mpg.imeji.logic.db.reader.JenaReader;
import de.mpg.imeji.logic.model.User;
import de.mpg.imeji.logic.search.jenasearch.ImejiSPARQL;
import de.mpg.imeji.logic.search.jenasearch.JenaCustomQueries;

/**
 * imeji WRITE operations (create/delete/update) in {@link Jena} <br/>
 * - Use {@link Transaction} <br/>
 * - For concurrency purpose, each write {@link Transaction} is made within a single {@link Thread}.
 * Use {@link ThreadedTransaction} <br/>
 * - for READ operations, uses {@link JenaReader}
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public class JenaWriter implements Writer {
  private final String modelURI;
  private static final ExecutorService WRITE_EXECUTOR = Executors.newSingleThreadExecutor();

  /**
   * Construct one {@link JenaWriter} for one {@link Model}
   *
   * @param modelURI
   */
  public JenaWriter(String modelURI) {
    this.modelURI = modelURI;
  }

  /**
   * Create a {@link List} of {@link Object} in {@link Jena}
   *
   * @param objects
   * @param user
   * @throws Exception
   */
  @Override
  public void create(List<Object> objects, User user) throws ImejiException {
    runTransaction(objects, CRUDTransactionType.CREATE, false);
  }

  /**
   * Delete a {@link List} of {@link Object} in {@link Jena}
   *
   * @param objects
   * @param user
   * @throws Exception
   */
  @Override
  public void delete(List<Object> objects, User user) throws ImejiException {
    runTransaction(objects, CRUDTransactionType.DELETE, false);
    for (final Object o : objects) {
      final URI uri = WriterFacade.extractID(o);
      if (uri != null) {
        ImejiSPARQL.execUpdate(JenaCustomQueries.updateRemoveGrantsFor(uri.toString()));
      }
    }
  }

  /**
   * Update a {@link List} of {@link Object} in {@link Jena}
   *
   * @param objects
   * @param user
   * @throws Exception
   */
  @Override
  public void update(List<Object> objects, User user) throws ImejiException {
    runTransaction(objects, CRUDTransactionType.UPDATE, false);
  }

  /**
   * Update LAZY a {@link List} of {@link Object} in {@link Jena}<br/>
   * - {@link List} contained within the {@link Object} are not updated: faster performance,
   * especially for objects with huge {@link List}
   *
   * @param objects
   * @param user
   * @throws Exception
   */
  @Override
  public void updateLazy(List<Object> objects, User user) throws ImejiException {
    runTransaction(objects, CRUDTransactionType.UPDATE, true);
  }

  /**
   * Run one WRITE operation in {@link Transaction} within a {@link ThreadedTransaction}
   *
   * @param objects
   * @param type
   * @param lazy
   * @throws Exception
   */
  private void runTransaction(List<Object> objects, CRUDTransactionType type, boolean lazy) throws ImejiException {
    final Transaction t = new CRUDTransaction(objects, type, modelURI, lazy);
    // Write Transaction needs to be added in a new Thread
    ThreadedTransaction.run(new ThreadedTransaction(t, Imeji.tdbPath), WRITE_EXECUTOR);
  }
}
