package de.mpg.imeji.logic.events.messages;

import de.mpg.imeji.logic.model.CollectionImeji;
import de.mpg.imeji.logic.util.ObjectHelper;

/**
 * Specific message for moved collection events
 * 
 * @author saquet
 *
 */
public class MoveCollectionMessage extends CollectionMessage {
  private static final long serialVersionUID = 4567055677966050909L;
  private final String previousParent;

  public MoveCollectionMessage(MessageType type, CollectionImeji collection, String newParent, String previousParent) {
    super(type, setNewParent(collection, newParent));
    this.previousParent = previousParent;
  }

  private static CollectionImeji setNewParent(CollectionImeji collection, String newParent) {
    collection.setCollection(ObjectHelper.getURI(CollectionImeji.class, newParent));
    return collection;
  }

  public String getPreviousParent() {
    return previousParent;
  }
}
