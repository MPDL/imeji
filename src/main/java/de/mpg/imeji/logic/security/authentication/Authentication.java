package de.mpg.imeji.logic.security.authentication;

import de.mpg.imeji.exceptions.AuthenticationError;
import de.mpg.imeji.logic.model.User;

/**
 * Authentication abstract class
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public interface Authentication {

  /**
   * Log in a user with a login (email or user name) and password
   *
   * @param login
   * @param pwd
   * @return
   * @throws AuthenticationError
   */
  public User doLogin() throws AuthenticationError;

  /**
   * Get the user Login
   *
   * @return
   */
  public String getUserLogin();

  /**
   * Get the user password
   *
   * @return
   */
  public String getUserPassword();
}
