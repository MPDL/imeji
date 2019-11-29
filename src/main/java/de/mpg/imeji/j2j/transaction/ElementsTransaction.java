package de.mpg.imeji.j2j.transaction;

import java.util.ArrayList;
import java.util.List;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.ReadWrite;

import de.mpg.imeji.exceptions.AlreadyExistsException;
import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.exceptions.NotFoundException;
import de.mpg.imeji.exceptions.ReloadBeforeSaveException;
import de.mpg.imeji.exceptions.UnprocessableError;
import de.mpg.imeji.j2j.controler.ResourceElementController;
import de.mpg.imeji.logic.init.ImejiInitializer;
import de.mpg.imeji.logic.model.aspects.AccessMember.ChangeMember;



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
public class ElementsTransaction extends Transaction {

  private List<ChangeMember> elementCRUDs;


  /**
   * The complete data object whose field was edited.
   */
  private List<Object> resultObjects;


  public enum ElementTransactionType {
    EDIT
  }


  public ElementsTransaction(List<ChangeMember> elementCRUDs) {
    super(null);
    this.elementCRUDs = elementCRUDs;
    this.resultObjects = new ArrayList<Object>();
  }



  public List<Object> getResults() {
    return this.resultObjects;
  }


  @Override
  protected void execute(Dataset ds) throws ImejiException {
    for (ChangeMember elementCRUD : this.elementCRUDs) {
      Object result = executeSingleElementCRUD(elementCRUD, ds);
      if (result != null) {
        this.resultObjects.add(result);
      }
    }
  }

  @Override
  protected ReadWrite getLockType() {
    return ReadWrite.WRITE;
  }



  private Object executeSingleElementCRUD(ChangeMember changeMember, Dataset ds)
      throws NotFoundException, AlreadyExistsException, UnprocessableError, ReloadBeforeSaveException {

    // find model of object
    String modelName = ImejiInitializer.getJenaModelName(changeMember.getImejiDataObject().getClass());
    this.setModel(modelName);

    ResourceElementController resourceController = new ResourceElementController(getModel(ds));
    Object result = resourceController.changeMember(changeMember);

    return result;
  }
}
