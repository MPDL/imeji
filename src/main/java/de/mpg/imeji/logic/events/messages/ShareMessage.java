package de.mpg.imeji.logic.events.messages;

import java.net.URI;

import de.mpg.imeji.logic.model.User;
import de.mpg.imeji.logic.model.UserGroup;

/**
 * A Message for shared operations
 * 
 * @author saquet
 *
 */
public class ShareMessage extends Message {
  private static final long serialVersionUID = 4301962668063850355L;
  private final String email;
  private final String groupId;

  /**
   * Create a message when a collection is shared to a User
   * 
   * @param type
   * @param collection
   * @param user
   */
  public ShareMessage(MessageType type, String collectionUri, User user) {
    super(type, URI.create(collectionUri));
    this.email = user.getEmail();
    this.groupId = null;
  }

  /**
   * Create a message when a collection is shared to a User group
   * 
   * @param type
   * @param collectionUri
   * @param group
   */
  public ShareMessage(MessageType type, String collectionUri, UserGroup group) {
    super(type, URI.create(collectionUri));
    this.email = null;
    this.groupId = group.getId().toString();
  }

  public String getEmail() {
    return email;
  }

  public String getGroupId() {
    return groupId;
  }

}
