package de.mpg.imeji.logic.batch.messageAggregations;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.config.Imeji;
import de.mpg.imeji.logic.core.collection.CollectionService;
import de.mpg.imeji.logic.events.Message;
import de.mpg.imeji.logic.events.MessageService;
import de.mpg.imeji.logic.events.Message.MessageType;
import de.mpg.imeji.logic.events.aggregation.Aggregation;
import de.mpg.imeji.logic.model.CollectionImeji;
import de.mpg.imeji.logic.model.User;
import de.mpg.imeji.logic.security.sharing.email.EmailService;
import de.mpg.imeji.logic.security.user.UserService;
import de.mpg.imeji.logic.util.ObjectHelper;

/**
 * {@link Aggregation} for all files which have been uploaded or modified over one day
 * 
 * @author saquet
 *
 */
public class NotifyUsersOnFileUploadAggregation implements Aggregation {
  private static final Logger LOGGER = Logger.getLogger(NotifyUsersOnFileUploadAggregation.class);
  private final MessageService messageService = new MessageService();
  public final static String COUNT = "count";
  public final static String ITEM_ID = "itemId";
  public final static String FILENAME = "filename";
  private final long now = System.currentTimeMillis();
  private final long from = getFrom();
  private Map<String, CollectionImeji> collectionMap = new HashMap<>();


  @Override
  public void aggregate() {
    for (User user : retrieveAllUsersWithRegistration()) {
      sendEmailToUser(user,
          aggregateMessagesByCollection(retrieveAllMessagesForUser(user, from, now,
              MessageType.UPLOAD_FILE, MessageType.MOVE_ITEM)),
          aggregateMessagesByCollection(
              retrieveAllMessagesForUser(user, from, now, MessageType.CHANGE_FILE)));
    }
  }

  /**
   * Send the email to every User with at least one message
   * 
   * @param user
   * @param uploadedFiles
   * @param modifiedFiles
   */
  private void sendEmailToUser(User user, Map<String, List<Message>> uploadedFiles,
      Map<String, List<Message>> modifiedFiles) {
    String text = "";
    if (!uploadedFiles.isEmpty() || !modifiedFiles.isEmpty()) {
      String subject =
          Imeji.RESOURCE_BUNDLE.getMessage("email_subscribtion_subject", Locale.ENGLISH)
              .replace("XXX_INSTANCE_NAME_XXX", Imeji.CONFIG.getInstanceName());
      String body = "";
      for (String collectionId : uploadedFiles.keySet()) {
        text += "\n\n" + buildTextWithListOfFiles(collectionId, uploadedFiles, modifiedFiles);
        body = Imeji.RESOURCE_BUNDLE.getMessage("email_subscribtion_body", Locale.ENGLISH)
            .replaceAll("XXX_INSTANCE_NAME_XXX", Imeji.CONFIG.getInstanceName())
            .replaceAll("XXX_USER_NAME_XXX", user.getPerson().getFirstnameLastname())
            .replace("XXX_TEXT_XXX", text);
      }
      try {
        new EmailService().sendMail(user.getEmail(), null, subject, body);
      } catch (ImejiException e) {
        LOGGER.error("Error sending Email to user", e);
      }
    }
  }

  private String buildTextWithListOfFiles(String collectionId,
      Map<String, List<Message>> uploadedFiles, Map<String, List<Message>> modifiedFiles) {
    String text = "";
    CollectionImeji c = retrieveCollection(collectionId);
    if (c != null) {
      text += getCollectionText(retrieveCollection(collectionId));
      text += uploadedFiles.containsKey(collectionId) ? uploadedFiles.get(collectionId).stream()
          .map(m -> getItemText(c, m)).collect(Collectors.joining("\n", "\nNew Files:\n", "")) : "";
      text += modifiedFiles.containsKey(collectionId) ? modifiedFiles.get(collectionId).stream()
          .map(m -> getItemText(c, m)).collect(Collectors.joining("\n", "\nModifiedFiles:\n", ""))
          : "";
    }
    return text;
  }

  /**
   * Aggregate Messages pro collection
   * 
   * @param messages
   * @return
   */
  private Map<String, List<Message>> aggregateMessagesByCollection(List<Message> messages) {
    return messages.stream().collect(Collectors.groupingBy(Message::getObjectId));
  }

  /**
   * Retrieve all messages which are relevant for a user
   * 
   * @param user
   * @param from
   * @param to
   * @return
   */
  private List<Message> retrieveAllMessagesForUser(User user, long from, long to,
      MessageType... type) {
    List<Message> messages = new ArrayList<>();
    for (String collectionId : user.getSubscriptionCollections()) {
      collectionId = ObjectHelper.getId(URI.create(collectionId));
      messages.addAll(messageService.readForObject(collectionId, from, to).stream()
          .filter(m -> Arrays.asList(type).contains(m.getType())).collect(Collectors.toList()));
    }
    return messages;
  }

  /**
   * Retrieve all users which have at least register to one collection
   * 
   * @return
   * @throws ImejiException
   */
  private List<User> retrieveAllUsersWithRegistration() {
    try {
      return new UserService().retrieveAll().stream()
          .filter(user -> user.getSubscriptionCollections() != null
              && !user.getSubscriptionCollections().isEmpty())
          .collect(Collectors.toList());
    } catch (ImejiException e) {
      LOGGER.error("Error retrieving users", e);
    }
    return new ArrayList<>();
  }

  private long getFrom() {
    Calendar from = Calendar.getInstance();
    from.add(Calendar.DAY_OF_MONTH, -1);
    return from.getTimeInMillis();
  }

  private String getCollectionText(CollectionImeji c) {
    return Imeji.RESOURCE_BUNDLE.getLabel("collection", Locale.ENGLISH) + " " + c.getTitle() + " ("
        + Imeji.PROPERTIES.getApplicationURL() + "collection/" + c.getIdString() + ")";
  }

  private String getItemText(CollectionImeji c, Message m) {
    return "* " + m.getContent().get(FILENAME) + " (" + Imeji.PROPERTIES.getApplicationURL()
        + "item/" + m.getContent().get(ITEM_ID) + ")";
  }

  /**
   * Retrieve the collection for the collectionId
   * 
   * @param collectionId
   * @return
   */
  private CollectionImeji retrieveCollection(String collectionId) {
    if (!collectionMap.containsKey(collectionId)) {
      try {
        collectionMap.put(collectionId, new CollectionService().retrieveLazy(
            ObjectHelper.getURI(CollectionImeji.class, collectionId), Imeji.adminUser));
      } catch (ImejiException e) {
        LOGGER.error("Error retrieving collection " + collectionId, e);
      }
    }
    return collectionMap.get(collectionId);
  }

}
