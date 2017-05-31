package de.mpg.imeji.logic.authorization.pwdreset;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.apache.log4j.Logger;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.exceptions.UnprocessableError;
import de.mpg.imeji.logic.config.Imeji;
import de.mpg.imeji.logic.db.keyValue.KeyValueStoreService;
import de.mpg.imeji.logic.db.keyValue.stores.HTreeMapStore;
import de.mpg.imeji.logic.user.UserService;
import de.mpg.imeji.logic.util.StringHelper;
import de.mpg.imeji.logic.vo.User;
import de.mpg.imeji.presentation.navigation.Navigation;
import de.mpg.imeji.util.DateHelper;

public class PasswordResetController {
  private static final Logger LOGGER = Logger.getLogger(PasswordResetController.class);

  private static final KeyValueStoreService tokens =
      new KeyValueStoreService(new HTreeMapStore("passwordResetStore"));

  public String generateResetURL(User user) {
    String tokenString = (new PasswordResetTokenGenerator()).generateToken();
    try {
      PasswordResetToken token = new PasswordResetToken(StringHelper.md5(tokenString), user);
      tokens.put(token.getEncryptedToken(), token);
      return Navigation.applicationUrl + "pwdreset?token=" + tokenString;
    } catch (ImejiException e) {
      LOGGER.error("Error reseting the password", e);
    }
    return null;
  }

  public boolean isValidToken(String tokenString) {
    try {
      PasswordResetToken token = (PasswordResetToken) tokens.get(StringHelper.md5(tokenString));
      return !isTokenExpired(token);
    } catch (ImejiException e) {
      return false;
    }
  }


  public void resetPassword(String tokenString, String password) throws UnprocessableError {
    PasswordResetToken token;
    try {
      token = (PasswordResetToken) tokens.get(StringHelper.md5(tokenString));
      token.getUser().setEncryptedPassword(StringHelper.md5(password));
      (new UserService()).update(token.getUser(), Imeji.adminUser);
      tokens.delete(token.getEncryptedToken());
    } catch (Exception e) {
      throw new UnprocessableError("The token or the user was not found");
    }
  }


  public void deleteExpiredTokens() throws ImejiException {
    for (final PasswordResetToken token : retrieveAll()) {
      if (isTokenExpired(token)) {
        tokens.delete(token.getEncryptedToken());
      }
    }
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


}
