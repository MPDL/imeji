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
package de.mpg.imeji.logic.reader;

import java.util.Arrays;
import java.util.List;

import de.mpg.imeji.exceptions.AuthenticationError;
import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.exceptions.NotAllowedError;
import de.mpg.imeji.exceptions.NotFoundException;
import de.mpg.imeji.j2j.helper.J2JHelper;
import de.mpg.imeji.logic.security.authorization.Authorization;
import de.mpg.imeji.logic.security.util.AuthUtil;
import de.mpg.imeji.logic.vo.User;

/**
 * Facade for using {@link Reader}. Check {@link Authorization} to readen objects
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public class ReaderFacade implements Reader {
  private Reader reader;

  /**
   * Constructor for a reader within a model
   */
  public ReaderFacade(String modelURI) {
    this.reader = ReaderFactory.create(modelURI);
  }

  /*
   * (non-Javadoc)
   *
   * @see de.mpg.imeji.logic.reader.Reader#read(java.lang.String, de.mpg.imeji.logic.vo.User,
   * java.lang.Object)
   */
  @Override
  public Object read(String uri, User user, Object o) throws ImejiException {
    o = reader.read(uri, user, o);
    checkSecurity(Arrays.asList(o), user);
    return o;
  }

  /*
   * (non-Javadoc)
   *
   * @see de.mpg.imeji.logic.reader.Reader#readLazy(java.lang.String, de.mpg.imeji.logic.vo.User,
   * java.lang.Object)
   */
  @Override
  public Object readLazy(String uri, User user, Object o) throws ImejiException {
    o = reader.readLazy(uri, user, o);
    if (o == null) {
      throw new NotFoundException("Object is not found or authentication is required.");
    }
    checkSecurity(Arrays.asList(o), user);
    return o;
  }

  /*
   * (non-Javadoc)
   *
   * @see de.mpg.imeji.logic.reader.Reader#read(java.util.List, de.mpg.imeji.logic.vo.User)
   */
  @Override
  public List<Object> read(List<Object> l, User user) throws ImejiException {
    l = reader.read(l, user);
    checkSecurity(l, user);
    return l;
  }

  /*
   * (non-Javadoc)
   *
   * @see de.mpg.imeji.logic.reader.Reader#readLazy(java.util.List, de.mpg.imeji.logic.vo.User)
   */
  @Override
  public List<Object> readLazy(List<Object> l, User user) throws ImejiException {
    l = reader.readLazy(l, user);
    checkSecurity(l, user);
    return l;
  }


  /**
   * @param list
   * @param user
   * @throws ImejiException
   */
  private void checkSecurity(List<Object> list, User user) throws ImejiException {
    for (int i = 0; i < list.size(); i++) {
      if (!AuthUtil.staticAuth().read(user, list.get(i))) {
        String id = J2JHelper.getId(list.get(i)).toString();
        String email = "Not logged in";
        if (user != null) {
          email = user.getEmail();
          throw new NotAllowedError(email + " not allowed to read " + id);
        } else if (user == null) {
          throw new AuthenticationError("Authentication is required for " + id);
        }
      }
    }
  }


}
