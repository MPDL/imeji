package de.mpg.imeji.logic.config;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import org.apache.jena.query.Dataset;

import de.mpg.imeji.logic.concurrency.LocksSurveyor;
import de.mpg.imeji.logic.config.emailcontent.ImejiEmailContentConfiguration;
import de.mpg.imeji.logic.model.User;

/**
 * imeji object.
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public class Imeji {
  public static String tdbPath = null;
  public static String collectionModel;
  public static String imageModel;
  public static String userModel;
  public static String statementModel;
  public static String facetModel;
  public static String contentModel;
  public static Dataset dataset;
  public static User adminUser;
  public static final String ADMIN_EMAIL_INIT = "admin@imeji.org";
  public static final String ADMIN_PASSWORD_INIT = "admin";
  public static ImejiConfiguration CONFIG;
  public static final ImejiProperties PROPERTIES = new ImejiProperties();
  public static final ImejiStartupConfig STARTUP = new ImejiStartupConfig();
  public static ImejiEmailContentConfiguration EMAIL_CONFIG;

  /**
   * The path for this servlet as defined in the web.xml
   */
  public static final String FILE_SERVLET_PATH = "file";

  public static final ImejiResourceBundle RESOURCE_BUNDLE = new ImejiResourceBundle();

  /**
   * Thread to check if locked objects can be unlocked
   */
  public static final LocksSurveyor locksSurveyor = new LocksSurveyor();
  /**
   * The {@link ExecutorService} which runs the thread in imeji
   */
  private static ExecutorService EXECUTOR = Executors.newCachedThreadPool();
  /**
   * Executor used for the content extraction
   */
  private static ThreadPoolExecutor CONTENT_EXTRACTION_EXECUTOR = (ThreadPoolExecutor) Executors.newFixedThreadPool(1);

  /**
   * Executor used to transform files by the internal storage
   */
  private static ThreadPoolExecutor INTERNAL_STORAGE_EXECUTOR = (ThreadPoolExecutor) Executors.newFixedThreadPool(10);

  /**
   * List of all ThreadPoolExecutors (threads of SingleThreadExecutors NOT included). Used in
   * Testing to wait for threads to finish.
   */
  private static final List<ThreadPoolExecutor> THREAD_POOL_EXECUTORS = Imeji.createThreadPoolList();

  /**
   * private Constructor
   */
  private Imeji() {
    // avoid constructor
  }

  /**
   * @return the eXECUTOR
   */
  public static ExecutorService getEXECUTOR() {
    if (EXECUTOR.isShutdown()) {
      THREAD_POOL_EXECUTORS.remove(EXECUTOR);
      EXECUTOR = Executors.newCachedThreadPool();
      THREAD_POOL_EXECUTORS.add((ThreadPoolExecutor) EXECUTOR);
    }
    return EXECUTOR;
  }

  /**
   * @return the cONTENT_EXTRACTION_EXECUTOR
   */
  public static ThreadPoolExecutor getCONTENT_EXTRACTION_EXECUTOR() {
    if (CONTENT_EXTRACTION_EXECUTOR.isShutdown()) {
      THREAD_POOL_EXECUTORS.remove(CONTENT_EXTRACTION_EXECUTOR);
      CONTENT_EXTRACTION_EXECUTOR = (ThreadPoolExecutor) Executors.newFixedThreadPool(1);
      THREAD_POOL_EXECUTORS.add(CONTENT_EXTRACTION_EXECUTOR);
    }
    return CONTENT_EXTRACTION_EXECUTOR;
  }

  /**
   * @return the iNTERNAL_STORAGE_EXECUTOR
   */
  public static ThreadPoolExecutor getINTERNAL_STORAGE_EXECUTOR() {
    if (INTERNAL_STORAGE_EXECUTOR.isShutdown()) {
      THREAD_POOL_EXECUTORS.remove(INTERNAL_STORAGE_EXECUTOR);
      INTERNAL_STORAGE_EXECUTOR = (ThreadPoolExecutor) Executors.newFixedThreadPool(10);
      THREAD_POOL_EXECUTORS.add(INTERNAL_STORAGE_EXECUTOR);
    }
    return INTERNAL_STORAGE_EXECUTOR;
  }

  private static List<ThreadPoolExecutor> createThreadPoolList() {
    List<ThreadPoolExecutor> threadPoolList = new ArrayList<>();
    threadPoolList.add((ThreadPoolExecutor) EXECUTOR);
    threadPoolList.add(CONTENT_EXTRACTION_EXECUTOR);
    threadPoolList.add(INTERNAL_STORAGE_EXECUTOR);

    return threadPoolList;
  }

  public static List<ThreadPoolExecutor> getThreadPoolExecutors() {
    return THREAD_POOL_EXECUTORS;
  }

  /**
   * Creates a new cached thread pool. Wrapper for {@link Executors#newCachedThreadPool()}. Adds the
   * thread pool to the list of all thread pools {@link Imeji#THREAD_POOL_EXECUTORS}
   * 
   * @return the created {@link ExecutorService}
   */
  public static ExecutorService createNewCachedThreadPool() {
    ExecutorService executerService = Executors.newCachedThreadPool();
    THREAD_POOL_EXECUTORS.add((ThreadPoolExecutor) executerService);
    return executerService;
  }

  /**
   * Creates a new single thread executor. Wrapper for {@link Executors#newSingleThreadExecutor()}.
   * 
   * @return the created {@link ExecutorService}
   */
  public static ExecutorService createNewSingleThreadExecutor() {
    ExecutorService executerService = Executors.newSingleThreadExecutor();
    //No thread pool => not added to the list to thread pools (THREAD_POOL_EXECUTORS)
    return executerService;
  }

}
