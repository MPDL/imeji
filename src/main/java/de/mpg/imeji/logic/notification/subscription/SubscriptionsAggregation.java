package de.mpg.imeji.logic.notification.subscription;

import java.net.URI;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.config.Imeji;
import de.mpg.imeji.logic.core.collection.CollectionService;
import de.mpg.imeji.logic.events.MessageService;
import de.mpg.imeji.logic.events.aggregation.Aggregation;
import de.mpg.imeji.logic.events.messages.ItemMessage;
import de.mpg.imeji.logic.events.messages.Message;
import de.mpg.imeji.logic.events.messages.Message.MessageType;
import de.mpg.imeji.logic.hierarchy.HierarchyService;
import de.mpg.imeji.logic.model.CollectionImeji;
import de.mpg.imeji.logic.model.Subscription;
import de.mpg.imeji.logic.model.Subscription.Type;
import de.mpg.imeji.logic.model.User;
import de.mpg.imeji.logic.notification.email.EmailService;
import de.mpg.imeji.logic.security.user.UserService;
import de.mpg.imeji.logic.util.ObjectHelper;

/**
 * {@link Aggregation} for all files which have been uploaded or modified over one day
 * 
 * @author saquet
 *
 */
public class SubscriptionsAggregation implements Aggregation {
  private static final Logger LOGGER = Logger.getLogger(SubscriptionsAggregation.class);
  public final static String COUNT = "count";
  public final static String ITEM_ID = "itemId";
  public final static String FILENAME = "filename";
  private Map<String, List<Subscription>> subscriptionsByUser;
  private Map<String, List<ItemMessage>> messagesByCollection;

  @Override
  public Period getPeriod() {
    return Period.NIGHTLY;
  }

  @Override
  public void aggregate() {
    try {
      init();
      for (User user : new UserService().retrieveAll()) {
        Map<String, List<ItemMessage>> messages = getMessagesForUser(user);
        for (String c : messages.keySet()) {
          System.out.println("c " + c);
          for (ItemMessage m : messages.get(c)) {
            System.out.println(m.getMessageId());
          }
        }
        if (!messages.isEmpty()) {
          String emailBody = createEmailBody(user, messages);
          sendEmail(user, emailBody);
        }
      }
    } catch (Exception e) {
      LOGGER.error("Error aggregating messages for the subscriptions", e);
    }
  }

  /**
   * Retrieve all messages and all subscriptions
   * 
   * @throws ImejiException
   */
  private void init() throws ImejiException {
    HierarchyService.reloadHierarchy();
    subscriptionsByUser = new SubscriptionService().retrieveByType(Type.DEFAULT, Imeji.adminUser)
        .stream().collect(Collectors.groupingBy(Subscription::getUserId));
    messagesByCollection = new MessageService().readAll().stream().map(m -> (ItemMessage) m)
        .collect(Collectors.groupingBy(Message::getObjectId));

    for (String c : messagesByCollection.keySet()) {
      System.out.println("c " + c);
      for (ItemMessage m : messagesByCollection.get(c)) {
        System.out.println(m.getMessageId());
      }
    }
  }

  /**
   * Create the Email body
   * 
   * @param user
   * @param messages
   * @return
   */
  private String createEmailBody(User user, Map<String, List<ItemMessage>> messages) {
    String collectionSummaries = messages.keySet().stream()
        .map(id -> getCollectionSummary(id, messages, user)).collect(Collectors.joining());
    String body = Imeji.RESOURCE_BUNDLE.getMessage("email_subscribtion_body", Locale.ENGLISH)
        .replaceAll("XXX_INSTANCE_NAME_XXX", Imeji.CONFIG.getInstanceName())
        .replaceAll("XXX_USER_NAME_XXX", user.getPerson().getFirstnameLastname())
        .replace("XXX_TEXT_XXX", collectionSummaries);
    return body;
  }

