package de.mpg.imeji.j2j.transaction;

import java.util.ArrayList;
import java.util.List;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.ReadWrite;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.j2j.controler.ResourceController;

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
      invokeResourceController(rc, o);
    }
  }

  public List<Object> getResults() {
    return this.results;
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
