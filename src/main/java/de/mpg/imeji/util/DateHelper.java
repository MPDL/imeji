package de.mpg.imeji.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import com.hp.hpl.jena.datatypes.xsd.XSDDateTime;

/**
 * Methods related to {@link Date}
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public class DateHelper {
  private static final ThreadLocal<SimpleDateFormat> format = new ThreadLocal<SimpleDateFormat>() {
    @Override
    protected SimpleDateFormat initialValue() {
      return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX", Locale.getDefault());
    }
  };

  private static final ThreadLocal<SimpleDateFormat> formatSmall =
      new ThreadLocal<SimpleDateFormat>() {
        @Override
        protected SimpleDateFormat initialValue() {
          return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        }
      };

  private static final ThreadLocal<SimpleDateFormat> formatDateAndTime =
      new ThreadLocal<SimpleDateFormat>() {
        @Override
        protected SimpleDateFormat initialValue() {
          return new SimpleDateFormat("yyyy-MM-dd', 'HH:mm", Locale.getDefault());
        }
      };


  /**
   * Return the current {@link Calendar} from the system
   *
   * @return
   */
  public static Calendar getCurrentDate() {
    final Calendar cal = Calendar.getInstance();
    return cal;
  }

  /**
   * Parse a {@link String} to a {@link Calendar}
   *
   * @param dateString
   * @return
   */
  public static Calendar parseDate(String dateString) {
    try {
      final Date d = format.get().parse(dateString);
      final Calendar cal = Calendar.getInstance();
      cal.setTime(d);
      return cal;
    } catch (final ParseException e) {
      throw new RuntimeException(
          "Error parsing date " + dateString + ": Format should be yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
          e);
    }
  }

  /**
   * True if the string is a well formed date
   * 
   * @param dateString
   * @return
   */
  public static boolean isValidDate(String dateString) {
    try {
      return DateFormatter.format(dateString) != null;
    } catch (Exception e) {
      return false;
    }
  }

  /**
   * Print a Date into a String: 2017-09.07
   *
   * @param c
   * @return
   */
  public static String printDate(Calendar c) {
    return formatSmall.get().format(c.getTime());
  }


  /**
   * Print a date so: 2017-09.07, 09:31
   * 
   * @param c
   * @return
   */
  public static String printDateWithTime(Calendar c) {
    return formatDateAndTime.get().format(c.getTime());
  }

  /**
   * Print a Calendar as formatted to jena
   * 
   * @param c
   * @return
   */
  public static String printJenaDate(Calendar c) {
    return new XSDDateTime(c).toString();
  }

  /**
   * Return the time as a calendar
   *
   * @param time
   * @return
   */
  public static Calendar getDate(long time) {
    final Date d = new Date(time);
    final Calendar cal = Calendar.getInstance();
    cal.setTime(d);
    return cal;
  }

}
