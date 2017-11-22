package de.mpg.imeji.logic.events;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.db.keyValue.KeyValueStoreService;
import de.mpg.imeji.logic.db.keyValue.stores.HTreeMapStore;
import de.mpg.imeji.logic.events.listener.ListenerService;
import de.mpg.imeji.logic.events.messages.Message;

/**
 * SErvice to manage {@link Message}
 * 
 * @author saquet
 *
 */
public class MessageService {
  private static final KeyValueStoreService QUEUE =
      new KeyValueStoreService(new HTreeMapStore("messageQueue"));
  private static Logger LOGGER = Logger.getLogger(MessageService.class);
  private final ListenerService subscriptionService = new ListenerService();


  /**
   * Register a new {@link Message}
   * 
   * @param message
   */
  public void add(Message message) {
    try {
      QUEUE.put(message.getMessageId(), message);
      subscriptionService.notifySubscribers(message);
    } catch (ImejiException e) {
      LOGGER.error("Error adding a new message", e);
    }
  }

  /**
   * Read and delete all messages for a specific object
   * 
   * @param objectId
   * @return
   */
  public List<Message> readForObject(String objectId) {
    try {
      return QUEUE.getList(objectId + ":*", Message.class);
    } catch (ImejiException e) {
      LOGGER.error("Error reading message queue for object " + objectId, e);
    }
    return new ArrayList<>();
  }

  /**
   * Read all messages
   * 
   * @return
   */
  public List<Message> readAll() {
    try {
      return QUEUE.getList(".*", Message.class);
    } catch (ImejiException e) {
      LOGGER.error("Error reading message queue ", e);
    }
    return new ArrayList<>();
  }

  /**
   * Read and delete all messages for a specific object between 2 times
   * 
   * @param objectId
   * @return
   */
  public List<Message> readForObject(String objectId, long from, long to) {
    try {
      return QUEUE.getList(objectId + ":.*", Message.class).stream()
          .filter(m -> m.getTime() > from && m.getTime() < to).collect(Collectors.toList());
    } catch (ImejiException e) {
      LOGGER.error("Error reading message queue for object " + objectId, e);
    }
    return new ArrayList<>();
  }

  /**
   * Delete all messages older than the timestamp
   * 
   * @param time
   */
  public void deleteOldMessages(long timestamp) {
    try {
      List<Message> allMessages = QUEUE.getList(".*", Message.class);
      for (Message message : allMessages.stream().filter(m -> m.getTime() < timestamp)
          .collect(Collectors.toList())) {
        QUEUE.delete(message.getMessageId());
      }
    } catch (ImejiException e) {
      LOGGER.error("Error deleting message before " + timestamp, e);
    }
  }

  /**
   * Delete all the messages
   * 
   * @param messages
   */
  public void deleteMessages(List<Message> messages) {
    for (Message m : messages) {
      try {
        QUEUE.delete(m.getMessageId());
      } catch (ImejiException e) {
        LOGGER.error("Error deleting message from queue", e);
      }
    }
  }
}
