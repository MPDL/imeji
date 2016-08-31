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
package de.mpg.imeji.logic.search;

import java.util.List;

import de.mpg.imeji.logic.search.model.SearchQuery;
import de.mpg.imeji.logic.search.model.SearchResult;
import de.mpg.imeji.logic.search.model.SortCriterion;
import de.mpg.imeji.logic.vo.User;

/**
 * Search Interface for imeji
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public interface Search {
  /**
   * Types of search (What objects types are retuned)
   *
   * @author saquet (initial creation)
   * @author $Author$ (last modification)
   * @version $Revision$ $LastChangedDate$
   */
  public static enum SearchObjectTypes {
    ITEM, COLLECTION, ALBUM, SPACE, PROFILE, USER, USERGROUPS, ALL;
  }

  /**
   * Search for imeji objects
   *
   * @param query
   * @param sortCri
   * @param user
   * @param folderUri TODO
   * @param from
   * @param size
   * @return
   */
  public SearchResult search(SearchQuery query, SortCriterion sortCri, User user, String folderUri,
      String spaceId, int offset, int size);

  /**
   * Get the {@link SearchIndexer} for this {@link Search} implementation
   *
   * @return
   */
  public SearchIndexer getIndexer();

  /**
   * Search for imeji objects belonging to a predefined list of possible results
   *
   * @param query
   * @param sortCri
   * @param user
   * @param uris
   * @param spaceId
   * @return
   */
  public SearchResult search(SearchQuery query, SortCriterion sortCri, User user, List<String> uris,
      String spaceId);

  /**
   * Search with a Simple {@link String}
   *
   * @param query
   * @param sort
   * @param user
   * @param from
   * @param size
   * @return
   */
  public SearchResult searchString(String query, SortCriterion sort, User user, int from, int size);
}
