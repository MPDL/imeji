/**
 * License: src/main/resources/license/escidoc.license
 */
package de.mpg.imeji.logic.controller;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import de.mpg.imeji.exceptions.AuthenticationError;
import de.mpg.imeji.exceptions.BadRequestException;
import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.exceptions.NotFoundException;
import de.mpg.imeji.exceptions.QuotaExceededException;
import de.mpg.imeji.exceptions.UnprocessableError;
import de.mpg.imeji.logic.Imeji;
import de.mpg.imeji.logic.auth.authorization.AuthorizationPredefinedRoles;
import de.mpg.imeji.logic.reader.ReaderFacade;
import de.mpg.imeji.logic.search.Search;
import de.mpg.imeji.logic.search.Search.SearchObjectTypes;
import de.mpg.imeji.logic.search.SearchFactory;
import de.mpg.imeji.logic.search.SearchFactory.SEARCH_IMPLEMENTATIONS;
import de.mpg.imeji.logic.search.SearchResult;
import de.mpg.imeji.logic.search.jenasearch.JenaCustomQueries;
import de.mpg.imeji.logic.util.IdentifierUtil;
import de.mpg.imeji.logic.util.QuotaUtil;
import de.mpg.imeji.logic.vo.CollectionImeji;
import de.mpg.imeji.logic.vo.Metadata;
import de.mpg.imeji.logic.vo.MetadataProfile;
import de.mpg.imeji.logic.vo.Organization;
import de.mpg.imeji.logic.vo.Person;
import de.mpg.imeji.logic.vo.Statement;
import de.mpg.imeji.logic.vo.User;
import de.mpg.imeji.logic.vo.UserGroup;
import de.mpg.imeji.logic.writer.WriterFacade;
import de.mpg.imeji.presentation.beans.ConfigurationBean;
import de.mpg.j2j.helper.DateHelper;

