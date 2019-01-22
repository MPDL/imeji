package de.mpg.imeji.testimpl.logic.businesscontroller;

import java.util.List;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.config.Imeji;
import de.mpg.imeji.logic.db.keyValue.KeyValueStoreService;
import de.mpg.imeji.logic.model.User;
import de.mpg.imeji.logic.model.factory.ImejiFactory;
import de.mpg.imeji.logic.security.authorization.util.SecurityUtil;
import de.mpg.imeji.logic.security.sharing.ShareService;
import de.mpg.imeji.logic.security.sharing.ShareService.ShareRoles;
import de.mpg.imeji.logic.security.sharing.invitation.Invitation;
import de.mpg.imeji.logic.security.sharing.invitation.InvitationService;
import de.mpg.imeji.logic.security.user.UserService;
import de.mpg.imeji.logic.security.user.UserService.USER_TYPE;
import de.mpg.imeji.test.logic.service.SuperServiceTest;

/**
 * Unit test for {@link InvitationService}
 * 
 * @author bastiens
 *
 */
public class InvitationBusinessControllerTest extends SuperServiceTest {
  private InvitationService invitationBC = new InvitationService();
  private static final Logger LOGGER = LogManager.getLogger(InvitationBusinessControllerTest.class);
  private static final String UNKNOWN_EMAIL = "unknown@imeji.org";

  @BeforeClass
  public static void specificSetup() {
    try {
      createCollection();
      createItemWithFile();
    } catch (ImejiException e) {
      LOGGER.error("Error initializing collection or item", e);
    }
  }

  @Before
  public void resetStore() {
    KeyValueStoreService.resetAllStores();
  }

  /**
   * Invite an unknown user, create the user, check that the user got the roles from the invitation
   * 
   * @throws ImejiException
   */
  @Test
  public void inviteAndConsume() throws ImejiException {
    List<String> roles = ShareService.rolesAsList(ShareRoles.READ, ShareRoles.EDIT, ShareRoles.ADMIN);
    Invitation invitation = new Invitation(UNKNOWN_EMAIL, collectionBasic.getId().toString(), ShareRoles.ADMIN.name());
    invitationBC.invite(invitation);
    UserService userController = new UserService();
    userController.create(getRegisteredUser(), USER_TYPE.DEFAULT);
    User user = userController.retrieve(UNKNOWN_EMAIL, Imeji.adminUser);
    Assert.assertTrue(SecurityUtil.authorization().read(user, collectionBasic));
    Assert.assertTrue(SecurityUtil.authorization().update(user, collectionBasic));
    Assert.assertTrue(SecurityUtil.authorization().administrate(user, collectionBasic));
    // Check the invitation has been deleted
    Assert.assertEquals(0, invitationBC.retrieveInvitationOfUser(UNKNOWN_EMAIL).size());
  }

  /**
   * Check that the invitation is still there after a restart of a store
   * 
   * @throws ImejiException
   */
  @Test
  public void inviteStopAndStartStore() throws ImejiException {
    List<String> roles = ShareService.rolesAsList(ShareRoles.READ, ShareRoles.EDIT, ShareRoles.ADMIN);
    Invitation invitation = new Invitation(UNKNOWN_EMAIL, collectionBasic.getId().toString(), ShareRoles.ADMIN.name());
    invitationBC.invite(invitation);
    List<Invitation> invitationsBefore = invitationBC.retrieveInvitationOfUser(UNKNOWN_EMAIL);
    KeyValueStoreService.stopAllStores();
    KeyValueStoreService.startAllStores();
    invitationBC = new InvitationService();
    List<Invitation> invitations = invitationBC.retrieveInvitationOfUser(UNKNOWN_EMAIL);
    // Check the invitation is here
    Assert.assertEquals(invitationsBefore.size(), invitations.size());
    Assert.assertTrue(invitations.size() > 0);
  }

  @Test
  public void getAllinvitationsOfUser() throws ImejiException {
    // create many invitations for different object for one user
    List<String> roles = ShareService.rolesAsList(ShareRoles.READ, ShareRoles.EDIT, ShareRoles.ADMIN);
    int numberOfInvitations = 15;
    for (int i = 0; i < numberOfInvitations; i++) {
      Invitation invitation = new Invitation(UNKNOWN_EMAIL, collectionBasic.getId().toString() + i, ShareRoles.ADMIN.name());
      invitationBC.invite(invitation);
    }
    List<Invitation> invitations = invitationBC.retrieveInvitationOfUser(UNKNOWN_EMAIL);
    Assert.assertEquals(numberOfInvitations, invitations.size());

    // Re-invite the user to the same objects, + one new objects -> allinvitations
    // should return
    // numberOfInvitations +1
    for (int i = 0; i < numberOfInvitations + 1; i++) {
      Invitation invitation = new Invitation(UNKNOWN_EMAIL, collectionBasic.getId().toString() + i, ShareRoles.ADMIN.name());
      invitationBC.invite(invitation);
    }
    invitations = invitationBC.retrieveInvitationOfUser(UNKNOWN_EMAIL);
    Assert.assertEquals(numberOfInvitations + 1, invitations.size());
  }

  @Test
  public void getAllInvitationsOfObject() throws ImejiException {
    // create many invitations for one object
    List<String> roles = ShareService.rolesAsList(ShareRoles.READ, ShareRoles.EDIT, ShareRoles.ADMIN);
    int numberOfInvitations = 15;
    for (int i = 0; i < numberOfInvitations; i++) {
      Invitation invitation = new Invitation(i + UNKNOWN_EMAIL, collectionBasic.getId().toString(), ShareRoles.ADMIN.name());
      invitationBC.invite(invitation);
    }
    List<Invitation> invitations = invitationBC.retrieveInvitationsOfObject(collectionBasic.getId().toString());
    Assert.assertEquals(numberOfInvitations, invitations.size());
    // Re-send same invitations for one object
    for (int i = 0; i < numberOfInvitations; i++) {
      Invitation invitation = new Invitation(i + UNKNOWN_EMAIL, collectionBasic.getId().toString(), ShareRoles.ADMIN.name());
      invitationBC.invite(invitation);
    }
    invitations = invitationBC.retrieveInvitationsOfObject(collectionBasic.getId().toString());
    Assert.assertEquals(numberOfInvitations, invitations.size());
  }

  private User getRegisteredUser() {
    User user = new User();
    user.setEmail(UNKNOWN_EMAIL);
    user.setPerson(ImejiFactory.newPerson("Unknown", "person", "somewhere"));
    return user;
  }

}
