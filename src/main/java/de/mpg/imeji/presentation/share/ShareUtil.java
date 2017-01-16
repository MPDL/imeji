package de.mpg.imeji.presentation.share;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import javax.faces.model.SelectItem;

import org.apache.log4j.Logger;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.Imeji;
import de.mpg.imeji.logic.collection.CollectionService;
import de.mpg.imeji.logic.share.ShareService.ShareRoles;
import de.mpg.imeji.logic.vo.CollectionImeji;
import de.mpg.imeji.logic.vo.Grant;
import de.mpg.imeji.logic.vo.User;
import de.mpg.imeji.logic.vo.UserGroup;

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
   * Menu the Role Menu for sharing
   *
   * @return
   */
  public static List<SelectItem> getRoleMenu(Locale locale) {
    return Arrays.asList(
        new SelectItem(ShareRoles.READ, Imeji.RESOURCE_BUNDLE.getLabel("read", locale)),
        new SelectItem(ShareRoles.EDIT, Imeji.RESOURCE_BUNDLE.getLabel("edit", locale)),
        new SelectItem(ShareRoles.ADMIN, Imeji.RESOURCE_BUNDLE.getLabel("admin", locale)));
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
        final CollectionImeji col =
            collectionController.retrieveLazy(URI.create(grant.getGrantFor()), sessionUser);
        roles.add(new ShareListItem(user, col.getId().toString(), col.getMetadata().getTitle(),
            sessionUser, locale, false));
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
        roles.add(new ShareListItem(group, col.getId().toString(), col.getMetadata().getTitle(),
            sessionUser, locale));
      } catch (final Exception e) {
        LOGGER.error("Error reading grants of user ", e);
      }
    }
    return roles;
  }
}
