package de.mpg.imeji.presentation.edit;

import de.mpg.imeji.logic.vo.Item;
import de.mpg.imeji.logic.vo.Metadata;
import de.mpg.imeji.logic.vo.Statement;

/**
 * Bean for item element in the metadata editors
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public class ItemWrapper {
  private final Item item;

  /**
   * Bean for item element in the metadata editors
   *
   * @param item
   */
  public ItemWrapper(Item item, boolean addEmtpyValue) {
    this.item = item;
  }

  /**
   * Get {@link ItemWrapper} as {@link Item}
   *
   * @return
   */
  public Item asItem() {
    return item;
  }

  /**
   * Add a Metadata of the same type as the passed metadata
   */
  public void addMetadata(MetadataWrapper smb) {

  }

  /**
   * Remove the active metadata
   */
  public void removeMetadata(MetadataWrapper smb) {

  }

  /**
   * Clear the {@link Metadata} for one {@link Statement}: remove all {@link Metadata} and its
   * Childs and add an empty one
   *
   * @param st
   */
  public void clear(Statement st) {

  }

  /**
   * get the filename of the {@link Item}
   *
   * @return
   */
  public String getFilename() {
    return item.getFilename();
  }


}
