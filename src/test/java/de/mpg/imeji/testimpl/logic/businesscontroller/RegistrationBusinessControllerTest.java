package de.mpg.imeji.testimpl.logic.businesscontroller;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.exceptions.NotFoundException;
import de.mpg.imeji.exceptions.UnprocessableError;
import de.mpg.imeji.logic.config.Imeji;
import de.mpg.imeji.logic.model.Grant;
import de.mpg.imeji.logic.model.Grant.GrantType;
import de.mpg.imeji.logic.model.User;
import de.mpg.imeji.logic.model.factory.ImejiFactory;
import de.mpg.imeji.logic.security.authorization.AuthorizationPredefinedRoles;
import de.mpg.imeji.logic.security.registration.Registration;
import de.mpg.imeji.logic.security.registration.RegistrationService;
import de.mpg.imeji.logic.security.sharing.ShareService.ShareRoles;
import de.mpg.imeji.logic.security.sharing.invitation.Invitation;
import de.mpg.imeji.logic.security.sharing.invitation.InvitationService;
import de.mpg.imeji.logic.security.user.UserService;
import de.mpg.imeji.test.logic.service.SuperServiceTest;

public class RegistrationBusinessControllerTest extends SuperServiceTest {

  private RegistrationService registrationBC = new RegistrationService();
  private static final Logger LOGGER = LogManager.getLogger(RegistrationBusinessControllerTest.class);
  private Grant defaultImejiGrant = new Grant(GrantType.EDIT, AuthorizationPredefinedRoles.IMEJI_GLOBAL_URI);

  /**
   * user@domain.org allowed for domain.org
   * 
   * @throws Exception
   */
  @Test
  public void registerAllowedEmail() throws Exception {
    Imeji.CONFIG.setRegistrationWhiteList("domain.org");
    User user = new User();
    user.setEmail("user@domain.org");
    user.setPerson(ImejiFactory.newPerson("family", "given", "org"));
    Registration registration = registrationBC.register(user);
    registrationBC.activate(registration);
    user = new UserService().retrieve(user.getEmail(), Imeji.adminUser);
    assertTrue(user.isActive());
    assertTrue(user.getGrants().contains(defaultImejiGrant.toGrantString()));

  }

  /**
   * user@subdomain.domain.org allowed for domain.org
   * 
   * @throws Exception
   */
  @Test
  public void registerAllowedEmail2() throws Exception {
    Imeji.CONFIG.setRegistrationWhiteList("domain.org");
    User user = new User();
    user.setEmail("user@subdomain.domain.org");
    user.setPerson(ImejiFactory.newPerson("family", "given", "org"));
    Registration registration = registrationBC.register(user);
    registrationBC.activate(registration);
    user = new UserService().retrieve(user.getEmail(), Imeji.adminUser);
    assertTrue(user.isActive());
    assertTrue(user.getGrants().contains(defaultImejiGrant.toGrantString()));
  }

  /**
   * user@domain.com not allowed for subdomain.domain.org,example.org,domain.com
   * 
   * @throws Exception
   */
  @Test
  public void registerAllowedEmail3() throws Exception {
    Imeji.CONFIG.setRegistrationWhiteList("subdomain.domain.org,example.org,domain.com");
    User user = new User();
    user.setEmail("user@domain.com");
    user.setPerson(ImejiFactory.newPerson("family", "given", "org"));
    Registration registration = registrationBC.register(user);
    registrationBC.activate(registration);
    user = new UserService().retrieve(user.getEmail(), Imeji.adminUser);
    assertTrue(user.isActive());
    assertTrue(user.getGrants().contains(defaultImejiGrant.toGrantString()));
  }

  /**
   * user@example.org not allowed for domain.org
   * 
   * @throws Exception
   */
  @Test
  public void registerNotAllowedEmail() throws Exception {
    Imeji.CONFIG.setRegistrationWhiteList("domain.org");
    User user = new User();
    user.setEmail("user@example.org");
    user.setPerson(ImejiFactory.newPerson("family", "given", "org"));
    Registration registration = registrationBC.register(user);
    registrationBC.activate(registration);
    user = new UserService().retrieve(user.getEmail(), Imeji.adminUser);
    assertTrue(user.isActive());
    assertFalse(user.getGrants().contains(defaultImejiGrant.toGrantString()));
  }

  /**
   * user@domain.org not allowed for subdomain.domain.org
   * 
   * @throws Exception
   */
  @Test
  public void registerNotAllowedEmail2() throws Exception {
    Imeji.CONFIG.setRegistrationWhiteList("subdomain.domain.org");
    User user = new User();
    user.setEmail("user2@domain.org");
    user.setPerson(ImejiFactory.newPerson("family", "given", "org"));
    Registration registration = registrationBC.register(user);
    registrationBC.activate(registration);
    user = new UserService().retrieve(user.getEmail(), Imeji.adminUser);
    assertTrue(user.isActive());
    assertFalse(user.getGrants().contains(defaultImejiGrant.toGrantString()));
  }

