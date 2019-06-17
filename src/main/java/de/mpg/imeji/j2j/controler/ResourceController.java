package de.mpg.imeji.j2j.controler;

import java.lang.reflect.Field;
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
  protected final Java2Jena java2rdf;
  protected final Jena2Java rdf2Java;

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


  /**
   * Create in Jena
   *
   * @throws AlreadyExistsException
   * @throws NotFoundException
   * @throws InterruptedException
   */
  public Object create(Object imejiDataObject) throws AlreadyExistsException, NotFoundException {
    if (java2rdf.exists(imejiDataObject)) {
      throw new AlreadyExistsException(
          "Error creating resource " + J2JHelper.getId(imejiDataObject).getPath().replace("imeji/", "") + ". Resource already exists! ");
    }
    setTimestamp(imejiDataObject);
    java2rdf.write(imejiDataObject);
    Object dataObjectInStore = this.read(imejiDataObject);
    return dataObjectInStore;
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
   * Update (remove and create) the complete {@link RDFResource}
   *
   * @param imejiDataObject
   * @throws NotFoundException
   */
  public Object update(Object imejiDataObject) throws NotFoundException, ReloadBeforeSaveException, UnprocessableError {

    // Check if object exists in Jena 
    if (!java2rdf.exists(imejiDataObject)) {
      throw new NotFoundException("Error updating resource " + imejiDataObject.toString() + " with id \"" + J2JHelper.getId(imejiDataObject)
          + "\". Resource doesn't exist in model " + model.toString());
    }
    // Check if object in database has been changed since it was last read
    checkModified(imejiDataObject);
    java2rdf.update(imejiDataObject);
    updateTimestampInJena(imejiDataObject);
    Object dataObjectInStore = this.read(imejiDataObject);
    return dataObjectInStore;
  }

  /**
   * Before updating a data object in Jena, check whether the object has been altered in database
   * since it was last read from there. Throw ReloadBeforeSaveException in case that resource has
   * been altered.
   * 
   * @param imejiDataObject
   * @throws ReloadBeforeSaveException
   * @throws UnprocessableError
   * @throws NotFoundException
   */
  private void checkModified(Object imejiDataObject) throws ReloadBeforeSaveException, UnprocessableError, NotFoundException {
    // Throw ReloadBeforeSaveException in case that object in Jena has been modified since we last read it.
    if (imejiDataObject instanceof ResourceLastModified) {
      if (imejiDataObject instanceof CloneURI) {
        Object currentObjectInJena = this.read(((CloneURI) imejiDataObject).cloneURI());
        Calendar lastModifiedInDatabase = ((ResourceLastModified) currentObjectInJena).getModified();
        Calendar imejiDataObjectLastModified = ((ResourceLastModified) imejiDataObject).getModified();
        if (lastModifiedInDatabase != null && imejiDataObjectLastModified != null) {
          if (lastModifiedInDatabase.getTimeInMillis() != imejiDataObjectLastModified.getTimeInMillis()) {
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
          "Error deleting resource " + J2JHelper.getId(o).getPath().replace("imeji/", "") + ". Resource doesn't exist! ");
    }
    java2rdf.remove(o);
  }



  /**
   * Sets the time stamp in the imeji data object to now.
   * 
   * @param imejiDataObject
   */
  protected void setTimestamp(Object imejiDataObject) {
    if (imejiDataObject instanceof ResourceLastModified) {
      Calendar now = Calendar.getInstance();
      ((ResourceLastModified) imejiDataObject).setModified(now);
    }
  }



  /**
   * Will update the time stamp field of the data object in database and set it to now. In order to
   * get the object with its latest time stamp you need to re-read the object from database.
   * 
   * @param imejiDataObject
   */
  protected void updateTimestampInJena(Object imejiDataObject) {

    if (imejiDataObject instanceof ResourceLastModified) {
      String resourceURI = J2JHelper.getId(imejiDataObject).toString();
      Field timeStampField = ((ResourceLastModified) imejiDataObject).getTimeStampField();
      if (timeStampField != null) {
        String predicateURI = J2JHelper.getNamespace(timeStampField);
        Calendar now = Calendar.getInstance();
        java2rdf.setTimestamp(resourceURI, predicateURI, now);
      } else {
        // Error
      }

    }
  }


  /**
   * Get id of object from URI
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
   * Get object type from URI
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



}
