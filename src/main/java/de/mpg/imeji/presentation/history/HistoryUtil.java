/**
 * License: src/main/resources/license/escidoc.license
 */
package de.mpg.imeji.presentation.history;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.mpg.imeji.logic.util.ObjectHelper;
import de.mpg.imeji.logic.util.UrlHelper;
import de.mpg.imeji.logic.vo.Album;
import de.mpg.imeji.logic.vo.CollectionImeji;
import de.mpg.imeji.logic.vo.Item;
import de.mpg.imeji.logic.vo.MetadataProfile;
import de.mpg.imeji.logic.vo.User;
import de.mpg.imeji.logic.vo.UserGroup;

/**
 * Helper for {@link URI} of {@link HistoryPage}
 * 
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public class HistoryUtil {
  private static Pattern findItemId = Pattern.compile(ImejiPages.ITEM_DETAIL.getRegex());
  private static Pattern findCollectionId = Pattern.compile(ImejiPages.COLLECTION_HOME.getRegex());
  private static Pattern findAlbumId = Pattern.compile(ImejiPages.ALBUM_HOME.getRegex());
  private static Pattern findUserGroupId = Pattern.compile(ImejiPages.USER_GROUP.getRegex());
  private static Pattern findUserId = Pattern.compile(ImejiPages.USER.getRegex());
  private static Pattern findProfileId = Pattern.compile(ImejiPages.PROFILE.getRegex());

  /**
   * Extract the URI of an imeji object from the url
   **/
  public static URI extractURI(String url) {
    Matcher m = findItemId.matcher(url);
    if (m.find()) {
      return ObjectHelper.getURI(Item.class, m.group(1));
    }
    m = findCollectionId.matcher(url);
    if (m.find()) {
      return ObjectHelper.getURI(CollectionImeji.class, m.group(1));
    }
    m = findAlbumId.matcher(url);
    if (m.find()) {
      return ObjectHelper.getURI(Album.class, m.group(1));
    }
    m = findUserId.matcher(url);
    if (m.find()) {
      return ObjectHelper.getURI(User.class, UrlHelper.decode(m.group(1)));
    }
    m = findUserGroupId.matcher(url);
    if (m.find()) {
      return ObjectHelper.getURI(UserGroup.class, UrlHelper.decode(m.group(1)));
    }

    m = findProfileId.matcher(url);
    if (m.find()) {
      return ObjectHelper.getURI(MetadataProfile.class, UrlHelper.decode(m.group(2)));
    }

    return null;
  }

  /**
   * Return the {@link ImejiPages} for the url
   * 
   * @param url
   * @return
   */
  public static ImejiPages getImejiPage(String url) {
    for (ImejiPages page : ImejiPages.values()) {
      if (page.matches(url)) {
        return page;
      }
    }
    return ImejiPages.HOME;
  }

  /**
   * Return the label of a page according to its url
   **/
  public static String getPageLabel(String url) {
    if (url == null)
      return "history_home";

    for (ImejiPages page : ImejiPages.values()) {
      if (url.matches(page.getRegex())) {
        return page.getLabel();
      }
    }
    return "history_home";
  }

  /**
   * return a {@link Map} of http paramter into on String as displayed in the url
   * 
   * @param params
   * @return
   */
  public static String paramsMapToString(Map<String, String[]> params) {
    String s = "";
    for (String key : params.keySet()) {
      String delim = "".equals(s) ? "?" : "&";
      try {
        s += delim + key + "=" + URLEncoder.encode(params.get(key)[0], "UTF-8");
      } catch (UnsupportedEncodingException e) {
        throw new RuntimeException("Error encoding " + params.get(key)[0], e);
      }
    }
    return s;
  }
}
