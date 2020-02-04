package de.mpg.imeji.j2j.transaction;


import java.net.URI;
import java.security.Security;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.NotImplementedException;
import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.Model;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.mpg.imeji.exceptions.AuthenticationError;
import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.exceptions.NotAllowedError;
import de.mpg.imeji.exceptions.NotFoundException;
import de.mpg.imeji.exceptions.WorkflowException;
import de.mpg.imeji.j2j.authorization.JenaAuthorization;
import de.mpg.imeji.j2j.controler.ResourceController;
import de.mpg.imeji.j2j.helper.J2JHelper;
import de.mpg.imeji.j2j.queries.Queries;
import de.mpg.imeji.logic.config.Imeji;
import de.mpg.imeji.logic.db.writer.WriterFacade;
import de.mpg.imeji.logic.init.ImejiInitializer;
import de.mpg.imeji.logic.model.Properties;
import de.mpg.imeji.logic.model.User;
import de.mpg.imeji.logic.model.UserGroup;
import de.mpg.imeji.logic.model.aspects.CloneURI;
import de.mpg.imeji.logic.search.jenasearch.JenaCustomQueries;
import de.mpg.imeji.logic.security.authorization.Authorization;
import de.mpg.imeji.logic.workflow.WorkflowValidator;


/**
 * Transaction for Jena operations with security check. Checks if a user is allowed to access an
 * object for a given operation and only proceeds if the security check was successful.
 * 
 * @author breddin
 *
 */
public abstract class SecureTransaction extends Transaction {


  private static final Logger LOGGER = LogManager.getLogger(SecureTransaction.class);
  private User issuingUser;
  private List<ObjectOperation> writeOperations;
  private List<Object> readObjects;


  /**
   * Constructor
   * 
   * @param modelURI Jena model to use for operation, i.e. Item, Collection, Content etc.
   * @param objectsToCheck List of objects that shall be manipulated
   * @param operations List of operations for above objects, i.e. one operation per object
   * @param issuingUser The user who wishes to execute the operation(s)
   */
  public SecureTransaction(String modelURI, List<Object> objectsToCheck, List<OperationType> operations, User issuingUser) {
    super(modelURI);
    this.issuingUser = issuingUser;
    setReadAndWriteOperations(objectsToCheck, operations);
  }


  public SecureTransaction(String modelURI, List<Object> objectsToCheck, OperationType operation, User issuingUser) {
    super(modelURI);
    this.issuingUser = issuingUser;
    setReadAndWriteOperations(objectsToCheck, operation);
  }

  /**
   * Do the {@link Transaction} over a {@link Dataset}.
   *
   * @param dataset
   */
  @Override
  public void start(Dataset dataset) {
    try {
      dataset.begin(getLockType());
      executeSecure(dataset);
      dataset.commit();
    } catch (final ImejiException exception) {
      dataset.abort();
      LOGGER.debug("Imeji Exception thrown during Jena access " + exception.getMessage());
      exceptionWasThrown = true;
      transactionException = exception;
    } catch (final Exception e) {
      dataset.abort();
      LOGGER.debug("Other Exception thrown during Jena access " + e.getMessage());
      exceptionWasThrown = true;
      transactionException = new ImejiException(e.getMessage(), e);
    } finally {
      dataset.end();
    }
  }


