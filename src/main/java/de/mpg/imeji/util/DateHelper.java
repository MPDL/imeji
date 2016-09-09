package de.mpg.imeji.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

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

  /**
   * Return the current {@link Calendar} from the system
   *
   * @return
   */
  public static Calendar getCurrentDate() {
    Calendar cal = Calendar.getInstance();
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
      Date d = format.get().parse(dateString);
      Calendar cal = Calendar.getInstance();

      cal.setTime(d);
      return cal;
    } catch (ParseException e) {
      throw new RuntimeException(
          "Error parsing date " + dateString + ": Format should be yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
          e);
    }
  }

  /**
   * Print a Date into a String
   *
   * @param c
   * @return
   */
  public static String printDate(Calendar c) {
    return formatSmall.get().format(c.getTime());
  }
}
