package de.mpg.imeji.logic.messaging;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.db.keyValue.KeyValueStoreService;
import de.mpg.imeji.logic.db.keyValue.stores.HTreeMapStore;

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

  /**
   * Register a new {@link Message}
   * 
   * @param message
   */
  public void add(Message message) {
    try {
      QUEUE.put(message.getMessageId(), message);
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
  public List<Message> readAndDeleteForObject(String objectId) {
    try {
      return QUEUE.getList(objectId + ":*", Message.class);
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
      List<Message> allMessages = QUEUE.getList("", Message.class);
      for (Message message : allMessages.stream().filter(m -> m.getTime() < timestamp)
          .collect(Collectors.toList())) {
        QUEUE.delete(message.getMessageId());
      }
    } catch (ImejiException e) {
      LOGGER.error("Error deleting message before " + timestamp, e);
    }
  }
}
