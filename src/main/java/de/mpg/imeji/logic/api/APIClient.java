package de.mpg.imeji.logic.api;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

import org.apache.log4j.Logger;
import org.jose4j.lang.JoseException;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.Imeji;
import de.mpg.imeji.logic.user.authentication.impl.APIKeyAuthentication;
import de.mpg.imeji.logic.user.controller.UserBusinessController;
import de.mpg.imeji.logic.vo.User;

/**
 * A Client to use the imeji REST API
 *
 * @author bastiens
 *
 */
public class APIClient {

  private WebTarget target;
  public static final String REST_PROFILES_PATH = "rest/profiles";
  private static final Logger LOGGER = Logger.getLogger(APIClient.class);

  public APIClient() {
    Client client = ClientBuilder.newClient();
    target = client.target(Imeji.PROPERTIES.getApplicationURL());
  }

  /**
   * Post to imeji API
   *
   * @param path
   * @param json
   * @param user
   * @return
   */
  public Response post(String path, String json, User user) {
    return target.path(path).request().accept("application/json")
        .header(HttpHeaders.AUTHORIZATION, getAuthorizationHeader(user)).post(Entity.json(json));
  }

  /**
   * The Authorization Header with API Key
   *
   * @param user
   * @return
   */
  private String getAuthorizationHeader(User user) {
    return "Bearer " + getAPIKey(user);
  }

  /**
   * Retrieve the API Key of the user, create a new if necessary
   *
   * @param user
   * @return
   */
  private String getAPIKey(User user) {
    if (user.getApiKey() != null) {
      return user.getApiKey();
    }
    try {
      UserBusinessController userController = new UserBusinessController();
      user.setApiKey(APIKeyAuthentication.generateKey(user.getId(), Integer.MAX_VALUE));
      userController.update(user, Imeji.adminUser);
    } catch (ImejiException | JoseException e) {
      LOGGER.error("Error generation api key for user " + user.getEmail(), e);
    }
    return user.getApiKey();

  }
}
