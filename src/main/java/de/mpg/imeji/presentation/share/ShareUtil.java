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
import de.mpg.imeji.logic.controller.business.ItemBusinessController;
import de.mpg.imeji.logic.controller.resource.AlbumController;
import de.mpg.imeji.logic.controller.resource.CollectionController;
import de.mpg.imeji.logic.security.util.SecurityUtil;
import de.mpg.imeji.logic.user.collaboration.share.ShareBusinessController.ShareRoles;
import de.mpg.imeji.logic.vo.Album;
import de.mpg.imeji.logic.vo.CollectionImeji;
import de.mpg.imeji.logic.vo.Grant;
import de.mpg.imeji.logic.vo.Item;
import de.mpg.imeji.logic.vo.User;
import de.mpg.imeji.logic.vo.UserGroup;
import de.mpg.imeji.presentation.share.ShareBean.SharedObjectType;

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
   * Menu for sharing collection
   *
   * @return
   */
  public static List<SelectItem> getCollectionRoleMenu(String profileUri, User user,
      Locale locale) {
    List<SelectItem> collectionRoleMenu = new ArrayList<>();
    collectionRoleMenu.add(new SelectItem(ShareRoles.READ,
        Imeji.RESOURCE_BUNDLE.getLabel("collection_share_read", locale)));
    collectionRoleMenu.add(new SelectItem(ShareRoles.CREATE,
        Imeji.RESOURCE_BUNDLE.getLabel("collection_share_image_upload", locale)));
    collectionRoleMenu.add(new SelectItem(ShareRoles.EDIT_ITEM,
        Imeji.RESOURCE_BUNDLE.getLabel("collection_share_image_edit", locale)));
    collectionRoleMenu.add(new SelectItem(ShareRoles.DELETE_ITEM,
        Imeji.RESOURCE_BUNDLE.getLabel("collection_share_image_delete", locale)));
    collectionRoleMenu.add(new SelectItem(ShareRoles.EDIT,
        Imeji.RESOURCE_BUNDLE.getLabel("collection_share_collection_edit", locale)));
    if (SecurityUtil.staticAuth().administrate(user, profileUri)) {
      collectionRoleMenu.add(new SelectItem(ShareRoles.EDIT_PROFILE,
          Imeji.RESOURCE_BUNDLE.getLabel("collection_share_profile_edit", locale)));
    }
    collectionRoleMenu.add(new SelectItem(ShareRoles.ADMIN,
        Imeji.RESOURCE_BUNDLE.getLabel("collection_share_admin", locale)));
    return collectionRoleMenu;
  }

  /**
   * Menu for sharing items
   *
   * @return
   */
  public static List<SelectItem> getItemRoleMenu(Locale locale) {
    return Arrays.asList(new SelectItem(ShareRoles.READ,
        Imeji.RESOURCE_BUNDLE.getLabel("collection_share_read", locale)));
  }

  /**
   * Menu for sharing Album
   *
   * @return
   */
  public static List<SelectItem> getAlbumRoleMenu(Locale locale) {
    List<SelectItem> albumRoleMenu = Arrays.asList(
        new SelectItem(ShareRoles.READ, Imeji.RESOURCE_BUNDLE.getLabel("album_share_read", locale)),
        new SelectItem(ShareRoles.CREATE,
            Imeji.RESOURCE_BUNDLE.getLabel("album_share_image_add", locale)),
        new SelectItem(ShareRoles.EDIT,
            Imeji.RESOURCE_BUNDLE.getLabel("album_share_album_edit", locale)),
        new SelectItem(ShareRoles.ADMIN,
            Imeji.RESOURCE_BUNDLE.getLabel("album_share_admin", locale)));
    return albumRoleMenu;
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
    List<String> shareToList = new ArrayList<String>();
    for (Grant g : user.getGrants()) {
      if (g.getGrantFor() != null) {
        if (!shareToList.contains(g.getGrantFor().toString())) {
          shareToList.add(g.getGrantFor().toString());
        }
      }
    }
    List<ShareListItem> roles = new ArrayList<ShareListItem>();
    for (String sharedWith : shareToList) {
      try {
        if (sharedWith.contains("/collection/")) {
          CollectionImeji c =
              new CollectionController().retrieveLazy(URI.create(sharedWith), sessionUser);
          if (c != null) {
            roles.add(new ShareListItem(user, SharedObjectType.COLLECTION, sharedWith,
                c.getProfile() != null ? c.getProfile().toString() : null,
                c.getMetadata().getTitle(), sessionUser, locale));
          }
        } else if (sharedWith.contains("/album/")) {
          Album a = new AlbumController().retrieveLazy(URI.create(sharedWith), sessionUser);
          if (a != null) {
            roles.add(new ShareListItem(user, SharedObjectType.ALBUM, sharedWith, null,
                a.getMetadata().getTitle(), sessionUser, locale));
          }
        } else if (sharedWith.contains("/item/")) {
          Item it = new ItemBusinessController().retrieveLazy(URI.create(sharedWith), sessionUser);
          if (it != null) {
            roles.add(new ShareListItem(user, SharedObjectType.ITEM, sharedWith, null,
                it.getFilename(), sessionUser, locale));
          }
        }
      } catch (Exception e) {
        LOGGER.error("Problem by getting user roles for user " + user.getEmail(), e);
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
    List<String> shareToList = new ArrayList<String>();
    for (Grant g : group.getGrants()) {
      if (!shareToList.contains(g.getGrantFor().toString())) {
        shareToList.add(g.getGrantFor().toString());
      }
    }
    List<ShareListItem> roles = new ArrayList<ShareListItem>();
    for (String sharedWith : shareToList) {
      if (sharedWith.contains("/collection/")) {
        CollectionImeji c =
            new CollectionController().retrieveLazy(URI.create(sharedWith), sessionUser);
        String profileId = c.getProfile() != null ? c.getProfile().toString() : null;
        roles.add(new ShareListItem(group, SharedObjectType.COLLECTION, sharedWith, profileId,
            c.getMetadata().getTitle(), sessionUser, locale));
      } else if (sharedWith.contains("/album/")) {
        Album a = new AlbumController().retrieveLazy(URI.create(sharedWith), sessionUser);
        roles.add(new ShareListItem(group, SharedObjectType.ALBUM, sharedWith, null,
            a.getMetadata().getTitle(), sessionUser, locale));
      } else if (sharedWith.contains("/item/")) {
        ItemBusinessController c = new ItemBusinessController();
        Item it = c.retrieveLazy(URI.create(sharedWith), sessionUser);
        roles.add(new ShareListItem(group, SharedObjectType.ITEM, sharedWith, null,
            it.getFilename(), sessionUser, locale));
      }
    }
    return roles;
  }
}
