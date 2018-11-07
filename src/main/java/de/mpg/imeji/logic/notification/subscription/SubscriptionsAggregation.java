package de.mpg.imeji.logic.notification.subscription;

import java.net.URI;
import java.util.AbstractMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.config.Imeji;
import de.mpg.imeji.logic.core.collection.CollectionService;
import de.mpg.imeji.logic.core.item.ItemService;
import de.mpg.imeji.logic.events.MessageService;
import de.mpg.imeji.logic.events.aggregation.Aggregation;
import de.mpg.imeji.logic.events.messages.CollectionMessage;
import de.mpg.imeji.logic.events.messages.ItemMessage;
import de.mpg.imeji.logic.events.messages.Message;
import de.mpg.imeji.logic.events.messages.Message.MessageType;
import de.mpg.imeji.logic.events.messages.MoveCollectionMessage;
import de.mpg.imeji.logic.events.messages.MoveItemMessage;
import de.mpg.imeji.logic.hierarchy.HierarchyService;
import de.mpg.imeji.logic.hierarchy.HierarchyService.CollectionUriNameWrapper;
import de.mpg.imeji.logic.model.CollectionImeji;
import de.mpg.imeji.logic.model.Item;
import de.mpg.imeji.logic.model.Subscription;
import de.mpg.imeji.logic.model.Subscription.Type;
import de.mpg.imeji.logic.model.User;
import de.mpg.imeji.logic.notification.email.EmailMessages;
import de.mpg.imeji.logic.notification.email.EmailService;
import de.mpg.imeji.logic.security.user.UserService;
import de.mpg.imeji.logic.util.ObjectHelper;
import de.mpg.imeji.logic.util.StringHelper;

/**
 * {@link Aggregation} for all files which have been uploaded or modified over
 * one day
 * 
 * @author saquet
 *
 */
public class SubscriptionsAggregation implements Aggregation {
	private static final Logger LOGGER = LogManager.getLogger(SubscriptionsAggregation.class);
	public final static String COUNT = "count";
	public final static String ITEM_ID = "itemId";
	public final static String FILENAME = "filename";
	private Map<String, List<Subscription>> subscriptionsByUser;
	private Map<String, List<Message>> messagesByCollection;
	private final HierarchyService hierarchyService = new HierarchyService();

	@Override
	public Period getPeriod() {
		return Period.NIGHTLY;
	}

	@Override
	public void aggregate() {
		try {
			init();
			for (User user : new UserService().retrieveAll()) {
				Map<String, List<Message>> messages = getMessagesForUser(user);
				if (isNotEmpty(messages)) {
					String emailBody = createEmailBody(user, messages);
					if (!StringHelper.isNullOrEmptyTrim(emailBody)) {
						sendEmail(user, emailBody);
					}
				}
			}
		} catch (Exception e) {
			LOGGER.error("Error aggregating messages for the subscriptions", e);
		}
	}

	/**
	 * True if there is at least one message
	 * 
	 * @param messages
	 * @return
	 */
	private boolean isNotEmpty(Map<String, List<Message>> messages) {
		return messages.values().stream().flatMap(l -> l.stream()).findAny().isPresent();
	}

	/**
	 * Retrieve all messages and all subscriptions
	 * 
	 * @throws ImejiException
	 */
	private void init() throws ImejiException {
		HierarchyService.reloadHierarchy();
		subscriptionsByUser = new SubscriptionService().retrieveByType(Type.DEFAULT, Imeji.adminUser).stream()
				.collect(Collectors.groupingBy(Subscription::getUserId));
		messagesByCollection = new MessageService().readAll().stream().collect(Collectors.groupingBy(m -> ObjectHelper
				.getId(URI.create(hierarchyService.getLastParent(getCollectionUri(m.getObjectId()))))));
	}

