/**
 * License: src/main/resources/license/escidoc.license
 */
package de.mpg.imeji.logic;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.hp.hpl.jena.query.Dataset;

import de.mpg.imeji.logic.concurrency.locks.LocksSurveyor;
import de.mpg.imeji.logic.config.ImejiConfiguration;
import de.mpg.imeji.logic.config.ImejiProperties;
import de.mpg.imeji.logic.config.ImejiResourceBundle;
import de.mpg.imeji.logic.config.ImejiStartupConfig;
import de.mpg.imeji.logic.vo.MetadataProfile;
import de.mpg.imeji.logic.vo.User;

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
  public static String albumModel;
  public static String imageModel;
  public static String userModel;
  public static String profileModel;
  public static String statementModel;
  public static String spaceModel;
  public static String contentModel;
  public static Dataset dataset;
  public static User adminUser;
  public static MetadataProfile defaultMetadataProfile;
  public static final String ADMIN_EMAIL_INIT = "admin@imeji.org";
  public static final String ADMIN_PASSWORD_INIT = "admin";
  public static ImejiConfiguration CONFIG;
  public static final ImejiProperties PROPERTIES = new ImejiProperties();
  public static final ImejiStartupConfig STARTUP = new ImejiStartupConfig();
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
  private static ExecutorService executor = Executors.newCachedThreadPool();


  /**
   * private Constructor
   */
  private Imeji() {
    // avoid constructor
  }


  public static ExecutorService getExecutor() {
    if (executor == null || executor.isShutdown()) {
      executor = Executors.newCachedThreadPool();
    }
    return executor;
  }
}
