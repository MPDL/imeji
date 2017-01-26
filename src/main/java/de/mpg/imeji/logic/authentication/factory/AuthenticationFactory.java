package de.mpg.imeji.logic.authentication.factory;

import javax.servlet.http.HttpServletRequest;

import de.mpg.imeji.logic.authentication.Authentication;
import de.mpg.imeji.logic.authentication.impl.DefaultAuthentication;
import de.mpg.imeji.logic.authentication.impl.HttpAuthentication;

/**
 * Factory for Authentication
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public class AuthenticationFactory {

  /**
   * Factory with a login and a password
   *
   * @param login
   * @param pwd
   * @return
   */
  public static Authentication factory(String login, String pwd) {
    return new DefaultAuthentication(login, pwd);
  }

  /**
   * Factory for http authentication
   *
   * @param request
   * @return
   */
  public static Authentication factory(HttpServletRequest request) {
    return new HttpAuthentication(request);
  }

  public static Authentication factory(String authorizationHeader) {
    return new HttpAuthentication(authorizationHeader);
  }
}
