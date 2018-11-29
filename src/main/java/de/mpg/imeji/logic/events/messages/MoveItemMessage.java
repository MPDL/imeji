package de.mpg.imeji.logic.events.messages;

import de.mpg.imeji.logic.model.CollectionImeji;
import de.mpg.imeji.logic.model.Item;
import de.mpg.imeji.logic.util.ObjectHelper;

/**
 * Specific ItemMessage for move operation
 * 
 * @author saquet
 *
 */
public class MoveItemMessage extends ItemMessage {
  private static final long serialVersionUID = 2251847978898480642L;
  private final String previousParent;

  public MoveItemMessage(MessageType type, Item item, String newParent, String previousParent) {
    super(type, setNewParent(item, newParent));
    this.previousParent = previousParent;
  }

  private static Item setNewParent(Item item, String newParent) {
    item.setCollection(ObjectHelper.getURI(CollectionImeji.class, newParent));
    return item;
  }

  public String getPreviousParent() {
    return previousParent;
  }
}
