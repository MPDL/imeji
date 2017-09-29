package de.mpg.imeji.presentation.share;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.log4j.Logger;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.core.collection.CollectionService;
import de.mpg.imeji.logic.model.CollectionImeji;
import de.mpg.imeji.logic.model.Grant;
import de.mpg.imeji.logic.model.User;
import de.mpg.imeji.logic.model.UserGroup;

/**
 * Utility class for Share implementation
 *
 * @author bastiens
 *
 */
public class ShareUtil {

  private static final Logger LOGGER = Logger.getLogger(ShareUtil.class);

  private ShareUtil() {
    // private constructor
  }


  /**
   * Read the role of the {@link User}
   *
   * @return
   * @throws ImejiException
   * @throws Exception
   */
  public static List<ShareListItem> getAllRoles(User user, User sessionUser, Locale locale)
      throws ImejiException {
    final CollectionService collectionController = new CollectionService();
    final List<ShareListItem> roles = new ArrayList<ShareListItem>();
    for (final String grantString : user.getGrants()) {
      try {
        final Grant grant = new Grant(grantString);
        if (grant.getGrantFor().contains("/collection/")) {
          final CollectionImeji col =
              collectionController.retrieveLazy(URI.create(grant.getGrantFor()), sessionUser);
          roles.add(new ShareListItem(user, col.getId().toString(), col.getTitle(), sessionUser,
              locale, false));
        }
      } catch (final Exception e) {
        LOGGER.error("Error reading grants of user ", e);
      }
    }
    return roles;
  }

  /**
   * Read the role of the {@link UserGroup}
   *
   * @return
   * @throws Exception
   */
  public static List<ShareListItem> getAllRoles(UserGroup group, User sessionUser, Locale locale)
      throws ImejiException {
    final CollectionService collectionController = new CollectionService();
    final List<ShareListItem> roles = new ArrayList<ShareListItem>();
    for (final String grantString : group.getGrants()) {
      try {
        final Grant grant = new Grant(grantString);
        final CollectionImeji col =
            collectionController.retrieveLazy(URI.create(grant.getGrantFor()), sessionUser);
        roles.add(
            new ShareListItem(group, col.getId().toString(), col.getTitle(), sessionUser, locale));
      } catch (final Exception e) {
        LOGGER.error("Error reading grants of user ", e);
      }
    }
    return roles;
  }
}
