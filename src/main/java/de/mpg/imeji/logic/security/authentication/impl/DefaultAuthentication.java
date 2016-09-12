/*
 *
 * CDDL HEADER START
 *
 * The contents of this file are subject to the terms of the Common Development and Distribution
 * License, Version 1.0 only (the "License"). You may not use this file except in compliance with
 * the License.
 *
 * You can obtain a copy of the license at license/ESCIDOC.LICENSE or http://www.escidoc.de/license.
 * See the License for the specific language governing permissions and limitations under the
 * License.
 *
 * When distributing Covered Code, include this CDDL HEADER in each file and include the License
 * file at license/ESCIDOC.LICENSE. If applicable, add the following below this CDDL HEADER, with
 * the fields enclosed by brackets "[]" replaced with your own identifying information: Portions
 * Copyright [yyyy] [name of copyright owner]
 *
 * CDDL HEADER END
 */
/*
 * Copyright 2006-2007 Fachinformationszentrum Karlsruhe Gesellschaft für
 * wissenschaftlich-technische Information mbH and Max-Planck- Gesellschaft zur Förderung der
 * Wissenschaft e.V. All rights reserved. Use is subject to license terms.
 */
package de.mpg.imeji.logic.security.authentication.impl;

import org.apache.log4j.Logger;

import de.mpg.imeji.exceptions.AuthenticationError;
import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.exceptions.InactiveAuthenticationError;
import de.mpg.imeji.logic.Imeji;
import de.mpg.imeji.logic.security.authentication.Authentication;
import de.mpg.imeji.logic.user.controller.UserBusinessController;
import de.mpg.imeji.logic.util.StringHelper;
import de.mpg.imeji.logic.vo.User;

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
      user = new UserBusinessController().retrieve(getUserLogin(), Imeji.adminUser);
    } catch (ImejiException e) {
      throw new AuthenticationError(
          "User could not be authenticated with provided credentials! " + getUserLogin());
    }
    if (!user.isActive()) {
      throw new InactiveAuthenticationError(
          "Not active user: please activate your account with the limk sent after your registration");
    }
    try {
      if (user.getEncryptedPassword().equals(StringHelper.convertToMD5(getUserPassword()))) {
        return user;
      }
    } catch (Exception e) {
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
