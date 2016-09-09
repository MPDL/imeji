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
package de.mpg.imeji.j2j.transaction;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.ReadWrite;
import com.hp.hpl.jena.query.Syntax;
import com.hp.hpl.jena.update.UpdateAction;
import com.hp.hpl.jena.update.UpdateFactory;
import com.hp.hpl.jena.update.UpdateRequest;

import de.mpg.imeji.exceptions.ImejiException;

/**
 * {@link Transaction} for SPARQL update Query
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public class SPARQLUpdateTransaction extends Transaction {
  private String query;

  /**
   * @param modelURI
   */
  public SPARQLUpdateTransaction(String modelURI, String query) {
    super(modelURI);
    this.query = query;
  }

  /*
   * (non-Javadoc)
   *
   * @see de.mpg.j2j.transaction.Transaction#execute(com.hp.hpl.jena.query.Dataset)
   */
  @Override
  protected void execute(Dataset ds) throws ImejiException {
    UpdateRequest request = UpdateFactory.create(query, Syntax.syntaxARQ);
    UpdateAction.execute(request, ds);
  }

  /*
   * (non-Javadoc)
   *
   * @see de.mpg.j2j.transaction.Transaction#getLockType()
   */
  @Override
  protected ReadWrite getLockType() {
    return ReadWrite.WRITE;
  }
}
