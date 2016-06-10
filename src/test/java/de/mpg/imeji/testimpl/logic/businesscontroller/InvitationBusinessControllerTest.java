package de.mpg.imeji.testimpl.logic.businesscontroller;

import java.util.List;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.Imeji;
import de.mpg.imeji.logic.auth.util.AuthUtil;
import de.mpg.imeji.logic.collaboration.invitation.Invitation;
import de.mpg.imeji.logic.collaboration.invitation.InvitationBusinessController;
import de.mpg.imeji.logic.collaboration.share.ShareBusinessController;
import de.mpg.imeji.logic.collaboration.share.ShareBusinessController.ShareRoles;
import de.mpg.imeji.logic.controller.resource.UserController;
import de.mpg.imeji.logic.controller.resource.UserController.USER_TYPE;
import de.mpg.imeji.logic.controller.util.ImejiFactory;
import de.mpg.imeji.logic.keyValueStore.KeyValueStoreBusinessController;
import de.mpg.imeji.logic.vo.User;
import de.mpg.imeji.test.logic.controller.ControllerTest;

/**
 * Unit test for {@link InvitationBusinessController}
 * 
 * @author bastiens
 *
 */
public class InvitationBusinessControllerTest extends ControllerTest {
  private InvitationBusinessController invitationBC = new InvitationBusinessController();
  private static final Logger LOGGER = Logger.getLogger(InvitationBusinessControllerTest.class);
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
    KeyValueStoreBusinessController.resetAllStores();
  }

  /**
   * Invite an unknown user, create the user, check that the user got the roles from the invitation
   * 
   * @throws ImejiException
   */
  @Test
  public void inviteAndConsume() throws ImejiException {
    List<String> roles =
        ShareBusinessController.rolesAsList(ShareRoles.READ, ShareRoles.EDIT, ShareRoles.CREATE);
    Invitation invitation = new Invitation(UNKNOWN_EMAIL, collection.getId().toString(), roles);
    invitationBC.invite(invitation);
    UserController userController = new UserController(Imeji.adminUser);
    userController.create(getRegisteredUser(), USER_TYPE.DEFAULT);
    User user = userController.retrieve(UNKNOWN_EMAIL);
    Assert.assertTrue(AuthUtil.staticAuth().read(user, collection));
    Assert.assertTrue(AuthUtil.staticAuth().update(user, collection));
    Assert.assertTrue(AuthUtil.staticAuth().createContent(user, collection));
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
    List<String> roles =
        ShareBusinessController.rolesAsList(ShareRoles.READ, ShareRoles.EDIT, ShareRoles.CREATE);
    Invitation invitation = new Invitation(UNKNOWN_EMAIL, collection.getId().toString(), roles);
    invitationBC.invite(invitation);
    List<Invitation> invitationsBefore = invitationBC.retrieveInvitationOfUser(UNKNOWN_EMAIL);
    KeyValueStoreBusinessController.stopAllStores();
    KeyValueStoreBusinessController.startAllStores();
    invitationBC = new InvitationBusinessController();
    List<Invitation> invitations = invitationBC.retrieveInvitationOfUser(UNKNOWN_EMAIL);
    // Check the invitation is here
    Assert.assertEquals(invitationsBefore.size(), invitations.size());
    Assert.assertTrue(invitations.size() > 0);
  }

  @Test
  public void getAllinvitationsOfUser() throws ImejiException {
    // create many invitations for different object for one user
    List<String> roles =
        ShareBusinessController.rolesAsList(ShareRoles.READ, ShareRoles.EDIT, ShareRoles.CREATE);
    int numberOfInvitations = 15;
    for (int i = 0; i < numberOfInvitations; i++) {
      Invitation invitation =
          new Invitation(UNKNOWN_EMAIL, collection.getId().toString() + i, roles);
      invitationBC.invite(invitation);
    }
    List<Invitation> invitations = invitationBC.retrieveInvitationOfUser(UNKNOWN_EMAIL);
    Assert.assertEquals(numberOfInvitations, invitations.size());

    // Re-invite the user to the same objects, + one new objects -> allinvitations shoud return
    // numberOfInvitations +1
    for (int i = 0; i < numberOfInvitations + 1; i++) {
      Invitation invitation =
          new Invitation(UNKNOWN_EMAIL, collection.getId().toString() + i, roles);
      invitationBC.invite(invitation);
    }
    invitations = invitationBC.retrieveInvitationOfUser(UNKNOWN_EMAIL);
    Assert.assertEquals(numberOfInvitations + 1, invitations.size());
  }

  @Test
  public void getAllInvitationsOfObject() throws ImejiException {
    // create many invitations for one object
    List<String> roles =
        ShareBusinessController.rolesAsList(ShareRoles.READ, ShareRoles.EDIT, ShareRoles.CREATE);
    int numberOfInvitations = 15;
    for (int i = 0; i < numberOfInvitations; i++) {
      Invitation invitation =
          new Invitation(i + UNKNOWN_EMAIL, collection.getId().toString(), roles);
      invitationBC.invite(invitation);
    }
    List<Invitation> invitations =
        invitationBC.retrieveInvitationsOfObject(collection.getId().toString());
    Assert.assertEquals(numberOfInvitations, invitations.size());
    // Re-send same invitations for one object
    for (int i = 0; i < numberOfInvitations; i++) {
      Invitation invitation =
          new Invitation(i + UNKNOWN_EMAIL, collection.getId().toString(), roles);
      invitationBC.invite(invitation);
    }
    invitations = invitationBC.retrieveInvitationsOfObject(collection.getId().toString());
    Assert.assertEquals(numberOfInvitations, invitations.size());
  }


  private User getRegisteredUser() {
    User user = new User();
    user.setEmail(UNKNOWN_EMAIL);
    user.setPerson(ImejiFactory.newPerson("Unknown", "person", "somewhere"));
    return user;
  }



}
