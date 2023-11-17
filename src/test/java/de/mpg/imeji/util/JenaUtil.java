package de.mpg.imeji.util;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.jena.tdb.StoreConnection;
import org.apache.jena.tdb.TDB;
import org.apache.jena.tdb.base.block.FileMode;
import org.apache.jena.tdb.base.file.Location;
import org.apache.jena.tdb.sys.SystemTDB;
import org.apache.jena.tdb.sys.TDBMaker;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.config.Imeji;
import de.mpg.imeji.logic.config.util.PropertyReader;
import de.mpg.imeji.logic.db.keyValue.KeyValueStoreService;
import de.mpg.imeji.logic.init.ImejiInitializer;
import de.mpg.imeji.logic.model.Organization;
import de.mpg.imeji.logic.model.Person;
import de.mpg.imeji.logic.model.User;
import de.mpg.imeji.logic.search.elasticsearch.ElasticInitializer;
import de.mpg.imeji.logic.search.elasticsearch.ElasticService;
import de.mpg.imeji.logic.security.authorization.AuthorizationPredefinedRoles;
import de.mpg.imeji.logic.security.user.UserService;
import de.mpg.imeji.logic.security.user.UserService.USER_TYPE;
import de.mpg.imeji.logic.util.StringHelper;
import de.mpg.imeji.logic.util.TempFileUtil;

