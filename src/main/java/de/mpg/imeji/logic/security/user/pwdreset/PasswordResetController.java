package de.mpg.imeji.logic.security.user.pwdreset;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.exceptions.UnprocessableError;
import de.mpg.imeji.logic.config.Imeji;
import de.mpg.imeji.logic.db.keyValue.KeyValueStoreService;
import de.mpg.imeji.logic.db.keyValue.stores.HTreeMapStore;
import de.mpg.imeji.logic.model.User;
import de.mpg.imeji.logic.security.user.UserService;
import de.mpg.imeji.logic.util.StringHelper;
import de.mpg.imeji.util.DateHelper;

public class PasswordResetController {
  private static final Logger LOGGER = LogManager.getLogger(PasswordResetController.class);
  private static final KeyValueStoreService tokenStore = new KeyValueStoreService(new HTreeMapStore("passwordResetStore"));

  /**
   * Generate a new Reset-Password token
   * 
   * @param user
   * @return
   */
  public String generateResetToken(User user) {
    String tokenString = (new PasswordResetTokenGenerator()).generateToken();
    try {
      PasswordResetToken token = new PasswordResetToken(StringHelper.md5(tokenString), user);
      tokenStore.put(token.getEncryptedToken(), token);
      return tokenString;
    } catch (ImejiException e) {
      LOGGER.error("Error reseting the password", e);
    }
    return null;
  }

  /**
   * True if the token is valid
   * 
   * @param tokenString
   * @return
   */
  public boolean isValidToken(String tokenString) {
    if (tokenString != null) {
      try {
        PasswordResetToken token = (PasswordResetToken) tokenStore.get(StringHelper.md5(tokenString));
        return !isTokenExpired(token);
      } catch (ImejiException e) {
        // invalid token
      }
    }
    return false;
  }

  /**
   * Reset the Password of the user associated to the passed token
   * 
   * @param tokenString
   * @param password
   * @return
   * @throws UnprocessableError
   */
  public User resetPassword(String tokenString, String password) throws UnprocessableError {
    try {
      PasswordResetToken token = retrieveToken(tokenString);
      User user = resetPassword(token.getUser(), password);
      tokenStore.delete(token.getEncryptedToken());
      return user;
    } catch (Exception e) {
      throw new UnprocessableError("The token or the user was not found");
    }
  }

  /**
   * Reset the password of the user
   * 
   * @param user
   * @param password
   * @return
   * @throws ImejiException
   */
  public User resetPassword(User user, String password) throws ImejiException {
    user.setEncryptedPassword(StringHelper.md5(password));
    return new UserService().update(user, Imeji.adminUser);
  }

  /**
   * Retrieve the token
   * 
   * @param token
   * @return
   * @throws ImejiException
   */
  public PasswordResetToken retrieveToken(String token) throws ImejiException {
    return (PasswordResetToken) tokenStore.get(StringHelper.md5(token));
  }

  public void deleteExpiredTokens() throws ImejiException {
    for (final PasswordResetToken token : retrieveAll()) {
      if (isTokenExpired(token)) {
        tokenStore.delete(token.getEncryptedToken());
      }
    }
  }

  public List<PasswordResetToken> retrieveAll() {
    try {
      return tokenStore.getList(".*", PasswordResetToken.class);
    } catch (final ImejiException e) {
      LOGGER.error("Error retrieving all registrations", e);
      return new ArrayList<>();
    }
  }

  private boolean isTokenExpired(PasswordResetToken token) {
    final Calendar expirationDate = token.getCreationDate();
    expirationDate.add(Calendar.DAY_OF_MONTH, Integer.valueOf(Imeji.CONFIG.getRegistrationTokenExpiry()));
    return DateHelper.getCurrentDate().after(expirationDate);
  }

}
