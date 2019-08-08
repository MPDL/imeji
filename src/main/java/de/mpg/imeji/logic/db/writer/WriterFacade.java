package de.mpg.imeji.logic.db.writer;

import java.net.URI;
import java.security.Security;
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
    // writer.create(objects, user);
    // indexer.indexBatch(objects);
    try {
      List<Object> createdObjectsInJena = executor.submit(new CreateTask(objects, user)).get();
      executor.submit(new IndexTask(createdObjectsInJena)).get();
      return createdObjectsInJena;
    } catch (ExecutionException | InterruptedException | CancellationException execExept) {
      if (execExept.getCause() instanceof ImejiException) {
        throw (ImejiException) execExept.getCause();
      } else {
        throw new ImejiException(execExept.getMessage());
      }
    }
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
    try {
      executor.submit(new DeleteIndexTask(objects)).get();
      executor.submit(new DeleteTask(objects, user)).get();
    } catch (ExecutionException | InterruptedException | CancellationException execExept) {
      if (execExept.getCause() instanceof ImejiException) {
        throw (ImejiException) execExept.getCause();
      } else {
        throw new ImejiException(execExept.getMessage());
      }
    }
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
    try {
      List<Object> updatedObjectsInJena = executor.submit(new UpdateTask(objects, user)).get();
      executor.submit(new IndexTask(updatedObjectsInJena)).get();
      return updatedObjectsInJena;
    } catch (ExecutionException | InterruptedException | CancellationException execExept) {
      if (execExept.getCause() instanceof ImejiException) {
        throw (ImejiException) execExept.getCause();
      } else {
        throw new ImejiException(execExept.getMessage());
      }
    }
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
    try {
      List<Object> objectsInJena = executor.submit(new UpdateTask(imejiDataObjects, user)).get();
      executor.submit(new IndexTask(objectsInJena)).get();
      return objectsInJena;
    } catch (ExecutionException | InterruptedException | CancellationException execExept) {
      if (execExept.getCause() instanceof ImejiException) {
        throw (ImejiException) execExept.getCause();
      } else {
        throw new ImejiException(execExept.getMessage());
      }
    }


  }


  /**
   * Use this function to change the value (add/edit/delete) of a specific field of a data object in
   * database and index without changing any other field of that object.
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

    try {
      Object dataObjectFromStore = executor.submit(new ChangeMemberTask(changeMember)).get();
      executor.submit(new IndexTask(Arrays.asList(dataObjectFromStore))).get();
      return dataObjectFromStore;
    } catch (ExecutionException | InterruptedException | CancellationException execExept) {
      if (execExept.getCause() instanceof ImejiException) {
        throw (ImejiException) execExept.getCause();
      } else {
        throw new ImejiException(execExept.getMessage());
      }
    }
  }

  /**
   * Use this function to change the value (add/edit/delete) of a specific field of list of data
   * objects in database and index. All "change value of this field of this data object" requests
   * will be processed within a single transaction.
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


    try {
      // write list of changes to Jena and get latest object version:
      List<Object> dataObjectsFromStore = (List<Object>) executor.submit(new EditElementsTask(changeElements)).get();
      // copy latest object version of object in Jena to ElasticSearch:
      Map<Class<?>, List<Object>> typedObjectMap = ObjectsHelper.createTypedObjectMap(dataObjectsFromStore);
      Set<Class<?>> dataTypes = typedObjectMap.keySet();
      for (Class<?> dataType : dataTypes) {
        List<Object> objectsToIndex = typedObjectMap.get(dataType);
        SearchObjectTypes typeToIndex = SearchObjectTypes.getFromDataType(dataType);
        SearchIndexer myIndexer = SearchFactory.create(typeToIndex, SEARCH_IMPLEMENTATIONS.ELASTIC).getIndexer();
        myIndexer.updateIndexBatch(objectsToIndex);
      }
      return dataObjectsFromStore;
    } catch (ExecutionException | InterruptedException | CancellationException execExept) {
      if (execExept.getCause() instanceof ImejiException) {
        throw (ImejiException) execExept.getCause();
      } else {
        throw new ImejiException(execExept.getMessage());
      }
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
   * Task to update objects
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
    public Integer call() throws ImejiException {
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
  private class IndexTask implements Callable<Object> {
    private final List<Object> objects;

    public IndexTask(List<Object> objects) {
      this.objects = objects;
    }

    @Override
    public Object call() throws ImejiException {
      indexer.indexBatch(objects);
      return null;
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
    public Integer call() throws ImejiException {
      indexer.deleteBatch(objects);
      return 1;
    }
  }


  private class ChangeMemberTask implements Callable<Object> {

    private ChangeMember changeMember;

    public ChangeMemberTask(ChangeMember changeMember) {
      this.changeMember = changeMember;
    }


    @Override
    public Object call() throws Exception {
      Object resultObject = writer.changeElement(this.changeMember);
      return resultObject;
    }

  }

  private class EditElementsTask implements Callable<Object> {

    private final List<ChangeMember> changeElements;

    public EditElementsTask(List<ChangeMember> changeElements) {
      this.changeElements = changeElements;
    }

    @Override
    public Object call() throws Exception {
      List<Object> resultObjects = writer.editElements(changeElements);
      return resultObjects;
    }
  }


}
