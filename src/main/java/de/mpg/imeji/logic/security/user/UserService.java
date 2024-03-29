package de.mpg.imeji.logic.security.user;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.lang.reflect.Field;

import de.mpg.imeji.exceptions.*;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.jose4j.lang.JoseException;

import de.mpg.imeji.logic.config.Imeji;
import de.mpg.imeji.logic.db.reader.ReaderFacade;
import de.mpg.imeji.logic.model.CollectionImeji;
import de.mpg.imeji.logic.model.Grant;
import de.mpg.imeji.logic.model.Item;
import de.mpg.imeji.logic.model.Organization;
import de.mpg.imeji.logic.model.Person;
import de.mpg.imeji.logic.model.SearchFields;
import de.mpg.imeji.logic.model.User;
import de.mpg.imeji.logic.model.aspects.ChangeMember.ActionType;
import de.mpg.imeji.logic.search.Search;
import de.mpg.imeji.logic.search.Search.SearchObjectTypes;
import de.mpg.imeji.logic.search.SearchQueryParser;
import de.mpg.imeji.logic.search.elasticsearch.ElasticIndexer;
import de.mpg.imeji.logic.search.elasticsearch.ElasticService.ElasticIndices;
import de.mpg.imeji.logic.search.factory.SearchFactory;
import de.mpg.imeji.logic.search.factory.SearchFactory.SEARCH_IMPLEMENTATIONS;
import de.mpg.imeji.logic.search.jenasearch.ImejiSPARQL;
import de.mpg.imeji.logic.search.jenasearch.JenaCustomQueries;
import de.mpg.imeji.logic.search.model.SearchOperators;
import de.mpg.imeji.logic.search.model.SearchPair;
import de.mpg.imeji.logic.search.model.SearchQuery;
import de.mpg.imeji.logic.search.model.SearchResult;
import de.mpg.imeji.logic.search.model.SortCriterion;
import de.mpg.imeji.logic.search.model.SortCriterion.SortOrder;
import de.mpg.imeji.logic.security.authentication.impl.APIKeyAuthentication;
import de.mpg.imeji.logic.security.authorization.AuthorizationPredefinedRoles;
import de.mpg.imeji.logic.security.authorization.util.SecurityUtil;
import de.mpg.imeji.logic.security.sharing.invitation.InvitationService;
import de.mpg.imeji.logic.security.user.util.QuotaUtil;
import de.mpg.imeji.logic.util.IdentifierUtil;
import de.mpg.imeji.util.DateHelper;

