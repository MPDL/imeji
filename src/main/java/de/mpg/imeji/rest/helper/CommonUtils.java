package de.mpg.imeji.rest.helper;

import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 *
 * @author bastiens
 *
 */
public class CommonUtils {

  /**
   * Format a Date in a user friendly way
   *
   * @param d
   * @return
   */
  public static String formatDate(Date d) {
    String output = "";
    SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd");
    output = f.format(d);
    f = new SimpleDateFormat("HH:mm:ss Z");
    output += "T" + f.format(d);
    return output;

  }

  public static String extractIDFromURI(URI uri) {
    return uri.getPath().substring(uri.getPath().lastIndexOf("/") + 1);
  }

}
