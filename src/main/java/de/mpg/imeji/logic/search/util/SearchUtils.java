package de.mpg.imeji.logic.search.util;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import de.mpg.imeji.logic.Imeji;
import de.mpg.imeji.logic.config.ImejiFileTypes.Type;
import de.mpg.imeji.logic.search.Search;
import de.mpg.imeji.logic.vo.Grant;
import de.mpg.imeji.logic.vo.Grant.GrantType;
import de.mpg.imeji.logic.vo.User;

/**
 * Utility class for the {@link Search}
 *
 * @author bastiens
 *
 */
public class SearchUtils {

  /**
   * Parse the query for File types and return a {@link List} of extension
   *
   * @param fileTypes
   * @return
   */
  public static List<String> parseFileTypesAsExtensionList(String fileTypes) {
    List<String> extensions = new ArrayList<>();
    for (String typeName : fileTypes.split(Pattern.quote("|"))) {
      Type type = Imeji.CONFIG.getFileTypes().getType(typeName);
      for (String ext : type.getExtensionArray()) {
        extensions.add(ext);
      }
    }
    return extensions;
  }

  /**
   * True if the user has the sysadmin grant
   * 
   * @param user
   * @return
   */
  public static boolean isSysAdmin(User user) {
    for (Grant g : user.getGrants()) {
      if (g.getGrantFor().toString().equals(Imeji.PROPERTIES.getBaseURI())
          && g.asGrantType().equals(GrantType.ADMIN)) {
        return true;
      }
    }
    return false;
  }
}
