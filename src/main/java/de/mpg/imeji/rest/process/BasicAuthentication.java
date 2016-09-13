package de.mpg.imeji.rest.process;

import javax.servlet.http.HttpServletRequest;

import de.mpg.imeji.exceptions.AuthenticationError;
import de.mpg.imeji.logic.user.authentication.Authentication;
import de.mpg.imeji.logic.user.authentication.factory.AuthenticationFactory;
import de.mpg.imeji.logic.vo.User;

/**
 * Helper the manage the authentication in the API
 *
 * @author bastiens
 *
 */
public class BasicAuthentication {

  public static User auth(HttpServletRequest req) throws AuthenticationError {
    Authentication auth = AuthenticationFactory.factory(req);
    User u = auth.doLogin();
    return u;
  }

  public static User auth(String authorizationHeader) throws AuthenticationError {
    Authentication auth = AuthenticationFactory.factory(authorizationHeader);
    User u = auth.doLogin();
    return u;
  }
}
