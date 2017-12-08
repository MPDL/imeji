package de.mpg.imeji.logic.db.writer;

import java.net.URI;
import java.security.Security;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import de.mpg.imeji.exceptions.AuthenticationError;
import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.exceptions.NotAllowedError;
import de.mpg.imeji.exceptions.UnprocessableError;
import de.mpg.imeji.exceptions.WorkflowException;
import de.mpg.imeji.logic.config.Imeji;
import de.mpg.imeji.logic.model.CollectionImeji;
import de.mpg.imeji.logic.model.Item;
import de.mpg.imeji.logic.model.Properties;
import de.mpg.imeji.logic.model.Subscription;
import de.mpg.imeji.logic.model.User;
import de.mpg.imeji.logic.model.UserGroup;
import de.mpg.imeji.logic.search.Search.SearchObjectTypes;
import de.mpg.imeji.logic.search.SearchIndexer;
import de.mpg.imeji.logic.search.factory.SearchFactory;
import de.mpg.imeji.logic.search.factory.SearchFactory.SEARCH_IMPLEMENTATIONS;
import de.mpg.imeji.logic.security.authorization.Authorization;
import de.mpg.imeji.logic.security.authorization.util.SecurityUtil;
import de.mpg.imeji.logic.validation.ValidatorFactory;
import de.mpg.imeji.logic.validation.impl.Validator;
import de.mpg.imeji.logic.workflow.WorkflowValidator;

