package util;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import com.hp.hpl.jena.tdb.TDB;
import com.hp.hpl.jena.tdb.TDBFactory;
import com.hp.hpl.jena.tdb.base.block.FileMode;
import com.hp.hpl.jena.tdb.base.file.Location;
import com.hp.hpl.jena.tdb.sys.SystemTDB;
import com.hp.hpl.jena.tdb.sys.TDBMaker;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.Imeji;
import de.mpg.imeji.logic.ImejiInitializer;
import de.mpg.imeji.logic.config.util.PropertyReader;
import de.mpg.imeji.logic.keyValueStore.KeyValueStoreBusinessController;
import de.mpg.imeji.logic.search.elasticsearch.ElasticInitializer;
import de.mpg.imeji.logic.search.elasticsearch.ElasticService;
import de.mpg.imeji.logic.security.authorization.AuthorizationPredefinedRoles;
import de.mpg.imeji.logic.user.controller.UserBusinessController;
import de.mpg.imeji.logic.user.controller.UserBusinessController.USER_TYPE;
import de.mpg.imeji.logic.util.StringHelper;
import de.mpg.imeji.logic.vo.Organization;
import de.mpg.imeji.logic.vo.Person;
import de.mpg.imeji.logic.vo.User;

/*
 * 
 * CDDL HEADER START
 * 
 * The contents of this file are subject to the terms of the Common Development and Distribution
 * License, Version 1.0 only (the "License"). You may not use this file except in compliance with
 * the License.
 * 
 * You can obtain a copy of the license at license/ESCIDOC.LICENSE or http://www.escidoc.de/license.
 * See the License for the specific language governing permissions and limitations under the
 * License.
 * 
 * When distributing Covered Code, include this CDDL HEADER in each file and include the License
 * file at license/ESCIDOC.LICENSE. If applicable, add the following below this CDDL HEADER, with
 * the fields enclosed by brackets "[]" replaced with your own identifying information: Portions
 * Copyright [yyyy] [name of copyright owner]
 * 
 * CDDL HEADER END
 */
/*
 * Copyright 2006-2007 Fachinformationszentrum Karlsruhe Gesellschaft für
 * wissenschaftlich-technische Information mbH and Max-Planck- Gesellschaft zur Förderung der
 * Wissenschaft e.V. All rights reserved. Use is subject to license terms.
 */
/**
 * Utility class to use Jena in the unit test
 * 
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public class JenaUtil {
  private static Logger LOGGER = Logger.getLogger(JenaUtil.class);
  public static User testUser;
  public static User testUser2;
  public static String TEST_USER_EMAIL = "test@imeji.org";
  public static String TEST_USER_EMAIL_2 = "test2@imeji.org";
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
    KeyValueStoreBusinessController.resetAndStopAllStores();
    Imeji.getExecutor().shutdownNow();
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
    new UserBusinessController().reindex(ElasticService.DATA_ALIAS);
    testUser = getMockupUser(TEST_USER_EMAIL, TEST_USER_NAME, TEST_USER_PWD);
    testUser2 = getMockupUser(TEST_USER_EMAIL_2, TEST_USER_NAME, TEST_USER_PWD);
    testUser = createUser(testUser);
    testUser2 = createUser(testUser2);
  }


  private static User createUser(User u) throws ImejiException {
    UserBusinessController c = new UserBusinessController();
    try {
      return c.create(u, USER_TYPE.DEFAULT);
    } catch (Exception e) {
      LOGGER.info(u.getEmail() + " already exists. Must not be created");
      return c.retrieve(u.getEmail(), Imeji.adminUser);
    }
  }

  /**
   * REturn a Mockup User with default rights
   * 
   * @param email
   * @param name
   * @param pwd
   * @throws Exception
   */
  private static User getMockupUser(String email, String name, String pwd) throws ImejiException {
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
    user.setEncryptedPassword(StringHelper.convertToMD5(pwd));
    user.setGrants(AuthorizationPredefinedRoles.defaultUser(user.getId().toString()));
    return user;
  }

  private static void deleteTDBDirectory() throws IOException {
    File f = new File(TDB_PATH);
    if (f.exists()) {
      LOGGER.info(f.getAbsolutePath() + " deleted: " + FileUtils.deleteQuietly(f));
    }
  }
}
