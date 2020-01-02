package de.mpg.imeji.j2j.transaction;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.NotImplementedException;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.ReadWrite;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.exceptions.NotFoundException;
import de.mpg.imeji.exceptions.WorkflowException;
import de.mpg.imeji.j2j.controler.ResourceController;
import de.mpg.imeji.logic.model.Properties;
import de.mpg.imeji.logic.model.aspects.CloneURI;
import de.mpg.imeji.logic.workflow.WorkflowValidator;

/**
 * {@link Transaction} for CRUD methods
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public class CRUDTransaction extends Transaction {

  private List<Object> objects = new ArrayList<Object>();
  private List<Object> results = new ArrayList<Object>();

  private final CRUDTransactionType type;
  private boolean lazy = false;


  public enum CRUDTransactionType {
    CREATE,
    READ,
    UPDATE,
    DELETE;
  }

  /**
   * Constructor for a {@link CRUDTransaction} with a {@link List} of {@link Object}
   *
   * @param objects
   * @param type
   * @param modelURI
   * @param lazy
   */
  public CRUDTransaction(List<Object> objects, CRUDTransactionType type, String modelURI, boolean lazy) {
    super(modelURI);
    this.objects = objects;
    this.type = type;
    this.lazy = lazy;
  }

  @Override
  protected void execute(Dataset ds) throws ImejiException {
    final ResourceController rc = new ResourceController(getModel(ds), lazy);
    for (final Object o : objects) {
      checkObjectStatus(rc, o);
      invokeResourceController(rc, o);
    }
  }

  public List<Object> getResults() {
    return this.results;
  }

  /**
   * For data objects that have a status (i.e. Item or CollectionImeji) check whether the status
   * allows proceeding with a create, update or delete operation.
   * 
   * @param object
   * @throws NotFoundException
   * @throws WorkflowException
   */
  private void checkObjectStatus(ResourceController resourceController, Object object) throws NotFoundException, WorkflowException {

    if (object instanceof Properties) {

      WorkflowValidator workflowManager = new WorkflowValidator();

      // create: check client object
      if (this.type == CRUDTransactionType.CREATE) {
        workflowManager.isCreateAllowed((Properties) object);
      }
      // update, delete: check database object (and not client object)
      else if (this.type == CRUDTransactionType.UPDATE || this.type == CRUDTransactionType.DELETE) {
        Object databaseObject = getCorrespondingObjectInDatabase(object, resourceController);
        switch (this.type) {
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
   * Make the CRUD operation for one {@link Object} thanks to the {@link ResourceController}
   *
   * @param rc
   * @param o
   * @throws ImejiException
   */
  private void invokeResourceController(ResourceController rc, Object o) throws ImejiException {
    Object result = null;
    switch (type) {
      case CREATE:
        result = rc.create(o);
        break;
      case READ:
        rc.read(o);
        break;
      case UPDATE:
        result = rc.update(o);
        break;
      case DELETE:
        rc.delete(o);
        break;
    }
    if (result != null) {
      this.results.add(result);
    }
  }


  @Override
  protected ReadWrite getLockType() {
    switch (type) {
      case READ:
        return ReadWrite.READ;
      default:
        return ReadWrite.WRITE;
    }
  }
}
