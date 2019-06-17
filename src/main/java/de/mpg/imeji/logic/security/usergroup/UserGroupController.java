package de.mpg.imeji.logic.security.usergroup;

import java.lang.reflect.Field;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.config.Imeji;
import de.mpg.imeji.logic.db.reader.ReaderFacade;
import de.mpg.imeji.logic.db.writer.WriterFacade;
import de.mpg.imeji.logic.generic.AccessElement;
import de.mpg.imeji.logic.model.User;
import de.mpg.imeji.logic.model.UserGroup;
import de.mpg.imeji.logic.model.aspects.AccessMember.ActionType;
import de.mpg.imeji.logic.model.aspects.AccessMember.ChangeMember;
import de.mpg.imeji.logic.search.Search.SearchObjectTypes;

/**
 * Resource Controller for {@link UserGroup}
 *
 * @author saquet
 *
 */
class UserGroupController implements AccessElement<UserGroup> {

  private static final Logger LOGGER = LogManager.getLogger(UserGroupController.class);
  private static final ReaderFacade READER = new ReaderFacade(Imeji.userModel);
  private static final WriterFacade WRITER = new WriterFacade(Imeji.userModel, SearchObjectTypes.USERGROUPS);

  /**
   * Create a {@link UserGroup}
   *
   * @param group
   * @throws ImejiException
   */
  public void create(UserGroup group, User user) throws ImejiException {
    WRITER.create(WriterFacade.toList(group), user);
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
    final List<UserGroup> groups = new ArrayList<>();
    for (final String uri : uris) {
      try {
        groups.add((UserGroup) READER.read(uri, user, new UserGroup()));
      } catch (final ImejiException e) {
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
    final List<UserGroup> groups = new ArrayList<>();
    for (final String uri : uris) {
      try {
        groups.add((UserGroup) READER.readLazy(uri, user, new UserGroup()));
      } catch (final ImejiException e) {
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
    WRITER.update(WriterFacade.toList(group), user, true);
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

  @Override
  public UserGroup changeElement(User user, UserGroup imejiDataObject, Field elementField, ActionType action, Object element)
      throws ImejiException {

    ChangeMember changeMember = new ChangeMember(action, imejiDataObject, elementField, element);
    UserGroup userGroup = (UserGroup) WRITER.changeElement(changeMember, user);
    return userGroup;
  }

}
