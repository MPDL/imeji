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
import java.net.URI;

import org.apache.log4j.Logger;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.Imeji;
import de.mpg.imeji.logic.export.format.ExplainExport;
import de.mpg.imeji.logic.search.SearchQueryParser;
import de.mpg.imeji.logic.search.SearchResult;
import de.mpg.imeji.logic.search.model.SearchIndex;
import de.mpg.imeji.logic.vo.CollectionImeji;
import de.mpg.imeji.logic.vo.Statement;
import de.mpg.imeji.presentation.util.ObjectCachedLoader;
import de.mpg.imeji.presentation.util.ObjectLoader;

/**
 * {@link ExplainExport} for the metadata search index
 * 
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public class MetadataExplainExport extends ExplainExport {

  private static final Logger LOGGER = Logger.getLogger(MetadataExplainExport.class);

  /*
   * (non-Javadoc)
   * 
   * @see de.mpg.imeji.logic.export.Export#export(java.io.OutputStream,
   * de.mpg.imeji.logic.search.SearchResult)
   */
  @Override
  public void export(OutputStream out, SearchResult sr) {
    PrintWriter writer = new PrintWriter(out);
    try {
      writer.append(getRDFTagOpen());
      for (String colURI : sr.getResults()) {
        try {
          // TODO Change this, logic should not call presentation!!!
          CollectionImeji col = ObjectCachedLoader.loadCollection(URI.create(colURI));
          for (Statement st : ObjectLoader.loadProfile(col.getProfile(), Imeji.adminUser)
              .getStatements()) {
            for (SearchIndex index : SearchIndex.getAllIndexForStatement(st)) {
              writer.append(getIndexTag(
                  SearchQueryParser.transformStatementToIndex(st.getId(), index.getField()),
                  index.getNamespace()));
            }
          }
        } catch (ImejiException iec) {
          LOGGER.info("Could not export the Collection with URI=" + colURI);
        }
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
