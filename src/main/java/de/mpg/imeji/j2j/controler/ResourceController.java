package de.mpg.imeji.j2j.controler;

import java.net.URI;

import com.hp.hpl.jena.rdf.model.Model;

import de.mpg.imeji.exceptions.AlreadyExistsException;
import de.mpg.imeji.exceptions.NotFoundException;
import de.mpg.imeji.j2j.annotations.j2jId;
import de.mpg.imeji.j2j.helper.J2JHelper;
import de.mpg.imeji.j2j.persistence.Java2Jena;
import de.mpg.imeji.j2j.persistence.Jena2Java;

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
  private Java2Jena java2rdf;
  private Jena2Java rdf2Java;

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
      throw new AlreadyExistsException("Error creating resource "
          + J2JHelper.getId(o).getPath().replace("imeji/", "") + ". Resource already exists! ");
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
      throw new NotFoundException(
          "Resource " + J2JHelper.getId(o).getPath().replace("imeji/", "") + " not found!");
    }
    o = rdf2Java.loadResource(o);
    return o;
  }

  /**
   * Update (remove and create) the complete {@link RDFResource}
   *
   * @param o
   * @throws NotFoundException
   */
  public void update(Object o) throws NotFoundException {
    if (!java2rdf.exists(o)) {
      throw new NotFoundException("Error updating resource " + o.toString() + " with id \""
          + J2JHelper.getId(o).getPath().replace("imeji/", "")
          + "\". Resource doesn't exists in model " + model.toString());
    }
    java2rdf.update(o);
  }

  /**
   * Delete a {@link RDFResource}
   *
   * @param o
   * @throws NotFoundException
   */
  public void delete(Object o) throws NotFoundException {
    if (!java2rdf.exists(o)) {
      throw new NotFoundException("Error deleting resource "
          + J2JHelper.getId(o).getPath().replace("imeji/", "") + ". Resource doesn't exists! ");
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
