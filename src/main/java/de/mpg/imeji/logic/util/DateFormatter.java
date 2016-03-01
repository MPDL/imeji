/**
 * License: src/main/resources/license/escidoc.license
 */
package de.mpg.imeji.logic.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.regex.Pattern;

/**
 * Utility class to format imeji metadata {@link de.mpg.imeji.logic.vo.predefinedMetadata.Date}
 * 
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public class DateFormatter {

  private DateFormatter() {}

  /**
   * Return the time of the {@link Date}, if the format is recognized
   * 
   * @param str
   * @return
   */
  public static long getTime(String str) {
    Date d = parseDate(str, "yyyy-MM-dd");
    if (d == null) {
      d = parseDate(str, "yyyy-MM");
    }
    if (d == null) {
      d = parseDate(str, "yyyy");
    }
    if (d != null) {
      return d.getTime();
    } else {
      return Long.MIN_VALUE;
    }
  }

  /**
   * Parse a {@link Date} as {@link String} according to a {@link Pattern}
   * 
   * @param str
   * @param pattern
   * @return
   */
  public static Date parseDate(String str, String pattern) {
    try {
      SimpleDateFormat sdf = new SimpleDateFormat(pattern);
      return sdf.parse(str);
    } catch (ParseException e) {
      return null;
    }
  }

  /**
   * Format a {@link String} as a {@link Date}
   * 
   * @param str
   * @return
   */
  public static String format(String str) {
    Date d = parseDate(str, "yyyy-MM-dd");
    if (d == null) {
      d = parseDate(str, "yyyy-MM");
    }
    if (d == null) {
      d = parseDate(str, "yyyy");
    }
    if (d != null) {
      SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
      return sdf.format(d);
    } else {
      throw new RuntimeException("Wrong date format");
    }
  }

  /**
   * Return a date in a conform format for SPARQL Queries (for instance: 2014-04-02T15:17:22.833Z)
   * 
   * @param str
   * @return
   */
  public static String formatToSparqlDateTime(String str) {
    Date d = parseDate(str, "yyyy-MM-dd'T'HH:mm:ss.SSSZ");
    if (d == null) {
      d = parseDate(str, "yyyy-MM-dd'T'HH:mm:ss.SSS");
    }
    if (d == null) {
      d = parseDate(str, "yyyy-MM-dd'T'HH:mm:ss");
    }
    if (d == null) {
      d = new Date(getTime(str));
    }
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
    return sdf.format(d);
  }

  /**
   * Return a date in a conform format for SPARQL Queries (for instance: 2014-04-02T15:17:22.833Z)
   * 
   * @param str
   * @return
   */
  public static String formatToSparqlDateTime(Calendar cal) {
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
    return sdf.format(cal.getTime());
  }
}
