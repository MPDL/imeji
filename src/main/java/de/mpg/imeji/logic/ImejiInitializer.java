package de.mpg.imeji.logic;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.List;

import org.apache.jena.atlas.lib.AlarmClock;
import org.apache.log4j.Logger;
import org.jose4j.lang.JoseException;

import com.hp.hpl.jena.Jena;
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
import de.mpg.imeji.logic.config.ImejiConfiguration;
import de.mpg.imeji.logic.config.util.PropertyReader;
import de.mpg.imeji.logic.controller.business.MetadataProfileBusinessController;
import de.mpg.imeji.logic.jobs.executors.NightlyExecutor;
import de.mpg.imeji.logic.keyValueStore.KeyValueStoreBusinessController;
import de.mpg.imeji.logic.search.elasticsearch.ElasticInitializer;
import de.mpg.imeji.logic.security.authorization.AuthorizationPredefinedRoles;
import de.mpg.imeji.logic.user.authentication.ImejiRsaKeys;
import de.mpg.imeji.logic.user.authentication.impl.APIKeyAuthentication;
import de.mpg.imeji.logic.user.controller.UserBusinessController;
import de.mpg.imeji.logic.user.controller.UserBusinessController.USER_TYPE;
import de.mpg.imeji.logic.util.StringHelper;
import de.mpg.imeji.logic.vo.Album;
import de.mpg.imeji.logic.vo.CollectionImeji;
import de.mpg.imeji.logic.vo.Item;
import de.mpg.imeji.logic.vo.MetadataProfile;
import de.mpg.imeji.logic.vo.Space;
import de.mpg.imeji.logic.vo.Statement;
import de.mpg.imeji.logic.vo.User;
import de.mpg.imeji.logic.vo.util.ImejiFactory;

/**
 * Initialize imeji
 * 
 * @author saquet
 *
 */
