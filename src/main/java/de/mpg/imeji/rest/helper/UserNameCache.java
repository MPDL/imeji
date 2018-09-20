package de.mpg.imeji.rest.helper;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.Logger; 
import org.apache.logging.log4j.LogManager;

import de.mpg.imeji.rest.api.UserAPIService;

/**
 * Class to be used as cache to read the usernames from the database
 *
 * @author bastiens
 *
 */
public class UserNameCache {
  private final Map<String, String> userNameMap = new HashMap<>();
  private static final Logger LOGGER = LogManager.getLogger(UserNameCache.class);

  public String getUserName(URI userId) {
    if (userId == null) {
      return null;
    }
    if (userNameMap.containsKey(userId.toString())) {
      return userNameMap.get(userId.toString());
    } else {
      try {
        final UserAPIService ucrud = new UserAPIService();
        final String name = ucrud.getCompleteName(userId);
        userNameMap.put(userId.toString(), name);
        return name;
      } catch (final Exception e) {
        LOGGER.error("Cannot read user: " + userId, e);
        return null;
      }
    }
  }
}
