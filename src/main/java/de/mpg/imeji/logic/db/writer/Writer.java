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
package de.mpg.imeji.logic.db.writer;

import java.util.List;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.vo.User;

/**
 * Write imeji objects in the persistence layer. Important: {@link Writer} doens't check
 * Authorization. Please use {@link WriterFacade} instead.
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public interface Writer {
  /**
   * Create a list of objects
   *
   * @param objects
   * @param user
   * @throws ImejiException
   */
  public void create(List<Object> objects, User user) throws ImejiException;

  /**
   * Delete a list of objects
   *
   * @param objects
   * @param user
   * @throws ImejiException
   */
  public void delete(List<Object> objects, User user) throws ImejiException;

  /**
   * Update a list of objects
   *
   * @param objects
   * @param user
   * @throws ImejiException
   */
  public void update(List<Object> objects, User user) throws ImejiException;

  /**
   * Lazy Update a list of objects (don't update lazy list)
   *
   * @param objects
   * @param user
   * @throws ImejiException
   */
  public void updateLazy(List<Object> objects, User user) throws ImejiException;
}
