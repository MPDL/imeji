package de.mpg.imeji.logic.registration;

import java.io.Serializable;
import java.util.Calendar;

import de.mpg.imeji.logic.vo.User;
import de.mpg.imeji.util.DateHelper;

/**
 * A Registration to imeji
 *
 * @author bastiens
 *
 */
public final class Registration implements Serializable {
  private static final long serialVersionUID = -4028051698769347597L;
  private final User user;
  private final String password;
  private final String token;
  private final Calendar creationDate = DateHelper.getCurrentDate();

  public Registration(String token, User user, String password) {
    this.token = token;
    this.user = user;
    this.password = password;
  }

  /**
   * @return the user
   */
  public User getUser() {
    return user;
  }

  public String getToken() {
    return token;
  }

  public String getPassword() {
    return password;
  }

  public String getKey() {
    return token + ":" + user.getEmail();
  }

  public Calendar getCreationDate() {
    return creationDate;
  }

}
