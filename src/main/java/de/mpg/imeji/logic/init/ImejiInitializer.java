package de.mpg.imeji.logic.init;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.List;

import org.apache.jena.atlas.lib.AlarmClock;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
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
import de.mpg.imeji.j2j.annotations.j2jModel;
import de.mpg.imeji.logic.batch.executors.NightlyExecutor;
import de.mpg.imeji.logic.config.Imeji;
import de.mpg.imeji.logic.config.ImejiConfiguration;
import de.mpg.imeji.logic.config.emailcontent.ImejiEmailContentConfiguration;
import de.mpg.imeji.logic.config.util.PropertyReader;
import de.mpg.imeji.logic.core.statement.StatementService;
import de.mpg.imeji.logic.db.keyValue.KeyValueStoreService;
import de.mpg.imeji.logic.events.listener.ListenerService;
import de.mpg.imeji.logic.hierarchy.HierarchyService;
import de.mpg.imeji.logic.model.CollectionImeji;
import de.mpg.imeji.logic.model.ContentVO;
import de.mpg.imeji.logic.model.Item;
import de.mpg.imeji.logic.model.Statement;
import de.mpg.imeji.logic.model.StatementType;
import de.mpg.imeji.logic.model.User;
import de.mpg.imeji.logic.model.factory.ImejiFactory;
import de.mpg.imeji.logic.model.util.StatementUtil;
import de.mpg.imeji.logic.search.Search;
import de.mpg.imeji.logic.search.elasticsearch.ElasticInitializer;
import de.mpg.imeji.logic.search.facet.model.Facet;
import de.mpg.imeji.logic.security.authentication.ImejiRsaKeys;
import de.mpg.imeji.logic.security.authentication.impl.APIKeyAuthentication;
import de.mpg.imeji.logic.security.authorization.AuthorizationPredefinedRoles;
import de.mpg.imeji.logic.security.user.UserService;
import de.mpg.imeji.logic.security.user.UserService.USER_TYPE;
import de.mpg.imeji.logic.util.StringHelper;

/**
 * Initialize imeji
 *
 * @author saquet
 *
 */
public class ImejiInitializer {
  private static final Logger LOGGER = LogManager.getLogger(ImejiInitializer.class);
  /**
   * Executes jobs over night
   */
  private static NightlyExecutor NIGHTLY_EXECUTOR = new NightlyExecutor();