/**
 * Controller for {@link User}
 * 
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public class UserController {
  private static final ReaderFacade reader = new ReaderFacade(Imeji.userModel);
  private static final WriterFacade writer = new WriterFacade(Imeji.userModel);
  private User user;
  static Logger LOGGER = Logger.getLogger(UserController.class);

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
   * Constructor
   * 
   * @param user
   */
  public UserController(User user) {
    this.user = user;
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
    if (user == null && !USER_TYPE.INACTIVE.equals(type)) {
      throw new BadRequestException(
          "Not sufficient permissions to create user other than a user with status INACTIVE!");
    }
    // Now set up the creator to Admin User, as necessary for permissions
    user = Imeji.adminUser;
    u.setUserStatus(User.UserStatus.ACTIVE);
    if (u.getQuota() < 0) {
      u.setQuota(QuotaUtil.getQuotaInBytes(ConfigurationBean.getDefaultQuotaStatic()));
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
    Calendar now = DateHelper.getCurrentDate();
    u.setCreated(now);
    u.setModified(now);
    writer.create(WriterFacade.toList(u), null, user);
    return u;
  }

  /**
   * Delete a {@link User}
   * 
   * @param user
   * @throws ImejiException
   */
  public void delete(User user) throws ImejiException {
    // remove User from User Groups
    UserGroupController ugc = new UserGroupController();
    ugc.removeUserFromAllGroups(user, this.user);
    // remove user grant
    writer.delete(new ArrayList<Object>(user.getGrants()), this.user);
    // remove user
    writer.delete(WriterFacade.toList(user), this.user);
  }

  /**
   * Retrieve a {@link User} according to its email
   * 
   * @param email
   * @return
   * @throws ImejiException
   */
  public User retrieve(String email) throws ImejiException {
    return retrieve(email, user);
  }

  public User retrieve(String email, User currentUser) throws ImejiException {
    Search search = SearchFactory.create(SEARCH_IMPLEMENTATIONS.JENA);
    SearchResult result =
        search.searchString(JenaCustomQueries.selectUserByEmail(email), null, null, 0, -1);
    if (result.getNumberOfRecords() == 1) {
      return retrieve(URI.create(result.getResults().get(0)), currentUser);
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
  public User retrieve(URI uri) throws ImejiException {
    return retrieve(uri, user);
  }



  /**
   * Retrieve a {@link User} according to its uri (id)
   * 
   * @param uri
   * @return
   * @throws ImejiException
   */
  public User retrieve(URI uri, User currentUser) throws ImejiException {
    User u = (User) reader.read(uri.toString(), currentUser, new User());
    if (u.isActive()) {
      UserGroupController ugc = new UserGroupController();
      u.setGroups((List<UserGroup>) ugc.searchByUser(u, currentUser));
    }
    return u;
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
   * Retrieve a {@link User} according to its email
   * 
   * @param registrationToken
   * @return
   * @throws ImejiException
   */
  public User retrieveRegisteredUser(String registrationToken) throws ImejiException {
    Search search = SearchFactory.create(SEARCH_IMPLEMENTATIONS.JENA);
    SearchResult result = search.searchString(
        JenaCustomQueries.selectUserByRegistrationToken(registrationToken), null, null, 0, -1);
    if (result.getNumberOfRecords() == 1) {
      String id = result.getResults().get(0);
      User u = (User) reader.read(id, user, new User());
      return u;
    }
    throw new NotFoundException("Invalid registration token!");
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
    updatedUser.setModified(DateHelper.getCurrentDate());
    writer.update(WriterFacade.toList(updatedUser), null, currentUser, true);
    return updatedUser;
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
   * Activae a {@link User}
   * 
   * @param registrationToken
   * @throws ImejiException
   * @return
   */
  public User activate(String registrationToken) throws ImejiException {
    try {
      User activateUser = retrieveRegisteredUser(registrationToken);

      if (activateUser.isActive()) {
        throw new UnprocessableError("User is already activated!");
      }

      Calendar now = DateHelper.getCurrentDate();
      if (!(activateUser.getCreated().before(now))) {
        throw new UnprocessableError(
            "Registration date does not match, its bigger then the current date!");
      }

      Calendar validUntil = activateUser.getCreated();
      validUntil.add(Calendar.DAY_OF_MONTH, ConfigurationBean.getRegistrationTokenExpiryStatic());

      if ((now.after(validUntil))) {
        throw new UnprocessableError("Activation period expired, user should be deleted!");
      }

      activateUser.setUserStatus(User.UserStatus.ACTIVE);
      activateUser
          .setGrants(AuthorizationPredefinedRoles.defaultUser(activateUser.getId().toString()));
      writer.update(WriterFacade.toList(activateUser), null, activateUser, true);
      return activateUser;

    } catch (NotFoundException e) {
      throw new NotFoundException("Invalid registration token!");
    }
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
  public long checkQuota(File file, CollectionImeji col) throws ImejiException {
    // do not check quota for admin
    if (this.user.isAdmin()) {
      return -1L;
    }
    User targetCollectionUser = this.user.getId().equals(col.getCreatedBy()) ? this.user
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
          + "; requested by user: " + this.user.getEmail() + "; targetCollectionUser: "
          + targetCollectionUser.getEmail());
    }
    long needed = currentDiskUsage + file.length();
    if (needed > targetCollectionUser.getQuota()) {
      throw new QuotaExceededException("Data quota (" + targetCollectionUser.getQuotaHumanReadable()
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
    Search search = SearchFactory.create();
    return loadUsers(
        search.searchString(JenaCustomQueries.selectUserAll(name), null, null, 0, -1).getResults());
  }

  /**
   * Search for all users having the grant for an object
   * 
   * @param grantFor
   * @return
   */
  public Collection<User> searchByGrantFor(String grantFor) {
    Search search = SearchFactory.create(SEARCH_IMPLEMENTATIONS.JENA);
    return loadUsers(
        search.searchString(JenaCustomQueries.selectUserWithGrantFor(grantFor), null, null, 0, -1)
            .getResults());
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
   * Retrieve a User by its API Key
   * 
   * @param key
   * @return
   * @throws ImejiException
   */
  public User retrieveByApiKey(String key) throws ImejiException {
    Search search = SearchFactory.create(SEARCH_IMPLEMENTATIONS.JENA);
    SearchResult result =
        search.searchString(JenaCustomQueries.selectUserByApiKey(key), null, null, 0, -1);
    if (result.getNumberOfRecords() != 1) {
      throw new AuthenticationError("API Key not valid!");
    }
    return retrieve(URI.create(result.getResults().get(0)));
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
   * Load all {@link User}
   * 
   * @param uris
   * @return
   * @throws ImejiException
   */
  public Collection<User> loadUsers(List<String> uris) {
    List<User> users = new ArrayList<User>();
    for (String uri : uris) {
      try {
        users.add((User) reader.read(uri, user, new User()));
      } catch (ImejiException e) {
        LOGGER.info("Could not find user with URI " + uri, e);
      }
    }

    // Always sort Users by complete name
    Comparator<User> comparator = new Comparator<User>() {
      public int compare(User c1, User c2) {
        return c1.getPerson().getCompleteName().toLowerCase()
            .compareTo(c2.getPerson().getCompleteName().toLowerCase()); // use your logic
      }
    };
    Collections.sort(users, comparator);
    return users;
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
        orgs.add((Organization) reader.read(uri, user, new Organization()));
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
        p.add((Person) reader.read(uri, user, new Person()));
      } catch (ImejiException e) {

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
        admins.add(retrieve(URI.create(uri)));
      } catch (ImejiException e) {
        LOGGER.info("Could not retrieve any admin in the list. Something is wrong!");
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
    return (List<User>) loadUsers(uris);
  }

  /*
   * Returns the user with which the UserController is invoked
   */
  public User getControllerUser() {
    return user;
  }


  /**
   * Remove all the {@link Metadata} not having a {@link Statement}. This happens when a
   * {@link Statement} has been removed from a {@link MetadataProfile}.
   */
  public int cleanInactiveUsers() {
    Search search = SearchFactory.create();
    List<String> uris =
        search.searchString(JenaCustomQueries.getInactiveUsers(), null, null, 0, -1).getResults();
    List<User> cleaningCandidates = (List<User>) loadUsers(uris);
    int i = 0;
    for (User u : cleaningCandidates) {
      Calendar expiry = u.getCreated();
      expiry.add(Calendar.DAY_OF_MONTH, ConfigurationBean.getRegistrationTokenExpiryStatic());
      if (!u.isActive() && DateHelper.getCurrentDate().after(expiry)) {
        try {
          delete(u);
          i++;
        } catch (ImejiException e) {
          LOGGER.info("There has been an error in the expiry for users. Inactive user with email "
              + u.getEmail() + " could not be removed!", e);
        }
      }
    }
    return i;
  }
}
