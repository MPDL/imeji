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
package de.mpg.imeji.logic.export.format.explain;

import java.io.OutputStream;
import java.io.PrintWriter;

import de.mpg.imeji.logic.search.model.SearchIndex;
import de.mpg.imeji.logic.search.model.SearchIndexes;
import de.mpg.imeji.logic.search.model.SearchResult;
import de.mpg.imeji.logic.vo.User;

/**
 * Explain the index for the search
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public class SearchExplainExport extends ExplainExport {
  /*
   * (non-Javadoc)
   *
   * @see de.mpg.imeji.logic.export.Export#export(java.io.OutputStream,
   * de.mpg.imeji.logic.search.SearchResult)
   */
  @Override
  public void export(OutputStream out, SearchResult sr, User user) {
    PrintWriter writer = new PrintWriter(out);
    try {
      writer.append(getRDFTagOpen());
      for (SearchIndex index : SearchIndexes.indexes.values()) {
        writer.append(getIndexTag(index.getName(), index.getNamespace()));
      }
      writer.append(getRDFTagClose());
    } finally {
      writer.close();
    }
  }

  /*
   * (non-Javadoc)
   *
   * @see de.mpg.imeji.logic.export.Export#init()
   */
  @Override
  public void init() {
    // Nothing to to
  }
}