	/**
	 * Create the Email body
	 * 
	 * @param user
	 * @param messages
	 *            internal system messages that denote system events
	 * @return
	 */
	private String createEmailBody(User user, Map<String, List<Message>> messages) {
		String collectionSummaries = messages.keySet().stream().map(id -> getCollectionSummary(id, messages, user))
				.collect(Collectors.joining());
		if (StringHelper.isNullOrEmptyTrim(collectionSummaries)) {
			return "";
		}
		return EmailMessages.getSubscriptionEmailBody(user, collectionSummaries, Locale.ENGLISH);
	}

	/**
	 * Build the text for one collection
	 * 
	 * @param collectionId
	 * @param messages
	 * @return
	 */
	private String getCollectionSummary(String collectionId, Map<String, List<Message>> messages, User user) {
		String text = "";
		CollectionImeji c = retrieveCollection(collectionId, user);
		if (c != null) {
			String newFilesText = messages.get(collectionId).stream()
					.filter(m -> m.getType() == MessageType.UPLOAD_FILE || m.getType() == MessageType.MOVE_ITEM)
					.map(m -> getItemText(c, m)).collect(Collectors.joining("\n"));
			String newCollectionText = messages.get(collectionId).stream()
					.filter(m -> m.getType() == MessageType.MOVE_COLLECTION).map(m -> getItemText(c, m))
					.collect(Collectors.joining("\n"));
			String changedFilesText = messages.get(collectionId).stream()
					.filter(m -> m.getType() == MessageType.CHANGE_FILE).map(m -> getItemText(c, m))
					.collect(Collectors.joining("\n"));

			if (!StringHelper.isNullOrEmptyTrim(newFilesText) || !StringHelper.isNullOrEmptyTrim(newCollectionText)
					|| !StringHelper.isNullOrEmptyTrim(changedFilesText)) {
				text += "\n- " + getCollectionText(c) + " -" + "\n\n";
				text += !StringHelper.isNullOrEmptyTrim(newFilesText)
						? " --- New files ---\n" + newFilesText + "\n\n"
						: "";
				text += !StringHelper.isNullOrEmptyTrim(newCollectionText)
						? " --- New collections ---\n" + newCollectionText + "\n\n"
						: "";
				text += !StringHelper.isNullOrEmptyTrim(changedFilesText)
						? " --- Changed files ---\n" + changedFilesText + "\n\n"
						: "";
			}
		}
		return text;
	}

	/**
	 * Return only the message if:
	 * <li>The user has subscribed to the collection
	 * <li>The user can read the collection (i.e. the subscription is still active)
	 * <li>In case of messages for move events, the moved objects have been moved
	 * into a new collection and not into a subcollection of the same collection
	 * 
	 * 
	 * @param messagesByCollection
	 * @param user
	 * @return
	 */
	private Map<String, List<Message>> getMessagesForUser(User user) {
		return messagesByCollection.entrySet().stream()
				.filter(e -> hasSubscribedfor(user, e.getKey()) && isActiveForUser(user, e.getKey()))
				.map(e -> filterMessages(e, user)).collect(Collectors.toMap(Entry::getKey, Entry::getValue));
	}

	/**
	 * Filter the messages of the Entry and return the entry with the filtered
	 * messages:
	 * <li>Remove messages for objects moved within the same collection
	 * 
	 * @param entry
	 * @return
	 */
	private Entry<String, List<Message>> filterMessages(Entry<String, List<Message>> entry, User user) {
		return new AbstractMap.SimpleEntry<String, List<Message>>(entry.getKey(), entry.getValue().stream()
				.filter(m -> !isMovedInsameCollection(m) && exists(m, user)).collect(Collectors.toList()));
	}

