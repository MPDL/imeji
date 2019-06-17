package de.mpg.imeji.j2j.transaction;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.ReadWrite;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.j2j.controler.ResourceElementController;
import de.mpg.imeji.logic.model.aspects.AccessMember.ChangeMember;


/**
 * Add, remove or edit imeji data object class members with a transaction.
 * 
 * @author breddin
 *
 */
public class ElementTransaction extends Transaction {


  private ChangeMember changeMember;


  private Object result;



  public ElementTransaction(String modelURI, ChangeMember changeMember) {
    super(modelURI);
    this.changeMember = changeMember;
    this.result = changeMember.getImejiDataObject(); // default
  }


  public Object getResult() {
    return this.result;
  }

  @Override
  protected void execute(Dataset ds) throws ImejiException {

    final ResourceElementController resourceController = new ResourceElementController(getModel(ds));
    this.result = resourceController.changeMember(this.changeMember);
  }

  @Override
  protected ReadWrite getLockType() {
    return ReadWrite.WRITE;
  }



}