/**
 * Utility class to use Jena in the unit test
 * 
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public class JenaUtil {
  private static final Logger LOGGER = LogManager.getLogger(JenaUtil.class);
  public static User testUser;
  public static User testUser2;
  public static User adminTestUser;
  public static String TEST_USER_EMAIL = "test@imeji.org";
  public static String TEST_USER_EMAIL_2 = "test2@imeji.org";
  public static String ADMIN_USER_EMAIL = "adminTest@imeji.org";
  public static String TEST_USER_NAME = "Test User";
  public static String TEST_USER_PWD = "password";
  public static String TDB_PATH;

  /**
   * Init a Jena Instance for Testing
   */
  public static void initJena() {
    try {
      // Read tdb location
      TDB_PATH = PropertyReader.getProperty("imeji.tdb.path");

      // Delete the TDB before the test, because on windows the TDB directory does not get deleted completely after the tests.
      deleteTDBDirectory();

      // Set Filemode: important to be able to delete TDB directory by
      // closing Jena
      SystemTDB.setFileMode(FileMode.direct);
      // Create new tdb
      ImejiInitializer.init(TDB_PATH);
      // Create new temp directory
      TempFileUtil.getOrCreateTempDirectory();

      initTestUser();
    } catch (Exception e) {
      throw new RuntimeException("Error initialiting Jena for testing: ", e);
    }
  }

  public static void closeJena() throws InterruptedException, IOException {
    KeyValueStoreService.resetAndStopAllStores();
    Imeji.getEXECUTOR().shutdown();
    Imeji.getCONTENT_EXTRACTION_EXECUTOR().shutdown();
    Imeji.getINTERNAL_STORAGE_EXECUTOR().shutdown();
    ImejiInitializer.getNIGHTLY_EXECUTOR().stop();

    LOGGER.info("Closing Jena:");
    TDB.sync(Imeji.dataset);
    LOGGER.info("Jena Sync done! ");
    // TDBFactory.reset();
    LOGGER.info("Reset internal state, releasing all datasets done! ");
    Imeji.dataset.close();
    LOGGER.info("Dataset closed!");
    TDB.closedown();
    TDBMaker.resetCache();
    StoreConnection.release(Location.create(TDB_PATH));
    LOGGER.info("TDB Location released!");

    // Remove old Database- and File-Directories
    //TODO: Move the deletion of the test-file-directories in an extra method.
    deleteTDBDirectory();
    deleteTempDirectory();
    deleteFilesDirectory();
  }

  private static void initTestUser() throws Exception {
    ElasticInitializer.reset();
    new UserService().reindex(ElasticService.DATA_ALIAS);
    testUser = getMockupUser(TEST_USER_EMAIL, TEST_USER_NAME, TEST_USER_PWD, false);
    testUser2 = getMockupUser(TEST_USER_EMAIL_2, TEST_USER_NAME, TEST_USER_PWD, false);
    adminTestUser = getMockupUser(ADMIN_USER_EMAIL, TEST_USER_NAME, TEST_USER_PWD, true);
    testUser = createUser(testUser, false);
    testUser2 = createUser(testUser2, false);
    adminTestUser = createUser(adminTestUser, true);
  }

  private static User createUser(User u, boolean isAdmin) throws ImejiException {
    UserService c = new UserService();
    try {
      if (isAdmin) {
        return c.create(u, USER_TYPE.ADMIN);
      } else {
        return c.create(u, USER_TYPE.DEFAULT);
      }

    } catch (Exception e) {
      LOGGER.info(u.getEmail() + " already exists. Must not be created");
      return c.retrieve(u.getEmail(), Imeji.adminUser);
    }
  }

  /**
   * REturn a Mockup User
   * 
   * @param email
   * @param name
   * @param pwd
   * @param isAdmin admin rights if true, default rights if false
   * @throws Exception
   */
  private static User getMockupUser(String email, String name, String pwd, boolean isAdmin) throws ImejiException {
    User user = new User();
    user.setEmail(email);
    Person userPerson = user.getPerson();
    userPerson.setFamilyName(name);
    Organization org = new Organization();
    org.setName("TEST-ORGANIZATION");
    List<Organization> orgCol = new ArrayList<Organization>();
    orgCol.add(org);
    userPerson.setOrganizations(orgCol);
    user.setPerson(userPerson);
    user.setQuota(Long.MAX_VALUE);
    user.setEncryptedPassword(StringHelper.md5(pwd));
    if (isAdmin) {
      user.setGrants(AuthorizationPredefinedRoles.imejiAdministrator(user.getId().toString()));
    } else {
      user.setGrants(AuthorizationPredefinedRoles.defaultUser(user.getId().toString()));
    }
    return user;
  }

  public static void deleteTDBDirectory() {
    File jenaDBDirectory = new File(TDB_PATH);
    String jenaDBDirectoryPath = jenaDBDirectory.getAbsolutePath();

    if (jenaDBDirectory.exists()) {
      try {
        //On Windows: Deleting the TDB directory results in an error. 
        //Probable Reason: Problem in mapDB -> The already closed DB (invitationStore) is still locked by Windows (no issue on Linux)
        FileUtils.deleteDirectory(jenaDBDirectory);
        LOGGER.info("Jena DB directory " + jenaDBDirectoryPath + " deleted.");
      } catch (IOException e) {
        LOGGER.error("Jena DB directory " + jenaDBDirectoryPath + " could not be deleted.", e);
      }
    } else {
      LOGGER.info("Jena DB directory " + jenaDBDirectoryPath + " does not exist.");
    }
  }

  public static void deleteFilesDirectory() throws IOException {
    File filesDirectory = new File(PropertyReader.getProperty("imeji.storage.path"));
    String filesDirectoryPath = filesDirectory.getAbsolutePath();

    if (filesDirectory.exists()) {
      try {
        FileUtils.deleteDirectory(filesDirectory);
        LOGGER.info("Files directory " + filesDirectoryPath + " deleted.");
      } catch (IOException e) {
        LOGGER.error("Files directory " + filesDirectoryPath + " could not be deleted.", e);
      }
    } else {
      LOGGER.info("Files directory " + filesDirectoryPath + " does not exist.");
    }
  }

  public static void deleteTempDirectory() {
    File tempDirectory = TempFileUtil.getTempDirectory();
    if (tempDirectory != null) {
      String tempDirectoryPath = tempDirectory.getAbsolutePath();

      if (tempDirectory.exists()) {
        try {
          FileUtils.deleteDirectory(tempDirectory);
          LOGGER.info("Temp directory " + tempDirectoryPath + " deleted.");
        } catch (IOException e) {
          LOGGER.error("Temp directory " + tempDirectoryPath + " could not be deleted.", e);
        }
      } else {
        LOGGER.info("Temp directory " + tempDirectoryPath + " does not exist.");
      }
    }
  }

}