  /**
   * Build the text for one collection
   * 
   * @param collectionId
   * @param messages
   * @return
   */
  private String getCollectionSummary(String collectionId, Map<String, List<ItemMessage>> messages,
      User user) {
    String text = "";
    CollectionImeji c = retrieveCollection(collectionId, user);
    if (c != null) {
      text += getCollectionText(c);
      text += messages.get(collectionId).stream()
          .filter(m -> m.getType() == MessageType.UPLOAD_FILE
              || m.getType() == MessageType.MOVE_COLLECTION || m.getType() == MessageType.MOVE_ITEM)
          .map(m -> getItemText(c, m)).collect(Collectors.joining("\n", "\nNew Files:\n", ""));
      text += messages.get(collectionId).stream()
          .filter(m -> m.getType() == MessageType.CHANGE_FILE).map(m -> getItemText(c, m))
          .collect(Collectors.joining("\n", "\nModifiedFiles:\n", ""));
      text += "\n";
    }
    return text;
  }

  /**
   * Return only the message related to one active subscription of the user
   * 
   * @param messagesByCollection
   * @param user
   * @return
   */
  private Map<String, List<ItemMessage>> getMessagesForUser(User user) {
    return messagesByCollection.entrySet().stream()
        .filter(e -> hasSubscibedfor(user, e.getKey())
            && isActiveForUser(user, getSubscription(user, e.getKey())))
        .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
  }

  /**
   * True if the user has subscribed to the collection
   * 
   * @param user
   * @param collectionId
   * @return
   */
  private boolean hasSubscibedfor(User user, String collectionId) {
    List<Subscription> userSubscription = subscriptionsByUser.get(user.getId().toString());
    if (userSubscription != null) {
      System.out.println(user.getEmail() + " has subscribed for " + getCollectionId(collectionId)
          + " folder: " + collectionId + " : "
          + userSubscription.stream()
              .filter(s -> s.getObjectId().equals(getCollectionId(collectionId))).findAny()
              .isPresent());
      return userSubscription.stream()
          .filter(s -> s.getObjectId().equals(getCollectionId(collectionId))).findAny().isPresent();
    }
    return false;
  }

  /**
   * True if the subscription is active for the user
   * 
   * @param user
   * @param subscription
   * @return
   */
  private boolean isActiveForUser(User user, Subscription subscription) {
    URI collectionURI = ObjectHelper.getURI(CollectionImeji.class, subscription.getObjectId());
    try {
      new CollectionService().retrieveLazy(collectionURI, user);
      return true;
    } catch (ImejiException e) {
      return false;
    }
  }

  /**
   * Return the Subscription of the user for this collection
   * 
   * @param user
   * @param message
   * @return
   */
  private Subscription getSubscription(User user, String collectionId) {
    return subscriptionsByUser.get(user.getId().toString()).stream()
        .filter(s -> s.getObjectId().equals(getCollectionId(collectionId))).findAny().get();
  }

  /**
   * Get the Return the collection uri as string (i.e the last parent) of the folder/collection
   * 
   * @param folderId
   * @return
   */
  private String getCollectionUri(String folderId) {
    String collectionUri = ObjectHelper.getURI(CollectionImeji.class, folderId).toString();
    return new HierarchyService().getLastParent(collectionUri);
  }

  /**
   * Return the collection id (i.e the last parent) of the folder/collection
   * 
   * @param folderId
   * @return
   */
  private String getCollectionId(String folderId) {
    return ObjectHelper.getId(URI.create(getCollectionUri(folderId)));
  }

  /***
   * Send Email to the user
   * 
   * @param user
   * @param body
   * @throws ImejiException
   */
  private void sendEmail(User user, String body) throws ImejiException {
    String subject = Imeji.RESOURCE_BUNDLE.getMessage("email_subscribtion_subject", Locale.ENGLISH)
        .replace("XXX_INSTANCE_NAME_XXX", Imeji.CONFIG.getInstanceName());
    new EmailService().sendMail(user.getEmail(), null, subject, body);
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

  private String getItemText(CollectionImeji c, ItemMessage m) {
    return "* " + m.getFilename() + " (" + Imeji.PROPERTIES.getApplicationURL() + "item/"
        + m.getItemId() + ")";
  }

  /**
   * Retrieve the collection for the collectionId
   * 
   * @param collectionId
   * @return
   */
  private CollectionImeji retrieveCollection(String collectionId, User user) {
    try {
      URI collectionURI = ObjectHelper.getURI(CollectionImeji.class, collectionId);
      return new CollectionService().retrieveLazy(collectionURI, user);
    } catch (ImejiException e) {
      // User cannot retrieve the collection
      return null;
    }
  }



}
