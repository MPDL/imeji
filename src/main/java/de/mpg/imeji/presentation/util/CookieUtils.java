package de.mpg.imeji.presentation.util;

import java.util.concurrent.TimeUnit;

import javax.faces.context.FacesContext;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

/**
 * Utility Class for http cookies
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public class CookieUtils {
  /**
   * private Constructor
   */
  private CookieUtils() {
    // avoid construction
  }

  /**
   * Read a cookie. If not found, the default value is returned. After the cookie has been read, the
   * cookie is updated (new max age, default value set if needed)
   *
   * @param name
   * @param defaultValue
   * @return the cookies value
   */
  public static String readNonNull(String name, String defaultValue) {
    // read Cookie from current http request
	Cookie c = readCookie(name);
    if (c == null) {
      c = new Cookie(name, defaultValue);
    }
    // set cookie in http response
    updateCookie(c);
    return c.getValue();
  }

  /**
   * Read a cookie from the current http request. 
   * If the cookie doesn't exist, return null
   *
   * @param name name of the cookie to read
   * @return retrieved Cookie
   */
  public static Cookie readCookie(String name) {
    return (Cookie) FacesContext.getCurrentInstance().getExternalContext().getRequestCookieMap()
        .get(name);
  }

  /**
   * Update a cookie (the max age is updated too)
   *
   * @param c
   */
  private static void updateCookie(Cookie c) {
    if (c != null) {
      // set max age of cookie
      setCookieProperties(c);
      ((HttpServletResponse) FacesContext.getCurrentInstance().getExternalContext().getResponse())
          .addCookie(c);
    }
  }

  /**
   * Update a cookie (the max age is updated too)
   *
   * @param c
   */
  public static void updateCookieValue(String name, String newValue) {
    Cookie c = readCookie(name);
    if (c == null) {
      c = new Cookie(name, newValue);
    }
    c.setValue(newValue);
    updateCookie(c);
  }

  /**
   * Set the max age of a cookie to 30 days, set the cookie Path
   *
   * @param c
   */
  public static void setCookieProperties(Cookie c) {
    c.setMaxAge((int) (TimeUnit.SECONDS.convert(365, TimeUnit.DAYS)));
    c.setPath("/");
  }
}
