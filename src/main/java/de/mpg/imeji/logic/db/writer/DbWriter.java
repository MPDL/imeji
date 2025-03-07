package de.mpg.imeji.logic.db.writer;

import de.mpg.imeji.exceptions.*;
import de.mpg.imeji.j2j.authorization.JenaAuthorization;
import de.mpg.imeji.j2j.controler.ResourceController;
import de.mpg.imeji.j2j.helper.J2JHelper;
import de.mpg.imeji.j2j.queries.Queries;
import de.mpg.imeji.j2j.transaction.*;
import de.mpg.imeji.logic.config.Imeji;
import de.mpg.imeji.logic.db.AuthService;
import de.mpg.imeji.logic.db.reader.JenaReader;
import de.mpg.imeji.logic.db.repositories.*;
import de.mpg.imeji.logic.init.ImejiInitializer;
import de.mpg.imeji.logic.model.Properties;
import de.mpg.imeji.logic.model.User;
import de.mpg.imeji.logic.model.UserGroup;
import de.mpg.imeji.logic.model.aspects.AccessMember;
import de.mpg.imeji.logic.model.aspects.ChangeMember;
import de.mpg.imeji.logic.model.aspects.CloneURI;
import de.mpg.imeji.logic.model.aspects.ResourceLastModified;
import de.mpg.imeji.logic.search.jenasearch.ImejiSPARQL;
import de.mpg.imeji.logic.search.jenasearch.JenaCustomQueries;
import de.mpg.imeji.logic.security.authorization.Authorization;
import de.mpg.imeji.logic.util.ObjectHelper;
import de.mpg.imeji.logic.workflow.WorkflowValidator;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.jena.Jena;
import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.Model;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.URI;
import java.security.Security;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * imeji WRITE operations (create/delete/update) in {@link Jena} <br/>
 * - Use {@link Transaction} <br/>
 * - For concurrency purpose, each write {@link Transaction} is made within a single {@link Thread}.
 * Use {@link ThreadedTransaction} <br/>
 * - for READ operations, uses {@link JenaReader}
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public class DbWriter implements Writer {
  private final String modelURI;
  private static final ExecutorService WRITE_EXECUTOR = Executors.newSingleThreadExecutor();
  protected static Logger LOGGER = LogManager.getLogger(DbWriter.class);

  private UserDbRepository userDbRepository = new UserDbRepository();
  private DbRepository dbRepository;

  /**
   * Construct one {@link DbWriter} for one {@link Model}
   *
   * @param modelURI
   */
  public DbWriter(String modelURI) {

    this.modelURI = modelURI;
    //LOGGER.info("Creating Writer for " + modelURI);
    if (modelURI != null) {
      ObjectHelper.ObjectType type = ObjectHelper.getObjectType(URI.create(modelURI));
      this.dbRepository = DbRepository.getRepositoryForModel(type);
    }

  }

  /**
   * Create a {@link List} of {@link Object} in {@link Jena}
   *
   * @param objects
   * @param user
   * @throws Exception
   */
  @Override
  public List<Object> create(List<Object> objects, User user) throws ImejiException {

    AuthService as = new AuthService(user, objects, OperationType.CREATE);
    as.checkLogin();
    as.checkSecurityForWriteOperations();
    List<Object> createdObjects = new ArrayList<>();
    for (Object o : objects) {
      AuthService.checkObjectStatus(dbRepository, o, OperationType.CREATE);
      setTimestamp(o);
      dbRepository.create(o);
      createdObjects.add(o);
    }
    as.checkSecurityForReadOperations();
    return createdObjects;
  }

  /**
   * Delete a {@link List} of {@link Object} in {@link Jena}
   *
   * @param objects
   * @param user
   * @throws Exception
   */
  @Override
  public void delete(List<Object> objects, User user) throws ImejiException {

    AuthService as = new AuthService(user, objects, OperationType.DELETE);
    as.checkLogin();
    as.checkSecurityForWriteOperations();
    List<Object> createdObjects = new ArrayList<>();
    for (Object o : objects) {
      String id = J2JHelper.getId(o).toString();
      Object objFromDb = dbRepository.read(id);
      AuthService.checkObjectStatus(dbRepository, objFromDb, OperationType.DELETE);
      dbRepository.delete(id);
      //userDbRepository.removeGrantsForObject(id);
    }
    as.checkSecurityForReadOperations();
  }

  /**
   * Update a {@link List} of {@link Object} in {@link Jena}
   *
   * @param objects
   * @param user
   * @throws Exception
   */
  @Override
  public List<Object> update(List<Object> objects, User user) throws ImejiException {
    AuthService as = new AuthService(user, objects, OperationType.UPDATE);
    as.checkLogin();
    as.checkSecurityForWriteOperations();
    List<Object> createdObjects = new ArrayList<>();
    for (Object o : objects) {
      AuthService.checkObjectStatus(dbRepository, o, OperationType.UPDATE);
      Object current = dbRepository.read(J2JHelper.getId(o).toString());
      checkModified(o, current);
      setTimestamp(o);
      Object updated = dbRepository.update(o);
      createdObjects.add(updated);
    }
    as.checkSecurityForReadOperations();
    return createdObjects;

  }

  /**
   * Update LAZY a {@link List} of {@link Object} in {@link Jena}<br/>
   * - {@link List} contained within the {@link Object} are not updated: faster performance,
   * especially for objects with huge {@link List}
   *
   * @param objects
   * @param user
   * @throws Exception
   */
  @Override
  public List<Object> updateLazy(List<Object> objects, User user) throws ImejiException {
    return this.update(objects, user);
  }

  @Override
  public List<Object> editElements(List<ChangeMember> changeElements, User issuingUser) throws ImejiException {

    // check if all operations specified in ChangeMembers are valid
    List<Object> objectsToChangeInDatabase = ChangeMember.getChangeObjects(changeElements);

    AuthService as = new AuthService(issuingUser, objectsToChangeInDatabase, OperationType.UPDATE);
    as.checkLogin();
    as.checkSecurityForWriteOperations();

    for (Object objectToChange : objectsToChangeInDatabase) {
      //setModel(objectToChange);
      ObjectHelper.ObjectType ot = ObjectHelper.getObjectType(J2JHelper.getId(objectToChange));
      DbRepository dbRepository = DbRepository.getRepositoryForModel(ot);;
      AuthService.checkObjectStatus(dbRepository, objectToChange, OperationType.UPDATE);
    }

    EntityManager em = EntityManagerHelper.factory.createEntityManager();
    EntityTransaction transaction = em.getTransaction();
    List<Object> updatedList = new ArrayList<>();
    try {
      transaction.begin();
      for (ChangeMember changeMember : changeElements) {

        if (changeMember.getImejiDataObject() instanceof CloneURI) {

          Object emptyObjectWithURI = ((CloneURI) changeMember.getImejiDataObject()).cloneURI();
          URI objectId = J2JHelper.getId(emptyObjectWithURI);
          //String model = J2JHelper.getModel(objectId);
          //DbRepository dbRepository = DbRepository.getRepositoryForModel(model)

          Object dataObjectInStore = em.find(changeMember.getImejiDataObject().getClass(), objectId.toString());


          if (dataObjectInStore instanceof AccessMember) {
            ((AccessMember) dataObjectInStore).accessMember(changeMember);


            checkModified(changeMember.getImejiDataObject(), dataObjectInStore);
            Object newObject = em.merge(dataObjectInStore);
            updatedList.add(newObject);
          }
        } else {
          throw new UnprocessableError(
              "Could not update member of " + J2JHelper.getId(changeMember.getImejiDataObject()).getPath().replace("imeji/", "")
                  + ". Reason: Required interfaces not implemented.");
        }
      }
      //work.accept(entityManager);
      transaction.commit();
    } catch (Exception e) {
      if (transaction.isActive())
        transaction.rollback();
      throw new ImejiException("Error with database", e);
    } finally {
      em.close();
    }


    as.checkSecurityForReadOperations();
    return updatedList;

  }


  /**
   * Run one WRITE operation in {@link Transaction} within a {@link ThreadedTransaction}
   *
   * @param objects
   * @param type
   * @param lazy
   * @throws Exception
   */
  private List<Object> runCRUDTransaction(List<Object> objects, OperationType type, User user, boolean lazy) throws ImejiException {
    final CRUDTransaction crudTransaction = new CRUDTransaction(objects, type, user, modelURI, lazy);
    // Write Transaction needs to be added in a new Thread
    ThreadedTransaction.run(new ThreadedTransaction(crudTransaction, Imeji.tdbPath), WRITE_EXECUTOR);
    return crudTransaction.getResults();
  }


  protected void setTimestamp(Object imejiDataObject) {
    if (imejiDataObject instanceof ResourceLastModified) {
      Calendar now = Calendar.getInstance();
      ((ResourceLastModified) imejiDataObject).setModified(now);
    }
  }


  private void checkModified(Object imejiDataObject, Object currentDbObject) throws ReloadBeforeSaveException, NotFoundException {
    // Throw ReloadBeforeSaveException in case that object in Jena has been modified since we last read it.
    if (imejiDataObject instanceof ResourceLastModified) {
      if (imejiDataObject instanceof CloneURI) {

        //Object currentObjectInJena = this.read(((CloneURI) imejiDataObject).cloneURI());
        Calendar lastModifiedInDatabase = ((ResourceLastModified) currentDbObject).getModified();
        Calendar imejiDataObjectLastModified = ((ResourceLastModified) imejiDataObject).getModified();
        if (lastModifiedInDatabase != null && imejiDataObjectLastModified != null) {
          if (lastModifiedInDatabase.getTimeInMillis() != imejiDataObjectLastModified.getTimeInMillis()) {
            throw new ReloadBeforeSaveException(currentDbObject);
          }
        } else {
          throw new NotImplementedException("Could not process update request, no timestamp for data synchronization available");
        }
      } else {
        throw new NotImplementedException(
            "Could not process update request, interface CloneURI not implemented (but needs to be) for class "
                + imejiDataObject.getClass());
      }
    }
  }



}
