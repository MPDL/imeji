package de.mpg.imeji.logic.messaging.subscription;

import java.util.concurrent.Callable;

import de.mpg.imeji.logic.messaging.Message;
import de.mpg.imeji.logic.messaging.Message.MessageType;

/**
 * A Subscriber to a particular MessageType is called as soon as one message with the same
 * MessageType is added to the queue
 * 
 * @author saquet
 *
 */
public abstract class Subscriber implements Callable<Integer> {
  private final MessageType messageType;
  protected Message message;

  public Subscriber(MessageType messageType) {
    this.messageType = messageType;
  }

  /**
   * Call Subscriber
   */
  public abstract void send(Message message);

  public MessageType getMessageType() {
    return messageType;
  }

}
