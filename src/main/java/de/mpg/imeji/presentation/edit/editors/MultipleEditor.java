/**
 * License: src/main/resources/license/escidoc.license
 */
package de.mpg.imeji.presentation.edit.editors;

import java.util.List;
import java.util.Locale;

import de.mpg.imeji.logic.vo.Item;
import de.mpg.imeji.logic.vo.Statement;
import de.mpg.imeji.logic.vo.User;

/**
 * Editor for multiple edit (edit selected items or edit all item of a collection)
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public class MultipleEditor extends AbstractMetadataEditor {
  /**
   * Default Constructor
   */
  public MultipleEditor() {
    super();
  }

  /**
   * Editor for multiple edit (edit selected items or edit all items of a collection)
   *
   * @param items
   * @param profile
   * @param statement
   */
  public MultipleEditor(List<Item> items, Statement statement, User sessionUser, Locale locale) {
    super(items, statement, sessionUser, locale);
  }
}