	/**
	 * True if the user has subscribed to the collection
	 * 
	 * @param user
	 * @param collectionId
	 * @return
	 */
	private boolean hasSubscribedfor(User user, String collectionId) {
		List<Subscription> userSubscription = subscriptionsByUser.get(user.getId().toString());
		if (userSubscription != null) {
			return userSubscription.stream().filter(s -> s.getObjectId().equals(getCollectionId(collectionId)))
					.findAny().isPresent();
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
	private boolean isActiveForUser(User user, String collectionId) {
		URI collectionURI = ObjectHelper.getURI(CollectionImeji.class, collectionId);
		try {
			new CollectionService().retrieveLazy(collectionURI, user);
			return true;
		} catch (ImejiException e) {
			return false;
		}
	}

	/**
	 * True if:
	 * <li>the message is related to a move operation (MOVE_ITEM or MOVE_COLLECTION)
	 * <li>AND the move operation has been done within one same collection
	 * 
	 * @param message
	 * @return
	 */
	private boolean isMovedInsameCollection(Message message) {
		if (message.getType() == MessageType.MOVE_ITEM) {
			MoveItemMessage m = (MoveItemMessage) message;
			return hierarchyService.getLastParent(getCollectionUri(m.getPreviousParent()))
					.equals(hierarchyService.getLastParent(getCollectionUri(m.getObjectId())));
		} else if (message.getType() == MessageType.MOVE_COLLECTION) {
			MoveCollectionMessage m = (MoveCollectionMessage) message;
			return hierarchyService.getLastParent(getCollectionUri(m.getPreviousParent()))
					.equals(hierarchyService.getLastParent(getCollectionUri(m.getObjectId())));
		}
		return false;
	}

	/**
	 * True if the object is a message
	 * 
	 * @param message
	 * @param user
	 * @return
	 */
	private boolean exists(Message message, User user) {
		try {
			if (message instanceof ItemMessage) {
				new ItemService().retrieveLazy(ObjectHelper.getURI(Item.class, ((ItemMessage) message).getItemId()),
						user);
			}
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * Get the Return the collection uri as string (i.e the last parent) of the
	 * folder/collection
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
		String subject = EmailMessages.getSubscriptionEmailSubject(Locale.ENGLISH);
		new EmailService().sendMail(user.getEmail(), null, subject, body);
	}

	/**
	 * Write the notification text for a collection
	 * 
	 * @param c
	 * @return
	 */
	private String getCollectionText(CollectionImeji c) {
		return Imeji.RESOURCE_BUNDLE.getLabel("collection", Locale.ENGLISH) + " " + c.getTitle();
	}

	/**
	 * Write the notification text for an item
	 * 
	 * @param c
	 * @param m
	 * @return
	 */
	private String getItemText(CollectionImeji c, Message m) {
		switch (m.getType()) {
			case MOVE_COLLECTION :
				CollectionMessage cm = (CollectionMessage) m;
				return getPath(cm) + cm.getName() + " (" + Imeji.PROPERTIES.getApplicationURL() + "collection/"
						+ cm.getObjectId() + ")";
			default :
				ItemMessage im = (ItemMessage) m;
				return getPath(im) + im.getFilename() + " (" + Imeji.PROPERTIES.getApplicationURL() + "item/"
						+ im.getItemId() + ")";
		}
	}

	/**
	 * Get the path of the object of the message
	 * 
	 * @param m
	 * @return
	 */
	private String getPath(Message m) {
		switch (m.getType()) {
			case UPLOAD_FILE :
				return getPath(m.getObjectId());
			case CHANGE_FILE :
				return getPath(m.getObjectId());
			case MOVE_ITEM :
				return getPath(((MoveItemMessage) m).getObjectId());
			case MOVE_COLLECTION :
				return getPath(((MoveCollectionMessage) m).getParent());
			default :
				return "";
		}
	}

	/**
	 * Return the path of the collectionId
	 * 
	 * @param collectionId
	 * @return
	 */
	private String getPath(String collectionId) {
		List<CollectionUriNameWrapper> parents = hierarchyService
				.findAllParentsWithNames(ObjectHelper.getURI(CollectionImeji.class, collectionId).toString(), true);
		parents.remove(0);
		if (parents.isEmpty()) {
			return "";
		}
		return parents.stream().map(p -> p.getName()).collect(Collectors.joining("/", "", "/"));
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
