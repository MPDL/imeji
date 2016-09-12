package de.mpg.imeji.logic.user.collaboration.invitation;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.mpg.imeji.exceptions.ImejiException;

/**
 * A list of {@link Invitation} for one email: Many invitations might be sent to one Email address
 *
 * @author bastiens
 *
 */
public class InvitationList implements Serializable {
  private static final long serialVersionUID = -930217806338498106L;
  private final Map<String, Invitation> invitations = new HashMap<>();
  private final String id;
  private static final String INVITATION_LIST_PREFiX = "invitation_list:";

  public InvitationList(String key) {
    this.id = toId(key);
  }

  /**
   * Add the invitation to the list
   *
   * @param invitation
   * @throws ImejiException
   */
  public void add(Invitation invitation) throws ImejiException {
    invitations.put(invitation.getId(), invitation);
  }

  /**
   * Remove the Invitation from the list
   *
   * @param invitation
   */
  public void remove(Invitation invitation) {
    invitations.remove(invitation).getId();
  }

  /**
   * Return a List of all invitations of the list
   *
   * @return
   */
  public List<String> getAllInvitationsId() {
    return new ArrayList<>(invitations.keySet());
  }

  /**
   * @return the id
   */
  public String getId() {
    return id;
  }

  private String toId(String email) {
    return INVITATION_LIST_PREFiX + email;
  }



}
