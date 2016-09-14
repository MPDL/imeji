/**
 * License: src/main/resources/license/escidoc.license
 */
package de.mpg.imeji.logic.controller.util;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.Imeji;
import de.mpg.imeji.logic.controller.resource.ProfileController;
import de.mpg.imeji.logic.vo.Item;
import de.mpg.imeji.logic.vo.MetadataProfile;
import de.mpg.imeji.logic.vo.Statement;
import de.mpg.imeji.logic.vo.util.MetadataAndProfileHelper;

/**
 * Helper methods related to {@link MetadataProfile}
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public class MetadataProfileUtil {
  /**
   * Private Constructor
   */
  private MetadataProfileUtil() {
    // Avoid creation
  }

  /**
   * Load the all th {@link MetadataProfile} found in a {@link List} of {@link Item}
   *
   * @param imgs
   * @return
   * @throws ImejiException
   * @throws Exception
   */
  public static Map<URI, MetadataProfile> loadProfiles(List<Item> imgs) throws ImejiException {
    Map<URI, MetadataProfile> pMap = new HashMap<URI, MetadataProfile>();

    for (Item im : imgs) {
      if (pMap.get(im.getMetadataSet().getProfile()) == null) {
        // safe to retrieve the profile as an admin user, as there is only one call to this method
        pMap.put(im.getMetadataSet().getProfile(),
            new ProfileController().retrieve(im.getMetadataSet().getProfile(), Imeji.adminUser));
        // ObjectCachedLoader.loadProfile(im.getMetadataSet().getProfile()));
      }
    }
    return pMap;
  }

  /**
   * Get the all {@link Statement} that are childs of the passed statement. If onlyFirst ist true,
   * then give back only childs that are direct child of this {@link Statement}
   *
   * @param statement
   * @param profile
   * @param onlyFirst
   * @return
   */
  public static List<Statement> getChilds(Statement statement, MetadataProfile profile,
      boolean onlyFirst) {
    List<Statement> childs = new ArrayList<Statement>();
    if (statement != null) {
      for (int i = ((List<Statement>) profile.getStatements()).indexOf(statement) + 1; i < profile
          .getStatements().size(); i++) {
        Statement st = ((List<Statement>) profile.getStatements()).get(i);
        if (st.getParent() != null && st.getParent().compareTo(statement.getId()) == 0) {
          childs.add(st);
          if (!onlyFirst) {
            childs.addAll(getChilds(st, profile, false));
          }
        }
      }
    }
    return childs;
  }

  /**
   * Return the {@link URI} of the last parent {@link Statement}. Null if no parent
   *
   * @param st
   * @param profile
   * @return
   */
  public static URI getLastParent(Statement st, MetadataProfile profile) {
    URI parent = st.getParent();
    URI lastParent = null;
    while (parent != null) {
      lastParent = parent;
      Statement parentStatement = MetadataAndProfileHelper.getStatement(parent, profile);
      if (parentStatement == null) {
        break;
      }
      parent = parentStatement.getParent();
    }
    return lastParent;
  }

  /**
   * True if {@link Statement} st1 is a parent of {@link Statement} st2
   *
   * @param st1
   * @param st2
   * @param profile
   * @return
   */
  public static boolean isParent(Statement st1, Statement st2, MetadataProfile profile) {
    boolean isParent = false;
    while (!isParent || st2.getParent() != null) {
      isParent = st2.getParent().compareTo(st1.getId()) == 0;
      isParent =
          isParent(st1, MetadataAndProfileHelper.getStatement(st2.getParent(), profile), profile);
    }
    return isParent;
  }
}
