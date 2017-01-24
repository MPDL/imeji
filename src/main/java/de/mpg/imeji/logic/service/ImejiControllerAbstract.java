package de.mpg.imeji.logic.service;

import java.util.Arrays;
import java.util.List;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.exceptions.NotSupportedMethodException;
import de.mpg.imeji.exceptions.UnprocessableError;
import de.mpg.imeji.exceptions.WorkflowException;
import de.mpg.imeji.logic.concurrency.locks.Locks;
import de.mpg.imeji.logic.config.Imeji;
import de.mpg.imeji.logic.user.UserController;
import de.mpg.imeji.logic.util.StringHelper;
import de.mpg.imeji.logic.vo.Grant;
import de.mpg.imeji.logic.vo.Grant.GrantType;
import de.mpg.imeji.logic.vo.ImejiLicenses;
import de.mpg.imeji.logic.vo.Item;
import de.mpg.imeji.logic.vo.License;
import de.mpg.imeji.logic.vo.Properties;
import de.mpg.imeji.logic.vo.User;
import de.mpg.imeji.logic.workflow.WorkflowManager;

/**
 * Abstract controller for all imeji controllers
 */
public abstract class ImejiControllerAbstract<T> {
  private static final WorkflowManager WORKFLOW_MANAGER = new WorkflowManager();

  /**
   * Create a Single object
   *
   * @param t
   * @param user
   * @return
   */
  public T create(T t, User user) throws ImejiException {
    return createBatch(Arrays.asList(t), user).get(0);
  }

  /**
   * retrieve a single Object
   *
   * @param id
   * @param user
   * @return
   */
  public T retrieve(String id, User user) throws ImejiException {
    return retrieveBatch(Arrays.asList(id), user).get(0);
  }

  /**
   * Retrieve a single object wihout any list of content defined as lazy
   *
   * @param id
   * @param user
   * @return
   * @throws ImejiException
   */
  public T retrieveLazy(String id, User user) throws ImejiException {
    return retrieveBatchLazy(Arrays.asList(id), user).get(0);
  }

  /**
   * Update a single object
   *
   * @param t
   * @param user
   * @return
   */
  public T update(T t, User user) throws ImejiException {
    return updateBatch(Arrays.asList(t), user).get(0);
  }

  /**
   * Delete a single Object
   *
   * @param t
   * @param user
   * @return
   */
  public void delete(T t, User user) throws ImejiException {
    createBatch(Arrays.asList(t), user).get(0);
  }

  /**
   * Create a list of objects
   *
   * @param l
   * @param user
   * @return
   */
  public abstract List<T> createBatch(List<T> l, User user) throws ImejiException;

  /**
   * Retrieve a list of objects
   *
   * @param ids
   * @param user
   * @return
   */
  public abstract List<T> retrieveBatch(List<String> ids, User user) throws ImejiException;

  /**
   * Retrieve a list of objects but without their lazy list content
   *
   * @param ids
   * @param user
   * @return
   * @throws ImejiException
   */
  public abstract List<T> retrieveBatchLazy(List<String> ids, User user) throws ImejiException;

  /**
   * Update a list of objects
   *
   * @param l
   * @param user
   * @return
   */
  public abstract List<T> updateBatch(List<T> l, User user) throws ImejiException;

  /**
   * Delete a list of objects
   *
   * @param l
   * @param user
   * @return
   */
  public abstract void deleteBatch(List<T> l, User user) throws ImejiException;


  /**
   * Cast a list of any Type to a list of object
   *
   * @param l
   * @return
   */
  @SuppressWarnings("unchecked")
  public static List<Object> toObjectList(List<?> l) {
    return (List<Object>) l;
  }

  /**
   * Add the {@link Properties} to an imeji object when it is created
   *
   * @param properties
   * @param user
   * @throws WorkflowException
   */
  protected void prepareCreate(Properties properties, User user) throws WorkflowException {
    WORKFLOW_MANAGER.prepareCreate(properties, user);
  }

  /**
   * Update the grants of the user who created the objects
   *
   * @param user
   * @param uri
   * @throws ImejiException
   */
  protected void updateCreatorGrants(User user, String uri) throws ImejiException {
    user.getGrants().add(new Grant(GrantType.ADMIN, uri).toGrantString());
    new UserController().update(user);
  }

  /**
   * Add the {@link Properties} to an imeji object when it is updated
   *
   * @param properties
   * @param user
   */
  protected void prepareUpdate(Properties properties, User user) {
    WORKFLOW_MANAGER.prepareUpdate(properties, user);
  }

  /**
   * Add the {@link Properties} to an imeji object when it is released
   *
   * @param properties
   * @param user
   * @throws WorkflowException
   * @throws NotSupportedMethodException
   */
  protected void prepareRelease(Properties properties, User user)
      throws WorkflowException, NotSupportedMethodException {
    WORKFLOW_MANAGER.prepareRelease(properties);
  }

  /**
   * Add the {@link Properties} to an imeji object when it is withdrawn
   *
   * @param properties
   * @param comment
   * @throws WorkflowException
   * @throws NotSupportedMethodException
   * @throws UnprocessableError
   */
  protected void prepareWithdraw(Properties properties, String comment)
      throws WorkflowException, NotSupportedMethodException {
    if (comment != null && !"".equals(comment)) {
      properties.setDiscardComment(comment);
    }
    WORKFLOW_MANAGER.prepareWithdraw(properties);
  }



  /**
   * True if at least one {@link Item} is locked by another {@link User}
   *
   * @param uris
   * @param user
   * @return
   */
  protected boolean hasImageLocked(List<String> uris, User user) {
    for (final String uri : uris) {
      if (Locks.isLocked(uri.toString(), user.getEmail())) {
        return true;
      }
    }
    return false;
  }

  /**
   * Get the instance default instance
   *
   * @return
   */
  public License getDefaultLicense() {
    final ImejiLicenses lic = StringHelper.isNullOrEmptyTrim(Imeji.CONFIG.getDefaultLicense())
        ? ImejiLicenses.CC0 : ImejiLicenses.valueOf(Imeji.CONFIG.getDefaultLicense());
    final License license = new License();
    license.setName(lic.name());
    license.setLabel(lic.getLabel());
    license.setUrl(lic.getUrl());
    return license;
  }
}
