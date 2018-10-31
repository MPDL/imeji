package de.mpg.imeji.logic.notification.subscription;

import de.mpg.imeji.logic.config.Imeji;
import de.mpg.imeji.logic.events.listener.Listener;
import de.mpg.imeji.logic.events.messages.Message.MessageType;
import de.mpg.imeji.logic.model.Subscription;

/**
 * {@link Listener} for events leading to remove the related
 * {@link Subscription}
 * 
 * @author saquet
 *
 */
public class RemoveSubscriptionListener extends Listener {

	public RemoveSubscriptionListener() {
		super(MessageType.DELETE_COLLECTION, MessageType.DISCARD_COLLECTION);
	}

	@Override
	public Integer call() throws Exception {
		SubscriptionService service = new SubscriptionService();
		for (Subscription s : service.retrieveByObjectId(getMessage().getObjectId(), Imeji.adminUser)) {
			service.unSubscribe(s, Imeji.adminUser);
		}
		return 1;
	}
}