  /**
   * 
   * @param dataset
   * @throws ImejiException
   */
  private void executeSecure(Dataset dataset) throws ImejiException {


    // (1) issuingUser == null if user is not logged in
    // (2) Imeji.adminUser is a static user object that is constructed upon server start
    // and represents general admin access rights. The constant is used in system operations
    // that involve no actual users. This user object does not exist in database.
    if (this.issuingUser != null && this.issuingUser != Imeji.adminUser) {
      // (1) check access rights:
      // (1a) load user object from database (if not Imeji.adminUser)    	
      String userModelURI = ImejiInitializer.getModelName(User.class);
      Model userModel = dataset.getNamedModel(userModelURI);
      final ResourceController userResourceController = new ResourceController(userModel, false);

      Object emptyUserObject = this.issuingUser.cloneURI();
      Object userInDatabase = userResourceController.read(emptyUserObject);
      if (userInDatabase == null) {
        throw new AuthenticationError(AuthenticationError.USER_MUST_BE_LOGGED_IN);
      }
      this.issuingUser = (User) userInDatabase;
      loadUsersUserGroups(userResourceController, dataset, userModelURI);
    }

    // (1b) check access rights of user
    checkSecurityForWriteOperations(dataset);
    // (2) execute operations (to be implemented by extending classes)
    execute(dataset);
    // (3) check security for read operations
    checkSecurityForReadOperations(dataset);

  }

  /**
   * For data objects that have a status (i.e. Item or CollectionImeji) check whether the status
   * allows proceeding with a create, update or delete operation.
   * 
   * @param object
   * @throws NotFoundException
   * @throws WorkflowException
   */
  protected void checkObjectStatus(ResourceController resourceController, Object object, OperationType operation)
      throws NotFoundException, WorkflowException {

    if (object instanceof Properties) {

      WorkflowValidator workflowManager = new WorkflowValidator();

      // create: check client object
      if (operation == OperationType.CREATE) {
        workflowManager.isCreateAllowed((Properties) object);
      }
      // update, delete: check database object (and not client object)
      else if (operation == OperationType.UPDATE || operation == OperationType.DELETE) {
        Object databaseObject = getCorrespondingObjectInDatabase(object, resourceController);
        switch (operation) {
          case DELETE:
            workflowManager.isDeleteAllowed((Properties) databaseObject);
            break;
          case UPDATE:
            workflowManager.isUpdateAllowed((Properties) databaseObject);
            break;
          default:
            // error
        }
      }
    }
  }

  /**
   * 
   * @param resourceController
   * @param dataset
   * @param userModelName
   * @param user
   * @throws NotFoundException
   */
  private void loadUsersUserGroups(ResourceController resourceController, Dataset dataset, String userModelName) throws NotFoundException {

    String getUserGroupsOfUserQuery = JenaCustomQueries.selectUserGroupOfUser(this.issuingUser);
    List<String> groupURIs = Queries.executeSPARQLQueryAndGetResults(getUserGroupsOfUserQuery, dataset, userModelName);
    if (groupURIs.size() > 0) {
      List<UserGroup> userGroupsWithUserInThem = new ArrayList<UserGroup>(groupURIs.size());
      for (String groupURI : groupURIs) {
        UserGroup groupToRead = new UserGroup();
        groupToRead.setId(URI.create(groupURI));
        Object readGroup = resourceController.read(groupToRead);
        if (readGroup instanceof UserGroup) {
          groupToRead = (UserGroup) readGroup;
          userGroupsWithUserInThem.add(groupToRead);
        }
      }
      this.issuingUser.setGroups(userGroupsWithUserInThem);
    }

  }

  /**
   * Given a data object that has been manipulated by a client, read the corresponding data object
   * from database. Useful in order to determine if database object has changed since it was last
   * read by the client.
   * 
   * @param clientImejiDataObject
   * @param resourceController
   * @return
   * @throws NotFoundException
   */
  private Object getCorrespondingObjectInDatabase(Object clientImejiDataObject, ResourceController resourceController)
      throws NotFoundException {

    if (clientImejiDataObject instanceof CloneURI) {
      Object currentObjectInJena = resourceController.read(((CloneURI) clientImejiDataObject).cloneURI());
      return currentObjectInJena;
    } else {
      throw new NotImplementedException("Could not process update request, interface CloneURI not implemented but should be in class "
          + clientImejiDataObject.getClass());
    }
  }

