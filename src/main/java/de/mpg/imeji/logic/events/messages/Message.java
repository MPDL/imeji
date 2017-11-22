package de.mpg.imeji.logic.events.messages;

import java.io.Serializable;
import java.net.URI;

import de.mpg.imeji.logic.util.IdentifierUtil;
import de.mpg.imeji.logic.util.ObjectHelper;

/**
 * An imeji Message
 * 
 * @author saquet
 *
 */
public abstract class Message implements Serializable {
  private static final long serialVersionUID = 7333376188527222587L;

  public enum MessageType {
    UPLOAD_FILE, CHANGE_FILE, MOVE_ITEM, DELETE_COLLECTION, UNSHARE, STATEMENT_CHANGED, MOVE_COLLECTION, CREATE_COLLECTION;
  }

  private final MessageType type;
  private final long time;
  private final String objectId;
  private final String id = IdentifierUtil.newRandomId();

  /**
   * Create a Message for a collection
   * 
   * @param type
   * @param collectionUri
   */
  public Message(MessageType type, URI objectUri) {
    this.type = type;
    this.objectId = ObjectHelper.getId(objectUri);
    this.time = System.currentTimeMillis();
  }

  public String getMessageId() {
    return objectId + ":" + time + ":" + id;
  }

  /**
   * @return the type
   */
  public MessageType getType() {
    return type;
  }

  /**
   * @return the time
   */
  public long getTime() {
    return time;
  }

  /**
   * @return the objectId
   */
  public String getObjectId() {
    return objectId;
  }
}
