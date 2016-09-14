/**
 * License: src/main/resources/license/escidoc.license
 */
package de.mpg.imeji.presentation.metadata.util;

import de.mpg.imeji.logic.Imeji;
import de.mpg.imeji.logic.util.StringHelper;
import de.mpg.imeji.logic.vo.predefinedMetadata.ConePerson;
import de.mpg.imeji.logic.vo.predefinedMetadata.Metadata;
import de.mpg.imeji.logic.vo.util.MetadataAndProfileHelper;
import de.mpg.imeji.presentation.session.BeanHelper;

/**
 * Uitlity class to {@link Metadata}
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public class MetadataHelper {

  /**
   * Return true if the {@link Metadata} has an empty value (which shouldn't be store in the
   * database)
   *
   * @param md
   * @return
   */
  public static boolean isEmpty(Metadata md) {
    return MetadataAndProfileHelper.isEmpty(md);
  }

  /**
   * Set the CoNE id of a {@link ConePerson}
   *
   * @param md
   * @return
   */
  public static Metadata setConeID(Metadata md) {
    if (md.getTypeNamespace().equals(Metadata.Types.CONE_PERSON.getClazzNamespace())) {
      String id = ((ConePerson) md).getPerson().getIdentifier();
      try {
        if (id.contains("http")) {
          ((ConePerson) md).setConeId(java.net.URI.create(id));
          return md;
        }

        String familyName = ((ConePerson) md).getPerson().getFamilyName();
        String givenName = ((ConePerson) md).getPerson().getGivenName();
        String completeName = givenName + ((givenName == null || givenName.isEmpty()
            || familyName == null || familyName.isEmpty()) ? "" : ", ") + familyName;
        completeName = completeName.trim();

        if (!StringHelper.isNullOrEmptyTrim(familyName)) {
          ((ConePerson) md).getPerson().setCompleteName(completeName);
        }
      } catch (Exception e) {
        BeanHelper
            .error(Imeji.RESOURCE_BUNDLE.getLabel("error", BeanHelper.getLocale()) + " CONE ID");
      }
      ((ConePerson) md).setConeId(null);
    }
    return md;
  }
}
