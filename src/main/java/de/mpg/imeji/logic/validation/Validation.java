package de.mpg.imeji.logic.validation;

import java.util.regex.Pattern;


/**
 * Collection of simple validation methods for different objects
 * 
 * @author breddin
 *
 */

public class Validation {

  /**
   * Regular expression checks whether a given URL follows the https or https scheme. Goal is to
   * make sure that URLs used in imeji start with http:// or https:// so JSF can recognize them as
   * external links.
   * 
   * 1 Does it start with http or https -> (http|https) 2 Followed by :// -> :// 3 (optional)
   * user:password@ section with user consisting of word characters only and password consisting of
   * any sort of characters -> (\\w+:.+@)? 4 domain and sub domain section: for sub domain and
   * domain names we allow all characters except / \ . domain and sub domain names must be separated
   * by a . -> ([^/\\\\]+\\.)+[^\\./\\\\]+ 5 (optional) port :8080 (number is not limited) ->
   * (:\\d+)? 6 (optional) after a / any character allowed -> (/.*)?
   */

  private static String HTTP_SCHEME_REGEX = "(http|https)://(\\w+:.+@)?([^/\\\\]+\\.)+[^\\./\\\\]+(:\\d+)?(/.*)?";



  public static boolean validateURLFormat(String urlString) {
    return Pattern.matches(HTTP_SCHEME_REGEX, urlString);
  }

}
