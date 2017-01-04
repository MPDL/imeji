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
package de.mpg.imeji.logic.db.reader;

import java.util.List;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.vo.User;

/**
 * Object reader interface for imeji. Important: {@link Reader} doens't check Authorization. Please
 * use {@link ReaderFacade} instead.
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public interface Reader {
  /**
   * Read a single object by its uri
   *
   * @param uri
   * @param user
   * @param o
   * @return
   * @throws Exception
   */
  public Object read(String uri, User user, Object o) throws ImejiException;

  /**
   * Read a single Object by its uri (Lazy means don't load Lazy list within the object)
   *
   * @param uri
   * @param user
   * @param o
   * @return
   * @throws Exception
   */
  public Object readLazy(String uri, User user, Object o) throws ImejiException;

  /**
   * Read a List of objects. All objects must have an uri
   *
   * @param objects
   * @param user
   * @return
   * @throws Exception
   */
  public List<Object> read(List<Object> objects, User user) throws ImejiException;

  /**
   * Read a List of objects. All objects must have an uri. (Lazy means don't load Lazy list within
   * the object)
   *
   * @param objects
   * @param user
   * @return
   * @throws Exception
   */
  public List<Object> readLazy(List<Object> objects, User user) throws ImejiException;
}
