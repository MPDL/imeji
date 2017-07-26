package de.mpg.imeji.logic.authorization.pwdreset;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.apache.log4j.Logger;

import de.mpg.imeji.exceptions.AlreadyExistsException;
import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.exceptions.NotFoundException;
import de.mpg.imeji.exceptions.UnprocessableError;
import de.mpg.imeji.logic.authorization.util.PasswordGenerator;
import de.mpg.imeji.logic.config.Imeji;
import de.mpg.imeji.logic.db.keyValue.KeyValueStoreService;
import de.mpg.imeji.logic.db.keyValue.stores.HTreeMapStore;
import de.mpg.imeji.logic.user.UserService;
import de.mpg.imeji.logic.user.UserService.USER_TYPE;
import de.mpg.imeji.logic.util.StringHelper;
import de.mpg.imeji.logic.validation.impl.UserValidator;
import de.mpg.imeji.logic.validation.impl.Validator.Method;
import de.mpg.imeji.logic.vo.User;
import de.mpg.imeji.util.DateHelper;

public class PasswordResetController {
  private static final Logger LOGGER = Logger.getLogger(PasswordResetController.class);

  private static final KeyValueStoreService tokens =
      new KeyValueStoreService(new HTreeMapStore("passwordResetStore"));

  public String generateResetToken(User user) {
    String tokenString = (new PasswordResetTokenGenerator()).generateToken();
    try {
      PasswordResetToken token = new PasswordResetToken(StringHelper.md5(tokenString), user);
      tokens.put(token.getEncryptedToken(), token);
      return tokenString;
    } catch (ImejiException e) {
      LOGGER.error("Error reseting the password", e);
    }
    return null;
  }

  public String register(User user) throws ImejiException {
    new UserValidator().validate(user, Method.CREATE);
    if (hasPendingRegistration(user.getEmail())) {
      throw new UnprocessableError("User has already a pending registration");
    }
    if (exists(user.getEmail())) {
      throw new AlreadyExistsException(user.getEmail() + " has already an account");
    }
    final String password = new PasswordGenerator().generatePassword();
    user.setEncryptedPassword(StringHelper.md5(password));
    return generateResetToken(user);
  }


  public boolean isValidToken(String tokenString) {
    try {
      PasswordResetToken token = (PasswordResetToken) tokens.get(StringHelper.md5(tokenString));
      return !isTokenExpired(token);
    } catch (ImejiException e) {
      return false;
    }
  }


  public User resetPassword(String tokenString, String password) throws UnprocessableError {
    PasswordResetToken token;
    try {
      token = (PasswordResetToken) tokens.get(StringHelper.md5(tokenString));
      token.getUser().setEncryptedPassword(StringHelper.md5(password));
      (new UserService()).update(token.getUser(), Imeji.adminUser);
      tokens.delete(token.getEncryptedToken());
      return token.getUser();
    } catch (Exception e) {
      throw new UnprocessableError("The token or the user was not found");
    }
  }

  public User activate(String tokenString, String password) throws UnprocessableError {
    PasswordResetToken token;
    try {
      token = (PasswordResetToken) tokens.get(StringHelper.md5(tokenString));
      token.getUser().setEncryptedPassword(StringHelper.md5(password));
    } catch (Exception e) {
      throw new UnprocessableError("The token was not found");
    }
    final USER_TYPE type =
        isAuthorizedEmail(token.getUser().getEmail()) ? USER_TYPE.DEFAULT : USER_TYPE.RESTRICTED;
    boolean exists = false;
    try {
      new UserService().update(token.getUser(), Imeji.adminUser);
      exists = true;
    } catch (ImejiException e) {
      // This is correct
    }
    if (!exists) {
      try {
        (new UserService()).create(token.getUser(), type);
        tokens.delete(token.getEncryptedToken());
      } catch (Exception e) {
        throw new UnprocessableError(
            "An error happend while trying to activate the user. Please try again.");
      }
    }
    return token.getUser();
  }


  public void deleteExpiredTokens() throws ImejiException {
    for (final PasswordResetToken token : retrieveAll()) {
      if (isTokenExpired(token)) {
        tokens.delete(token.getEncryptedToken());
      }
    }
  }

  /**
   * Retrieve the registration by email
   *
   * @param email
   * @return
   * @throws ImejiException
   */
  public PasswordResetToken retrieveByEmail(String email) throws ImejiException {
    final List<PasswordResetToken> registrations = new ArrayList<PasswordResetToken>();
    List<PasswordResetToken> allTokens = tokens.getList("", PasswordResetToken.class);
    for (PasswordResetToken token : allTokens) {
      if (token.getUser().getEmail().equals(email)) {
        registrations.add(token);
      }
    }
    if (registrations.size() == 1) {
      return registrations.get(0);
    }
    throw new NotFoundException(
        "Count of registration with email " + email + ": " + registrations.size());
  }

  public List<PasswordResetToken> retrieveAll() {
    try {
      return tokens.getList(".*", PasswordResetToken.class);
    } catch (final ImejiException e) {
      LOGGER.error("Error retrieving all registrations", e);
      return new ArrayList<>();
    }
  }

  private boolean isTokenExpired(PasswordResetToken token) {
    final Calendar expirationDate = token.getCreationDate();
    expirationDate.add(Calendar.DAY_OF_MONTH,
        Integer.valueOf(Imeji.CONFIG.getRegistrationTokenExpiry()));
    return DateHelper.getCurrentDate().after(expirationDate);
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
      deleteExpiredTokens();
      retrieveByEmail(email);
      return true;
    } catch (final NotFoundException e) {
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
      new UserService().retrieve(email, Imeji.adminUser);
      return true;
    } catch (final NotFoundException e) {
      return false;
    }
  }

  /**
   * True if the email is allowed according to the Registration white list
   *
   * @param email
   * @return
   */
  private boolean isAuthorizedEmail(String email) {
    final String rwl = Imeji.CONFIG.getRegistrationWhiteList();
    if (StringHelper.isNullOrEmptyTrim(rwl)) {
      return true;
    }
    for (final String suffix : rwl.split(",")) {
      if (email.trim().endsWith(suffix)) {
        return true;
      }
    }
    return false;
  }


}
