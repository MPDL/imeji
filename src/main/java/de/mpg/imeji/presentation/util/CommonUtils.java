package de.mpg.imeji.presentation.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Common utility class
 *
 * @author bastiens
 *
 */
public class CommonUtils {
  private static final Pattern REMOVE_TAGS = Pattern.compile("<.+?>");

  /**
   * Private Constructor
   */
  private CommonUtils() {
    // avoid to construct
  }

  /**
   * Remove html tags from a {@link String}
   *
   * @param string
   * @return
   */
  public static String removeTags(String string) {
    if (string == null || string.length() == 0) {
      return string;
    }
    final Matcher m = REMOVE_TAGS.matcher(string);
    return m.replaceAll("").trim();
  }

  /**
   * From a {@link String} extract the value of a field, when define in the String by field:value
   *
   * @param pattern
   * @param s
   * @return
   */
  public static String extractFieldValue(String field, String s) {
    Pattern p = Pattern.compile("\\b" + field + ":" + ".*\\s", Pattern.CASE_INSENSITIVE);
    String r = executeAndReturnFirstResult(p, s);
    if (r == null) {
      p = Pattern.compile("\\b" + field + ":" + ".*\\b", Pattern.CASE_INSENSITIVE);
      r = executeAndReturnFirstResult(p, s);
    }
    if (r != null) {
      return r.replace(field + ":", "");
    }
    return null;
  }

  /**
   * Execute a {@link Pattern} and return the first result
   *
   * @param p
   * @param s
   * @return
   */
  public static String executeAndReturnFirstResult(Pattern p, String s) {
    final Matcher m = p.matcher(s);
    if (m.find()) {
      return m.group();
    }
    return null;
  }
}
