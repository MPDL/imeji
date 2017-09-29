package de.mpg.imeji.logic.security.authentication.impl;

import org.apache.log4j.Logger;

import de.mpg.imeji.exceptions.AuthenticationError;
import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.exceptions.InactiveAuthenticationError;
import de.mpg.imeji.logic.config.Imeji;
import de.mpg.imeji.logic.model.User;
import de.mpg.imeji.logic.security.authentication.Authentication;
import de.mpg.imeji.logic.security.user.UserService;
import de.mpg.imeji.logic.util.StringHelper;

/**
 * Simple {@link Authentication} in the local database
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public final class DefaultAuthentication implements Authentication {
  private static final Logger LOGGER = Logger.getLogger(DefaultAuthentication.class);
  private final String login;
  private final String pwd;

  /**
   * Constructor
   */
  public DefaultAuthentication(String login, String pwd) {
    this.login = login;
    this.pwd = pwd;
  }

  /*
   * (non-Javadoc)
   *
   * @see de.mpg.imeji.logic.auth.Authentification#doLogin()
   */
  @Override
  public User doLogin() throws AuthenticationError {
    if (StringHelper.isNullOrEmptyTrim(getUserLogin())
        && StringHelper.isNullOrEmptyTrim(getUserPassword())) {
      return null;
    }
    User user;
    try {
      user = new UserService().retrieve(getUserLogin(), Imeji.adminUser);
    } catch (final ImejiException e) {
      throw new AuthenticationError(
          "User could not be authenticated with provided credentials! " + getUserLogin());
    }
    if (!user.isActive()) {
      throw new InactiveAuthenticationError(
          "Not active user: please activate your account with the limk sent after your registration");
    }
    try {
      if (user.getEncryptedPassword().equals(StringHelper.md5(getUserPassword()))) {
        return user;
      }
    } catch (final Exception e) {
      LOGGER.error("Error checking user password", e);
    }

    throw new AuthenticationError("User could not be authenticated with provided credentials!");
  }

  /*
   * (non-Javadoc)
   *
   * @see de.mpg.imeji.logic.auth.Authentification#getUserLogin()
   */
  @Override
  public String getUserLogin() {
    return this.login;
  }

  /*
   * (non-Javadoc)
   *
   * @see de.mpg.imeji.logic.auth.Authentification#getUserPassword()
   */
  @Override
  public String getUserPassword() {
    return this.pwd;
  }
}
