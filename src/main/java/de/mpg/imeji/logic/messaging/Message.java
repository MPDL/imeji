package de.mpg.imeji.logic.messaging;

import java.io.Serializable;

/**
 * An imeji Message
 * 
 * @author saquet
 *
 */
public class Message implements Serializable {
  private static final long serialVersionUID = 7333376188527222587L;

  public enum MESSAGE_TYPES {
    UPLOAD_FILE, CHANGE_FILE;
  }

  private final MESSAGE_TYPES type;
  private final long time;
  private final String objectId;
  private final String content;

  /**
   * Create a new Message. Ensure not to create one message for the same object at the same time
   * 
   * @param type
   * @param objecdId
   * @param content
   * @return
   */
  public static synchronized Message mewMessage(MESSAGE_TYPES type, String objecdId,
      String content) {
    return new Message(type, objecdId, content);
  }

  /**
   * Create a new Message
   * 
   * @param type a {@link MESSAGE_TYPES}
   * @param objecdId The Id of the object
   * @param content The specific content to this message
   */
  private Message(MESSAGE_TYPES type, String objecdId, String content) {
    this.type = type;
    this.objectId = objecdId;
    this.content = content;
    this.time = System.currentTimeMillis();
  }

  public String getMessageId() {
    return objectId + ":" + time;
  }

  /**
   * @return the type
   */
  public MESSAGE_TYPES getType() {
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
  public String getContent() {
    return content;
  }


}
