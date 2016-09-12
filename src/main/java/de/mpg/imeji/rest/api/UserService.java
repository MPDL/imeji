package de.mpg.imeji.rest.api;

import static de.mpg.imeji.logic.Imeji.adminUser;

import java.net.URI;
import java.util.List;

import org.jose4j.lang.JoseException;

import de.mpg.imeji.exceptions.AuthenticationError;
import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.Imeji;
import de.mpg.imeji.logic.search.Search;
import de.mpg.imeji.logic.search.factory.SearchFactory;
import de.mpg.imeji.logic.search.jenasearch.JenaCustomQueries;
import de.mpg.imeji.logic.security.authentication.impl.APIKeyAuthentication;
import de.mpg.imeji.logic.user.controller.UserBusinessController;
import de.mpg.imeji.logic.vo.User;
import de.mpg.imeji.rest.to.SearchResultTO;
import de.mpg.imeji.rest.to.UserTO;
import de.mpg.imeji.rest.transfer.TransferObjectFactory;

/**
 * API Service for {@link UserTO}
 *
 * @author bastiens
 *
 */
public class UserService implements API<UserTO> {


  public User read(URI uri) throws ImejiException {
    return adminUser.getId().equals(uri) ? adminUser
        : new UserBusinessController().retrieve(uri, Imeji.adminUser);
  }

  public User read(String email) throws ImejiException {
    return adminUser.getEmail().equals(email) ? adminUser
        : new UserBusinessController().retrieve(email, Imeji.adminUser);
  }

  public String getCompleteName(URI uri) throws ImejiException {
    Search search = SearchFactory.create();
    List<String> results =
        search.searchString(JenaCustomQueries.selectUserCompleteName(uri), null, null, 0, -1)
            .getResults();
    return results.size() == 1 ? results.get(0) : null;
  }

  @Override
  public UserTO create(UserTO o, User u) throws ImejiException {
    return null;
  }

  @Override
  public UserTO read(String id, User u) throws ImejiException {
    return null;
  }

  @Override
  public UserTO update(UserTO userTO, User u) throws ImejiException {
    return null;
  }

  @Override
  public boolean delete(String i, User u) throws ImejiException {
    return false;
  }

  @Override
  public UserTO release(String i, User u) throws ImejiException {
    return null;
  }

  @Override
  public UserTO withdraw(String i, User u, String discardComment) throws ImejiException {
    return null;
  }

  @Override
  public void share(String id, String userId, List<String> roles, User u) throws ImejiException {}

  @Override
  public void unshare(String id, String userId, List<String> roles, User u) throws ImejiException {}

  @Override
  public SearchResultTO<UserTO> search(String q, int offset, int size, User u)
      throws ImejiException {
    return null;
  }

  /**
   * Update the key of a user in the database
   *
   * @param user
   * @param key
   * @throws ImejiException
   * @throws JoseException
   */
  public UserTO updateUserKey(User userVO, boolean login) throws ImejiException, JoseException {
    // This method must be called with proper user authentication
    if (userVO == null) {
      throw new AuthenticationError("Authentication is required to call this method!");
    }
    if ((login && (userVO.getApiKey() == null || "".equals(userVO.getApiKey()))) || !login) {
      // If it is login, then update the key only if it is null
      userVO.setApiKey(generateNewKey(userVO));
      new UserBusinessController().update(userVO, userVO);
    }
    return TransferObjectFactory.transferUser(userVO);
  }


  /**
   * Generate a new Key for the {@link User}. Key is saved in the database
   *
   * @param user
   * @return
   * @throws JoseException
   * @throws ImejiException
   */
  private String generateNewKey(User user) throws JoseException, ImejiException {
    return APIKeyAuthentication.generateKey(user.getId(), Integer.MAX_VALUE);
  }

}
