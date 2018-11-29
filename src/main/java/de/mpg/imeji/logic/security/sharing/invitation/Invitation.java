package de.mpg.imeji.logic.security.sharing.invitation;

import java.io.Serializable;

/**
 * An invitation sent by a user to another user for a Object with some roles. Invitation ids follow
 * the pattern: invitation:{inviteeEmail}:{objectUri}
 *
 * @author bastiens
 *
 */
public final class Invitation implements Serializable {
  private static final long serialVersionUID = 658949804870284864L;
  private final String id;
  private final String inviteeEmail;
  private final String objectUri;
  private final String role;

  /**
   * Create a new Invitation
   *
   * @param invitor
   * @param invitee
   * @param objectUri
   * @param roles
   */
  public Invitation(String inviteeEmail, String objectUri, String role) {
    this.inviteeEmail = inviteeEmail;
    this.objectUri = objectUri;
    this.role = role;
    this.id = inviteeEmail + ":" + objectUri;
  }

  /**
   * @return the invitee
   */
  public String getInviteeEmail() {
    return inviteeEmail;
  }

  /**
   * @return the objectUri
   */
  public String getObjectUri() {
    return objectUri;
  }

  /**
   * @return the role
   */
  public String getRole() {
    return role;
  }

  /**
   * @return the id
   */
  public String getId() {
    return id;
  }

}
