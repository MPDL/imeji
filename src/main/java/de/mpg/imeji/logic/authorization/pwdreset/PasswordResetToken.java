package de.mpg.imeji.logic.authorization.pwdreset;

import java.io.Serializable;
import java.util.Calendar;

import de.mpg.imeji.logic.vo.User;
import de.mpg.imeji.util.DateHelper;

public class PasswordResetToken implements Serializable {
  private static final long serialVersionUID = -2011385424245112709L;

  private final String encryptedToken;
  private final User user;
  private final Calendar creationDate = DateHelper.getCurrentDate();

  public PasswordResetToken(String encryptedToken, User user) {
    this.encryptedToken = encryptedToken;
    this.user = user;
  }

  public String getEncryptedToken() {
    return encryptedToken;
  }

  public Calendar getCreationDate() {
    return creationDate;
  }

  public User getUser() {
    return user;
  }


}
