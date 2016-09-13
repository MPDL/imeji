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
package de.mpg.imeji.logic.user.authentication;

import de.mpg.imeji.exceptions.AuthenticationError;
import de.mpg.imeji.logic.vo.User;

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
