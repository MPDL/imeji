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
package de.mpg.imeji.logic.user.controller;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.log4j.Logger;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.Imeji;
import de.mpg.imeji.logic.controller.resource.GroupController;
import de.mpg.imeji.logic.search.Search;
import de.mpg.imeji.logic.search.Search.SearchObjectTypes;
import de.mpg.imeji.logic.search.factory.SearchFactory;
import de.mpg.imeji.logic.search.factory.SearchFactory.SEARCH_IMPLEMENTATIONS;
import de.mpg.imeji.logic.search.jenasearch.JenaCustomQueries;
import de.mpg.imeji.logic.search.model.SearchQuery;
import de.mpg.imeji.logic.search.model.SearchResult;
import de.mpg.imeji.logic.search.model.SortCriterion;
import de.mpg.imeji.logic.vo.User;
import de.mpg.imeji.logic.vo.UserGroup;

/**
 * Implements CRUD Methods for a {@link UserGroup}
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public class GroupBusinessController {
  private final GroupController controller = new GroupController();
  private Search search =
      SearchFactory.create(SearchObjectTypes.USERGROUPS, SEARCH_IMPLEMENTATIONS.ELASTIC);
  private static final Logger LOGGER = Logger.getLogger(GroupBusinessController.class);

  /**
   * Create a {@link UserGroup}
   *
   * @param group
   * @throws ImejiException
   */
  public void create(UserGroup group, User user) throws ImejiException {
    controller.create(group, user);
  }

  /**
   * Read a {@link UserGroup} with the given uri
   *
   * @param uri
   * @return
   * @throws ImejiException
   */
  public UserGroup retrieve(String uri, User user) throws ImejiException {
    return controller.retrieve(uri, user);
  }

  /**
   * Read a {@link UserGroup} with the given uri
   *
   * @param uri
   * @return
   * @throws ImejiException
   */
  public UserGroup retrieveLazy(String uri, User user) throws ImejiException {
    return retrieveLazy(uri, user);
  }

  /**
   * Retrieve a list of {@link UserGroup}
   * 
   * @param uris
   * @param user
   * @return
   * @throws ImejiException
   */
  public List<UserGroup> retrieveBatch(List<String> uris, User user) {
    return retrieveBatch(uris, user);
  }

  /**
   * Retrieve a list of {@link UserGroup}
   * 
   * @param uris
   * @param user
   * @return
   * @throws ImejiException
   */
  public List<UserGroup> retrieveBatchLazy(List<String> uris, User user) {
    return retrieveBatchLazy(uris, user);
  }

  /**
   * Read a {@link UserGroup} with the given {@link URI}
   *
   * @param uri
   * @return
   * @throws ImejiException
   */
  public UserGroup read(URI uri, User user) throws ImejiException {
    return retrieve(uri.toString(), user);
  }

  /**
   * Update a {@link UserGroup}
   *
   * @param group
   * @param user
   * @throws ImejiException
   */
  public void update(UserGroup group, User user) throws ImejiException {
    controller.update(group, user);
  }

  /**
   * Delete a {@link UserGroup}
   *
   * @param group
   * @param user
   * @throws ImejiException
   */
  public void delete(UserGroup group, User user) throws ImejiException {
    controller.delete(group, user);
  }

  /**
   * Search for {@link UserGroup}
   * 
   * @param q
   * @param sort
   * @param user
   * @param offset
   * @param size
   * @return
   * @throws ImejiException
   */
  public SearchResult search(SearchQuery q, SortCriterion sort, User user, int offset, int size) {
    return search.search(q, sort, user, null, null, offset, size);
  }

  /**
   * Search for {@link UserGroup}
   * 
   * @param q
   * @param sort
   * @param user
   * @param offset
   * @param size
   * @return
   * @throws ImejiException
   */
  public List<UserGroup> searchAndRetrieve(SearchQuery q, SortCriterion sort, User user, int offset,
      int size) {
    return retrieveBatch(search(q, sort, user, offset, size).getResults(), user);
  }

  /**
   * Search for {@link UserGroup}
   * 
   * @param q
   * @param sort
   * @param user
   * @param offset
   * @param size
   * @return
   * @throws ImejiException
   */
  public List<UserGroup> searchAndRetrieveLazy(SearchQuery q, SortCriterion sort, User user,
      int offset, int size) {
    return retrieveBatchLazy(search(q, sort, user, offset, size).getResults(), user);
  }

  /**
   * Retrieve all {@link UserGroup} Only allowed for System administrator
   *
   * @return
   */
  public Collection<UserGroup> searchByName(String q, User user) {
    return searchBySPARQLQuery(JenaCustomQueries.selectUserGroupAll(q), user);
  }

  /**
   * Retrieve all {@link UserGroup}
   *
   * @return
   * @throws ImejiException
   */
  public Collection<UserGroup> retrieveAll() throws ImejiException {
    return searchBySPARQLQuery(JenaCustomQueries.selectUserGroupAll(), Imeji.adminUser);
  }

  /**
   * Retrieve all {@link UserGroup} a user is member of
   *
   * @return
   */
  public Collection<UserGroup> searchByUser(User member, User user) {
    return searchBySPARQLQuery(JenaCustomQueries.selectUserGroupOfUser(member), Imeji.adminUser);
  }

  /**
   * Search {@link UserGroup} according a SPARQL Query
   *
   * @param q
   * @param user
   * @return
   */
  private Collection<UserGroup> searchBySPARQLQuery(String q, User user) {
    Collection<UserGroup> userGroups = new ArrayList<UserGroup>();
    Search search = SearchFactory.create();
    for (String uri : search.searchString(q, null, null, 0, -1).getResults()) {
      try {
        userGroups.add(controller.read(URI.create(uri), user));
      } catch (ImejiException e) {
        LOGGER.info("User group with uri " + uri + " not found.");
      }
    }
    return userGroups;
  }

  /**
   * Removes single user from all user groups where he is a member Of
   * 
   * @param userToRemove
   * @param userRemover
   * @throws ImejiException
   */
  public void removeUserFromAllGroups(User userToRemove, User userRemover) throws ImejiException {
    for (UserGroup memberIn : searchByUser(userToRemove, userRemover)) {
      memberIn.getUsers().remove(userToRemove.getId());
      update(memberIn, userRemover);
      // Write to log to inform
      LOGGER.info("User " + userToRemove.getId() + " (" + userToRemove.getEmail()
          + ") has been removed from group " + memberIn.getName());
    }
  }
}
