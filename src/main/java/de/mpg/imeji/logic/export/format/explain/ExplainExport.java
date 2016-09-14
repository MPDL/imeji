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

import de.mpg.imeji.logic.export.format.Export;
import de.mpg.imeji.logic.search.model.SearchIndex;

/**
 * {@link Export} for explain
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public abstract class ExplainExport extends Export {
  /*
   * (non-Javadoc)
   *
   * @see de.mpg.imeji.logic.export.Export#getContentType()
   */
  @Override
  public String getContentType() {
    return "application/xml";
  }

  /**
   * Return a {@link SearchIndex} in rdf
   *
   * @param title
   * @param namespace
   * @param parent
   * @return
   */
  protected String getIndexTag(String title, String namespace) {
    String s = "<imeji:index>" + "<dcterms:title>" + title + "</dcterms:title>"
        + "<imeji:namespace>" + namespace + "</imeji:namespace>" + "</imeji:index>";
    return s;
  }

  /**
   * Return the rdf tag with all namespaces
   *
   * @return
   */
  protected String getRDFTagOpen() {
    return "<?xml version=\"1.0\" encoding=\"UTF-8\"?><rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\" xmlns:imeji=\"http://imeji.org/terms/\" xmlns:dcterms=\"http://purl.org/dc/terms/\">";
  }

  /**
   * Return the tag to close the rdf file
   *
   * @return
   */
  protected String getRDFTagClose() {
    return "</rdf:RDF>";
  }
}
