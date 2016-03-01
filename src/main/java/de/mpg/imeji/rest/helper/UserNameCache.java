package de.mpg.imeji.rest.helper;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import de.mpg.imeji.rest.api.UserService;

/**
 * Class to be used as cache to read the usernames from the database
 * 
 * @author bastiens
 *
 */
public class UserNameCache {
  private Map<String, String> userNameMap = new HashMap<>();
  private static final Logger LOGGER = Logger.getLogger(UserNameCache.class);

  public String getUserName(URI userId) {
    if (userId == null) {
      return null;
    }
    if (userNameMap.containsKey(userId.toString())) {
      return userNameMap.get(userId.toString());
    } else {
      try {
        UserService ucrud = new UserService();
        String name = ucrud.getCompleteName(userId);
        userNameMap.put(userId.toString(), name);
        return name;
      } catch (Exception e) {
        LOGGER.error("Cannot read user: " + userId, e);
        return null;
      }
    }
  }


}
