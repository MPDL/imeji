package de.mpg.imeji.logic.db.writer;

import java.io.IOException;
import java.net.URI;
import java.security.Security;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.mpg.imeji.exceptions.AuthenticationError;
import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.exceptions.NotAllowedError;
import de.mpg.imeji.exceptions.SearchIndexBulkFailureException;
import de.mpg.imeji.exceptions.SearchIndexFailureException;
import de.mpg.imeji.exceptions.UnprocessableError;
import de.mpg.imeji.exceptions.WorkflowException;
import de.mpg.imeji.logic.config.Imeji;
import de.mpg.imeji.logic.db.indexretry.RetryIndex;
import de.mpg.imeji.logic.db.indexretry.model.RetryBaseRequest;
import de.mpg.imeji.logic.db.indexretry.model.RetryDeleteFromIndexRequest;
import de.mpg.imeji.logic.db.indexretry.model.RetryIndexRequest;
import de.mpg.imeji.logic.db.indexretry.queue.RetryQueue;
import de.mpg.imeji.logic.model.CollectionImeji;
import de.mpg.imeji.logic.model.Item;
import de.mpg.imeji.logic.model.Properties;
import de.mpg.imeji.logic.model.Subscription;
import de.mpg.imeji.logic.model.User;
import de.mpg.imeji.logic.model.UserGroup;
import de.mpg.imeji.logic.model.aspects.AccessMember.ChangeMember;
import de.mpg.imeji.logic.search.Search.SearchObjectTypes;
import de.mpg.imeji.logic.search.SearchIndexer;
import de.mpg.imeji.logic.search.factory.SearchFactory;
import de.mpg.imeji.logic.search.factory.SearchFactory.SEARCH_IMPLEMENTATIONS;
import de.mpg.imeji.logic.security.authorization.Authorization;
import de.mpg.imeji.logic.security.authorization.util.SecurityUtil;
import de.mpg.imeji.logic.util.ObjectsHelper;
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

  private static final Logger LOGGER = LogManager.getLogger(WriterFacade.class);
  private final Writer writer;
  private final SearchIndexer indexer;
  private final WorkflowValidator workflowManager = new WorkflowValidator();
  private final ExecutorService executor = Executors.newCachedThreadPool();

  /**
   * Constructor without explicit model. Use when you want to write objects of multiple types within
   * one transaction
   */
  public WriterFacade() {
    this.writer = new JenaWriter(null);
    this.indexer = null;
  }

  /**
   * Constructor for one model
   */
  public WriterFacade(String modelURI) {
    this.writer = WriterFactory.create(modelURI);
    if (modelURI.equals(Imeji.imageModel)) {
      indexer = SearchFactory.create(SearchObjectTypes.ITEM, SEARCH_IMPLEMENTATIONS.ELASTIC).getIndexer();
    } else if (modelURI.equals(Imeji.collectionModel)) {
      indexer = SearchFactory.create(SearchObjectTypes.COLLECTION, SEARCH_IMPLEMENTATIONS.ELASTIC).getIndexer();
    } else if (modelURI.equals(Imeji.userModel)) {
      indexer = SearchFactory.create(SearchObjectTypes.USER, SEARCH_IMPLEMENTATIONS.ELASTIC).getIndexer();
    } else if (modelURI.equals(Imeji.contentModel)) {
      indexer = SearchFactory.create(SearchObjectTypes.CONTENT, SEARCH_IMPLEMENTATIONS.ELASTIC).getIndexer();
    } else {
      indexer = SearchFactory.create(SEARCH_IMPLEMENTATIONS.JENA).getIndexer();
    }
  }

  /**
   * Constructor to decouple model and searchtype. Needed for usergroup which has same model than
   * users.
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
   * @see de.mpg.imeji.logic.writer.Writer#create(java.util.List,
   * de.mpg.imeji.logic.vo.User)
   */
  public List<Object> create(List<Object> objects, User user) throws ImejiException {
    if (objects.isEmpty()) {
      return objects;
    }
    checkWorkflow(objects, "create");
    checkSecurity(objects, user, true);
    validate(objects, Validator.Method.CREATE);
    List<Object> createdObjects = writeAndIndex(new CreateTask(objects, user), new IndexTask());
    return createdObjects;

  }

  /*
   * (non-Javadoc)
   *
   * @see de.mpg.imeji.logic.writer.Writer#delete(java.util.List,
   * de.mpg.imeji.logic.vo.User)
   */
  public void delete(List<Object> objects, User user) throws ImejiException {
    if (objects.isEmpty()) {
      return;
    }
    checkWorkflow(objects, "delete");
    checkSecurity(objects, user, false);
    validate(objects, Validator.Method.DELETE);
    writeAndIndex(new DeleteTask(objects, user), new DeleteIndexTask());

  }

  /*
   * (non-Javadoc)
   *
   * @see de.mpg.imeji.logic.writer.Writer#update(java.util.List,
   * de.mpg.imeji.logic.vo.User), choose to check security
   */
  public List<Object> update(List<Object> objects, final User user, boolean doCheckSecurity) throws ImejiException {
    if (objects.isEmpty()) {
      return objects;
    }
    checkWorkflow(objects, "update");
    if (doCheckSecurity) {
      checkSecurity(objects, user, false);
    }
    validate(objects, Validator.Method.UPDATE);
    List<Object> updatedObjects = writeAndIndex(new UpdateTask(objects, user), new IndexTask());
    return updatedObjects;
  }

  /**
   * Do a full update without any validation
   *
   * @param imejiDataObjects
   * @param user
   * @throws ImejiException
   */
  public List<Object> updateWithoutValidation(List<Object> imejiDataObjects, User user) throws ImejiException {
    if (imejiDataObjects.isEmpty()) {
      return imejiDataObjects;
    }
    throwAuthorizationException(user != null, SecurityUtil.authorization().administrate(user, Imeji.PROPERTIES.getBaseURI()),
        "Only admin can use update wihout validation");
    List<Object> updatedObjects = writeAndIndex(new UpdateTask(imejiDataObjects, user), new IndexTask());
    return updatedObjects;
  }


  /**
   * Use this function to change the value (add/edit/delete) of a specific field of a data object in
   * database and search index without changing any other field of that object.
   * 
   * @param changeMember
   * @param user
   * @return
   * @throws ImejiException
   */
  public Object changeElement(ChangeMember changeMember, User user) throws ImejiException {

    if (changeMember.getImejiDataObject() == null) {
      return changeMember.getImejiDataObject();
    }

    List<Object> imejiDataObjectList = Arrays.asList(changeMember.getImejiDataObject());
    checkWorkflow(imejiDataObjectList, "update");
    checkSecurity(imejiDataObjectList, user, true);
    Object valueToSet = changeMember.getValue();
    validate(valueToSet, Validator.Method.UPDATE);

    List<Object> updatedObjects = writeAndIndex(new EditElementsTask(changeMember), new IndexTask());
    return updatedObjects.get(0);

  }

  /**
   * Use this function to change the value (add/edit/delete) of a specific field of list of data
   * objects in database and search index. All "change value of this field of this data object"
   * requests will be processed within a single transaction.
   * 
   * @param changeElements
   * @param user
   * @return
   * @throws ImejiException
   */
  public List<Object> editElements(List<ChangeMember> changeElements, User user) throws ImejiException {

    List<Object> imejiDataObjectList =
        changeElements.stream().map(dataObject -> dataObject.getImejiDataObject()).collect(Collectors.toList());
    checkWorkflow(imejiDataObjectList, "update");
    checkSecurity(imejiDataObjectList, user, true);
    for (ChangeMember changeMember : changeElements) {
      Object valueToSet = changeMember.getValue();
      validate(valueToSet, Validator.Method.UPDATE);
    }

    List<Object> dataObjectsChangedInStore = writeToDatabase(new EditElementsTask(changeElements));

    // copy latest object version of object in Jena to ElasticSearch:
    Map<Class<?>, List<Object>> typedObjectMap = ObjectsHelper.createTypedObjectMap(dataObjectsChangedInStore);
    Set<Class<?>> dataTypes = typedObjectMap.keySet();
    for (Class<?> dataType : dataTypes) {
      List<Object> objectsToIndex = typedObjectMap.get(dataType);
      SearchObjectTypes typeToIndex = SearchObjectTypes.getFromDataType(dataType);
      SearchIndexer myIndexer = SearchFactory.create(typeToIndex, SEARCH_IMPLEMENTATIONS.ELASTIC).getIndexer();
      IndexTask indexTask = new IndexTask();
      indexTask.setIndexer(myIndexer);
      indexInSearchIndexWithRetry(objectsToIndex, indexTask);
    }
    return dataObjectsChangedInStore;

  }

  /**
   * Write an object/document to search index. Throws exception if not successful.
   * 
   * @param objectToIndex
   * @param retry
   * @throws Exception
   */
  public void indexObject(Object objectToIndex) throws Exception {

    SearchObjectTypes typeToIndex = SearchObjectTypes.getFromDataType(objectToIndex.getClass());
    SearchIndexer myIndexer = SearchFactory.create(typeToIndex, SEARCH_IMPLEMENTATIONS.ELASTIC).getIndexer();
    IndexTask indexTask = new IndexTask();
    indexTask.setIndexer(myIndexer);
    indexInSearchIndex(Arrays.asList(objectToIndex), indexTask);

  }

  /**
   * Delete an object/document from search index. Throws exception if not successful.
   * 
   * @param objectToDeleteFromIndex
   * @throws Exception
   */
  public void deleteObjectFromIndex(Object objectToDeleteFromIndex) throws Exception {

    SearchObjectTypes typeToIndex = SearchObjectTypes.getFromDataType(objectToDeleteFromIndex.getClass());
    SearchIndexer myIndexer = SearchFactory.create(typeToIndex, SEARCH_IMPLEMENTATIONS.ELASTIC).getIndexer();
    DeleteIndexTask deleteTask = new DeleteIndexTask();
    deleteTask.setIndexer(myIndexer);
    indexInSearchIndex(Arrays.asList(objectToDeleteFromIndex), deleteTask);
  }


  /**
   * Write a list of objects first to database then to search index or delete an object first from
   * database and then from search index. Throws exception if writing to database fails.
   * 
   * @param databaseTask a create/delete/update task for objects in database
   * @param indexTask an index or delete task for indexing objects or deleting objects from search
   *        index
   * @return list of written/deleted objects
   * @throws ImejiException
   */
  private List<Object> writeAndIndex(Callable<List<Object>> databaseTask, SearchIndexTask indexTask) throws ImejiException {

    // 1. Write to database
    List<Object> objectsChangedInDatabase = writeToDatabase(databaseTask);

    // 2. If writing to database was successful and we got a result from database
    // (latest version of the written objects), index the objects (copy them) in search index 	  
    indexInSearchIndexWithRetry(objectsChangedInDatabase, indexTask);

    // finally return written objects
    return objectsChangedInDatabase;
  }

  /**
   * Write a list of objects to database or delete a list of objects from database.
   * 
   * @param databaseTask
   * @return list of objects currently (after writing) in database
   * @throws ImejiException
   */
  private List<Object> writeToDatabase(Callable<List<Object>> databaseTask) throws ImejiException {

    List<Object> objectsInDatabase = new ArrayList<>(0);

    // 1. Write to database
    try {
      objectsInDatabase = executor.submit(databaseTask).get();
    } catch (ExecutionException | InterruptedException | CancellationException execExept) {
      if (execExept.getCause() instanceof ImejiException) {
        throw (ImejiException) execExept.getCause();
      } else {
        throw new ImejiException(execExept.getMessage());
      }
    }

    return objectsInDatabase;
  }


  /**
   * Access search index and index or delete documents. In case of failure, save documents in a
   * retry queue in order to retry indexing/deleting later on.
   * 
   * @param objectsToIndex
   * @param indexTask either IndexTask oder DeleteIndexTask
   */
  private void indexInSearchIndexWithRetry(List<Object> objectsToIndex, SearchIndexTask indexTask) {

    // 2. If writing to database was successful and we got a result from database
    // (latest version of the written objects), index the objects in (copy them to) search index 	  
    if (!objectsToIndex.isEmpty()) {
      indexTask.setObjects(objectsToIndex);

      try {
        executor.submit(indexTask).get();
      } catch (ExecutionException executionException) {
        Throwable taskException = executionException.getCause();
        if (taskException instanceof IOException) {
          // SearchIndex is down, connection timed out
          List<RetryBaseRequest> retryIndexList = indexTask.getRetryRequests();
          RetryQueue.getInstance().addRetryIndexRequests(retryIndexList);
        } else if (taskException instanceof SearchIndexFailureException) {
          // single operation failed
          SearchIndexFailureException failedException = (SearchIndexFailureException) taskException;
          RetryBaseRequest retryRequest = failedException.getRetryRequest(objectsToIndex);
          RetryQueue.getInstance().addRetryIndexRequests(Arrays.asList(retryRequest));
        } else if (taskException instanceof SearchIndexBulkFailureException) {
          // in a bulk request one or more operations failed
          SearchIndexBulkFailureException failedException = (SearchIndexBulkFailureException) taskException;
          List<RetryBaseRequest> retryRequests = failedException.getRetryRequests(objectsToIndex);
          RetryQueue.getInstance().addRetryIndexRequests(retryRequests);
        } else if (taskException instanceof UnprocessableError) {
          // there were problems transforming an object to a json representation, development error
          LOGGER.error("Could not parse data object to json representation", taskException);
        }

      } catch (InterruptedException | CancellationException interruptedOrCanceled) {
        // thread was interrupted before executing: retry all objects
        List<RetryBaseRequest> retryIndexList = indexTask.getRetryRequests();
        RetryQueue.getInstance().addRetryIndexRequests(retryIndexList);

      }

    }
  }


  /**
   * Index a list of objects in search index. In case of failure exception is thrown.
   * 
   * @param objectsToIndex
   * @param indexTask
   * @throws Exception
   */
  private void indexInSearchIndex(List<Object> objectsToIndex, SearchIndexTask indexTask) throws Exception {
    if (!objectsToIndex.isEmpty()) {
      indexTask.setObjects(objectsToIndex);
      executor.submit(indexTask).get();
    }

  }


  // /*
  // * (non-Javadoc)
  // *
  // * @see de.mpg.imeji.logic.writer.Writer#updateLazy(java.util.List,
  // de.mpg.imeji.logic.vo.User)
  // */
  // public void updateLazy(List<Object> objects, User user) throws ImejiException
  // {
  // if (objects.isEmpty()) {
  // return;
  // }
  // checkWorkflow(objects, "update");
  // checkSecurity(objects, user, false);
  // validate(objects, Validator.Method.UPDATE);
  // writer.updateLazy(objects, user);
  // indexer.indexBatch(objects);
  // }
  /**
   * Validate a list of same-type data objects
   * 
   * @param list
   * @param method
   * @throws UnprocessableError
   */
  @SuppressWarnings("unchecked")
  private void validate(List<Object> list, Validator.Method method) throws UnprocessableError {
    if (list.isEmpty()) {
      return;
    }
    final Validator<Object> validator = (Validator<Object>) ValidatorFactory.newValidator(list.get(0), method);
    for (final Object o : list) {
      validator.validate(o, method);
    }
  }


  private void validate(Object object, Validator.Method method) throws UnprocessableError {

    final Validator<Object> validator = (Validator<Object>) ValidatorFactory.newValidator(object, method);
    validator.validate(object, method);
  }

  private void checkWorkflow(List<Object> objects, String operation) throws WorkflowException {

    for (final Object o : objects) {
      if (o instanceof Properties) {
        switch (operation) {
          case "create":
            workflowManager.isCreateAllowed((Properties) o);
            break;
          case "delete":
            workflowManager.isDeleteAllowed((Properties) o);
            break;
          case "update":
            workflowManager.isUpdateAllowed((Properties) o);
            break;
        }
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
  private void checkSecurity(List<Object> list, final User user, boolean create) throws NotAllowedError, AuthenticationError {

    String message = user != null ? user.getEmail() : "";
    for (final Object o : list) {
      message += " not allowed to " + (create ? "create " : "edit ") + extractID(o);
      if (create) {
        throwAuthorizationException(user != null, SecurityUtil.authorization().create(user, o), message);
      } else {
        throwAuthorizationException(user != null, SecurityUtil.authorization().update(user, o), message);
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
  private void throwAuthorizationException(boolean loggedIn, boolean allowed, String message) throws NotAllowedError, AuthenticationError {
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
  private class CreateTask implements Callable<List<Object>> {
    private List<Object> objects;
    private final User user;

    public CreateTask(List<Object> objects, User user) {
      this.objects = objects;
      this.user = user;
    }

    @Override
    public List<Object> call() throws ImejiException {
      List<Object> results = writer.create(objects, user);
      return results;
    }
  }

  /**
   * Task to update objects in database
   * 
   * @author saquet
   *
   */
  private class UpdateTask implements Callable<List<Object>> {
    private final List<Object> objects;
    private final User user;

    public UpdateTask(List<Object> objects, User user) {
      this.objects = objects;
      this.user = user;
    }

    @Override
    public List<Object> call() throws ImejiException {
      List<Object> results = writer.update(objects, user);
      return results;
    }
  }

  /**
   * Task to delete objects in database.
   * 
   * @author saquet
   *
   */
  private class DeleteTask implements Callable<List<Object>> {
    private final List<Object> objects;
    private final User user;

    public DeleteTask(List<Object> objects, User user) {
      this.objects = objects;
      this.user = user;
    }

    @Override
    public List<Object> call() throws ImejiException {
      writer.delete(objects, user);
      return objects;
    }
  }

  /**
   * Base class for all search index tasks.
   * 
   * @author breddin
   *
   */
  private abstract class SearchIndexTask implements Callable<Integer>, RetryIndex {

    protected List<Object> objects;
    protected SearchIndexer taskIndexer = indexer; // default is indexer of WriterFacade

    public void setObjects(List<Object> objects) {
      this.objects = objects;
    }

    public void setIndexer(SearchIndexer myIndexer) {
      this.taskIndexer = myIndexer;
    }
  }


  /**
   * Task to index objects.
   * 
   * @author saquet
   *
   */
  private class IndexTask extends SearchIndexTask {

    @Override
    public Integer call() throws Exception {
      taskIndexer.indexBatch(this.objects);
      return 1;
    }

    @Override
    public List<RetryBaseRequest> getRetryRequests() {
      return RetryIndexRequest.getRetryIndexRequests(this.objects);
    }
  }

  /**
   * Task to delete objects from index.
   * 
   * @author saquet
   *
   */
  private class DeleteIndexTask extends SearchIndexTask {

    @Override
    public Integer call() throws Exception {

      taskIndexer.deleteBatch(this.objects);
      return 1;
    }

    @Override
    public List<RetryBaseRequest> getRetryRequests() {
      return RetryDeleteFromIndexRequest.getRetryDeleteFromIndexRequests(this.objects);
    }

  }


  /**
   * Task to write a list of changes/updates to a list of objects in database.
   * 
   * @author breddin
   *
   */
  private class EditElementsTask implements Callable<List<Object>> {

    private final List<ChangeMember> changeElements;

    public EditElementsTask(ChangeMember changeMember) {
      this.changeElements = new ArrayList<ChangeMember>(1);
      this.changeElements.add(changeMember);
    }


    public EditElementsTask(List<ChangeMember> changeElements) {
      this.changeElements = changeElements;
    }

    @Override
    public List<Object> call() throws Exception {
      List<Object> resultObjects = writer.editElements(changeElements);
      return resultObjects;
    }
  }


}
