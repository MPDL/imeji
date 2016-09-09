/**
 * License: src/main/resources/license/escidoc.license
 */
package de.mpg.imeji.logic;

import static com.google.common.base.Strings.isNullOrEmpty;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.jena.atlas.lib.AlarmClock;
import org.apache.log4j.Logger;
import org.apache.log4j.lf5.util.StreamUtils;
import org.jose4j.lang.JoseException;

import com.hp.hpl.jena.Jena;
import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.ReadWrite;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.tdb.TDB;
import com.hp.hpl.jena.tdb.TDBFactory;
import com.hp.hpl.jena.tdb.base.file.Location;
import com.hp.hpl.jena.tdb.sys.TDBMaker;

import de.mpg.imeji.exceptions.AlreadyExistsException;
import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.exceptions.NotFoundException;
import de.mpg.imeji.j2j.annotations.j2jModel;
import de.mpg.imeji.logic.auth.ImejiRsaKeys;
import de.mpg.imeji.logic.auth.authentication.impl.APIKeyAuthentication;
import de.mpg.imeji.logic.auth.authorization.AuthorizationPredefinedRoles;
import de.mpg.imeji.logic.concurrency.locks.LocksSurveyor;
import de.mpg.imeji.logic.config.ImejiConfiguration;
import de.mpg.imeji.logic.config.ImejiProperties;
import de.mpg.imeji.logic.config.ImejiResourceBundle;
import de.mpg.imeji.logic.config.ImejiStartupConfig;
import de.mpg.imeji.logic.controller.business.MetadataProfileBusinessController;
import de.mpg.imeji.logic.controller.resource.UserController;
import de.mpg.imeji.logic.controller.resource.UserController.USER_TYPE;
import de.mpg.imeji.logic.controller.util.ImejiFactory;
import de.mpg.imeji.logic.jobs.executors.NightlyExecutor;
import de.mpg.imeji.logic.keyValueStore.KeyValueStoreBusinessController;
import de.mpg.imeji.logic.search.elasticsearch.ElasticService;
import de.mpg.imeji.logic.search.jenasearch.ImejiSPARQL;
import de.mpg.imeji.logic.util.PropertyReader;
import de.mpg.imeji.logic.util.StringHelper;
import de.mpg.imeji.logic.vo.Album;
import de.mpg.imeji.logic.vo.CollectionImeji;
import de.mpg.imeji.logic.vo.Item;
import de.mpg.imeji.logic.vo.MetadataProfile;
import de.mpg.imeji.logic.vo.Space;
import de.mpg.imeji.logic.vo.Statement;
import de.mpg.imeji.logic.vo.User;

