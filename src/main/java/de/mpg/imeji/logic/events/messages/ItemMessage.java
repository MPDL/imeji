package de.mpg.imeji.logic.events.messages;

import de.mpg.imeji.logic.model.Item;

/**
 * Message for events related to items
 * 
 * @author saquet
 *
 */
public class ItemMessage extends Message {
  private static final long serialVersionUID = 8062771460965677729L;
  private final String filename;
  private final String itemId;

  public ItemMessage(MessageType type, Item item) {
    super(type, item.getCollection());
    this.filename = item.getFilename();
    this.itemId = item.getIdString();
  }

  /**
   * @return the filename
   */
  public String getFilename() {
    return filename;
  }

  /**
   * @return the itemId
   */
  public String getItemId() {
    return itemId;
  }

}
