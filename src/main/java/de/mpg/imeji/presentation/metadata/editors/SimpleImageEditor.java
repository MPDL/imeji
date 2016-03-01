/**
 * License: src/main/resources/license/escidoc.license
 */
package de.mpg.imeji.presentation.metadata.editors;

import java.util.Arrays;
import java.util.List;

import de.mpg.imeji.logic.vo.Item;
import de.mpg.imeji.logic.vo.MetadataProfile;
import de.mpg.imeji.logic.vo.Statement;
import de.mpg.imeji.presentation.metadata.EditorItemBean;

/**
 * Editor for one item (by the item detail page)
 * 
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public class SimpleImageEditor extends MetadataEditor {
  /**
   * Editor for one item (by the item detail page)
   * 
   * @param items
   * @param profile
   * @param statement
   */
  public SimpleImageEditor(List<Item> items, MetadataProfile profile, Statement statement) {
    super(items, profile, statement, true);
  }

  /**
   * Convenient constructor for one {@link Item}
   * 
   * @param item
   * @param profile
   * @param statement
   */
  public SimpleImageEditor(Item item, MetadataProfile profile, Statement statement) {
    super(Arrays.asList(item), profile, statement, true);
  }

  @Override
  public void initialize() {}

  @Override
  public boolean prepareUpdate() {
    for (EditorItemBean eib : items) {
      eib.getMds().trim();
    }
    if (items.size() == 0) {
      return false;
    }
    return true;
  }

}
