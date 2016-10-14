package de.mpg.imeji.j2j.persistence;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.hp.hpl.jena.Jena;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;

import de.mpg.imeji.j2j.helper.J2JHelper;
import de.mpg.imeji.j2j.helper.LiteralHelper;
import de.mpg.imeji.util.LocalizedString;

/**
 * Write Java objects annotated with j2jannotations as Jena {@link Resource} of {@link Literal}
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public class Java2Jena {
  private Model model;
  private LiteralHelper literalHelper;
  private static Logger LOGGER = Logger.getLogger(Java2Jena.class);
  private boolean lazy = false;

  /**
   * Construct a {@link Java2Jena} for one {@link Model}
   *
   * @param model
   * @param lazy - defined whether the write operations will be lazy or not (i.e. skip or not the
   *        {@link List})
   */
  public Java2Jena(Model model, boolean lazy) {
    this.model = model;
    literalHelper = new LiteralHelper(model);
    this.lazy = lazy;
  }

  /**
   * Write a {@link Object} in Jena
   *
   * @param r
   */
  public void write(Object o) {
    if (J2JHelper.getId(o) == null) {
      throw new NullPointerException("Fatal error: Resource " + o + " with a null id");
    }
    Resource type = model.createResource(J2JHelper.getResourceNamespace(o).toString(), RDFS.Class);
    Resource r = model.createResource(J2JHelper.getId(o).toString(), type);
    addProperties2Resource(r, o);
  }

  /**
   * Update a {@link RDFResource}. Remove all the properties of the {@link RDFResource} and write
   * the new again
   *
   * @param r
   */
  public void update(Object o) {
    if (J2JHelper.getId(o) == null) {
      throw new NullPointerException("Fatal error: Resource " + o + " with a null id");
    }
    if (lazy) {
      removeLazy(o);
    } else {
      remove(o);
    }
    write(o);
  }

  /**
   * Remove a {@link RDFResource} in Jena
   *
   * @param r
   */
  public void remove(Object o) {
    if (J2JHelper.getId(o) == null) {
      throw new NullPointerException("Fatal error: Resource " + o + " with a null id");
    }
    Resource r = model.getResource(J2JHelper.getId(o).toString());// createResource(o);
    for (Resource e : getEmbeddedResources(o)) {
      model.removeAll(e, null, null);
    }
    model.removeAll(r, null, null);
  }

  /**
   * Update a single Triple
   *
   * @param uri
   * @param property
   * @param obj
   */
  public void update(String uri, String property, Object obj) {
    Resource r = model.createResource(uri);
    Property p = model.createProperty(property);
    model.removeAll(r, p, null);
    if (obj instanceof URI) {
      Resource o = model.createResource(((URI) obj).toString());
      if (o != null) {
        model.add(r, p, o);
      }
    } else if (obj instanceof LocalizedString) {
      Literal o = model.createLiteral(((LocalizedString) obj).getValue(),
          ((LocalizedString) obj).getLang());
      p = RDFS.label;
      if (o != null) {
        model.add(r, p, o);
      }
    } else {
      Literal o = literalHelper.java2Literal(obj);
      if (o != null) {
        model.add(r, p, o);
      }
    }
  }

  /**
   * Remove only relation not defined in a lazy list
   *
   * @param o
   */
  private void removeLazy(Object o) {
    Resource r = createResource(o);
    for (Field f : J2JHelper.getAllObjectFields(o.getClass())) {
      if (!J2JHelper.isLazyList(f)) {
        String ns = J2JHelper.getNamespace(f);
        if (ns != null) {
          Property p = model.createProperty(ns);
          model.removeAll(r, p, null);
        }
      }
    }
    for (Resource e : getEmbeddedResources(o)) {
      model.removeAll(e, null, null);
    }
  }

  /**
   * True if resource exists, throw {@link NullPointerException} if {@link RDFResource} has a null
   * ID
   *
   * @param rdfR
   * @return
   */
  public boolean exists(Object o) {
    if (J2JHelper.getId(o) == null) {
      return false;
    }
    // Resource r = createResource(o); //This seems to be a problem, new
    // method is simpler and faster
    Resource r = model.getResource(J2JHelper.getId(o).toString());
    return model.contains(r, null);
  }

  /**
   * True if the uri exists
   *
   * @param uri
   * @return
   */
  public boolean exists(String uri) {
    if (uri == null) {
      return false;
    }
    Resource r = model.getResource(uri);
    return model.contains(r, null);
  }

  /**
   * Create a new {@link Resource} for one {@link Object}
   *
   * @param o
   * @return
   */
  private Resource createResource(Object o) {
    if (J2JHelper.hasDataType(o)) {
      Resource type = model.createResource(J2JHelper.getType(o));
      type.addProperty(RDF.type, o.getClass().getName());
      return model.createResource(J2JHelper.getId(o).toString(), type);
    } else {
      return model.createResource(J2JHelper.getId(o).toString());
    }
  }

  /**
   * Add all properties to the {@link Resource} according to the {@link RDFObject}
   *
   * @param s
   * @param o
   */
  private void addProperties2Resource(Resource s, Object o) {
    for (Field f : J2JHelper.getAllObjectFields(o.getClass())) {
      try {
        Object r = J2JHelper.getFieldAsJavaObject(f, o);
        if (r instanceof List<?>) {
          addList2Resource(s, ((List<?>) r), f);
        } else {
          addProperty(s, r, f);
        }
      } catch (Exception e) {
        throw new RuntimeException(
            "Error adding property for field " + f + " to object " + o.getClass(), e);
      }
    }
  }

  /**
   * Add a list to the {@link Resource}
   *
   * @param s
   * @param list
   */
  private void addList2Resource(Resource s, List<?> list, Field f) {
    if (!(lazy && J2JHelper.isLazyList(f))) {
      for (int i = 0; i < list.size(); i++) {
        Object listElement = list.get(i);
        if (J2JHelper.isResource(listElement) && J2JHelper.getId(listElement) == null) {
          URI ns = URI.create(J2JHelper.getResourceNamespace(listElement));
          String objectName = ns.getPath().replaceAll("/terms/", "");
          String subjectId = s.getURI();
          listElement =
              J2JHelper.setId(listElement, URI.create(subjectId + "/" + objectName + "@pos" + i));
        }
        updatePosition(listElement, i);
        addProperty(s, listElement, f);
      }
    }
  }

  /**
   * Update the position according to the current place of this list element in the list
   *
   * @param id
   * @return
   */
  private void updatePosition(Object listElement, int pos) {
    URI id = J2JHelper.getId(listElement);
    if (id != null) {
      String[] s = id.getPath().split("@pos");
      if (s.length > 1 && pos != Integer.parseInt(s[1])) {
        listElement = J2JHelper.setId(listElement, URI.create(s[0] + "@pos" + pos));
      }
    }
  }

  /**
   * Add a {@link Property} to a {@link Resource}. The {@link Property} can be either a new
   * {@link Resource} or a {@link Literal}
   *
   * @param s
   * @param obj
   */
  private void addProperty(Resource s, Object obj, Field f) {
    try {
      if (obj == null) {
        return;
      } else if (J2JHelper.isResource(obj) && J2JHelper.getId(obj) != null) {
        writeResource(s, obj);
      } else if (J2JHelper.isLiteral(f) || J2JHelper.isLazyLitereal(f)) {
        addLiteral(s, obj, f);
      } else if ((J2JHelper.isURIResource(obj, f) || J2JHelper.isLazyURIResource(obj, f))
          && obj != null) {
        addURIResource(s, obj, f);
      } else if (obj instanceof LocalizedString) {
        addLabel(s, (LocalizedString) obj);
      } else if (J2JHelper.isList(f)) {
        addLiteral(s, obj, f);
      } else {
        LOGGER.error("Not adding field " + f);
      }
    } catch (Exception e) {
      throw new RuntimeException("Error adding property", e);
    }
  }

  /**
   * Write the object (resourceObject) as a {@link Resource}
   *
   * @param s
   * @param obj
   */
  private void writeResource(Resource s, Object resourceObject) {
    if (J2JHelper.getId(resourceObject) != null) {
      Property p = model.createProperty(J2JHelper.getResourceNamespace(resourceObject));
      Resource o = createResource(resourceObject);// model.createResource(J2JHelper.getId(resourceObject).toString());
      model.add(s, p, o);
      addProperties2Resource(o, resourceObject);
    } else {
      LOGGER.warn("Can not create resource " + resourceObject + " because of id null");
    }
  }

  /**
   * Write the object (literalObject) as a literal
   *
   * @param s
   * @param literalObject
   * @param f
   */
  private void addLiteral(Resource s, Object literalObject, Field f) {
    if (literalObject != null) {
      Property p = model.createProperty(J2JHelper.getLiteralNamespace(f));
      Literal o = literalHelper.java2Literal(literalObject);
      if (o != null) {
        model.add(s, p, o);
      }
    }
  }

  /**
   * Write the object (ls) as a {@link RDFS} label
   *
   * @param s
   * @param ls
   */
  private void addLabel(Resource s, LocalizedString ls) {
    Literal o = model.createLiteral(ls.getValue(), ls.getLang());
    Property p = RDFS.label;
    if (o != null) {
      model.add(s, p, o);
    }
  }

  /**
   * Write the object (resourceURI) as {@link Resource} without any childs
   *
   * @param s
   * @param obj
   * @param f
   */
  private void addURIResource(Resource s, Object resourceURI, Field f) {
    Property p = model.createProperty(J2JHelper.getURIResourceNamespace(resourceURI, f));
    Resource o = model.createResource(resourceURI.toString());
    if (o != null) {
      model.add(s, p, o);
    }
  }

  /**
   * Get all Embedded {@link Resource} of an {@link Object} stored in {@link Jena} as a
   * {@link Resource}
   *
   * @param s - the {@link Resource}
   * @param r - {@link Object}
   * @return
   */
  private List<Resource> getEmbeddedResources(Object r) {
    List<Resource> l = new ArrayList<Resource>();
    for (Field f : J2JHelper.getAllObjectFields(r.getClass())) {
      if (!(lazy && J2JHelper.isLazyList(f))) {
        try {
          Object r2 = J2JHelper.getFieldAsJavaObject(f, r);
          if (J2JHelper.isResource(r2) && exists(r2)) {
            Resource o = model.getResource(J2JHelper.getId(r2).toString());
            l.add(o);
            l.addAll(getEmbeddedResources(r2));
          } else if (J2JHelper.isLazyList(f) || J2JHelper.isList(f)) {
            String predicate = J2JHelper.getNamespace(r2, f);
            // Get the class of the object in the list
            ParameterizedType paramType = (ParameterizedType) f.getGenericType();
            Type type = paramType.getActualTypeArguments()[0];
            boolean isListOfResources = J2JHelper.isResource((Class<?>) type);
            if (isListOfResources) {
              Resource parent = model.getResource(J2JHelper.getId(r).toString());
              // Find all child resources for this predicate: <parent> <predicate> <childs>
              for (StmtIterator iterator =
                  parent.listProperties(model.createProperty(predicate)); iterator.hasNext();) {
                Statement st = iterator.next();
                if (st.getObject().isResource()) {
                  l.add(st.getResource());
                }
              }
              // Search for other objects
              for (Object o : ((List<?>) r2)) {
                if (J2JHelper.isResource(o) && exists(o)) {
                  l.addAll(getEmbeddedResources(o));
                }
              }
            }
          }
        } catch (Exception e) {
          throw new RuntimeException("Error getting all embedded resources for " + r, e);
        }
      }
    }
    return l;
  }
}
