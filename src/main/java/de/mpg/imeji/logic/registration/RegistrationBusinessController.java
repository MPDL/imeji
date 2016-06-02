package de.mpg.imeji.logic.registration;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.apache.log4j.Logger;

import de.mpg.imeji.exceptions.AlreadyExistsException;
import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.exceptions.NotFoundException;
import de.mpg.imeji.exceptions.UnprocessableError;
import de.mpg.imeji.logic.Imeji;
import de.mpg.imeji.logic.auth.util.PasswordGenerator;
import de.mpg.imeji.logic.controller.resource.UserController;
import de.mpg.imeji.logic.controller.resource.UserController.USER_TYPE;
import de.mpg.imeji.logic.keyValueStore.KeyValueStoreBusinessController;
import de.mpg.imeji.logic.keyValueStore.stores.HTreeMapStore;
import de.mpg.imeji.logic.util.IdentifierUtil;
import de.mpg.imeji.logic.util.StringHelper;
import de.mpg.imeji.logic.validation.impl.UserValidator;
import de.mpg.imeji.logic.validation.impl.Validator.Method;
import de.mpg.imeji.logic.vo.User;
import de.mpg.j2j.helper.DateHelper;

/**
 * Business Controller for user registration
 *
 * @author bastiens
 *
 */
public class RegistrationBusinessController {
  private static final Logger LOGGER = Logger.getLogger(RegistrationBusinessController.class);
  private static final KeyValueStoreBusinessController KEY_VALUE_STORE_BC =
      new KeyValueStoreBusinessController(new HTreeMapStore("registrationStore"));

  /**
   * Retrieve a registration by its token
   *
   * @param token
   * @return
   * @throws ImejiException
   */
  public Registration retrieveByToken(String token) throws ImejiException {
    List<Registration> registrations =
        KEY_VALUE_STORE_BC.getList(token + ":.*", Registration.class);
    if (registrations.size() == 1) {
      return registrations.get(0);
    }
    throw new NotFoundException(
        "Count of registration with token " + token + ": " + registrations.size());
  }

  /**
   * Retrieve the registration by email
   *
   * @param email
   * @return
   * @throws ImejiException
   */
  public Registration retrieveByEmail(String email) throws ImejiException {
    List<Registration> registrations =
        KEY_VALUE_STORE_BC.getList(".*:" + email, Registration.class);
    if (registrations.size() == 1) {
      return registrations.get(0);
    }
    throw new NotFoundException(
        "Count of registration with email " + email + ": " + registrations.size());
  }

  /**
   * Register a user to imeji: A registration is created
   *
   * @param user
   * @return
   * @throws Exception
   */
  public Registration register(User user) throws ImejiException {
    new UserValidator().validate(user, Method.CREATE);
    if (hasPendingRegistration(user.getEmail())) {
      throw new UnprocessableError("User has already a pending registration");
    }
    if (exists(user.getEmail())) {
      throw new AlreadyExistsException(user.getEmail() + " has already an account");
    }
    String password = new PasswordGenerator().generatePassword();
    user.setEncryptedPassword(StringHelper.convertToMD5(password));
    Registration registration =
        new Registration(IdentifierUtil.newUniversalUniqueId(), user, password);
    KEY_VALUE_STORE_BC.put(registration.getKey(), registration);
    return registration;
  }

  /**
   * Delete the Registration
   *
   * @param token
   * @throws ImejiException
   */
  public void delete(Registration registration) throws ImejiException {
    KEY_VALUE_STORE_BC.delete(registration.getKey());
  }

  /**
   * Remove all expired Registration
   *
   * @throws ImejiException
   */
  public void deleteExpiredRegistration() throws ImejiException {
    for (Registration registration : retrieveAll()) {
      if (isExpired(registration)) {
        delete(registration);
      }
    }
  }

  /**
   * Retrieve all pending registrations
   *
   * @return
   * @throws ImejiException
   */
  public List<Registration> retrieveAll() {
    try {
      return KEY_VALUE_STORE_BC.getList(".*", Registration.class);
    } catch (ImejiException e) {
      LOGGER.error("Error retrieving all registrations", e);
      return new ArrayList<>();
    }
  }

  /**
   * Search for users which are registered but still not activated
   *
   * @param q
   * @return
   * @throws ImejiException
   */
  public List<User> searchInactiveUsers(String q) {
    List<User> users = new ArrayList<>();
    for (Registration r : retrieveAll()) {
      if (matchUser(r.getUser(), q)) {
        users.add(r.getUser());
      }
    }
    return users;
  }



  /**
   * Remove all pending registrations
   *
   * @throws ImejiException
   */
  public void removeAll() throws ImejiException {
    for (Registration registration : retrieveAll()) {
      KEY_VALUE_STORE_BC.delete(registration.getKey());
    }
  }

