package de.mpg.imeji.j2j.transaction;

import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.logging.log4j.Logger;

import de.mpg.imeji.exceptions.ImejiException;

import org.apache.logging.log4j.LogManager;

import org.apache.jena.Jena;
import org.apache.jena.query.Dataset;
import org.apache.jena.tdb.TDBFactory;


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
  private static ExecutorService EXECUTOR = Executors.newCachedThreadPool();
  private final Transaction myTransaction;
  private final String tdbPath;
  protected static Logger LOGGER = LogManager.getLogger(ThreadedTransaction.class);

  /**
   * Construct a new {@link ThreadedTransaction} for one {@link Transaction}
   *
   * @param transaction
   */
  public ThreadedTransaction(Transaction transaction, String tdbPath) {
    this.myTransaction = transaction;
    this.tdbPath = tdbPath;
  }

  /*
   * (non-Javadoc)
   *
   * @see java.util.concurrent.Callable#call()
   */
  @Override
  public Integer call() throws Exception {
    final Dataset ds = TDBFactory.createDataset(tdbPath);
    try {
      myTransaction.start(ds);
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
  public void throwImejiException() throws ImejiException {
    myTransaction.rethrowException();
  }

  /**
   * Run a {@link ThreadedTransaction} with the {@link ExecutorService} of imeji
   *
   * @param t
   * @throws Exception
   */
  public static void run(ThreadedTransaction t) throws Exception {
    run(t, EXECUTOR);
  }

  /**
   * Run a {@link ThreadedTransaction} with the {@link ExecutorService} of imeji
   *
   * @param transactionThread
   * @throws Exception
   */
  public static void run(ThreadedTransaction transactionThread, ExecutorService executor) throws ImejiException {
    final Future<Integer> future = executor.submit(transactionThread);
    // wait for the transaction to be finished
    try {
      future.get();
    } catch (CancellationException | ExecutionException | InterruptedException e) {
      LOGGER.info("Exception in Jena transaction thread", e);
    }
    transactionThread.throwImejiException();
  }

}
