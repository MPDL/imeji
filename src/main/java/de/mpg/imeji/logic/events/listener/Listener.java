package de.mpg.imeji.logic.events.listener;

import java.util.concurrent.Callable;

import de.mpg.imeji.logic.events.messages.Message;
import de.mpg.imeji.logic.events.messages.Message.MessageType;

/**
 * Listen for a particular MessageType is called as soon as one message with the same MessageType is
 * added to the queue
 * 
 * @author saquet
 *
 */
public abstract class Listener implements Callable<Integer> {
  private final MessageType[] messageType;
  private Message message;

  public Listener(MessageType... messageType) {
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
