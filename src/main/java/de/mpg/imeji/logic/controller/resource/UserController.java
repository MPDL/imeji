package de.mpg.imeji.logic.controller.resource;

import java.net.URI;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.log4j.Logger;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.Imeji;
import de.mpg.imeji.logic.reader.ReaderFacade;
import de.mpg.imeji.logic.search.jenasearch.ImejiSPARQL;
import de.mpg.imeji.logic.search.jenasearch.JenaCustomQueries;
import de.mpg.imeji.logic.user.controller.GroupBusinessController;
import de.mpg.imeji.logic.vo.Grant;
import de.mpg.imeji.logic.vo.Item;
import de.mpg.imeji.logic.vo.User;
import de.mpg.imeji.logic.vo.UserGroup;
import de.mpg.imeji.logic.writer.WriterFacade;
import de.mpg.imeji.util.DateHelper;

/**
 * Resource Controllers for {@link User}
 * 
 * @author saquet
 *
 */
public class UserController {
  private static final ReaderFacade READER = new ReaderFacade(Imeji.userModel);
  private static final WriterFacade WRITER = new WriterFacade(Imeji.userModel);

  private static final Logger LOGGER = Logger.getLogger(UserController.class);
  private static final Comparator<User> USER_COMPARATOR_BY_NAME = new Comparator<User>() {
    @Override
    public int compare(User c1, User c2) {
      return c1.getPerson().getCompleteName().toLowerCase()
          .compareTo(c2.getPerson().getCompleteName().toLowerCase());
    }
  };

  public User create(User user) throws ImejiException {
    Calendar now = DateHelper.getCurrentDate();
    user.setCreated(now);
    user.setModified(now);
    WRITER.create(WriterFacade.toList(user), null, Imeji.adminUser);
    return user;
  }


  /**
   * Retrieve a {@link User} according to its uri (id)
   *
   * @param uri
   * @return
   * @throws ImejiException
   */
  public User retrieve(URI uri, User user) throws ImejiException {
    User u = (User) READER.read(uri.toString(), user, new User());
    if (u.isActive()) {
      GroupBusinessController ugc = new GroupBusinessController();
      u.setGroups((List<UserGroup>) ugc.searchByUser(u, Imeji.adminUser));
    }
    return u;
  }

  /**
   * Retrieve all {@link Item} (all status, all users) in imeji
   *
   * @return
   * @throws ImejiException
   */
  public List<User> retrieveAll() throws ImejiException {
    List<String> uris = ImejiSPARQL.exec(JenaCustomQueries.selectUserAll(), Imeji.userModel);
    List<User> users = new ArrayList<>();
    for (String uri : uris) {
      users.add(retrieve(URI.create(uri), Imeji.adminUser));
    }
    return users;
  }

  /**
   * Load all {@link User}
   *
   * @param uris
   * @return
   * @throws ImejiException
   */
  public List<User> retrieveBatchLazy(List<String> uris, int limit) {
    int max = limit < uris.size() && limit > 0 ? limit : uris.size();
    List<User> users = new ArrayList<User>(max);
    for (int i = 0; i < max; i++) {
      try {
        users.add((User) READER.readLazy(uris.get(i), Imeji.adminUser, new User()));
      } catch (ImejiException e) {
        LOGGER.error("Error reading user", e);
      }
    }
    Collections.sort(users, USER_COMPARATOR_BY_NAME);
    return users;
  }

  /**
   * Load all {@link User}
   *
   * @param uris
   * @return
   * @throws ImejiException
   */
  public Collection<User> retrieveBatch(List<String> uris, int limit) {
    int max = limit < uris.size() && limit > 0 ? limit : uris.size();
    List<User> users = new ArrayList<User>(max);
    for (int i = 0; i < max; i++) {
      try {
        users.add((User) READER.read(uris.get(i), Imeji.adminUser, new User()));
      } catch (ImejiException e) {
        LOGGER.error("Error reading user", e);
      }
    }
    Collections.sort(users, USER_COMPARATOR_BY_NAME);
    return users;
  }


  /**
   * Update a {@link User}
   *
   * @param updatedUser : The user who is updated in the database
   * @param currentUser
   * @throws ImejiException
   * @return
   */
  public User update(User updatedUser) throws ImejiException {
    updatedUser.setModified(DateHelper.getCurrentDate());
    WRITER.update(WriterFacade.toList(updatedUser), null, Imeji.adminUser, true);
    return updatedUser;
  }

  /**
   * Delete a {@link User}
   *
   * @param user
   * @throws ImejiException
   */
  public void delete(User user) throws ImejiException {
    // remove User from User Groups
    GroupBusinessController ugc = new GroupBusinessController();
    ugc.removeUserFromAllGroups(user, Imeji.adminUser);
    // remove user grant
    WRITER.delete(new ArrayList<Object>(user.getGrants()), Imeji.adminUser);
    // remove user
    WRITER.delete(WriterFacade.toList(user), Imeji.adminUser);
  }

  /**
   * Remove the following grants
   * 
   * @param grants
   * @throws ImejiException
   */
  public void deleteGrants(List<Grant> grants) throws ImejiException {
    WRITER.delete(new ArrayList<Object>(grants), Imeji.adminUser);
  }

}
