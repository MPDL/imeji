package de.mpg.imeji.j2j.transaction;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.ReadWrite;

import de.mpg.imeji.exceptions.AlreadyExistsException;
import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.exceptions.NotFoundException;
import de.mpg.imeji.exceptions.ReloadBeforeSaveException;
import de.mpg.imeji.exceptions.UnprocessableError;
import de.mpg.imeji.exceptions.WorkflowException;
import de.mpg.imeji.j2j.controler.ResourceController;
import de.mpg.imeji.j2j.controler.ResourceElementController;
import de.mpg.imeji.j2j.helper.J2JHelper;
import de.mpg.imeji.logic.init.ImejiInitializer;
import de.mpg.imeji.logic.model.User;
import de.mpg.imeji.logic.model.aspects.ChangeMember;



/**
 * Use this for transactional access to Jena if you want to
 * 
 * - edit different types of data objects within one transaction (i.e. editing collection and item
 * data objects within one atomic action)
 * 
 * - edit single fields of data objects in store (i.e. change only the license field of an item, as
 * opposed to writing the whole item object new)
 * 
 * 
 * @author breddin
 *
 */
public class ElementsTransaction extends SecureTransaction {

  private List<ChangeMember> elementCRUDs;


  /**
   * The complete data object whose field was edited.
   */
  private Map<URI, Object> resultMap;


  public enum ElementTransactionType {
    EDIT
  }


  public ElementsTransaction(List<ChangeMember> elementCRUDs, User issuingUser) {

    super(null, ChangeMember.getChangeObjects(elementCRUDs), OperationType.UPDATE, issuingUser);
    this.elementCRUDs = elementCRUDs;
    this.resultMap = new HashMap<URI, Object>();
  }



  public List<Object> getResults() {

    Collection<Object> resultsFromMap = this.resultMap.values();
    List<Object> results = new ArrayList<Object>(resultsFromMap.size());
    results.addAll(resultsFromMap);
    return results;
  }


  @Override
  protected void execute(Dataset ds) throws ImejiException {

    // check if all operations specified in ChangeMembers are valid 
    checkOperationsValid(ds);
    // write changes to database
    for (ChangeMember elementCRUD : this.elementCRUDs) {
      Object result = executeSingleElementCRUD(elementCRUD, ds);
      if (result != null) {
        URI id = J2JHelper.getId(result);
        if (id != null) {
          this.resultMap.put(id, result);
        }
      }
    }
  }

  @Override
  protected ReadWrite getLockType() {
    return ReadWrite.WRITE;
  }

  /**
   * Checks if given the current state of database, the operations specified in the list of
   * ChangeMembers are valid. Does not check if the operations in the list of ChangeMembers are
   * valid with respect to each other. Throws error if at least one operation is not valid.
   * 
   * @throws WorkflowException
   * @throws NotFoundException
   * 
   */
  private void checkOperationsValid(Dataset ds) throws NotFoundException, WorkflowException {

    List<Object> objectsToChangeInDatabase = ChangeMember.getChangeObjects(this.elementCRUDs);

    for (Object objectToChange : objectsToChangeInDatabase) {
      setModel(objectToChange);
      ResourceController resourceController = new ResourceController(getModel(ds), false);
      checkObjectStatus(resourceController, objectToChange, OperationType.UPDATE);
    }

  }

  private Object executeSingleElementCRUD(ChangeMember changeMember, Dataset ds)
      throws NotFoundException, AlreadyExistsException, UnprocessableError, ReloadBeforeSaveException, WorkflowException {

    setModel(changeMember.getImejiDataObject());
    ResourceElementController resourceController = new ResourceElementController(getModel(ds));
    Object result = resourceController.changeMember(changeMember);
    return result;
  }

  private void setModel(Object imejiDataObject) {

    // find model of object
    String modelName = ImejiInitializer.getJenaModelName(imejiDataObject.getClass());
    this.setModel(modelName);
  }

}
