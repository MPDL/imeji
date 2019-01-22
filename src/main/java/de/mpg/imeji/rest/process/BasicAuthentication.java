package de.mpg.imeji.rest.process;

import javax.servlet.http.HttpServletRequest;

import de.mpg.imeji.exceptions.AuthenticationError;
import de.mpg.imeji.logic.model.User;
import de.mpg.imeji.logic.security.authentication.Authentication;
import de.mpg.imeji.logic.security.authentication.factory.AuthenticationFactory;

/**
 * Helper the manage the authentication in the API
 *
 * @author bastiens
 *
 */
public class BasicAuthentication {

  public static User auth(HttpServletRequest req) throws AuthenticationError {
    final Authentication auth = AuthenticationFactory.factory(req);
    final User u = auth.doLogin();
    return u;
  }

  public static User auth(String authorizationHeader) throws AuthenticationError {
    final Authentication auth = AuthenticationFactory.factory(authorizationHeader);
    final User u = auth.doLogin();
    return u;
  }
}
