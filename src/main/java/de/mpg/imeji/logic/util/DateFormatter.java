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
 * Utility class for dates in inemi
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public class DateFormatter {

  private DateFormatter() {
    // avoid constructor
  }


  /**
   * Return the time of the {@link Date}, if the format is recognized. This method use parseDate()
   *
   * @param str
   * @return
   */
  public static long getTime(String str) {
    Date d = parseDate(str);
    if (d != null) {
      return d.getTime();
    } else {
      return Long.MIN_VALUE;
    }
  }


  /**
   * Return the date from the String as following: <br/>
   * * 2016 -> 01.01.2016 - 00:00 <br/>
   * * 2016-04 -> 01.04.2016 - 00:00 <br/>
   * * 2016-04-14 -> 14.04.2016 - 00:00
   *
   * @param str
   * @return
   */
  public static Date parseDate(String str) {
    Date d = parseDate(str, "yyyy-MM-dd");
    if (d == null) {
      d = parseDate(str, "yyyy-MM");
    }
    if (d == null) {
      d = parseDate(str, "yyyy");
    }
    if (d != null) {
      return d;
    } else {
      return null;
    }
  }

  /**
   * Return the date from the String as following: <br/>
   * * 2016 -> 31.12.2016 - 23:59:59 <br/>
   * * 2016-04 -> 31.04.2016 - 23:59:59 <br/>
   * * 2016-04-14 -> 14.04.2016 - 23:59:59
   *
   * @param str
   * @return
   */
  public static Date parseDate2(String str) {
    int field = Calendar.DAY_OF_MONTH;
    Date d = parseDate(str, "yyyy-MM-dd");
    if (d == null) {
      d = parseDate(str, "yyyy-MM");
      field = Calendar.MONTH;
    }
    if (d == null) {
      d = parseDate(str, "yyyy");
      field = Calendar.YEAR;
    }
    if (d != null) {
      Calendar c = Calendar.getInstance();
      c.setTime(d);
      if (field == Calendar.YEAR) {
        c.set(Calendar.MONTH, c.getActualMaximum(Calendar.MONTH));
        c.set(Calendar.DAY_OF_MONTH, c.getActualMaximum(Calendar.DAY_OF_MONTH));
      } else if (field == Calendar.MONTH) {
        c.set(Calendar.MONTH, c.getActualMaximum(Calendar.MONTH));
      }
      c.set(Calendar.HOUR_OF_DAY, c.getActualMaximum(Calendar.HOUR_OF_DAY));
      c.set(Calendar.MINUTE, c.getActualMaximum(Calendar.MINUTE));
      c.set(Calendar.SECOND, c.getActualMaximum(Calendar.SECOND));
      c.set(Calendar.MILLISECOND, c.getActualMaximum(Calendar.MILLISECOND));
      return c.getTime();
    } else {
      return null;
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
