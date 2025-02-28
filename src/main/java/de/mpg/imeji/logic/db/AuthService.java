package de.mpg.imeji.logic.db;

import de.mpg.imeji.exceptions.*;
import de.mpg.imeji.j2j.authorization.DbAuthorization;
import de.mpg.imeji.j2j.authorization.JenaAuthorization;
import de.mpg.imeji.j2j.controler.ResourceController;
import de.mpg.imeji.j2j.helper.J2JHelper;
import de.mpg.imeji.j2j.queries.Queries;
import de.mpg.imeji.j2j.transaction.OperationType;
import de.mpg.imeji.j2j.transaction.SecureTransaction;
import de.mpg.imeji.logic.config.Imeji;
import de.mpg.imeji.logic.db.repositories.DbRepository;
import de.mpg.imeji.logic.db.repositories.UserDbRepository;
import de.mpg.imeji.logic.db.writer.WriterFacade;
import de.mpg.imeji.logic.init.ImejiInitializer;
import de.mpg.imeji.logic.model.Properties;
import de.mpg.imeji.logic.model.User;
import de.mpg.imeji.logic.model.UserGroup;
import de.mpg.imeji.logic.model.aspects.CloneURI;
import de.mpg.imeji.logic.search.jenasearch.JenaCustomQueries;
import de.mpg.imeji.logic.security.authorization.Authorization;
import de.mpg.imeji.logic.workflow.WorkflowValidator;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.Model;

import java.net.URI;
import java.security.Security;
import java.util.ArrayList;
import java.util.List;

public class AuthService {

    private User issuingUser;
    private List<ObjectOperation> writeOperations;
    private List<Object> readObjects;

    private UserDbRepository userDbRepository = new UserDbRepository();

    public AuthService(User issuingUser, List<Object> objectsToCheck, OperationType operation) {
        this.issuingUser = issuingUser;
        this.setReadAndWriteOperations(objectsToCheck, operation);
    }

    public AuthService(User issuingUser, List<Object> objectsToCheck, List<OperationType> operations) {
        this.issuingUser = issuingUser;
        this.setReadAndWriteOperations(objectsToCheck, operations);
    }


    public void checkLogin() throws ImejiException {

        // (1) issuingUser == null if user is not logged in
        // (2) Imeji.adminUser is a static user object that is constructed upon server start
        // and represents general admin access rights. The constant is used in system operations
        // that involve no actual users. This user object does not exist in database.
        if (this.issuingUser != null && this.issuingUser != Imeji.adminUser) {
            // (1) check access rights:
            // (1a) load user object from database (if not Imeji.adminUser)
            String userModelURI = ImejiInitializer.getModelName(User.class);
            //Model userModel = dataset.getNamedModel(userModelURI);
            //final ResourceController userResourceController = new ResourceController(userModel, false);

            //Object emptyUserObject = this.issuingUser.cloneURI();
            Object userInDatabase = userDbRepository.read(this.issuingUser.getId().toString());
            if (userInDatabase == null) {
                throw new AuthenticationError(AuthenticationError.USER_MUST_BE_LOGGED_IN);
            }
            this.issuingUser = (User) userInDatabase;
            //loadUsersUserGroups(userResourceController, dataset, userModelURI);
        }

        /*
        // (1b) check access rights of user
        checkSecurityForWriteOperations();
        // (2) execute operations (to be implemented by extending classes)
        execute(dataset);
        // (3) check security for read operations
        checkSecurityForReadOperations();

         */

    }

    /**
     * For data objects that have a status (i.e. Item or CollectionImeji) check whether the status
     * allows proceeding with a create, update or delete operation.
     *
     * @param object
     * @throws NotFoundException
     * @throws WorkflowException
     */
    public static void checkObjectStatus(DbRepository dbRepository, Object object, OperationType operation)
            throws NotFoundException, WorkflowException {

        if (object instanceof Properties) {

            WorkflowValidator workflowManager = new WorkflowValidator();

            // create: check client object
            if (operation == OperationType.CREATE) {
                workflowManager.isCreateAllowed((Properties) object);
            }
            // update, delete: check database object (and not client object)
            else if (operation == OperationType.UPDATE || operation == OperationType.DELETE) {
                Object databaseObject = getCorrespondingObjectInDatabase(object, dbRepository);
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
    /*
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

     */

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
    private static Object getCorrespondingObjectInDatabase(Object clientImejiDataObject, DbRepository dbRepository)
            throws NotFoundException {

        if (clientImejiDataObject instanceof CloneURI) {
            try {
                URI id = J2JHelper.getId(clientImejiDataObject);
                Object currentObjectInJena = dbRepository.read(id.toString());
                return currentObjectInJena;
            } catch (ImejiException e) {
                throw new NotFoundException(e);
            }
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
    public void checkSecurityForWriteOperations() throws NotAllowedError, AuthenticationError {

        Authorization authorization = new DbAuthorization();
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
    public void checkSecurityForReadOperations() throws NotAllowedError {

        if (this.readObjects != null) {

            Authorization authorization = new DbAuthorization();
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
    public void setReadAndWriteOperations(List<Object> dataObjects, OperationType operation) {
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
