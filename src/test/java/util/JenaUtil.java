package util;

import java.io.File;
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

import de.mpg.imeji.logic.Imeji;
import de.mpg.imeji.logic.auth.authorization.AuthorizationPredefinedRoles;
import de.mpg.imeji.logic.controller.UserController;
import de.mpg.imeji.logic.controller.UserController.USER_TYPE;
import de.mpg.imeji.logic.search.elasticsearch.ElasticService;
import de.mpg.imeji.logic.util.StringHelper;
import de.mpg.imeji.logic.vo.Organization;
import de.mpg.imeji.logic.vo.Person;
import de.mpg.imeji.logic.vo.User;
import de.mpg.imeji.presentation.beans.PropertyBean;
import de.mpg.imeji.presentation.util.PropertyReader;

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
      // Init PropertyBean
      new PropertyBean();
      // Read tdb location
      TDB_PATH = PropertyReader.getProperty("imeji.tdb.path");
      // remove old Database
      deleteTDBDirectory();
      // Set Filemode: important to be able to delete TDB directory by
      // closing Jena
      SystemTDB.setFileMode(FileMode.direct);
      // Create new tdb
      Imeji.init(TDB_PATH);
      initTestUser();
    } catch (Exception e) {
      throw new RuntimeException("Error initialiting Jena for testing: ", e);
    }
  }


  public static void closeJena() throws InterruptedException {
    // Imeji.executor.shutdownNow();
    LOGGER.info("Closing Jena:");
    TDB.sync(Imeji.dataset);
    LOGGER.info("Jena Sync done! ");
    TDBFactory.reset();
    LOGGER.info("Reset internal state, releasing all datasets done! ");
    Imeji.dataset.close();
    LOGGER.info("Dataset closed!");
    TDB.closedown();
    TDBMaker.releaseLocation(new Location(TDB_PATH));
    LOGGER.info("TDB Location released!");
    deleteTDBDirectory();
    ElasticService.reset();
  }

  private static void initTestUser() throws Exception {
    testUser = getMockupUser(TEST_USER_EMAIL, TEST_USER_NAME, TEST_USER_PWD);
    testUser2 = getMockupUser(TEST_USER_EMAIL_2, TEST_USER_NAME, TEST_USER_PWD);
    createUser(testUser);
    createUser(testUser2);
  }


  private static void createUser(User u) {
    try {
      UserController c = new UserController(Imeji.adminUser);
      c.create(u, USER_TYPE.DEFAULT);
    } catch (Exception e) {
      LOGGER.info(u.getEmail() + " already exists. Must not be created");
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
  private static User getMockupUser(String email, String name, String pwd) throws Exception {
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

  private static void deleteTDBDirectory() {
    File f = new File(TDB_PATH);
    if (f.exists()) {
      LOGGER.info("TDB directory deleted: " + FileUtils.deleteQuietly(f));
    }
  }
}