  /**
   * user@domain.org not allowed for subdomain.domain.org,example.org,domain.com
   * 
   * @throws Exception
   */
  @Test
  public void registerNotAllowedEmail3() throws Exception {
    Imeji.CONFIG.setRegistrationWhiteList("subdomain.domain.org,example.org,domain.com");
    User user = new User();
    user.setEmail("user3@domain.org");
    user.setPerson(ImejiFactory.newPerson("family", "given", "org"));
    Registration registration = registrationBC.register(user);
    registrationBC.activate(registration);
    user = new UserService().retrieve(user.getEmail(), Imeji.adminUser);
    assertTrue(user.isActive());
    assertFalse(user.getGrants().contains(defaultImejiGrant.toGrantString()));
  }

  @Test
  @Ignore
  public void registerAfterInvitation() throws Exception {
    User user = new User();
    user.setEmail("invited-user@example.org");
    user.setPerson(ImejiFactory.newPerson("test", "user", "orga"));
    // allow all users to register
    Imeji.CONFIG.setRegistrationWhiteList("");
    // create a collection
    createCollection();
    // invite the user to
    InvitationService invitationBusinessController = new InvitationService();
    invitationBusinessController.invite(new Invitation(user.getEmail(), collectionBasic.getId().toString(), ShareRoles.READ.name()));
    // Register
    Registration registration = registrationBC.register(user);
    Assert.assertNotNull(registrationBC.retrieveByToken(registration.getToken()));
    registrationBC.activate(registration);
    // check if the user exists
    Assert.assertNotNull(new UserService().retrieve(user.getEmail(), Imeji.adminUser));
  }

  @Test
  public void register() throws Exception {
    User user = new User();
    user.setEmail("register-user@example.org");
    user.setPerson(ImejiFactory.newPerson("test", "user", "orga"));
    // allow all users to register
    Imeji.CONFIG.setRegistrationWhiteList("");
    // Register
    Registration registration = registrationBC.register(user);
    Assert.assertNotNull(registrationBC.retrieveByToken(registration.getToken()));
    registrationBC.activate(registration);
    // check if the user exists
    Assert.assertNotNull(new UserService().retrieve(user.getEmail(), Imeji.adminUser));
  }

  @Test
  public void clearExpiredRegistration() throws Exception {
    try {
      User user = new User();
      user.setEmail("clear-expired-registration-user@example.org");
      user.setPerson(ImejiFactory.newPerson("test", "user", "orga"));
      // allow all users to register
      Imeji.CONFIG.setRegistrationWhiteList("");
      Imeji.CONFIG.setRegistrationTokenExpiry("0");
      // Register
      Registration registration = registrationBC.register(user);
      Assert.assertNotNull(registrationBC.retrieveByToken(registration.getToken()));
      registrationBC.deleteExpiredRegistration();
      try {
        registrationBC.retrieveByToken(registration.getToken());
        Assert.fail("The registration should be removed");
      } catch (NotFoundException e) {
        // OK
      }
    } finally {
      Imeji.CONFIG.setRegistrationTokenExpiry("1");
    }
  }

  @Test
  public void retrieveAllRegistrations() throws Exception {
    registrationBC.removeAll();
    User user1 = new User();
    user1.setEmail("retrieve-all-1@example.org");
    user1.setPerson(ImejiFactory.newPerson("test", "user", "orga"));
    User user2 = new User();
    user2.setEmail("retrieve-all-2@example.org");
    user2.setPerson(ImejiFactory.newPerson("test", "user", "orga"));
    registrationBC.register(user1);
    registrationBC.register(user2);
    Assert.assertEquals(registrationBC.retrieveAll().size(), 2);
  }

  /**
   * Test when the admin activate a user manually from the users interface
   * 
   * @throws Exception
   */
  @Test
  public void activateByEmail() throws Exception {
    User user = new User();
    user.setEmail("activate-inactive-user@example.org");
    user.setPerson(ImejiFactory.newPerson("test", "user", "orga"));
    // allow all users to register
    Imeji.CONFIG.setRegistrationWhiteList("");
    // Register
    registrationBC.register(user);
    registrationBC.activate(registrationBC.retrieveByEmail(user.getEmail()));
  }

  /**
   * Register with an email which have an expired registration (still not removed by clear job)
   * <br/>
   * Sess issue: https://github.com/MPDL-Innovations/spot/issues/534
   * 
   * @throws ImejiException
   */
  @Test
  public void registerAfterExpiration() throws ImejiException {
    User user = new User();
    user.setEmail("clear-expired-registration-user@example.org");
    user.setPerson(ImejiFactory.newPerson("test", "user", "orga"));
    // allow all users to register
    Imeji.CONFIG.setRegistrationWhiteList("");
    Imeji.CONFIG.setRegistrationTokenExpiry("0");
    // Register
    Registration registration = registrationBC.register(user);
    Assert.assertNotNull(registrationBC.retrieveByToken(registration.getToken()));
    try {
      registrationBC.activate(registration);
      Assert.fail("The registration shouldn't be activable since expired");
    } catch (UnprocessableError e) {
      // registration is expired -> ok
    }
    registration = registrationBC.register(user);
    Assert.assertNotNull(registrationBC.retrieveByToken(registration.getToken()));
    Imeji.CONFIG.setRegistrationTokenExpiry("1");
    registrationBC.activate(registration);
    // check if the user exists
    Assert.assertNotNull(new UserService().retrieve(user.getEmail(), Imeji.adminUser));
  }
}
