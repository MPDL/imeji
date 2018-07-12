package de.mpg.imeji.logic.config;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import com.hp.hpl.jena.query.Dataset;

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
  private static ThreadPoolExecutor CONTENT_EXTRACTION_EXECUTOR =
      (ThreadPoolExecutor) Executors.newFixedThreadPool(1);

  /**
   * Executor used to transform files by the internal storage
   */
  private static ThreadPoolExecutor INTERNAL_STORAGE_EXECUTOR =
      (ThreadPoolExecutor) Executors.newFixedThreadPool(10);

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
      EXECUTOR = Executors.newCachedThreadPool();
    }
    return EXECUTOR;
  }

  /**
   * @return the cONTENT_EXTRACTION_EXECUTOR
   */
  public static ThreadPoolExecutor getCONTENT_EXTRACTION_EXECUTOR() {
    if (CONTENT_EXTRACTION_EXECUTOR.isShutdown()) {
      return CONTENT_EXTRACTION_EXECUTOR = (ThreadPoolExecutor) Executors.newFixedThreadPool(1);
    }
    return CONTENT_EXTRACTION_EXECUTOR;
  }

  /**
   * @return the iNTERNAL_STORAGE_EXECUTOR
   */
  public static ThreadPoolExecutor getINTERNAL_STORAGE_EXECUTOR() {
    if (INTERNAL_STORAGE_EXECUTOR.isShutdown()) {
      return INTERNAL_STORAGE_EXECUTOR = (ThreadPoolExecutor) Executors.newFixedThreadPool(10);
    }
    return INTERNAL_STORAGE_EXECUTOR;
  }

}