/**
 * Controller for {@link User}
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public class UserService {
  private final UserController controller = new UserController();
  private static final Logger LOGGER = LogManager.getLogger(UserService.class);
  private final Search search = SearchFactory.create(SearchObjectTypes.USER, SEARCH_IMPLEMENTATIONS.ELASTIC);

  /**
   * User type (restricted: can not create collection)
   *
   * @author saquet
   *
   */
  public enum USER_TYPE {
    DEFAULT,
    ADMIN,
    RESTRICTED,
    COPY,
    INACTIVE;
  }

  /**
   * Create a new user in the database with predefined roles (ADMIN, DEFAULT or RESTRICTED)
   *
   * @param u
   * @param type
   * @return
   * @throws ImejiException
   */
  public User create(User u, USER_TYPE type) throws ImejiException {
    u.setUserStatus(User.UserStatus.ACTIVE);
    if (u.getQuota() < 0) {
      u.setQuota(QuotaUtil.getQuotaInBytes(Imeji.CONFIG.getDefaultQuota()));
    }
    switch (type) {
      case ADMIN:
        u.setGrants(AuthorizationPredefinedRoles.imejiAdministrator(u.getId().toString()));
        break;
      case RESTRICTED:
        u.setGrants(AuthorizationPredefinedRoles.restrictedUser(u.getId().toString()));
        break;
      case DEFAULT:
        u.setGrants(AuthorizationPredefinedRoles.defaultUser(u.getId().toString()));
        break;
      case COPY:
        // Don't change the grants of the user
        break;
      case INACTIVE:
        // Don't change the grants of the user, but set the status to Inactive
        u.setUserStatus(User.UserStatus.INACTIVE);
        u.setRegistrationToken(IdentifierUtil.newUniversalUniqueId());
        break;
    }
    u = controller.create(u);
    u = new InvitationService().consume(u);
    try {
      u.setApiKey(APIKeyAuthentication.generateKey(u.getId(), Integer.MAX_VALUE));
      controller.update(u, Imeji.adminUser);
    } catch (final JoseException e) {
      LOGGER.error("Error creating API Key during user creation", e);
    }
    return u;
  }

  /**
   * Delete a {@link User}
   *
   * @param user
   * @throws ImejiException
   */
  public void delete(User user) throws ImejiException {
    final Search search = SearchFactory.create(); // default is JENA
    final List<String> results = search.searchString(JenaCustomQueries.selectCreatorOrModifiedBy(user.getId().toString()), null, null,
        Search.SEARCH_FROM_START_INDEX, Search.GET_ALL_RESULTS).getResults();
    if (results != null && results.size() > 0) {
      throw new WorkflowException("User cannot be deleted, as they own or modified collections",
          "User cannot be deleted, as they own or modified collections");
    }
    controller.delete(user);
  }

  /**
   * Retrieve a {@link User} according to its email
   *
   * @param email
   * @return
   * @throws ImejiException
   */
  public User retrieve(String email, User user) throws ImejiException {
    final SearchQuery query = new SearchQuery();
    query.addPair(new SearchPair(SearchFields.email, SearchOperators.EQUALS, email, false));
    final SearchResult result = search.search(query, null, Imeji.adminUser, null, 0, 1);
    if (result.getNumberOfRecords() == 1) {
      return controller.retrieve(URI.create(result.getResults().get(0)), user);
    }
    throw new NotFoundException("User with email " + email + " not found");
  }

  /**
   * Retrieve a {@link User} according to its uri (id)
   *
   * @param uri
   * @return
   * @throws ImejiException
   */
  public User retrieve(URI uri, User user) throws ImejiException {
    return controller.retrieve(uri, user);
  }

  /**
   * Load all {@link User}
   *
   * @param uris
   * @return
   * @throws ImejiException
   */
  public List<User> retrieveBatchLazy(List<String> uris, int limit) {
    return controller.retrieveBatchLazy(uris, limit);
  }

  /**
   * Load all {@link User}
   *
   * @param uris
   * @return
   * @throws ImejiException
   */
  public Collection<User> retrieveBatch(List<String> uris, int limit) {
    return controller.retrieveBatch(uris, limit);
  }

  /**
   * Retrieve all {@link Item} (all status, all users) in imeji
   *
   * @return
   * @throws ImejiException
   */
  public List<User> retrieveAll() throws ImejiException {
    return controller.retrieveAll();
  }

  /**
   * Update a {@link User}
   *
   * @param updatedUser : The user who is updated in the database
   * @param currentUser
   * @throws ImejiException
   * @return
   */
  public User update(User updatedUser, User currentUser) throws ImejiException {
    return controller.update(updatedUser, currentUser);
  }

  public List<User> updateBatch(List<User> users, User currentUser) throws ImejiException {
    return controller.updateBatch(users, currentUser);
  }

  /**
   * Add one or more grants to a User object
   * 
   * @param userToAddGrants
   * @param grantsToAdd
   * @throws ImejiException
   */
  public User addEditGrantToUser(User issuingUser, User userToAddEditGrant, Grant grantToAddEdit) throws ImejiException {

    User userWithNewGrant = userToAddEditGrant;
    try {
      Field grantField = User.class.getDeclaredField("grants");
      userWithNewGrant =
          this.controller.changeElement(issuingUser, userToAddEditGrant, grantField, ActionType.ADD_OVERRIDE, grantToAddEdit);

    } catch (NoSuchFieldException | SecurityException e) {
      e.printStackTrace();
    }
    return userWithNewGrant;
  }

  /**
   * Remove one or more grants from a User object
   * 
   * @param userToRemoveGrantsFrom
   * @param grantsToRemove
   * @throws ImejiException
   */
  public User removeGrantsFromUser(User issuingUser, User userToRemoveGrantsFrom, Grant grantToRemove) throws ImejiException {

    User userWithoutGrant = userToRemoveGrantsFrom;
    try {
      Field grantField = User.class.getDeclaredField("grants");
      userWithoutGrant = this.controller.changeElement(issuingUser, userToRemoveGrantsFrom, grantField, ActionType.REMOVE, grantToRemove);
    } catch (NoSuchFieldException | SecurityException e) {
      e.printStackTrace();
    }
    return userWithoutGrant;
  }


  /**
   * True if a user object has been modified in database since it was read last. In this case the
   * last modification date of the user in the database is younger than the last modification of the
   * user in the session. For instance when an object has been shared with the user.
   *
   * @param currentUserSessionObject
   * @return
   */
  public boolean hasBeenModified(User currentUserSessionObject) {
    final SearchResult result = SearchFactory.create()
        .searchString(JenaCustomQueries.selectLastModifiedDate(currentUserSessionObject.getId()), null, currentUserSessionObject, 0, 1);
    return result.getNumberOfRecords() > 0 && (currentUserSessionObject.getModified() == null
        || DateHelper.parseDate(result.getResults().get(0)).after(currentUserSessionObject.getModified()));
  }

  /**
   * Will set the modified date field (time stamp field) of a User object to now. Goal: Trigger the
   * reload-user-mechanism. This mechanism will update the (session's) User object of a logged-in
   * user with it's latest version from the database. See also {@link SecurityFilter.isReloadUser}.
   * Useful when a user who is currently logged-in has been added to a user group or a user's user
   * group has gotten/lost access rights while this user is logged-in.
   * 
   * @param recentlyModifiedUserId The User id
   */
  public void setRecentlyModified(URI recentlyModifiedUserId) {
    final String sparqlQuery = JenaCustomQueries.setUserLastModifiedToNow(recentlyModifiedUserId);
    ImejiSPARQL.execUpdate(sparqlQuery);
  }

  /**
   * Check user disk space quota. Quota is calculated for user of target collection.
   *
   * @param file
   * @param col
   * @throws ImejiException
   * @return remained disk space after successfully uploaded <code>file</code>; <code>-1</code> will
   *         be returned for unlimited quota
   */
  public long checkQuota(User user, File file, CollectionImeji col) throws ImejiException {
    // do not check quota for admin
    if (SecurityUtil.authorization().isSysAdmin(user)) {
      return -1L;
    }
    final User targetCollectionUser = user.getId().equals(col.getCreatedBy()) ? user : retrieve(col.getCreatedBy(), Imeji.adminUser);

    final Search search = SearchFactory.create();
    final List<String> results = search.searchString(JenaCustomQueries.selectUserFileSize(col.getCreatedBy().toString()), null, null,
        Search.SEARCH_FROM_START_INDEX, Search.GET_ALL_RESULTS).getResults();
    long currentDiskUsage = 0L;
    try {
      currentDiskUsage = Long.parseLong(results.get(0).toString());
    } catch (final NumberFormatException e) {
      throw new UnprocessableError("Cannot parse currentDiskSpaceUsage " + results.get(0).toString() + "; requested by user: "
          + user.getEmail() + "; targetCollectionUser: " + targetCollectionUser.getEmail(), e);
    }
    final long needed = currentDiskUsage + file.length();
    if (needed > targetCollectionUser.getQuota()) {
      throw new QuotaExceededException("Data quota (" + QuotaUtil.getQuotaHumanReadable(targetCollectionUser.getQuota(), Locale.ENGLISH)
          + " allowed) has been exceeded (" + FileUtils.byteCountToDisplaySize(currentDiskUsage) + " used)");
    }
    return targetCollectionUser.getQuota() - needed;
  }

  /**
   * Retrieve all {@link User} in imeji<br/>
   * Only allowed for System administrator
   *
   * @return
   */
  public Collection<User> searchUserByName(String name) {
    try {
      SearchResult searchResult =
          search.search(SearchQueryParser.parseStringQuery(name, false), new SortCriterion(SearchFields.completename, SortOrder.ASCENDING),
              Imeji.adminUser, null, Search.SEARCH_FROM_START_INDEX, Search.GET_ALL_RESULTS);
      return retrieveBatchLazy(searchResult.getResults(), Search.GET_ALL_RESULTS);
    } catch (final UnprocessableError e) {
      LOGGER.error("Error search users", e);
    }
    return new ArrayList<>();
  }

  /**
   * Return the complete name of a user
   * 
   * @param uri
   * @param locale
   * @return
   */
  public String getCompleteName(URI uri, Locale locale) {
    final Search search = SearchFactory.create(SearchObjectTypes.USER, SEARCH_IMPLEMENTATIONS.JENA);
    final List<String> users = search.searchString(JenaCustomQueries.selectUserCompleteName(uri), null, Imeji.adminUser, 0, 1).getResults();
    if (users != null && users.size() > 0) {
      return users.get(0);
    } else {
      return Imeji.RESOURCE_BUNDLE.getLabel("unknown_user", locale);
    }
  }

  /**
   * Search for users
   *
   * @param q
   * @param sort
   * @param user
   * @param offset
   * @param size
   * @return
   */
  public SearchResult search(SearchQuery q, SortCriterion sort, User user, int offset, int size) {
    return search.search(q, sort, user, null, offset, size);
  }

  /**
   * Search for users
   *
   * @param q
   * @param sort
   * @param user
   * @param offset
   * @param size
   * @return
   */
  public List<User> searchAndRetrieve(SearchQuery q, SortCriterion sort, User user, int offset, int size) {
    return (List<User>) retrieveBatch(search.search(q, sort, user, null, offset, size).getResults(), size);
  }

  /**
   * Search for users
   *
   * @param q
   * @param sort
   * @param user
   * @param offset
   * @param size
   * @return
   */
  public List<User> searchAndRetrieveLazy(SearchQuery q, SortCriterion sort, User user, int offset, int size) {

    List<User> foundUsers = new ArrayList<User>(0);
    SearchResult searchResult = search.search(q, sort, user, null, offset, size);
    if (searchResult != null) {
      List<String> foundUris = searchResult.getResults();
      if (foundUris != null) {
        foundUsers = retrieveBatchLazy(foundUris, size);
      }
    }
    return foundUsers;
  }

  /**
   * Load a {@link User} by its uri
   *
   * @param id
   * @return
   */
  public Person retrievePersonById(String id) {
    final List<String> l = new ArrayList<String>();
    l.add(id);
    Collection<Person> c = new ArrayList<Person>();
    try {
      c = loadPersons(l, Imeji.userModel);
    } catch (final Exception e) {
      c.addAll(loadPersons(l, Imeji.collectionModel));
    }
    return c.iterator().next();
  }

  /**
   * Load an {@link Organization} by its uri
   *
   * @param id
   * @return
   */
  public Organization retrieveOrganizationById(String id) {
    final List<String> l = new ArrayList<String>();
    l.add(id);
    Collection<Organization> c = new ArrayList<Organization>();
    try {
      c = loadOrganizations(l, Imeji.userModel);
    } catch (final Exception e) {
      c.addAll(loadOrganizations(l, Imeji.collectionModel));
    }
    return c.iterator().next();
  }

  /**
   * Search for all {@link Organization} in imeji, i.e. t The search looks within the {@link User}
   * and the {@link Collection} what {@link Organization are already existing.
   *
   * @param name
   * @return
   * @throws UnprocessableError
   */
  public Collection<Organization> searchOrganizationByName(String name) {
    final Collection<Organization> l = searchOrganizationByNameInUsers(name);
    final Map<String, Organization> map = new HashMap<>();
    for (final Organization o : l) {
      // map.put(o.getIdentifier(), o);
      map.put(o.getName().toLowerCase(), o);
    }
    return map.values();
  }

  /**
   * Search all {@link Organization} which are defined in a {@link User}
   *
   * @param name
   * @return
   */
  private Collection<Organization> searchOrganizationByNameInUsers(String name) {
    final Search search = SearchFactory.create(SearchObjectTypes.USER, SEARCH_IMPLEMENTATIONS.JENA);
    return loadOrganizations(search
        .searchString(JenaCustomQueries.selectOrganizationByName(name), null, null, Search.SEARCH_FROM_START_INDEX, Search.GET_ALL_RESULTS)
        .getResults(), Imeji.userModel);
  }

  /**
   * Load Organizations
   *
   * @param uris
   * @param model
   * @return
   */
  public Collection<Organization> loadOrganizations(List<String> uris, String model) {
    final Collection<Organization> orgs = new ArrayList<Organization>();
    for (final String uri : uris) {
      try {
        final ReaderFacade reader = new ReaderFacade(model);
        orgs.add((Organization) reader.read(uri, Imeji.adminUser, new Organization()));
      } catch (final ImejiException e) {
        LOGGER.info("Organization with " + uri + " not found");
      }
    }
    return orgs;
  }

  /**
   * Load Organizations
   *
   * @param uris
   * @param model
   * @return
   */
  private Collection<Person> loadPersons(List<String> uris, String model) {
    final Collection<Person> p = new ArrayList<Person>();
    for (final String uri : uris) {
      try {
        final ReaderFacade reader = new ReaderFacade(model);
        p.add((Person) reader.read(uri, Imeji.adminUser, new Person()));
      } catch (final ImejiException e) {
        LOGGER.error("Error reding person", e);
      }
    }
    return p;
  }

  /**
   * This method checks if a admin user exists for this instance
   *
   * @return true of no admin user exists, false otherwise
   */
  public static boolean adminUserExist() {
    boolean exist = false;
    final Search search = SearchFactory.create();
    final List<String> uris =
        search.searchString(JenaCustomQueries.selectUserSysAdmin(), null, null, Search.SEARCH_FROM_START_INDEX, Search.GET_ALL_RESULTS)
            .getResults();
    if (uris != null && uris.size() > 0) {
      exist = true;
    }
    return exist;
  }

  /**
   * Retrieve all admin users
   *
   * @return
   * @throws ImejiException
   */
  public List<User> retrieveAllAdmins() {
    final Search search = SearchFactory.create();
    final List<String> uris =
        search.searchString(JenaCustomQueries.selectUserSysAdmin(), null, null, Search.SEARCH_FROM_START_INDEX, Search.GET_ALL_RESULTS)
            .getResults();
    final List<User> admins = new ArrayList<User>();
    for (final String uri : uris) {
      try {
        admins.add(retrieve(URI.create(uri), Imeji.adminUser));
      } catch (final ImejiException e) {
        LOGGER.info("Could not retrieve any admin in the list. Something is wrong!", e);
      }
    }
    return admins;
  }

  /**
   * Search for users to be notified by item download of the collection
   *
   * @param user
   * @param c
   * @return
   */
  public List<User> searchUsersToBeNotified(User user, CollectionImeji c) {
    final Search search = SearchFactory.create();
    final List<String> uris = search.searchString(JenaCustomQueries.selectUsersToBeNotifiedByFileDownload(user, c), null, null,
        Search.SEARCH_FROM_START_INDEX, Search.GET_ALL_RESULTS).getResults();
    return retrieveBatchLazy(uris, Search.GET_ALL_RESULTS);
  }

  public void reindex(String index) throws Exception {
    LOGGER.info("Indexing users...");
    final ElasticIndexer indexer = new ElasticIndexer(ElasticIndices.users.name());
    final List<User> users = retrieveAll();
    LOGGER.info("+++ " + users.size() + " users to index +++");
    indexer.indexBatch(users);
    LOGGER.info("...users reindexed!");
  }
}
