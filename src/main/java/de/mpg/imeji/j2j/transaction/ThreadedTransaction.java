package de.mpg.imeji.j2j.transaction;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.log4j.Logger;

import com.hp.hpl.jena.Jena;
import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.tdb.TDBFactory;

import de.mpg.imeji.exceptions.ImejiException;

/**
 * Run a {@link Transaction} in a new {@link Thread}. A new {@link Dataset} is created for this
 * thread <br/>
 * Necessary to follow the {@link Jena} per {@link Thread} Readers–writer lock
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public class ThreadedTransaction implements Callable<Integer> {
  /**
   * The {@link ExecutorService} which runs the thread in imeji
   */
  public static final ExecutorService EXECUTOR = Executors.newCachedThreadPool();
  private Transaction transaction;
  private String tdbPath;
  protected static Logger LOGGER = Logger.getLogger(ThreadedTransaction.class);

  /**
   * Construct a new {@link ThreadedTransaction} for one {@link Transaction}
   *
   * @param transaction
   */
  public ThreadedTransaction(Transaction transaction, String tdbPath) {
    this.transaction = transaction;
    this.tdbPath = tdbPath;
  }

  /*
   * (non-Javadoc)
   *
   * @see java.util.concurrent.Callable#call()
   */
  @Override
  public Integer call() throws Exception {
    Dataset ds = TDBFactory.createDataset(tdbPath);
    try {
      transaction.start(ds);
    } finally {
      ds.close();
    }
    return 1;
  }

  /**
   * If the run Method caught an Exception, throw this exception
   *
   * @throws Exception
   */
  public void throwException() throws ImejiException {
    transaction.throwException();
  }

  /**
   * Run a {@link ThreadedTransaction} with the {@link ExecutorService} of imeji
   *
   * @param t
   * @throws Exception
   */
  public static void run(ThreadedTransaction t) throws ImejiException {
    Future<Integer> f = EXECUTOR.submit(t);
    // wait for the transaction to be finished
    try {
      f.get();
    } catch (Exception e) {
      LOGGER.info("An exception happened in Transaction ", e);
    }
    t.throwException();
  }
}