public class ImejiInitializer {
  private static final Logger LOGGER = Logger.getLogger(ImejiInitializer.class);
  /**
   * Executes jobs over night
   */
  private static final NightlyExecutor NIGHTLY_EXECUTOR = new NightlyExecutor();

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
    Imeji.tdbPath = PropertyReader.getProperty("imeji.tdb.path");
    ElasticInitializer.start();
    ImejiInitializer.init(Imeji.tdbPath);
    NIGHTLY_EXECUTOR.start();
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
      Imeji.tdbPath = f.getAbsolutePath();
    }
    LOGGER.info("Initializing Jena dataset (" + Imeji.tdbPath + ")...");
    Imeji.dataset = Imeji.tdbPath != null ? TDBFactory.createDataset(Imeji.tdbPath)
        : TDBFactory.createDataset();
    LOGGER.info("... dataset done!");
    LOGGER.info("Initializing Jena models...");
    Imeji.albumModel = ImejiInitializer.getModelName(Album.class);
    Imeji.collectionModel = ImejiInitializer.getModelName(CollectionImeji.class);
    Imeji.imageModel = ImejiInitializer.getModelName(Item.class);
    Imeji.userModel = ImejiInitializer.getModelName(User.class);
    Imeji.statementModel = ImejiInitializer.getModelName(Statement.class);
    Imeji.profileModel = ImejiInitializer.getModelName(MetadataProfile.class);
    Imeji.spaceModel = ImejiInitializer.getModelName(Space.class);
    ImejiInitializer.initModel(Imeji.albumModel);
    ImejiInitializer.initModel(Imeji.collectionModel);
    ImejiInitializer.initModel(Imeji.imageModel);
    ImejiInitializer.initModel(Imeji.userModel);
    ImejiInitializer.initModel(Imeji.statementModel);
    ImejiInitializer.initModel(Imeji.profileModel);
    ImejiInitializer.initModel(Imeji.spaceModel);
    LOGGER.info("... models done!");
    Imeji.CONFIG = new ImejiConfiguration();
    KeyValueStoreBusinessController.startAllStores();
    ImejiInitializer.initRsaKeys();
    ImejiInitializer.initadminUser();
    ImejiInitializer.initDefaultMetadataProfile();
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
      Imeji.dataset.begin(ReadWrite.READ);
      if (Imeji.dataset.containsNamedModel(name)) {
        Imeji.dataset.getNamedModel(name);
      } else {
        Model m = ModelFactory.createDefaultModel();
        Imeji.dataset.addNamedModel(name, m);
      }
      Imeji.dataset.commit();
    } catch (Exception e) {
      Imeji.dataset.abort();
      LOGGER.fatal("Error initialising model " + name, e);
    } finally {
      Imeji.dataset.end();
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
      Imeji.adminUser = new User();
      Imeji.adminUser.setPerson(ImejiFactory.newPerson("Admin", "imeji", "imeji community"));
      Imeji.adminUser.setEmail(Imeji.ADMIN_EMAIL_INIT);
      Imeji.adminUser.setEncryptedPassword(StringHelper.convertToMD5(Imeji.ADMIN_PASSWORD_INIT));
      Imeji.adminUser.getGrants().addAll(
          AuthorizationPredefinedRoles.imejiAdministrator(Imeji.adminUser.getId().toString()));
      Imeji.adminUser
          .setApiKey(APIKeyAuthentication.generateKey(Imeji.adminUser.getId(), Integer.MAX_VALUE));
      // create
      LOGGER.info("Checking admin users..");
      UserBusinessController uc = new UserBusinessController();
      List<User> admins = uc.retrieveAllAdmins();
      if (admins.size() == 0) {
        try {
          LOGGER.info("... No admin found! Creating one.");
          User admin = uc.retrieve(Imeji.adminUser.getEmail(), Imeji.adminUser);
          LOGGER.warn(admin.getEmail() + " already exists: " + admin.getId());
          LOGGER.error(
              "!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
          LOGGER.error(
              "!!!! Something went wrong: No users with admin rights found, but default admin user found !!!!!");
          LOGGER.warn("Creating a new sysadmin");
          LOGGER.warn("Use this user to recover your system");
          String newEmail = System.currentTimeMillis() + Imeji.adminUser.getEmail();
          LOGGER.warn("EMAIL: " + newEmail);
          LOGGER.warn("PASSWORD: " + Imeji.ADMIN_PASSWORD_INIT);
          LOGGER.error(
              "!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
          Imeji.adminUser.setEmail(newEmail);
          Imeji.adminUser
              .setEncryptedPassword(StringHelper.convertToMD5(Imeji.ADMIN_PASSWORD_INIT));
          Imeji.adminUser = uc.create(Imeji.adminUser, USER_TYPE.ADMIN);
        } catch (NotFoundException e) {
          LOGGER.info(
              "!!! IMPORTANT !!! Create admin@imeji.org as system administrator with password admin. !!! CHANGE PASSWORD !!!");
          Imeji.adminUser = uc.create(Imeji.adminUser, USER_TYPE.ADMIN);
          LOGGER.info("Created admin user successfully:" + Imeji.adminUser.getEmail(), e);
        }
      } else {
        LOGGER.info("Admin users found:");
        for (User admin : admins) {
          LOGGER.info(admin.getEmail() + " is admin + (" + admin.getId() + ")");
        }
      }
    } catch (AlreadyExistsException e) {
      LOGGER.warn(Imeji.adminUser.getEmail() + " already exists", e);
    } catch (Exception e) {
      LOGGER.error("Error initializing Admin user! ", e);
    }
  }


  public static void initDefaultMetadataProfile() {
    MetadataProfileBusinessController metadataProfileBC = new MetadataProfileBusinessController();
    LOGGER.info("Initializing default metadata profile...");
    try {
      Imeji.defaultMetadataProfile = metadataProfileBC.initDefaultMetadataProfile();
    } catch (Exception e) {
      LOGGER.error("error retrieving/creating default metadata profile: ", e);
    }
    if (Imeji.defaultMetadataProfile != null) {
      LOGGER.info("Default metadata profile is set-up to " + Imeji.defaultMetadataProfile.getId());
    } else {
      LOGGER.info(
          "Checking for default metadata profile is finished: no default metadata profile has been set.");

    }
  }


  /**
   * Return the name of the model if defined in a {@link Class} with {@link j2jModel} annotation
   *
   * @param voClass
   * @return
   */
  private static String getModelName(Class<?> voClass) {
    j2jModel j2jModel = voClass.getAnnotation(j2jModel.class);
    return "http://imeji.org/" + j2jModel.value();
  }


  /**
   * Shutdown imeji
   */
  public static void shutdown() {
    LOGGER.info("Shutting down thread executor...");
    Imeji.getExecutor().shutdown();
    NIGHTLY_EXECUTOR.stop();
    LOGGER.info("executor shutdown shutdown? " + Imeji.getExecutor().isShutdown());
    ElasticInitializer.shutdown();
    KeyValueStoreBusinessController.stopAllStores();
    LOGGER.info("Ending LockSurveyor...");
    Imeji.locksSurveyor.terminate();
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

}
