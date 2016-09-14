package de.mpg.imeji.presentation.util;

import javax.servlet.DispatcherType;
import javax.servlet.Filter;
import javax.servlet.Servlet;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;

/**
 * Utility class for {@link Servlet} and {@link Filter}
 *
 * @author bastiens
 *
 */
public class ServletUtil {

  private ServletUtil() {
    // private constructor
  }

  /**
   * True if the {@link ServletRequest} is an HTTP GET
   *
   * @param serv
   * @return
   */
  public static boolean isGetRequest(ServletRequest serv) {
    if (DispatcherType.FORWARD.compareTo(serv.getDispatcherType()) == 0) {
      HttpServletRequest request = (HttpServletRequest) serv;
      if ("GET".equals(request.getMethod())) {
        return true;
      }
    }
    return false;
  }


  /**
   * Return a Session Object
   *
   * @param req
   * @param classSimpleName
   * @return
   */
  public static Object getSession(HttpServletRequest req, String classSimpleName) {
    return req.getSession(true).getAttribute(classSimpleName);
  }
}
