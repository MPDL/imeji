package de.mpg.imeji.logic.events.subscription;

import java.util.concurrent.Callable;

import de.mpg.imeji.logic.events.Message;
import de.mpg.imeji.logic.events.Message.MessageType;

/**
 * A Subscriber to a particular MessageType is called as soon as one message with the same
 * MessageType is added to the queue
 * 
 * @author saquet
 *
 */
public abstract class Subscriber implements Callable<Integer> {
  private final MessageType[] messageType;
  private Message message;

  public Subscriber(MessageType... messageType) {
    this.messageType = messageType;
  }

  /**
   * Send the {@link Message} to the Subscriber, which can then be called
   */
  public void send(Message message) {
    this.message = message;
  }

  public Message getMessage() {
    return message;
  }

  public MessageType[] getMessageType() {
    return messageType;
  }

}
