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
   * @return
   */
  public static String readNonNull(String name, String defaultValue) {
    Cookie c = readCookie(name);
    if (c == null) {
      c = new Cookie(name, defaultValue);
    }
    updateCookie(c);
    return c.getValue();
  }

  /**
   * Read a cookie. If the cookie doesn't exist, then return null
   *
   * @param name
   * @return
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
  public static void updateCookie(Cookie c) {
    if (c != null) {
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
