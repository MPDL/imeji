package de.mpg.imeji.presentation.history;

/**
 * Pages of imeji. This pages are used by the {@link HistoryFilter}
 *
 * @author saquet
 *
 */
public enum ImejiPages {

  /*
   * Order is important: The History will follow the regex rules following this order to find a
   * match MAKE SURE "HOME" (history_home) is the last in this Enum (experienced not-matching where
   * it should otherwise)
   */
  ITEM_DETAIL("", ".*/item/([A-Za-z0-9-_]+).*"), COLLECTIONS("history_collections",
      "/collections"), COLLECTION_ITEMS("history_images_collection",
          ".*/collection/([A-Za-z0-9-_]+)/browse.*"), COLLECTION_UPLOAD("history_upload",
              ".*/collection/([A-Za-z0-9-_]+)/upload.*"), COLLECTION_INFOS(
                  "history_collection_info",
                  ".*/collection/([A-Za-z0-9-_]+)/infos.*"), COLLECTION_HOME("collection",
                      ".*/collection/([A-Za-z0-9-_]+).*"), ALBUMS("albums", "/albums"), ALBUM_ITEMS(
                          "history_images_album",
                          ".*/album/([A-Za-z0-9-_]+)/browse.*"), ALBUM_INFOS("history_album_info",
                              ".*/album/([A-Za-z0-9-_]+)/infos.*"), ALBUM_HOME("history_album",
                                  ".*/album/([A-Za-z0-9-_]+).*"), ADMINISTRATION("admin",
                                      "/admin"), USER("user", ".*/user\\?id=(.*)&?"), USERS(
                                          "admin_info_users",
                                          "/users"), USER_GROUP_CREATE("admin_userGroup_new",
                                              "/createusergroup"), USER_CREATE("admin_user_new",
                                                  "/createuser"), USER_GROUP("admin_userGroup",
                                                      ".*/usergroup\\?id=(.*)&?"), USER_GROUPS(
                                                          "admin_userGroups_view",
                                                          "/usergroups"), HELP("help",
                                                              "/help"), SEARCH(
                                                                  "history_advanced_search",
                                                                  "/search"), BATCH_EDIT_ITEMS(
                                                                      "edit_images",
                                                                      ".*/edit\\?.*type=.*"), UPLOAD(
                                                                          "upload",
                                                                          "/singleupload"), BROWSE(
                                                                              "history_images",
                                                                              "/browse"), CREATE_COLLECTION(
                                                                                  "collection_create",
                                                                                  ".*/createcollection.*"), CREATE_ALBUM(
                                                                                      "album_create",
                                                                                      ".*/createalbum"), PROFILE(
                                                                                          "metadata_profile",
                                                                                          ".*/(profile|metadataProfile)/([a-zA-Z_0-9-]+).*"), HOME(
                                                                                              "history_home",
                                                                                              "/");

  /**
   * The key to the label
   */
  private String label;
  /**
   * The regex to match the page from the current url
   */
  private String regex;

  /**
   * Constructor for {@link ImejiPages}
   *
   * @param label
   * @param regex
   */
  ImejiPages(String label, String regex) {
    this.setLabel(label);
    this.setRegex(regex);
  }

  /**
   * True is the {@link ImejiPages} matches the String
   *
   * @param s
   * @return
   */
  public boolean matches(String s) {
    return s.matches(regex) || s.contains(regex);
  }

  public String getLabel() {
    return label;
  }

  public void setLabel(String label) {
    this.label = label;
  }

  public String getRegex() {
    return regex;
  }

  public void setRegex(String regex) {
    this.regex = regex;
  }
}
