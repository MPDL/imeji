package de.mpg.imeji.j2j.controler;

import java.net.URI;
import java.util.Calendar;

import org.apache.jena.rdf.model.Model;

import de.mpg.imeji.exceptions.AlreadyExistsException;
import de.mpg.imeji.exceptions.NotFoundException;
import de.mpg.imeji.exceptions.ReloadBeforeSaveException;
import de.mpg.imeji.exceptions.UnprocessableError;
import de.mpg.imeji.j2j.annotations.j2jId;
import de.mpg.imeji.j2j.helper.J2JHelper;
import de.mpg.imeji.j2j.persistence.Java2Jena;
import de.mpg.imeji.j2j.persistence.Jena2Java;
import de.mpg.imeji.logic.model.aspects.CloneURI;
import de.mpg.imeji.logic.model.aspects.ResourceLastModified;

/**
 * Controller for {@link RDFResource} Attention: Non transactional!!!! Don't use directly, use
 * JenaWriter of JenaReader instead
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public class ResourceController {
  private Model model = null;
  private final Java2Jena java2rdf;
  private final Jena2Java rdf2Java;

  /**
   * Use for transaction. The model must have been created/retrieved within the transaction
   *
   * @param model
   * @param lazy
   */
  public ResourceController(Model model, boolean lazy) {
    if (model == null) {
      throw new NullPointerException("Fatal error: Model is null");
    }
    this.model = model;
    this.java2rdf = new Java2Jena(model, lazy);
    this.rdf2Java = new Jena2Java(model, lazy);
  }

  /**
   * Create into Jena
   *
   * @throws AlreadyExistsException
   * @throws InterruptedException
   */
  public void create(Object o) throws AlreadyExistsException {
    if (java2rdf.exists(o)) {
      throw new AlreadyExistsException(
          "Error creating resource " + J2JHelper.getId(o).getPath().replace("imeji/", "") + ". Resource already exists! ");
    }
    java2rdf.write(o);
  }

  /**
   * Read the uri and write it into the {@link RDFResource}
   *
   * @param uri
   * @param javaObject
   * @return
   * @throws NotFoundException
   */
  public Object read(URI uri, Object o) throws NotFoundException {
    J2JHelper.setId(o, uri);
    return read(o);
  }

  /**
   * read a {@link Object} if it has an id defined by a {@link j2jId}
   *
   * @param o
   * @return
   * @throws NotFoundException
   */
  public Object read(Object o) throws NotFoundException {
    if (!java2rdf.exists(o)) {
      throw new NotFoundException(getObjectType(J2JHelper.getId(o)) + " " + getObjectId(J2JHelper.getId(o)) + " not found!");
    }
    o = rdf2Java.loadResource(o);
    return o;
  }

  /**
   * Find the Id of the object into tje uri
   * 
   * @param uri
   * @return
   */
  private String getObjectId(URI uri) {
    try {
      String path = uri.getPath();
      return path.substring(path.lastIndexOf('/') + 1);
    } catch (Exception e) {
      return uri != null ? uri.toString() : "";
    }
  }

  /**
   * Find the object type according to the uri
   * 
   * @param uri
   * @return
   */
  private String getObjectType(URI uri) {
    try {
      String path = uri.getPath().replace("/" + getObjectId(uri), "");
      return path.substring(path.lastIndexOf('/') + 1);
    } catch (Exception e) {
      return "Object";
    }
  }

  /**
   * Update (remove and create) the complete {@link RDFResource}
   *
   * @param imejiDataObject
   * @throws NotFoundException
   */
  public void update(Object imejiDataObject) throws NotFoundException, ReloadBeforeSaveException, UnprocessableError {

    // Check if object exists in Jena 
    if (!java2rdf.exists(imejiDataObject)) {
      throw new NotFoundException("Error updating resource " + imejiDataObject.toString() + " with id \"" + J2JHelper.getId(imejiDataObject)
          + "\". Resource doesn't exists in model " + model.toString());
    }
    // Throw ReloadBeforeSaveException in case that object in Jena has been modified since we last read it.
    if (imejiDataObject instanceof ResourceLastModified) {
      if (imejiDataObject instanceof CloneURI) {
        Object currentObjectInJena = this.read(((CloneURI) imejiDataObject).cloneURI());
        Calendar lastModifiedInDatabase = ((ResourceLastModified) currentObjectInJena).getModified();
        Calendar imejiDataObjectLastModified = ((ResourceLastModified) imejiDataObject).getLastTimeStampReadFromDatabase();
        if (lastModifiedInDatabase != null && imejiDataObjectLastModified != null) {
          if (lastModifiedInDatabase.getTimeInMillis() > imejiDataObjectLastModified.getTimeInMillis()) {
            throw new ReloadBeforeSaveException(currentObjectInJena);
          }
        } else {
          throw new UnprocessableError("Could not process update request, no timestamp for data synchronization available");
        }
      } else {
        throw new UnprocessableError("Could not process update request, interface CloneURI not implemented (but needs to be) for class "
            + imejiDataObject.getClass());
      }
    }
    java2rdf.update(imejiDataObject);
  }

  /**
   * Delete a {@link RDFResource}
   *
   * @param o
   * @throws NotFoundException
   */
  public void delete(Object o) throws NotFoundException {
    if (!java2rdf.exists(o)) {
      throw new NotFoundException(
          "Error deleting resource " + J2JHelper.getId(o).getPath().replace("imeji/", "") + ". Resource doesn't exists! ");
    }
    java2rdf.remove(o);
  }

  /**
   * getter
   *
   * @return
   */
  public Model getModel() {
    return model;
  }

  /**
   * setter
   *
   * @param model
   */
  public void setModel(Model model) {
    this.model = model;
  }
}
