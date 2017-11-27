package de.mpg.imeji.logic.hierarchy;

import de.mpg.imeji.logic.events.listener.Listener;
import de.mpg.imeji.logic.events.messages.Message.MessageType;

/**
 * {@link Listener} for move collection events. Reload then the hierarchy
 * 
 * @author saquet
 *
 */
public class UpdateHierarchyListener extends Listener {

  public UpdateHierarchyListener() {
    super(MessageType.MOVE_COLLECTION, MessageType.CREATE_COLLECTION,
        MessageType.DELETE_COLLECTION);
  }

  @Override
  public Integer call() throws Exception {
    HierarchyService.reloadHierarchy();
    return 1;
  }

}
