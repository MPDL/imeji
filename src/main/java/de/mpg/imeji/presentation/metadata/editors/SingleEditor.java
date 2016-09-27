/**
 * License: src/main/resources/license/escidoc.license
 */
package de.mpg.imeji.presentation.metadata.editors;

import java.util.Arrays;
import java.util.Locale;

import de.mpg.imeji.logic.vo.Item;
import de.mpg.imeji.logic.vo.MetadataProfile;
import de.mpg.imeji.logic.vo.Statement;
import de.mpg.imeji.logic.vo.User;
import de.mpg.imeji.presentation.component.LicenseEditor;

/**
 * Editor for one item (by the item detail page)
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public class SingleEditor extends AbstractMetadataEditor {

  /**
   * Convenient constructor for one {@link Item}
   *
   * @param item
   * @param profile
   * @param statement
   */
  public SingleEditor(Item item, MetadataProfile profile, Statement statement, User sessionUser,
      Locale locale) {
    super(Arrays.asList(item), profile, statement, sessionUser, locale);
    setLicenseEditor(new LicenseEditor(locale, item));
  }
}
