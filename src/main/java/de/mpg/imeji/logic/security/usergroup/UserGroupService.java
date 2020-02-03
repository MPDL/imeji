package de.mpg.imeji.logic.security.usergroup;

import java.lang.reflect.Field;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.config.Imeji;
import de.mpg.imeji.logic.model.Grant;
import de.mpg.imeji.logic.model.User;
import de.mpg.imeji.logic.model.UserGroup;
import de.mpg.imeji.logic.model.aspects.ChangeMember.ActionType;
import de.mpg.imeji.logic.search.Search;
import de.mpg.imeji.logic.search.Search.SearchObjectTypes;
import de.mpg.imeji.logic.search.elasticsearch.ElasticIndexer;
import de.mpg.imeji.logic.search.factory.SearchFactory;
import de.mpg.imeji.logic.search.factory.SearchFactory.SEARCH_IMPLEMENTATIONS;
import de.mpg.imeji.logic.search.jenasearch.JenaCustomQueries;
import de.mpg.imeji.logic.search.model.SearchQuery;
import de.mpg.imeji.logic.search.model.SearchResult;
import de.mpg.imeji.logic.search.model.SortCriterion;
import de.mpg.imeji.logic.security.user.UserService;

/**
 * Implements CRUD Methods for a {@link UserGroup}
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public class UserGroupService {
  private final UserGroupController controller = new UserGroupController();
  private final Search search = SearchFactory.create(SearchObjectTypes.USERGROUPS, SEARCH_IMPLEMENTATIONS.ELASTIC);
  private static final Logger LOGGER = LogManager.getLogger(UserGroupService.class);

  /**
   * Create a {@link UserGroup}
   *
   * @param group
   * @throws ImejiException
   */
  public void create(UserGroup group, User user) throws ImejiException {
    controller.create(group, user);
    this.updateUsersOfUserGroupForReload(group);
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
    this.updateUsersOfUserGroupForReload(group);
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
    this.updateUsersOfUserGroupForReload(group);
  }


  public UserGroup addUserToGroup(User issuingUser, UserGroup userGroup, User addThisUser) throws ImejiException {

    UserGroup groupWithNewUser = userGroup;
    try {
      Field userListField = UserGroup.class.getDeclaredField("users");
      groupWithNewUser = this.controller.changeElement(issuingUser, userGroup, userListField, ActionType.ADD, addThisUser.getId());
      this.updateUserForReload(addThisUser.getId());

    } catch (NoSuchFieldException | SecurityException e) {
      e.printStackTrace();
    }
    return groupWithNewUser;
  }


  public UserGroup removeUserFromGroup(User issuingUser, UserGroup userGroup, User removeThisUser) throws ImejiException {

    UserGroup groupWithoutUser = userGroup;
    try {
      Field userListField = UserGroup.class.getDeclaredField("users");
      groupWithoutUser = this.controller.changeElement(issuingUser, userGroup, userListField, ActionType.REMOVE, removeThisUser.getId());
      this.updateUserForReload(removeThisUser.getId());

    } catch (NoSuchFieldException | SecurityException e) {
      e.printStackTrace();
    }
    return groupWithoutUser;
  }



  public UserGroup addEditGrantToGroup(User issuingUser, UserGroup userGroup, Grant grant) throws ImejiException {

    UserGroup groupWithNewGrant = userGroup;
    try {
      Field grantField = UserGroup.class.getDeclaredField("grants");
      groupWithNewGrant = this.controller.changeElement(issuingUser, userGroup, grantField, ActionType.ADD_OVERRIDE, grant);
      this.updateUsersOfUserGroupForReload(groupWithNewGrant);

    } catch (NoSuchFieldException | SecurityException e) {
      e.printStackTrace();
    }
    return groupWithNewGrant;
  }


  public UserGroup removeGrantFromGroup(User issuingUser, UserGroup userGroup, Grant grantToRemove) throws ImejiException {

    UserGroup groupWithoutGrant = userGroup;
    try {
      Field grantField = UserGroup.class.getDeclaredField("grants");
      groupWithoutGrant = this.controller.changeElement(issuingUser, userGroup, grantField, ActionType.REMOVE, grantToRemove);
      this.updateUsersOfUserGroupForReload(userGroup);
    } catch (NoSuchFieldException | SecurityException e) {
      e.printStackTrace();
    }
    return groupWithoutGrant;
  }

  /**
   * Use this function after updating/creating/deleting user groups. Will set the last modified
   * field of all group users in database to now, so in case a user is logged in while he is added
   * or deleted from a user group or one of his user groups is altered, the corresponding session
   * object will be updated with the reload-user-mechanism. See {@ link SecurityFilter} function
   * isReloadUser.
   * 
   * @param userGroup
   */
  private void updateUsersOfUserGroupForReload(UserGroup userGroup) {
    for (URI user : userGroup.getUsers()) {
      new UserService().setRecentlyModified(user);
    }
  }

  private void updateUserForReload(URI usersURI) {
    new UserService().setRecentlyModified(usersURI);
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
  public List<UserGroup> searchAndRetrieve(SearchQuery q, SortCriterion sort, User user, int offset, int size) {
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
  public List<UserGroup> searchAndRetrieveLazy(SearchQuery q, SortCriterion sort, User user, int offset, int size) {
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
    for (final String uri : search.searchString(q, null, null, Search.SEARCH_FROM_START_INDEX, Search.GET_ALL_RESULTS).getResults()) {
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
   * @param issuingUser
   * @throws ImejiException
   */
  public void removeUserFromAllGroups(User userToRemove, User issuingUser) throws ImejiException {
    for (final UserGroup memberIn : searchByUser(userToRemove, issuingUser)) {
      this.removeUserFromGroup(issuingUser, memberIn, userToRemove);
      // Write to log to inform
      LOGGER.info("User " + userToRemove.getId() + " (" + userToRemove.getEmail() + ") has been removed from group " + memberIn.getName());
    }
  }

  /**
   * Reindex all user groups
   *
   * @param index
   * @throws ImejiException
   */
  public void reindex(String index) throws Exception {
    LOGGER.info("Indexing users...");
    final ElasticIndexer indexer = new ElasticIndexer(index);
    final List<UserGroup> groups = (List<UserGroup>) retrieveAll();
    LOGGER.info("+++ " + groups.size() + " user groups to index +++");
    indexer.indexBatch(groups);
    // indexer.commit();
    LOGGER.info("...user groups reindexed!");
  }
}
