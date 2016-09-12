package de.mpg.imeji.logic.user.collaboration.invitation;

import java.io.IOException;
import java.util.List;

import de.mpg.imeji.exceptions.AlreadyExistsException;
import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.exceptions.NotFoundException;
import de.mpg.imeji.logic.Imeji;
import de.mpg.imeji.logic.keyValueStore.KeyValueStoreBusinessController;
import de.mpg.imeji.logic.keyValueStore.stores.HTreeMapStore;
import de.mpg.imeji.logic.user.collaboration.share.ShareBusinessController;
import de.mpg.imeji.logic.user.controller.UserBusinessController;
import de.mpg.imeji.logic.vo.User;

/**
 * Business Controller for invitation of Users.
 *
 * @author bastiens
 *
 */
public class InvitationBusinessController {
  private final UserBusinessController userController = new UserBusinessController();
  private final ShareBusinessController shareBC = new ShareBusinessController();
  private static final KeyValueStoreBusinessController KEY_VALUE_STORE_BC =
      new KeyValueStoreBusinessController(new HTreeMapStore("invitationStore"));

  /**
   * Invite a user to collaborate for an object
   *
   * @param invitation
   * @throws ImejiException
   */
  public void invite(Invitation invitation) throws ImejiException {
    User invitee = retrieveInvitee(invitation.getInviteeEmail());
    if (invitee == null) {
      add(invitation);
    } else {
      throw new AlreadyExistsException(invitation.getInviteeEmail() + "exists already");
    }
  }

  /**
   * Check for pending invitations of this User, add grants to this users according to the
   * invitation, remove the invitations
   *
   * @param user
   * @return
   * @throws ImejiException
   */
  public User consume(User user) throws ImejiException {
    for (Invitation invitation : retrieveInvitationOfUser(user.getEmail())) {
      user = shareBC.shareToUser(Imeji.adminUser, user, invitation.getObjectUri(),
          invitation.getRoles());
      cancel(invitation.getId());
    }
    return user;
  }

  /**
   * Cancel an Invitation. (if invitation has not been already consumed)
   *
   * @param invitation
   * @throws ImejiException
   */
  public void cancel(String invitationId) throws ImejiException {
    Invitation invitation = retrieve(invitationId);
    remove(invitation);
  }

  /**
   * Retrieve an Invitation
   *
   * @param id
   * @return
   * @throws ImejiException
   */
  public Invitation retrieve(String id) throws ImejiException {
    Object invitation = KEY_VALUE_STORE_BC.get(id);
    if (invitation instanceof Invitation) {
      return (Invitation) invitation;
    }
    throw new NotFoundException("No invitation found with id: " + id);
  }

  /**
   * Retrieve all the invitation for this object id
   *
   * @param objectUri
   * @return
   * @throws ImejiException
   * @throws IOException
   * @throws ClassNotFoundException
   */
  public List<Invitation> retrieveInvitationsOfObject(String objectUri) throws ImejiException {
    return KEY_VALUE_STORE_BC.getList(".*:" + objectUri, Invitation.class);
  }

  /**
   * Return all Invitationd for this user (according to his Email)
   *
   * @param email
   * @return
   * @throws ImejiException
   */
  public List<Invitation> retrieveInvitationOfUser(String email) throws ImejiException {
    return KEY_VALUE_STORE_BC.getList(email + ":.*", Invitation.class);
  }

  /**
   * Get all pending invitations
   *
   * @return
   * @throws ImejiException
   */
  public List<Invitation> retrieveAll() throws ImejiException {
    return KEY_VALUE_STORE_BC.getList(".*", Invitation.class);
  }

  /**
   * remove all pending invitations
   *
   * @throws ImejiException
   */
  public void clear() throws ImejiException {
    for (Invitation invitation : retrieveAll()) {
      remove(invitation);
    }
  }

  /**
   * Add an Invitation to the store with relations (email->Invitation and object->invitation)
   *
   * @param invitation
   * @throws ImejiException
   */
  private void add(Invitation invitation) throws ImejiException {
    KEY_VALUE_STORE_BC.put(invitation.getId(), invitation);
  }

  /**
   * Remove an Invitation and its relation
   *
   * @param invitation
   * @throws ImejiException
   */
  private void remove(Invitation invitation) throws ImejiException {
    KEY_VALUE_STORE_BC.delete(invitation.getId());
  }

  /**
   *
   * @param user
   * @return
   */
  private User retrieveInvitee(String email) {
    try {
      return userController.retrieve(email, Imeji.adminUser);
    } catch (ImejiException e) {
      return null;
    }
  }
}
