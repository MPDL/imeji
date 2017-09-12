package de.mpg.imeji.logic.messaging;

import java.io.Serializable;
import java.util.Map;

import de.mpg.imeji.logic.util.IdentifierUtil;

/**
 * An imeji Message
 * 
 * @author saquet
 *
 */
public class Message implements Serializable {
  private static final long serialVersionUID = 7333376188527222587L;

  public enum MessageType {
    UPLOAD_FILE, CHANGE_FILE, MOVE_ITEM;
  }

  private final MessageType type;
  private final long time;
  private final String objectId;
  private final Map<String, String> content;
  private final String id = IdentifierUtil.newRandomId();


  /**
   * Create a new Message
   * 
   * @param type a {@link MessageType}
   * @param objecdId The Id of the object
   * @param content The specific content to this message
   */
  public Message(MessageType type, String objecdId, Map<String, String> content) {
    this.type = type;
    this.objectId = objecdId;
    this.content = content;
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

  /**
   * @return the content
   */
  public Map<String, String> getContent() {
    return content;
  }


}
