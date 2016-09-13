/**
 * License: src/main/resources/license/escidoc.license
 */
package de.mpg.imeji.logic.user.controller;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.jose4j.lang.JoseException;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.exceptions.NotFoundException;
import de.mpg.imeji.exceptions.QuotaExceededException;
import de.mpg.imeji.exceptions.UnprocessableError;
import de.mpg.imeji.logic.Imeji;
import de.mpg.imeji.logic.controller.resource.UserController;
import de.mpg.imeji.logic.reader.ReaderFacade;
import de.mpg.imeji.logic.search.Search;
import de.mpg.imeji.logic.search.Search.SearchObjectTypes;
import de.mpg.imeji.logic.search.SearchQueryParser;
import de.mpg.imeji.logic.search.factory.SearchFactory;
import de.mpg.imeji.logic.search.factory.SearchFactory.SEARCH_IMPLEMENTATIONS;
import de.mpg.imeji.logic.search.jenasearch.JenaCustomQueries;
import de.mpg.imeji.logic.search.model.SearchIndex.SearchFields;
import de.mpg.imeji.logic.search.model.SearchOperators;
import de.mpg.imeji.logic.search.model.SearchPair;
import de.mpg.imeji.logic.search.model.SearchQuery;
import de.mpg.imeji.logic.search.model.SearchResult;
import de.mpg.imeji.logic.search.model.SortCriterion;
import de.mpg.imeji.logic.search.model.SortCriterion.SortOrder;
import de.mpg.imeji.logic.security.authorization.AuthorizationPredefinedRoles;
import de.mpg.imeji.logic.security.util.SecurityUtil;
import de.mpg.imeji.logic.user.authentication.impl.APIKeyAuthentication;
import de.mpg.imeji.logic.user.collaboration.invitation.InvitationBusinessController;
import de.mpg.imeji.logic.util.IdentifierUtil;
import de.mpg.imeji.logic.util.QuotaUtil;
import de.mpg.imeji.logic.vo.CollectionImeji;
import de.mpg.imeji.logic.vo.Item;
import de.mpg.imeji.logic.vo.Organization;
import de.mpg.imeji.logic.vo.Person;
import de.mpg.imeji.logic.vo.User;
import de.mpg.imeji.util.DateHelper;

