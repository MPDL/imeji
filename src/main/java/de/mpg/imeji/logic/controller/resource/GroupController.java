package de.mpg.imeji.logic.controller.resource;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.Imeji;
import de.mpg.imeji.logic.reader.ReaderFacade;
import de.mpg.imeji.logic.search.Search.SearchObjectTypes;
import de.mpg.imeji.logic.vo.User;
import de.mpg.imeji.logic.vo.UserGroup;
import de.mpg.imeji.logic.writer.WriterFacade;

/**
 * Resource Controller for {@link UserGroup}
 * 
 * @author saquet
 *
 */
public class GroupController {
  private static final Logger LOGGER = Logger.getLogger(GroupController.class);
  private static final ReaderFacade READER = new ReaderFacade(Imeji.userModel);
  private static final WriterFacade WRITER =
      new WriterFacade(Imeji.userModel, SearchObjectTypes.USERGROUPS);

  /**
   * Create a {@link UserGroup}
   *
   * @param group
   * @throws ImejiException
   */
  public void create(UserGroup group, User user) throws ImejiException {
    WRITER.create(WriterFacade.toList(group), null, user);
  }

  /**
   * Read a {@link UserGroup} with the given uri
   *
   * @param uri
   * @return
   * @throws ImejiException
   */
  public UserGroup retrieve(String uri, User user) throws ImejiException {
    return (UserGroup) READER.read(uri, user, new UserGroup());
  }

  /**
   * Read a {@link UserGroup} with the given uri
   *
   * @param uri
   * @return
   * @throws ImejiException
   */
  public UserGroup retrieveLazy(String uri, User user) throws ImejiException {
    return (UserGroup) READER.readLazy(uri, user, new UserGroup());
  }

  /**
   * Retrieve a list of {@link UserGroup}
   * 
   * @param uris
   * @param user
   * @return
   * @throws ImejiException
   */
  public List<UserGroup> retrieveBatch(List<String> uris, User user) {
    List<UserGroup> groups = new ArrayList<>();
    for (String uri : uris) {
      try {
        groups.add((UserGroup) READER.read(uri, user, new UserGroup()));
      } catch (ImejiException e) {
        LOGGER.error("Error reading user group" + uri, e);
      }
    }
    return groups;
  }

  /**
   * Retrieve a list of {@link UserGroup}
   * 
   * @param uris
   * @param user
   * @return
   * @throws ImejiException
   */
  public List<UserGroup> retrieveBatchLazy(List<String> uris, User user) {
    List<UserGroup> groups = new ArrayList<>();
    for (String uri : uris) {
      try {
        groups.add((UserGroup) READER.readLazy(uri, user, new UserGroup()));
      } catch (ImejiException e) {
        LOGGER.error("Error reading user group" + uri, e);
      }
    }
    return groups;
  }

  /**
   * Read a {@link UserGroup} with the given {@link URI}
   *
   * @param uri
   * @return
   * @throws ImejiException
   */
  public UserGroup read(URI uri, User user) throws ImejiException {
    return retrieve(uri.toString(), user);
  }

  /**
   * Update a {@link UserGroup}
   *
   * @param group
   * @param user
   * @throws ImejiException
   */
  public void update(UserGroup group, User user) throws ImejiException {
    WRITER.update(WriterFacade.toList(group), null, user, true);
  }

  /**
   * Delete a {@link UserGroup}
   *
   * @param group
   * @param user
   * @throws ImejiException
   */
  public void delete(UserGroup group, User user) throws ImejiException {
    WRITER.delete(WriterFacade.toList(group), user);
  }

}
