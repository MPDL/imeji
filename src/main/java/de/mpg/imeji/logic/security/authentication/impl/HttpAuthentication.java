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

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.codec.binary.Base64;

import de.mpg.imeji.exceptions.AuthenticationError;
import de.mpg.imeji.logic.security.authentication.Authentication;
import de.mpg.imeji.logic.vo.User;

/**
 * {@link Authentification} for {@link HttpServletRequest}
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public class HttpAuthentication implements Authentication {
  /**
   * The content of the http header
   */
  private String usernamePassword = null;
  private String apiKey = null;

  /**
   * Constructor with {@link HttpServletRequest}
   */
  public HttpAuthentication(HttpServletRequest request) {
    this(request.getHeader("Authorization"));
  }

  /**
   * Constructor with the authorization header
   *
   * @param authorizationHeader
   */
  public HttpAuthentication(String authorizationHeader) {
    parseAuthorizationHeader(authorizationHeader);
  }

  /*
   * (non-Javadoc)
   *
   * @see de.mpg.imeji.logic.auth.Authentification#doLogin()
   */
  @Override
  public User doLogin() throws AuthenticationError {
    if (apiKey != null) {
      APIKeyAuthentication keyAuthentication = new APIKeyAuthentication(apiKey);
      return keyAuthentication.doLogin();
    } else if (usernamePassword != null) {
      int p = usernamePassword.indexOf(":");
      if (p != -1) {
        return new DefaultAuthentication(getUserLogin(), getUserPassword()).doLogin();
      }
    }
    // not logged in
    return null;
  }


  /**
   * Parse the authprization header and set the variables
   *
   * @param authHeader
   */
  private void parseAuthorizationHeader(String authHeader) {
    if (authHeader != null && authHeader.contains("Basic")) {
      usernamePassword =
          new String(Base64.decodeBase64(authHeader.replace("Basic ", "").trim().getBytes()));
    }
    if (authHeader != null && authHeader.contains("Bearer ")) {
      apiKey = authHeader.replace("Bearer ", "").trim();
    }
  }

  /*
   * (non-Javadoc)
   *
   * @see de.mpg.imeji.logic.auth.Authentification#getUserLogin()
   */
  @Override
  public String getUserLogin() {
    if (usernamePassword != null) {
      int p = usernamePassword.indexOf(":");
      if (p != -1) {
        return usernamePassword.substring(0, p);
      }
    }
    return null;
  }

  /*
   * (non-Javadoc)
   *
   * @see de.mpg.imeji.logic.auth.Authentification#getUserPassword()
   */
  @Override
  public String getUserPassword() {
    if (usernamePassword != null) {
      int p = usernamePassword.indexOf(":");
      if (p != -1) {
        return usernamePassword.substring(p + 1);
      }
    }
    return null;
  }

  /**
   * True if the request has informations about the login (user and password)
   *
   * @return
   */
  public boolean hasLoginInfos() {
    return usernamePassword != null;
  }
}