/**
 * Controller for {@link User}
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public class UserBusinessController {
  private final UserController controller = new UserController();
  private static final Logger LOGGER = Logger.getLogger(UserBusinessController.class);
  private final Search search =
      SearchFactory.create(SearchObjectTypes.USER, SEARCH_IMPLEMENTATIONS.ELASTIC);



  /**
   * User type (restricted: can not create collection)
   *
   * @author saquet
   *
   */
  public enum USER_TYPE {
    DEFAULT, ADMIN, RESTRICTED, COPY, INACTIVE;
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
    new InvitationBusinessController().consume(u);
    try {
      u.setApiKey(APIKeyAuthentication.generateKey(u.getId(), Integer.MAX_VALUE));
      controller.update(u);
    } catch (JoseException e) {
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
    SearchQuery query = new SearchQuery();
    query.addPair(new SearchPair(SearchFields.email, SearchOperators.EQUALS, email, false));
    SearchResult result = search.search(query, null, Imeji.adminUser, null, null, 0, 1);
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
   * Retrieve a {@link User} according to its email
   *
   * @param email
   * @return
   * @throws ImejiException
   */
  public boolean existsUserWitheMail(String email, String userUri, boolean newUser) {
    Search search = SearchFactory.create(SearchObjectTypes.USER, SEARCH_IMPLEMENTATIONS.JENA);
    SearchResult result =
        search.searchString(JenaCustomQueries.selectUserByEmail(email), null, null, 0, -1);
    if (result.getNumberOfRecords() == 0) {
      return false;
    } else {
      // New users always have assigned Id, thus we do not check if it is existing user here
      if (newUser && result.getNumberOfRecords() > 0) {
        return true;
      }

      // Check if it is existing user here who has same email
      boolean thereIsOtherUser = false;
      for (String userId : result.getResults()) {
        if (!userUri.equals(userId)) {
          thereIsOtherUser = true;
        }
      }
      return thereIsOtherUser;
    }
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
    return controller.update(updatedUser);
  }

  /**
   * True if a user has been Modified, i.e the last modification of the user in the database is
   * older than the last modification of the user in the session. (For instance when an object has
   * been shared with the user)
   *
   * @param u
   * @return
   */
  public boolean isModified(User u) {
    SearchResult result = SearchFactory.create()
        .searchString(JenaCustomQueries.selectLastModifiedDate(u.getId()), null, u, 0, 1);
    return result.getNumberOfRecords() > 0 && (u.getModified() == null
        || DateHelper.parseDate(result.getResults().get(0)).after(u.getModified()));
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
    if (SecurityUtil.isSysAdmin(user)) {
      return -1L;
    }
    User targetCollectionUser = user.getId().equals(col.getCreatedBy()) ? user
        : retrieve(col.getCreatedBy(), Imeji.adminUser);

    Search search = SearchFactory.create();
    List<String> results =
        search.searchString(JenaCustomQueries.selectUserFileSize(col.getCreatedBy().toString()),
            null, null, 0, -1).getResults();
    long currentDiskUsage = 0L;
    try {
      currentDiskUsage = Long.parseLong(results.get(0).toString());
    } catch (NumberFormatException e) {
      throw new UnprocessableError("Cannot parse currentDiskSpaceUsage " + results.get(0).toString()
          + "; requested by user: " + user.getEmail() + "; targetCollectionUser: "
          + targetCollectionUser.getEmail(), e);
    }
    long needed = currentDiskUsage + file.length();
    if (needed > targetCollectionUser.getQuota()) {
      throw new QuotaExceededException("Data quota ("
          + QuotaUtil.getQuotaHumanReadable(targetCollectionUser.getQuota(), Locale.ENGLISH)
          + " allowed) has been exceeded (" + FileUtils.byteCountToDisplaySize(currentDiskUsage)
          + " used)");
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
      return retrieveBatchLazy(search.search(SearchQueryParser.parseStringQuery(name),
          new SortCriterion(SearchFields.person_family, SortOrder.ASCENDING), Imeji.adminUser, null,
          null, 0, -1).getResults(), -1);
    } catch (UnprocessableError e) {
      LOGGER.error("Error search users", e);
    }
    return new ArrayList<>();
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
    return search.search(q, sort, user, null, null, offset, size);
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
  public List<User> searchAndRetrieve(SearchQuery q, SortCriterion sort, User user, int offset,
      int size) {
    return (List<User>) retrieveBatch(
        search.search(q, sort, user, null, null, offset, size).getResults(), size);
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
  public List<User> searchAndRetrieveLazy(SearchQuery q, SortCriterion sort, User user, int offset,
      int size) {
    return (List<User>) retrieveBatchLazy(
        search.search(q, sort, user, null, null, offset, size).getResults(), size);
  }



  /**
   * Search for all {@link Person} by their names. The search looks within the {@link User} and the
   * {@link Collection} what {@link Person} are already existing.
   *
   * @param name
   * @return
   */
  public Collection<Person> searchPersonByName(String name) {
    return searchPersonByNameInUsers(name);
  }

  /**
   * Load a {@link User} by its uri
   *
   * @param id
   * @return
   */
  public Person retrievePersonById(String id) {
    List<String> l = new ArrayList<String>();
    l.add(id);
    Collection<Person> c = new ArrayList<Person>();
    try {
      c = loadPersons(l, Imeji.userModel);
    } catch (Exception e) {
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
    List<String> l = new ArrayList<String>();
    l.add(id);
    Collection<Organization> c = new ArrayList<Organization>();
    try {
      c = loadOrganizations(l, Imeji.userModel);
    } catch (Exception e) {
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
   */
  public Collection<Organization> searchOrganizationByName(String name) {
    Collection<Organization> l = searchOrganizationByNameInUsers(name);
    Map<String, Organization> map = new HashMap<>();
    for (Organization o : l) {
      // map.put(o.getIdentifier(), o);
      map.put(o.getName().toLowerCase(), o);
    }
    return map.values();
  }

  /**
   * Search all {@link Person} which are defined in a {@link User}
   *
   * @param name
   * @return
   */
  private Collection<Person> searchPersonByNameInUsers(String name) {
    Search search = SearchFactory.create(SearchObjectTypes.USER, SEARCH_IMPLEMENTATIONS.JENA);
    return loadPersons(search
        .searchString(JenaCustomQueries.selectPersonByName(name), null, null, 0, -1).getResults(),
        Imeji.userModel);
  }


  /**
   * Search all {@link Organization} which are defined in a {@link User}
   *
   * @param name
   * @return
   */
  private Collection<Organization> searchOrganizationByNameInUsers(String name) {
    Search search = SearchFactory.create(SearchObjectTypes.USER, SEARCH_IMPLEMENTATIONS.JENA);
    return loadOrganizations(
        search.searchString(JenaCustomQueries.selectOrganizationByName(name), null, null, 0, -1)
            .getResults(),
        Imeji.userModel);
  }



  /**
   * Load Organizations
   *
   * @param uris
   * @param model
   * @return
   */
  public Collection<Organization> loadOrganizations(List<String> uris, String model) {
    Collection<Organization> orgs = new ArrayList<Organization>();
    for (String uri : uris) {
      try {
        ReaderFacade reader = new ReaderFacade(model);
        orgs.add((Organization) reader.read(uri, Imeji.adminUser, new Organization()));
      } catch (ImejiException e) {
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
    Collection<Person> p = new ArrayList<Person>();
    for (String uri : uris) {
      try {
        ReaderFacade reader = new ReaderFacade(model);
        p.add((Person) reader.read(uri, Imeji.adminUser, new Person()));
      } catch (ImejiException e) {
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
    Search search = SearchFactory.create();
    List<String> uris =
        search.searchString(JenaCustomQueries.selectUserSysAdmin(), null, null, 0, -1).getResults();
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
    Search search = SearchFactory.create();
    List<String> uris =
        search.searchString(JenaCustomQueries.selectUserSysAdmin(), null, null, 0, -1).getResults();
    List<User> admins = new ArrayList<User>();
    for (String uri : uris) {
      try {
        admins.add(retrieve(URI.create(uri), Imeji.adminUser));
      } catch (ImejiException e) {
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
    Search search = SearchFactory.create();
    List<String> uris =
        search.searchString(JenaCustomQueries.selectUsersToBeNotifiedByFileDownload(user, c), null,
            null, 0, -1).getResults();
    return (List<User>) retrieveBatchLazy(uris, -1);
  }
}
