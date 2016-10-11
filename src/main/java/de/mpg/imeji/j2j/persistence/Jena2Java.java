package de.mpg.imeji.j2j.persistence;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;

import de.mpg.imeji.j2j.annotations.j2jId;
import de.mpg.imeji.j2j.annotations.j2jResource;
import de.mpg.imeji.j2j.helper.J2JHelper;
import de.mpg.imeji.j2j.helper.LiteralHelper;
import de.mpg.imeji.util.LocalizedString;

/**
 * Class to load a {@link Resource} into a java object. This java object must have been annotated
 * with j2j annotations
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public class Jena2Java {
  private Model model;
  private boolean lazy = false;
  private static Logger LOGGER = Logger.getLogger(Jena2Java.class);

  public Jena2Java(Model model, boolean lazy) {
    this.model = model;
    this.lazy = lazy;
  }

  /**
   * Load a {@link Resource} into a Java Object. The Java Object must define a {@link j2jResource}
   * and have and id non null (defined by {@link j2jId})
   *
   * @param item : The uri of the resource
   * @param o : The java Object in which the resource should be written
   * @return
   */
  public Object loadResource(Object o) {
    if (J2JHelper.getId(o) == null) {
      throw new NullPointerException("Fatal error: Resource " + o + " with a null id");
    }
    return loadResourceFields(o);
  }

  /**
   * Load all {@link Field} defined in the Object. The Object must be a {@link Resource} (defined in
   * Java by {@link j2jResource})
   *
   * @param javaObject
   * @return
   */
  private Object loadResourceFields(Object javaObject) {
    Resource subject = model.getResource(J2JHelper.getId(javaObject).toString());
    if (J2JHelper.hasDataType(javaObject) && isTypedResource(subject)) {
      javaObject = createJavaObjectFromDataType(subject);
    }
    for (Field f : J2JHelper.getAllObjectFields(javaObject.getClass())) {
      Object object = J2JHelper.getFieldAsJavaObject(f, javaObject);
      object = loadObject(subject, f, object, 0, null);
      setField(javaObject, f, object);
    }
    return javaObject;
  }

  /**
   * Load the Object of a Triple. The {@link Field} defines the relation between the subject and the
   * object (i.e. the field contains predicate information)
   *
   * @param subject
   * @param f
   * @param object
   * @param position
   * @return
   */
  private Object loadObject(Resource subject, Field f, Object object, int position, Statement st) {
    if (J2JHelper.isResource(object)) {
      object = loadResourceObject(subject, object, position);
    } else if (J2JHelper.isLiteral(f)) {
      object = loadObjectLiteral(f, subject, object, position, st);
    } else if (J2JHelper.isURIResource(object, f)) {
      object = readURIResource(f, subject, object, position);
    } else if (object instanceof List<?>) {
      object = readList(f, subject);
    } else if (object instanceof LocalizedString) {
      object = readLocalizedString(f, subject, object, position, st);
    } else if (J2JHelper.isList(f) && J2JHelper.getLiteralNamespace(f) != null) {
      // field of a list which has not been handle so for (i.e not an URI or not a localized String)
      object = loadObjectLiteral(f, subject, object, position, st);
    }
    return object;
  }

  /**
   * Set object into the {@link Field} of the subject
   *
   * @param subject
   * @param f
   * @param object
   * @return
   */
  private Object setField(Object subject, Field f, Object object) {
    try {
      f.setAccessible(true);
      object = LiteralHelper.jenaTypeToJ2jType(object);
      if (object != null) {
        f.set(subject, object);
      }
      return subject;
    } catch (Exception e) {
      throw new RuntimeException("Error writing " + object + " to " + subject + " for field " + f,
          e);
    }
  }

  /**
   * Load the object the the statement (subject - predicate - object). The object must be a
   * {@link Resource}. The position defines which object should be loaded in case of many statement
   * with the same subject and predicate.
   *
   * @param subject
   * @param object
   * @param position
   * @return
   */
  private Object loadResourceObject(Resource subject, Object object, int position) {
    String predicate = J2JHelper.getResourceNamespace(object);
    Statement st = getStatement(subject, predicate, position);
    if (st != null) {
      URI uri = URI.create(st.getObject().toString());
      J2JHelper.setId(object, uri);
      object = loadResource(object);
    }
    return object;
  }

  /**
   * Load the object the the statement (subject - predicate - object). The object must be a
   * {@link Literal}. The position defines which object should be loaded in case of many statement
   * with the same subject and predicate.
   *
   * @param f
   * @param subject
   * @param object
   * @param position
   * @return
   */
  private Object loadObjectLiteral(Field f, Resource subject, Object object, int position,
      Statement statement) {
    String predicate = J2JHelper.getLiteralNamespace(f);
    Statement st = statement == null ? getStatement(subject, predicate, position) : statement;
    if (st != null) {
      object = LiteralHelper.jenaTypeToJ2jType(st.getLiteral().getValue());
    }
    return object;
  }

  /**
   * Load the object the the statement (subject - predicate - object). The object must be a
   * {@link j2jResource} and an {@link URI}. The position defines which object should be loaded in
   * case of many statement with the same subject and predicate.
   *
   * @param f
   * @param subject
   * @param object
   * @param position
   * @return
   */
  private Object readURIResource(Field f, Resource subject, Object object, int position) {
    String predicate = J2JHelper.getURIResourceNamespace(object, f);
    Statement st = getStatement(subject, predicate, position);
    if (st != null) {
      object = URI.create(st.getObject().toString());
    }
    return object;
  }

  /**
   * Load the object the the statement (subject - predicate - object). The object must be a
   * {@link LocalizedString}. The position defines which object should be loaded in case of many
   * statement with the same subject and predicate.
   *
   * @param f
   * @param subject
   * @param object
   * @param position
   * @return
   */
  private Object readLocalizedString(Field f, Resource subject, Object object, int position,
      Statement statement) {
    Statement st =
        statement == null ? getStatement(subject, RDFS.label.getURI(), position) : statement;
    LocalizedString ls =
        new LocalizedString(st.getObject().asLiteral().getValue().toString(), st.getLanguage());
    return ls;
  }

  /**
   * Load the object the the statement (subject - predicate - object). The object must be a
   * {@link List} The position defines which object should be loaded in case of many statement with
   * the same subject and predicate.
   *
   * @param f
   * @param subject
   * @return
   */
  private Object readList(Field f, Resource subject) {
    List<Object> object = new ArrayList<Object>();
    String predicate = J2JHelper.getListNamespace(f);
    if (predicate == null && !lazy) {
      predicate = J2JHelper.getLazyListNamespace(f);
    }
    if (predicate == null) {
      return object;
    }
    int count = 0;
    for (StmtIterator iterator = subject.listProperties(model.createProperty(predicate)); iterator
        .hasNext();) {
      Statement st = iterator.nextStatement();
      Object listObject = null;
      if (st.getObject().isResource() && isTypedResource(st.getResource())) {
        listObject = createJavaObjectFromDataType(st.getResource());
      } else {
        listObject = createJavaObjectForListElements(f);
      }
      if (listObject != null) {
        if (J2JHelper.isResource(listObject)) {
          J2JHelper.setId(listObject, URI.create(st.getResource().toString()));
          Object o = loadResource(listObject);
          object.add(o);
        } else {
          Object o = loadObject(st.getSubject(), f, listObject, count, st);
          object.add(o);
          count++;
        }
      }
    }
    return object;
  }

  /**
   * True if the {@link Resource} is a {@link RDF}.type. This type must define the Java class of the
   * resource,
   *
   * @param r
   * @return
   */
  private boolean isTypedResource(Resource r) {
    Statement type = r.getProperty(RDF.type);
    return type != null && type.getProperty(RDF.type).getObject().isLiteral();
  }

  /**
   * Create an {@link Object} of the type defined in the {@link RDF}.type value defined for this
   * {@link Resource}
   *
   * @param r
   * @return
   */
  private Object createJavaObjectFromDataType(Resource r) {
    Statement statementType = r.getProperty(RDF.type);
    String clazz = statementType.getResource().getProperty(RDF.type).getString();
    try {
      Object o = this.getClass().getClassLoader().loadClass(clazz).newInstance();
      J2JHelper.setId(o, URI.create(r.getURI()));
      return o;
    } catch (Exception e) {
      LOGGER.info("Error initializing resource with a datatype: ", e);
    }
    return null;
  }

  /**
   * Load the full statement defined by one subject and one predicate for a specific position (if
   * many statements with same subject and predicate)
   *
   * @param subject
   * @param predicateUri
   * @param position
   * @return
   */
  private Statement getStatement(Resource subject, String predicateUri, int position) {
    int count = 0;
    for (StmtIterator iterator =
        subject.listProperties(model.createProperty(predicateUri)); iterator.hasNext();) {
      Statement st = iterator.next();
      if (position == count) {
        return st;
      }
      count++;
    }
    return null;
  }

  /**
   * Return an object instance contained in a List
   *
   * @param f
   * @return
   */
  private Object createJavaObjectForListElements(Field f) {
    Type genericFieldType = f.getGenericType();
    if (genericFieldType instanceof ParameterizedType) {
      ParameterizedType aType = (ParameterizedType) genericFieldType;
      Type[] fieldArgTypes = aType.getActualTypeArguments();
      for (Type fieldArgType : fieldArgTypes) {
        Class<?> fieldArgClass = (Class<?>) fieldArgType;
        try {
          if (fieldArgClass == URI.class) {
            return URI.create("");
          }
          return fieldArgClass.newInstance();
        } catch (Exception e) {
          throw new RuntimeException("Error initializing " + fieldArgClass);
        }
      }
    }
    return null;
  }
}
