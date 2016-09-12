package de.mpg.imeji.logic.user.collaboration.invitation;

import java.io.Serializable;
import java.util.List;

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
  private final List<String> roles;

  /**
   * Create a new Invitation
   *
   * @param invitor
   * @param invitee
   * @param objectUri
   * @param roles
   */
  public Invitation(String inviteeEmail, String objectUri, List<String> roles) {
    this.inviteeEmail = inviteeEmail;
    this.objectUri = objectUri;
    this.roles = roles;
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
   * @return the roles
   */
  public List<String> getRoles() {
    return roles;
  }

  /**
   * @return the id
   */
  public String getId() {
    return id;
  }

}