  /**
   * True if the registration is expired
   *
   * @param registration
   * @return
   */
  private boolean isExpired(Registration registration) {
    Calendar expirationDate = registration.getCreationDate();
    expirationDate.add(Calendar.DAY_OF_MONTH,
        Integer.valueOf(Imeji.CONFIG.getRegistrationTokenExpiry()));
    return DateHelper.getCurrentDate().after(expirationDate);
  }

  /**
   * Activate a user: Get the registration, create the user
   *
   * @param token
   * @return
   * @throws ImejiException
   */
  public User activate(Registration registration) throws ImejiException {
    if (isExpired(registration)) {
      throw new UnprocessableError("Registration is expired");
    }
    USER_TYPE type = isAuthorizedEmail(registration.getUser().getEmail()) ? USER_TYPE.DEFAULT
        : USER_TYPE.RESTRICTED;
    User user = new UserController(Imeji.adminUser).create(registration.getUser(), type);
    delete(registration);
    return user;
  }


  // /**
  // * Retrieve a {@link User} according to its email
  // *
  // * @param registrationToken
  // * @return
  // * @throws ImejiException
  // */
  // public User retrieveRegisteredUser(String registrationToken) throws ImejiException {
  // Search search = SearchFactory.create(SEARCH_IMPLEMENTATIONS.JENA);
  // SearchResult result = search.searchString(
  // JenaCustomQueries.selectUserByRegistrationToken(registrationToken), null, null, 0, -1);
  // if (result.getNumberOfRecords() == 1) {
  // String id = result.getResults().get(0);
  // return userController.retrieve(URI.create(id));
  // }
  // throw new NotFoundException("Invalid registration token!");
  // }
  //
  // /**
  // * Register a user an return the generated password
  // *
  // * @param user
  // * @return
  // * @throws Exception
  // */
  // public String registerOld(User user) throws Exception {
  // PasswordGenerator generator = new PasswordGenerator();
  // String password = generator.generatePassword();
  // user.setEncryptedPassword(StringHelper.convertToMD5(password));
  // user = userController.create(user, UserController.USER_TYPE.INACTIVE);
  // return password;
  // }
  //
  // /**
  // * Activae a {@link User}
  // *
  // * @param registrationToken
  // * @throws ImejiException
  // * @return
  // */
  // public User activateOld(String registrationToken) throws ImejiException {
  // try {
  // User activateUser = retrieveRegisteredUser(registrationToken);
  //
  // if (activateUser.isActive()) {
  // throw new UnprocessableError("User is already activated!");
  // }
  //
  // Calendar now = DateHelper.getCurrentDate();
  // if (!(activateUser.getCreated().before(now))) {
  // throw new UnprocessableError(
  // "Registration date does not match, its bigger then the current date!");
  // }
  //
  // Calendar validUntil = activateUser.getCreated();
  // validUntil.add(Calendar.DAY_OF_MONTH,
  // Integer.valueOf(Imeji.CONFIG.getRegistrationTokenExpiry()));
  //
  // if ((now.after(validUntil))) {
  // throw new UnprocessableError("Activation period expired, user should be deleted!");
  // }
  //
  // activateUser.setUserStatus(User.UserStatus.ACTIVE);
  //
  // List<Grant> grants = isAuthorizedEmail(activateUser.getEmail())
  // ? AuthorizationPredefinedRoles.defaultUser(activateUser.getId().toString())
  // : AuthorizationPredefinedRoles.restrictedUser(activateUser.getId().toString());
  //
  // activateUser.getGrants().addAll(grants);
  // userController.update(activateUser, Imeji.adminUser);
  // return activateUser;
  // } catch (NotFoundException e) {
  // throw new NotFoundException("Invalid registration token!");
  // }
  // }

  /**
   * True if the email is allowed according to the Registration white list
   *
   * @param email
   * @return
   */
  private boolean isAuthorizedEmail(String email) {
    String rwl = Imeji.CONFIG.getRegistrationWhiteList();
    if (StringHelper.isNullOrEmptyTrim(rwl)) {
      return true;
    }
    for (String suffix : rwl.split(",")) {
      if (email.endsWith(suffix)) {
        return true;
      }
    }
    return false;
  }

  /**
   * True if the query matches the user
   *
   * @param user
   * @param q
   * @return
   */
  private boolean matchUser(User user, String q) {
    return StringHelper.isNullOrEmptyTrim(q) || user.getPerson().getCompleteName().contains(q)
        || user.getPerson().getOrganizationString().contains(q) || user.getEmail().contains(q);
  }

  /**
   * True if there is a pending registration for this email
   *
   * @param email
   * @return
   * @throws ImejiException
   */
  private boolean hasPendingRegistration(String email) throws ImejiException {
    try {
      retrieveByEmail(email);
      return true;
    } catch (NotFoundException e) {
      return false;
    }
  }

  /**
   * True if the email is already used by a user in imeji
   *
   * @param email
   * @return
   * @throws ImejiException
   */
  private boolean exists(String email) throws ImejiException {
    try {
      new UserController(Imeji.adminUser).retrieve(email);
      return true;
    } catch (NotFoundException e) {
      return false;
    }
  }

}