/**
 * Facade implementing Writer {@link Authorization}
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public class WriterFacade {
  private final Writer writer;
  private final SearchIndexer indexer;
  private final WorkflowValidator workflowManager = new WorkflowValidator();
  private final ExecutorService executor = Executors.newCachedThreadPool();

  /**
   * Constructor for one model
   */
  public WriterFacade(String modelURI) {
    this.writer = WriterFactory.create(modelURI);
    if (modelURI.equals(Imeji.imageModel)) {
      indexer =
          SearchFactory.create(SearchObjectTypes.ITEM, SEARCH_IMPLEMENTATIONS.ELASTIC).getIndexer();
    } else if (modelURI.equals(Imeji.collectionModel)) {
      indexer = SearchFactory.create(SearchObjectTypes.COLLECTION, SEARCH_IMPLEMENTATIONS.ELASTIC)
          .getIndexer();

    } else if (modelURI.equals(Imeji.userModel)) {
      indexer =
          SearchFactory.create(SearchObjectTypes.USER, SEARCH_IMPLEMENTATIONS.ELASTIC).getIndexer();
    } else if (modelURI.equals(Imeji.contentModel)) {
      indexer = SearchFactory.create(SearchObjectTypes.CONTENT, SEARCH_IMPLEMENTATIONS.ELASTIC)
          .getIndexer();
    } else {
      indexer = SearchFactory.create(SEARCH_IMPLEMENTATIONS.JENA).getIndexer();
    }
  }

  /**
   * Constructor to decouple model and searchtype. Needed for usergroup which have same mode than
   * users
   *
   * @param modelURI
   * @param type
   */
  public WriterFacade(String modelURI, SearchObjectTypes type) {
    this.writer = WriterFactory.create(modelURI);
    indexer = SearchFactory.create(type, SEARCH_IMPLEMENTATIONS.ELASTIC).getIndexer();
  }

  /*
   * (non-Javadoc)
   *
   * @see de.mpg.imeji.logic.writer.Writer#create(java.util.List, de.mpg.imeji.logic.vo.User)
   */
  public void create(List<Object> objects, User user) throws ImejiException {
    if (objects.isEmpty()) {
      return;
    }
    checkSecurity(objects, user, true);
    validate(objects, Validator.Method.CREATE);
    // writer.create(objects, user);
    // indexer.indexBatch(objects);
    try {
      Future<Integer> createTask = executor.submit(new CreateTask(objects, user));
      Future<Integer> indexTask = executor.submit(new IndexTask(objects));
      createTask.get();
      indexTask.get();
    } catch (Exception e) {
      new ImejiException("Error updating objects", e);
    }

  }

  /*
   * (non-Javadoc)
   *
   * @see de.mpg.imeji.logic.writer.Writer#delete(java.util.List, de.mpg.imeji.logic.vo.User)
   */
  public void delete(List<Object> objects, User user) throws ImejiException {
    if (objects.isEmpty()) {
      return;
    }
    checkWorkflowForDelete(objects);
    checkSecurity(objects, user, false);
    validate(objects, Validator.Method.DELETE);
    // writer.delete(objects, user);
    // indexer.deleteBatch(objects);
    try {
      Future<Integer> deleteTask = executor.submit(new DeleteTask(objects, user));
      Future<Integer> deleteIndexTask = executor.submit(new DeleteIndexTask(objects));
      deleteTask.get();
      deleteIndexTask.get();
    } catch (Exception e) {
      new ImejiException("Error updating objects", e);
    }
  }

  /*
   * (non-Javadoc)
   *
   * @see de.mpg.imeji.logic.writer.Writer#update(java.util.List, de.mpg.imeji.logic.vo.User),
   * choose to check security
   */
  public void update(List<Object> objects, User user, boolean doCheckSecurity)
      throws ImejiException {
    if (objects.isEmpty()) {
      return;
    }
    if (doCheckSecurity) {
      checkSecurity(objects, user, false);
    }
    validate(objects, Validator.Method.UPDATE);
    // writer.update(objects, user);
    // indexer.updateIndexBatch(objects);
    try {
      Future<Integer> updateTask = executor.submit(new UpdateTask(objects, user));
      Future<Integer> indexTask = executor.submit(new IndexTask(objects));
      updateTask.get();
      indexTask.get();
    } catch (Exception e) {
      new ImejiException("Error updating objects", e);
    }

  }

  /**
   * Do a full update without any validation
   *
   * @param objects
   * @param user
   * @throws ImejiException
   */
  public void updateWithoutValidation(List<Object> objects, User user) throws ImejiException {
    if (objects.isEmpty()) {
      return;
    }
    throwAuthorizationException(user != null,
        SecurityUtil.authorization().administrate(user, Imeji.PROPERTIES.getBaseURI()),
        "Only admin ca use update wihout validation");
    try {
      Future<Integer> updateTask = executor.submit(new UpdateTask(objects, user));
      Future<Integer> indexTask = executor.submit(new IndexTask(objects));
      updateTask.get();
      indexTask.get();
    } catch (Exception e) {
      new ImejiException("Error updating objects", e);
    }

  }

  /*
   * (non-Javadoc)
   *
   * @see de.mpg.imeji.logic.writer.Writer#updateLazy(java.util.List, de.mpg.imeji.logic.vo.User)
   */
  public void updateLazy(List<Object> objects, User user) throws ImejiException {
    if (objects.isEmpty()) {
      return;
    }
    checkSecurity(objects, user, false);
    validate(objects, Validator.Method.UPDATE);
    writer.updateLazy(objects, user);
    indexer.indexBatch(objects);
  }

  @SuppressWarnings("unchecked")
  private void validate(List<Object> list, Validator.Method method) throws UnprocessableError {
    if (list.isEmpty()) {
      return;
    }
    final Validator<Object> validator =
        (Validator<Object>) ValidatorFactory.newValidator(list.get(0), method);
    for (final Object o : list) {
      validator.validate(o, method);
    }
  }

  private void checkWorkflowForDelete(List<Object> objects) throws WorkflowException {
    for (final Object o : objects) {
      if (o instanceof Properties) {
        workflowManager.isDeleteAllowed((Properties) o);
      }
    }
  }

  /**
   * Check {@link Security} for WRITE operations
   *
   * @param list
   * @param user
   * @param opType
   * @throws NotAllowedError
   * @throws AuthenticationError
   */
  private void checkSecurity(List<Object> list, User user, boolean create)
      throws NotAllowedError, AuthenticationError {
    String message = user != null ? user.getEmail() : "";
    for (final Object o : list) {
      message += " not allowed to " + (create ? "create " : "edit ") + extractID(o);
      if (create) {
        throwAuthorizationException(user != null, SecurityUtil.authorization().create(user, o),
            message);
      } else {
        throwAuthorizationException(user != null, SecurityUtil.authorization().update(user, o),
            message);
      }
    }
  }

  /**
   * Extract the id (as {@link URI}) of an imeji {@link Object},
   *
   * @param o
   * @return
   */
  public static URI extractID(Object o) {
    if (o instanceof Item) {
      return ((Item) o).getId();
    } else if (o instanceof CollectionImeji) {
      return ((CollectionImeji) o).getId();
    } else if (o instanceof User) {
      return ((User) o).getId();
    } else if (o instanceof UserGroup) {
      return ((UserGroup) o).getId();
    } else if (o instanceof Subscription) {
      return URI.create(((Subscription) o).getUserId());
    }
    return null;
  }

  /**
   * If false, throw a {@link NotAllowedError}
   *
   * @param b
   * @param message
   * @throws NotAllowedError
   * @throws AuthenticationError
   */
  private void throwAuthorizationException(boolean loggedIn, boolean allowed, String message)
      throws NotAllowedError, AuthenticationError {
    if (!allowed) {
      if (!loggedIn) {
        throw new AuthenticationError(AuthenticationError.USER_MUST_BE_LOGGED_IN);
      } else {
        throw new NotAllowedError(message);
      }

    }
  }

  /**
   * Transform a single {@link Object} into a {@link List} with one {@link Object}
   *
   * @param o
   * @return
   * @deprecated
   */
  public static List<Object> toList(Object o) {
    return Arrays.asList(o);
  }

  /**
   * Task to update objects
   * 
   * @author saquet
   *
   */
  private class CreateTask implements Callable<Integer> {
    private final List<Object> objects;
    private final User user;

    public CreateTask(List<Object> objects, User user) {
      this.objects = objects;
      this.user = user;
    }

    @Override
    public Integer call() throws Exception {
      writer.create(objects, user);
      return 1;
    }
  }

  /**
   * Task to update objects
   * 
   * @author saquet
   *
   */
  private class UpdateTask implements Callable<Integer> {
    private final List<Object> objects;
    private final User user;

    public UpdateTask(List<Object> objects, User user) {
      this.objects = objects;
      this.user = user;
    }

    @Override
    public Integer call() throws Exception {
      writer.update(objects, user);
      return 1;
    }
  }

  /**
   * Task to delete objects
   * 
   * @author saquet
   *
   */
  private class DeleteTask implements Callable<Integer> {
    private final List<Object> objects;
    private final User user;

    public DeleteTask(List<Object> objects, User user) {
      this.objects = objects;
      this.user = user;
    }

    @Override
    public Integer call() throws Exception {
      writer.delete(objects, user);
      return 1;
    }
  }

  /**
   * Task to index objects
   * 
   * @author saquet
   *
   */
  private class IndexTask implements Callable<Integer> {
    private final List<Object> objects;

    public IndexTask(List<Object> objects) {
      this.objects = objects;
    }

    @Override
    public Integer call() throws Exception {
      indexer.indexBatch(objects);
      return 1;
    }
  }

  /**
   * Task to index objects
   * 
   * @author saquet
   *
   */
  private class DeleteIndexTask implements Callable<Integer> {
    private final List<Object> objects;

    public DeleteIndexTask(List<Object> objects) {
      this.objects = objects;
    }

    @Override
    public Integer call() throws Exception {
      indexer.deleteBatch(objects);
      return 1;
    }
  }
}