  /**
   * Initialize the {@link Jena} database according to imeji.properties<br/>
   * Called when the server (Tomcat of JBoss) is started
   *
   * @throws URISyntaxException
   * @throws IOException
   * @throws ImejiException
   *
   */
  public static void init() {
    try {
      Imeji.tdbPath = PropertyReader.getProperty("imeji.tdb.path");
      ElasticInitializer.start();
      ImejiInitializer.init(Imeji.tdbPath);
      NIGHTLY_EXECUTOR.start();
    } catch (final Exception e) {
      LOGGER.fatal("Error initializing imeji", e);
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
      final File f = new File(path);
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
    Imeji.collectionModel = ImejiInitializer.getModelName(CollectionImeji.class);
    Imeji.imageModel = ImejiInitializer.getModelName(Item.class);
    Imeji.userModel = ImejiInitializer.getModelName(User.class);
    Imeji.statementModel = ImejiInitializer.getModelName(Statement.class);
    Imeji.contentModel = ImejiInitializer.getModelName(ContentVO.class);
    Imeji.facetModel = ImejiInitializer.getModelName(Facet.class);
    ImejiInitializer.initModel(Imeji.collectionModel);
    ImejiInitializer.initModel(Imeji.imageModel);
    ImejiInitializer.initModel(Imeji.userModel);
    ImejiInitializer.initModel(Imeji.statementModel);
    ImejiInitializer.initModel(Imeji.contentModel);
    ImejiInitializer.initModel(Imeji.facetModel);
    LOGGER.info("... models done!");
    Imeji.CONFIG = new ImejiConfiguration();
    Imeji.EMAIL_CONFIG = new  ImejiEmailContentConfiguration(Imeji.CONFIG);
    KeyValueStoreService.startAllStores();
    initRsaKeys();
    initadminUser();
    initDefaultStatements();
    new ListenerService().init();
    HierarchyService.reloadHierarchy();
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
        final Model m = ModelFactory.createDefaultModel();
        Imeji.dataset.addNamedModel(name, m);
      }
      Imeji.dataset.commit();
    } catch (final Exception e) {
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
      Imeji.adminUser
          .setPerson(ImejiFactory.newPerson("Admin", "imeji", "Max Planck Digital Library"));
      Imeji.adminUser.setEmail(Imeji.ADMIN_EMAIL_INIT);
      Imeji.adminUser.setEncryptedPassword(StringHelper.md5(Imeji.ADMIN_PASSWORD_INIT));
      Imeji.adminUser
          .setApiKey(APIKeyAuthentication.generateKey(Imeji.adminUser.getId(), Integer.MAX_VALUE));
      Imeji.adminUser.setGrants(
          AuthorizationPredefinedRoles.imejiAdministrator(Imeji.PROPERTIES.getBaseURI()));
      // create
      LOGGER.info("Checking admin users..");
      final UserService uc = new UserService();
      final List<User> admins = uc.retrieveAllAdmins();
      if (admins.size() == 0) {
        final String newEmail = System.currentTimeMillis() + Imeji.adminUser.getEmail();
        LOGGER.error("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
        LOGGER.error(" ");
        LOGGER.warn("CREATING NEW SYSADMIN");
        LOGGER.warn("Use this user to recover your system");
        LOGGER.error(" ");
        LOGGER.warn("EMAIL: " + newEmail);
        LOGGER.warn("PASSWORD: " + Imeji.ADMIN_PASSWORD_INIT);
        Imeji.adminUser.setEmail(newEmail);
        Imeji.adminUser = uc.create(Imeji.adminUser, USER_TYPE.ADMIN);
        Imeji.STARTUP.setReindex(true);
        LOGGER.error(" ");
        LOGGER.info("Created admin user successfully!");
        LOGGER.error(" ");
        LOGGER.info("PLEASE CHANGE USER AND PASSWORD IMMEDIATELY");
        LOGGER.error(" ");
        LOGGER.error("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
      } else {
        LOGGER.info("Admin users found:");
        for (final User admin : admins) {
          LOGGER.info(admin.getEmail() + " is admin + (" + admin.getId() + ")");
        }
      }
    } catch (final AlreadyExistsException e) {
      LOGGER.warn(Imeji.adminUser.getEmail() + " already exists", e);
    } catch (final Exception e) {
      LOGGER.error("Error initializing Admin user! ", e);
    }
  }

  /**
   * Initialize the default Statements of the instance
   */
  public static void initDefaultStatements() {
    List<Statement> l = new ArrayList<>();
    l.add(ImejiFactory.newStatement().setIndex("Title").setType(StatementType.TEXT).build());
    l.add(ImejiFactory.newStatement().setIndex("Author").setType(StatementType.PERSON).build());
    l.add(ImejiFactory.newStatement().setIndex("Date").setType(StatementType.DATE).build());
    l.add(ImejiFactory.newStatement().setIndex("Description").setType(StatementType.TEXT).build());
    l.add(ImejiFactory.newStatement().setIndex("Location").setType(StatementType.GEOLOCATION)
        .build());
    l.add(ImejiFactory.newStatement().setIndex("Link").setType(StatementType.URL).build());

    StatementService service = new StatementService();
    try {
      if (service.search(null, null, Imeji.adminUser, Search.SEARCH_FROM_START_INDEX, Search.GET_ALL_RESULTS).getNumberOfRecords() == 0) {
        service.createBatch(l, Imeji.adminUser);
        Imeji.CONFIG.setStatements(StatementUtil.toStatementNamesString(l.subList(0, 1)));
        Imeji.CONFIG.saveConfig();
      }
    } catch (Exception e) {
      LOGGER.error("Error creating default statements", e);
    }
  }

  /**
   * Return the name of the model if defined in a {@link Class} with {@link j2jModel} annotation
   *
   * @param voClass
   * @return
   */
  private static String getModelName(Class<?> voClass) {
    final j2jModel j2jModel = voClass.getAnnotation(j2jModel.class);
    return "http://imeji.org/" + j2jModel.value();
  }


  /**
   * Shutdown imeji
   */
  public static void shutdown() {
    LOGGER.info("Shutting down thread executors...");
    Imeji.getEXECUTOR().shutdown();
    Imeji.getCONTENT_EXTRACTION_EXECUTOR().shutdown();
    Imeji.getINTERNAL_STORAGE_EXECUTOR().shutdown();
    NIGHTLY_EXECUTOR.stop();
    LOGGER.info("imeji executor shutdown? " + Imeji.getEXECUTOR().isShutdown());
    LOGGER.info("content extraction executor shutdown? "
        + Imeji.getCONTENT_EXTRACTION_EXECUTOR().isShutdown());
    LOGGER.info("internal executor shutdown shutdown? "
        + Imeji.getINTERNAL_STORAGE_EXECUTOR().isShutdown());
    LOGGER.info("nightly executor shutdown shutdown? " + NIGHTLY_EXECUTOR.isShutdown());
    ElasticInitializer.shutdown();
    KeyValueStoreService.stopAllStores();
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
    final AlarmClock alarmClock = AlarmClock.get();
    alarmClock.release();
    LOGGER.info("done");
  }

  /**
   * @return the nIGHTLY_EXECUTOR
   */
  public static NightlyExecutor getNIGHTLY_EXECUTOR() {
    if (NIGHTLY_EXECUTOR.isShutdown()) {
      NIGHTLY_EXECUTOR = new NightlyExecutor();
    }
    return NIGHTLY_EXECUTOR;
  }
}
