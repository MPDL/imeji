package de.mpg.imeji.logic.security.usergroup;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.log4j.Logger;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.config.Imeji;
import de.mpg.imeji.logic.model.User;
import de.mpg.imeji.logic.model.UserGroup;
import de.mpg.imeji.logic.search.Search;
import de.mpg.imeji.logic.search.Search.SearchObjectTypes;
import de.mpg.imeji.logic.search.elasticsearch.ElasticIndexer;
import de.mpg.imeji.logic.search.elasticsearch.ElasticService;
import de.mpg.imeji.logic.search.elasticsearch.ElasticService.ElasticTypes;
import de.mpg.imeji.logic.search.factory.SearchFactory;
import de.mpg.imeji.logic.search.factory.SearchFactory.SEARCH_IMPLEMENTATIONS;
import de.mpg.imeji.logic.search.jenasearch.JenaCustomQueries;
import de.mpg.imeji.logic.search.model.SearchQuery;
import de.mpg.imeji.logic.search.model.SearchResult;
import de.mpg.imeji.logic.search.model.SortCriterion;

/**
 * Implements CRUD Methods for a {@link UserGroup}
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public class UserGroupService {
  private final UserGroupController controller = new UserGroupController();
  private final Search search =
      SearchFactory.create(SearchObjectTypes.USERGROUPS, SEARCH_IMPLEMENTATIONS.ELASTIC);
  private static final Logger LOGGER = Logger.getLogger(UserGroupService.class);

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
    return controller.retrieveLazy(uri, user);
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
    return controller.retrieveBatch(uris, user);
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
    return controller.retrieveBatchLazy(uris, user);
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
   * @return
   * @throws ImejiException
   */
  public UserGroup update(UserGroup group, User user) throws ImejiException {
    controller.update(group, user);
    return group;
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
    return search.search(q, sort, user, null, offset, size);
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
    final Collection<UserGroup> userGroups = new ArrayList<UserGroup>();
    final Search search = SearchFactory.create();
    for (final String uri : search.searchString(q, null, null, 0, -1).getResults()) {
      try {
        userGroups.add(controller.read(URI.create(uri), user));
      } catch (final ImejiException e) {
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
    for (final UserGroup memberIn : searchByUser(userToRemove, userRemover)) {
      memberIn.getUsers().remove(userToRemove.getId());
      update(memberIn, userRemover);
      // Write to log to inform
      LOGGER.info("User " + userToRemove.getId() + " (" + userToRemove.getEmail()
          + ") has been removed from group " + memberIn.getName());
    }
  }

  /**
   * Reindex all user groups
   *
   * @param index
   * @throws ImejiException
   */
  public void reindex(String index) throws ImejiException {
    LOGGER.info("Indexing users...");
    final ElasticIndexer indexer =
        new ElasticIndexer(index, ElasticTypes.usergroups, ElasticService.ANALYSER);
    final List<UserGroup> groups = (List<UserGroup>) retrieveAll();
    LOGGER.info("+++ " + groups.size() + " user groups to index +++");
    indexer.indexBatch(groups);
    indexer.commit();
    LOGGER.info("...user groups reindexed!");
  }
}