/**
 * {@link Jena} interface for imeji
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public class Imeji {
  private static final Logger LOGGER = Logger.getLogger(Imeji.class);
  public static String tdbPath = null;
  public static String collectionModel;
  public static String albumModel;
  public static String imageModel;
  public static String userModel;
  public static String profileModel;
  public static String statementModel;
  public static String spaceModel;
  public static Dataset dataset;
  public static User adminUser;
  public static MetadataProfile defaultMetadataProfile;
  private static final String ADMIN_EMAIL_INIT = "admin@imeji.org";
  private static final String ADMIN_PASSWORD_INIT = "admin";
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
  public static final ExecutorService executor = Executors.newCachedThreadPool();
  /**
   * Executes jobs over night
   */
  public static final NightlyExecutor nightlyExecutor = new NightlyExecutor();

  /**
   * private Constructor
   */
  private Imeji() {
    // avoid constructor
  }

  /**
   * Initialize the {@link Jena} database according to imeji.properties<br/>
   * Called when the server (Tomcat of JBoss) is started
   *
   * @throws URISyntaxException
   * @throws IOException
   * @throws ImejiException
   *
   */
  public static void init() throws IOException, URISyntaxException, ImejiException {
    tdbPath = PropertyReader.getProperty("imeji.tdb.path");
    ElasticService.start();
    init(tdbPath);
    nightlyExecutor.start();
  }

  /**
   * Run the migration instruction (SPARQL Update queries defines in the migration.xml file)
   *
   * @throws IOException
   */
  public static void runMigration() throws IOException {
    File f = new File(Imeji.tdbPath + StringHelper.urlSeparator + "migration.txt");
    FileInputStream in = null;
    try {
      in = new FileInputStream(f);
    } catch (FileNotFoundException e) {
      LOGGER.info("No" + f.getAbsolutePath() + " found, no migration runs", e);
    }
    if (in != null) {
      String migrationRequests = new String(StreamUtils.getBytes(in), "UTF-8");
      LOGGER.info("Running migration with query: ");
      LOGGER.info(migrationRequests);
      ImejiSPARQL.execUpdate(migrationRequests);
      LOGGER.info("Migration done!");
    }
  }

  /**
   * Initialize a {@link Jena} database according at one path location in filesystem
   *
   * @param path
   * @throws ImejiException
   * @throws URISyntaxException
   * @throws IOException
   */
  public static void init(String path) throws IOException, URISyntaxException, ImejiException {
    if (path != null) {
      File f = new File(path);
      if (!f.exists()) {
        f.getParentFile().mkdirs();
      }
      tdbPath = f.getAbsolutePath();
    }
    LOGGER.info("Initializing Jena dataset (" + tdbPath + ")...");
    dataset = tdbPath != null ? TDBFactory.createDataset(tdbPath) : TDBFactory.createDataset();
    LOGGER.info("... dataset done!");
    LOGGER.info("Initializing Jena models...");
    albumModel = getModelName(Album.class);
    collectionModel = getModelName(CollectionImeji.class);
    imageModel = getModelName(Item.class);
    userModel = getModelName(User.class);
    statementModel = getModelName(Statement.class);
    profileModel = getModelName(MetadataProfile.class);
    spaceModel = getModelName(Space.class);
    initModel(albumModel);
    initModel(collectionModel);
    initModel(imageModel);
    initModel(userModel);
    initModel(statementModel);
    initModel(profileModel);
    initModel(spaceModel);
    LOGGER.info("... models done!");
    CONFIG = new ImejiConfiguration();
    KeyValueStoreBusinessController.startAllStores();
    initRsaKeys();
    initadminUser();
    initDefaultMetadataProfile();
  }

  /**
   * Reset imeji, i.e. remove all data
   */
  public static void reset() {
    TDBFactory.reset();
    ElasticService.reset();
  }

  /**
   * Initialize (Create when not existing) a {@link Model} with a given name
   *
   * @param name
   */
  private static void initModel(String name) {
    try {
      // Careful: This is a read locks. A write lock would lead to
      // corrupted graph
      dataset.begin(ReadWrite.READ);
      if (dataset.containsNamedModel(name)) {
        dataset.getNamedModel(name);
      } else {
        Model m = ModelFactory.createDefaultModel();
        dataset.addNamedModel(name, m);
      }
      dataset.commit();
    } catch (Exception e) {
      dataset.abort();
      LOGGER.fatal("Error initialising model " + name, e);
    } finally {
      dataset.end();
    }
  }

  /**
   * Initialize the RSA Keys, used to generate API Keys
   */
  private static void initRsaKeys() {
    try {
      ImejiRsaKeys.init(Imeji.CONFIG.getRsaPublicKey(), Imeji.CONFIG.getRsaPrivateKey());
      Imeji.CONFIG.setRsaPublicKey(ImejiRsaKeys.getPublicKeyJson());
      Imeji.CONFIG.setRsaPrivateKey(ImejiRsaKeys.getPrivateKeyString());
      Imeji.CONFIG.saveConfig();
    } catch (JoseException | NoSuchAlgorithmException | InvalidKeySpecException e) {
      LOGGER.error("!!! Error initalizing API Key !!!", e);
    }
  }

  /**
   * Initialize the system administrator {@link User}, accoring to credentials in imeji.properties
   */
  private static void initadminUser() {
    try {
      // Init the User
      adminUser = new User();
      adminUser.setPerson(ImejiFactory.newPerson("Admin", "imeji", "imeji community"));
      adminUser.setEmail(ADMIN_EMAIL_INIT);
      adminUser.setEncryptedPassword(StringHelper.convertToMD5(ADMIN_PASSWORD_INIT));
      adminUser.getGrants()
          .addAll(AuthorizationPredefinedRoles.imejiAdministrator(adminUser.getId().toString()));
      adminUser.setApiKey(APIKeyAuthentication.generateKey(adminUser.getId(), Integer.MAX_VALUE));
      // create
      LOGGER.info("Checking admin users..");
      UserController uc = new UserController(adminUser);
      List<User> admins = uc.retrieveAllAdmins();
      if (admins.size() == 0) {
        try {
          LOGGER.info("... No admin found! Creating one.");
          User admin = uc.retrieve(adminUser.getEmail());
          LOGGER.warn(admin.getEmail() + " already exists: " + admin.getId());
          LOGGER.error(
              "!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
          LOGGER.error(
              "!!!! Something went wrong: No users with admin rights found, but default admin user found !!!!!");
          LOGGER.warn("Creating a new sysadmin");
          LOGGER.warn("Use this user to recover your system");
          String newEmail = System.currentTimeMillis() + adminUser.getEmail();
          LOGGER.warn("EMAIL: " + newEmail);
          LOGGER.warn("PASSWORD: " + ADMIN_PASSWORD_INIT);
          LOGGER.error(
              "!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
          adminUser.setEmail(newEmail);
          adminUser.setEncryptedPassword(StringHelper.convertToMD5(ADMIN_PASSWORD_INIT));
          uc.create(adminUser, USER_TYPE.ADMIN);
        } catch (NotFoundException e) {
          LOGGER.info(
              "!!! IMPORTANT !!! Create admin@imeji.org as system administrator with password admin. !!! CHANGE PASSWORD !!!");
          adminUser = uc.create(adminUser, USER_TYPE.ADMIN);
          LOGGER.info("Created admin user successfully:" + adminUser.getEmail(), e);
        }
      } else {
        LOGGER.info("Admin users found:");
        for (User admin : admins) {
          LOGGER.info(admin.getEmail() + " is admin + (" + admin.getId() + ")");
        }
      }
    } catch (AlreadyExistsException e) {
      LOGGER.warn(adminUser.getEmail() + " already exists", e);
    } catch (Exception e) {
      LOGGER.error("Error initializing Admin user! ", e);
    }
  }

  private static void initDefaultMetadataProfile() {
    MetadataProfileBusinessController metadataProfileBC = new MetadataProfileBusinessController();
    LOGGER.info("Initializing default metadata profile...");
    try {
      defaultMetadataProfile = metadataProfileBC.initDefaultMetadataProfile();
    } catch (Exception e) {
      LOGGER.error("error retrieving/creating default metadata profile: ", e);
    }
    if (defaultMetadataProfile != null) {
      LOGGER.info("Default metadata profile is set-up to " + defaultMetadataProfile.getId());
    } else {
      LOGGER.info(
          "Checking for default metadata profile is finished: no default metadata profile has been set.");

    }
  }

  public static void shutdown() {
    LOGGER.info("Shutting down thread executor...");
    Imeji.executor.shutdown();
    nightlyExecutor.stop();
    LOGGER.info("executor shutdown shutdown? " + Imeji.executor.isShutdown());
    ElasticService.shutdown();
    KeyValueStoreBusinessController.stopAllStores();
    LOGGER.info("Ending LockSurveyor...");
    locksSurveyor.terminate();
    LOGGER.info("...done");
    LOGGER.info("Closing Jena! TDB...");
    TDB.sync(Imeji.dataset);
    LOGGER.info("sync done");
    Imeji.dataset.close();
    LOGGER.info("dataset closed");
    TDB.closedown();
    LOGGER.info("tdb closed");
    TDBMaker.releaseLocation(Location.create(Imeji.tdbPath));
    LOGGER.info("location released");
    LOGGER.info("...done!");

    // This is a bug of com.hp.hpl.jena.sparql.engine.QueryExecutionBase
    // implementation:
    // AlarmClock is not correctly released, it leads to the memory leaks
    // after tomcat stop
    // see https://github.com/imeji-community/imeji/issues/966!
    LOGGER.info("Release AlarmClock...");
    AlarmClock alarmClock = AlarmClock.get();
    alarmClock.release();
    LOGGER.info("done");
  }

  /**
   * Return the name of the model if defined in a {@link Class} with {@link j2jModel} annotation
   *
   * @param voClass
   * @return
   */
  public static String getModelName(Class<?> voClass) {
    j2jModel j2jModel = voClass.getAnnotation(j2jModel.class);
    return "http://imeji.org/" + j2jModel.value();
  }

  /**
   * Returns true if checksum of uploaded files will be checked for duplicates within a single
   * collection according to settings in properties. If properties do not exist, checksum duplicate
   * checking will be set as default
   *
   */

  public static boolean isValidateChecksumInCollection() {
    String validateChecksum;
    try {
      validateChecksum = PropertyReader.getProperty("imeji.validate.checksum");
    } catch (Exception e) {
      return true;
    }
    if (isNullOrEmpty(validateChecksum)) {
      return true;
    }
    return Boolean.valueOf(validateChecksum);
  }
}
