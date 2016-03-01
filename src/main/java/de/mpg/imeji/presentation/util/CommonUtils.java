package de.mpg.imeji.presentation.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.mpg.imeji.logic.vo.Metadata;
import de.mpg.imeji.logic.vo.predefinedMetadata.Geolocation;
import de.mpg.imeji.logic.vo.predefinedMetadata.License;

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
  private CommonUtils() {}

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
    Matcher m = REMOVE_TAGS.matcher(string);
    return m.replaceAll("").trim();
  }

  /**
   * Return a the value of a {@link Metadata} as a {@link String} with all the fields
   * 
   * @param md
   * @return
   */
  public static String toStringCustomField(Metadata md) {
    if (md != null) {
      String s = "";
      if (md instanceof License) {
        if (((License) md).getLicense() != null)
          s += " name:" + ((License) md).getLicense();
        if (((License) md).getExternalUri() != null)
          s += " uri:" + ((License) md).getExternalUri();
      }
      if (md instanceof Geolocation) {
        if (((Geolocation) md).getName() != null)
          s += " name:" + ((Geolocation) md).getName();
        if (!Double.isNaN(((Geolocation) md).getLatitude()))
          s += " lat:" + ((Geolocation) md).getLatitude();
        if (!Double.isNaN(((Geolocation) md).getLongitude()))
          s += " long:" + ((Geolocation) md).getLongitude();
      }
      return s.trim().replace("  ", " ");
    }
    return null;
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
    Matcher m = p.matcher(s);
    if (m.find()) {
      return m.group();
    }
    return null;
  }
}
