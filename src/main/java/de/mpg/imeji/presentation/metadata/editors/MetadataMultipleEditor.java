/**
 * License: src/main/resources/license/escidoc.license
 */
package de.mpg.imeji.presentation.metadata.editors;

import java.util.List;

import de.mpg.imeji.logic.vo.Item;
import de.mpg.imeji.logic.vo.MetadataProfile;
import de.mpg.imeji.logic.vo.Statement;
import de.mpg.imeji.presentation.metadata.EditorItemBean;

/**
 * Editor for multiple edit (edit selected items or edit all item of a collection)
 * 
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public class MetadataMultipleEditor extends MetadataEditor {
  /**
   * Default Constructor
   */
  public MetadataMultipleEditor() {
    super();
  }

  /**
   * Editor for multiple edit (edit selected items or edit all items of a collection)
   * 
   * @param items
   * @param profile
   * @param statement
   */
  public MetadataMultipleEditor(List<Item> items, MetadataProfile profile, Statement statement) {
    super(items, profile, statement, false);
  }

  @Override
  public void initialize() {
    for (EditorItemBean eib : items) {
      eib.getMds().addEmtpyValues();
    }
  }

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
