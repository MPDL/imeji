package de.mpg.imeji.testimpl.logic.auth;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import de.mpg.imeji.exceptions.AuthenticationError;
import de.mpg.imeji.exceptions.InactiveAuthenticationError;
import de.mpg.imeji.logic.security.util.PasswordGenerator;
import de.mpg.imeji.logic.user.authentication.Authentication;
import de.mpg.imeji.logic.user.authentication.impl.DefaultAuthentication;
import de.mpg.imeji.logic.user.controller.UserBusinessController;
import de.mpg.imeji.logic.util.StringHelper;
import de.mpg.imeji.logic.vo.User;
import de.mpg.imeji.logic.vo.util.ImejiFactory;
import util.JenaUtil;

/**
 * Test the simple {@link Authentication}
 * 
 * @author saquet
 *
 */
public class DefaultAuthenticationTest {

  @BeforeClass
  public static void setup() {
    JenaUtil.initJena();
  }

  @AfterClass
  public static void tearDown() throws Exception {
    JenaUtil.closeJena();
  }

  @Test
  public void testLoginWrongPassword() {
    Authentication simpAuth =
        new DefaultAuthentication(JenaUtil.TEST_USER_EMAIL, JenaUtil.TEST_USER_PWD + "a");
    User user = null;
    try {
      user = simpAuth.doLogin();
    } catch (AuthenticationError e) {
      user = null;

    }
    Assert.assertNull(user);
  }

  @Test
  public void testUserNotExist() {
    Authentication simpAuth =
        new DefaultAuthentication("abdc" + JenaUtil.TEST_USER_EMAIL, JenaUtil.TEST_USER_PWD);
    User user = null;
    try {
      user = simpAuth.doLogin();
    } catch (AuthenticationError e) {

      user = null;
    }
    Assert.assertNull(user);
  }

  @Test
  public void testDoLogin() {
    // test if login if working for test user
    Authentication simpAuth =
        new DefaultAuthentication(JenaUtil.TEST_USER_EMAIL, JenaUtil.TEST_USER_PWD);
    User user = null;
    try {
      user = simpAuth.doLogin();
    } catch (AuthenticationError e) {
      user = null;
    }
    Assert.assertNotNull(user);
  }

  @Test
  public void testInactiveUser() throws Exception {
    UserBusinessController controller = new UserBusinessController();
    PasswordGenerator generator = new PasswordGenerator();
    String password = generator.generatePassword();
    User user = new User();
    user.setEmail("inactive_user@unit-test-imeji.org");
    user.setEncryptedPassword(StringHelper.convertToMD5(password));
    user.setPerson(ImejiFactory.newPerson("fam", "giv", "org"));
    user = controller.create(user, UserBusinessController.USER_TYPE.INACTIVE);
    Authentication simpAuth = new DefaultAuthentication(user.getEmail(), password);
    try {
      user = simpAuth.doLogin();
      Assert.fail("Not active user should not log in");
    } catch (InactiveAuthenticationError e) {
      // OK
    } catch (AuthenticationError e) {
      Assert.fail("Wrong Exception type, should be " + InactiveAuthenticationError.class);
    } catch (Exception e) {
      Assert.fail("Error log in user: " + e.getMessage());
    }
  }
}