  /**
   * Check {@link Security} for write operations (NOOPERATION, EDIT, DELETE, CREATE)
   * 
   * @throws NotAllowedError
   * @throws AuthenticationError
   */
  private void checkSecurityForWriteOperations(Dataset dataset) throws NotAllowedError, AuthenticationError {

    Authorization authorization = new JenaAuthorization(dataset);
    if (this.writeOperations != null) {

      String message = "";
      if (this.issuingUser != null) {
        message = this.issuingUser.getEmail() + " ";
      }
      for (final ObjectOperation writeOperation : this.writeOperations) {

        URI objectId = WriterFacade.extractID(writeOperation.dataObject);
        if (writeOperation.operationToPerform == OperationType.NOOPERATION) {
          // throw error
          throw new NotAllowedError(message + "not allowed to perform operation on " + objectId);
        } else if (writeOperation.operationToPerform == OperationType.CREATE) {
          String usermessage = message + "not allowed to create " + objectId;
          boolean authorized = authorization.create(this.issuingUser, writeOperation.dataObject);
          checkAndThrowException(authorized, usermessage);
        }
        // edit, delete
        else {
          String usermessage = message + "not allowed to edit or delete " + objectId;
          boolean authorized = authorization.update(this.issuingUser, writeOperation.dataObject);
          checkAndThrowException(authorized, usermessage);
        }
      }
    }
  }


  /**
   * Checks if read operation on object is allowed
   * 
   * @throws NotAllowedError
   * @throws ImejiException
   */
  private void checkSecurityForReadOperations(Dataset dataset) throws NotAllowedError {

    if (this.readObjects != null) {

      Authorization authorization = new JenaAuthorization(dataset);
      for (Object dataObject : this.readObjects) {
        if (!authorization.read(this.issuingUser, dataObject)) {
          final String id = J2JHelper.getId(dataObject).toString();
          String message = "Not logged in";
          if (this.issuingUser != null) {
            message = this.issuingUser.getEmail() + " not allowed to read " + id;
          }
          throw new NotAllowedError(message);
        }
      }
    }
  }

  /**
   * If false, throw a {@link NotAllowedError}
   *
   * @param b
   * @param message
   * @throws NotAllowedError
   * @throws AuthenticationError
   */
  private void checkAndThrowException(boolean allowed, String message) throws NotAllowedError, AuthenticationError {
    if (!allowed) {
      throw new NotAllowedError(message);
    }
  }


  /**
   * Create internal representations of objects and operations
   * 
   * @param dataObject list of data objects
   * @param operations list of operations
   */
  private void setReadAndWriteOperations(List<Object> dataObjects, List<OperationType> operations) {

    this.readObjects = new ArrayList<Object>(dataObjects.size());
    this.writeOperations = new ArrayList<ObjectOperation>(dataObjects.size());

    int i = 0;
    for (Object dataObject : dataObjects) {
      OperationType operation = OperationType.NOOPERATION;
      if (i < operations.size()) {
        operation = operations.get(i);
      }
      if (operation == OperationType.READ) {
        this.readObjects.add(dataObject);
      } else {
        this.writeOperations.add(new ObjectOperation(dataObject, operation));
      }
      i = i + 1;
    }

  }

  /**
   * Create internal representations of objects and operations
   * 
   * @param dataObjects
   * @param operation
   */
  private void setReadAndWriteOperations(List<Object> dataObjects, OperationType operation) {
    if (operation == OperationType.READ) {
      this.readObjects = dataObjects;
    } else {
      this.writeOperations = new ArrayList<ObjectOperation>(dataObjects.size());
      for (Object dataObject : dataObjects) {
        this.writeOperations.add(new ObjectOperation(dataObject, operation));
      }
    }
  }



  private class ObjectOperation {

    public Object dataObject;
    public OperationType operationToPerform;

    public ObjectOperation(Object dataObject, OperationType operationType) {
      this.dataObject = dataObject;
      this.operationToPerform = operationType;

    }

  }

}
