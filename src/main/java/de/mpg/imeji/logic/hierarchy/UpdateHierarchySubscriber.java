package de.mpg.imeji.logic.hierarchy;

import de.mpg.imeji.logic.events.Message.MessageType;
import de.mpg.imeji.logic.events.subscription.Subscriber;

/**
 * {@link Subscriber} for move collection events. Reload then the hierarchy
 * 
 * @author saquet
 *
 */
public class UpdateHierarchySubscriber extends Subscriber {

  public UpdateHierarchySubscriber() {
    super(MessageType.MOVE_COLLECTION, MessageType.CREATE_COLLECTION);
  }

  @Override
  public Integer call() throws Exception {
    HierarchyService.reloadHierarchy();
    return 1;
  }

}
