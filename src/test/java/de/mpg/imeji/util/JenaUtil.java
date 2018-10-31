package de.mpg.imeji.util;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import com.hp.hpl.jena.tdb.TDB;
import com.hp.hpl.jena.tdb.TDBFactory;
import com.hp.hpl.jena.tdb.base.block.FileMode;
import com.hp.hpl.jena.tdb.base.file.Location;
import com.hp.hpl.jena.tdb.sys.SystemTDB;
import com.hp.hpl.jena.tdb.sys.TDBMaker;

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
			// remove old Database
			deleteTDBDirectory();
			deleteFilesDirectory();
			// Set Filemode: important to be able to delete TDB directory by
			// closing Jena
			SystemTDB.setFileMode(FileMode.direct);
			// Create new tdb
			ImejiInitializer.init(TDB_PATH);
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
		TDBFactory.reset();
		LOGGER.info("Reset internal state, releasing all datasets done! ");
		Imeji.dataset.close();
		LOGGER.info("Dataset closed!");
		TDB.closedown();
		TDBMaker.reset();
		TDBMaker.releaseLocation(Location.create(TDB_PATH));
		LOGGER.info("TDB Location released!");
		deleteTDBDirectory();
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
	 * @param isAdmin
	 *            admin rights if true, default rights if false
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

	private static void deleteTDBDirectory() throws IOException {
		File f = new File(TDB_PATH);
		if (f.exists()) {
			LOGGER.info(f.getAbsolutePath() + " deleted: " + FileUtils.deleteQuietly(f));
		}
	}

	private static void deleteFilesDirectory() throws IOException, URISyntaxException {
		File f = new File(PropertyReader.getProperty("imeji.storage.path"));
		if (f.exists()) {
			LOGGER.info(f.getAbsolutePath() + " deleted: " + FileUtils.deleteQuietly(f));
		}
	}
}
