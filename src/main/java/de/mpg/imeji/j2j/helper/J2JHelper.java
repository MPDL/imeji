package de.mpg.imeji.j2j.helper;

import java.lang.reflect.Field;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import de.mpg.imeji.j2j.annotations.j2jDataType;
import de.mpg.imeji.j2j.annotations.j2jId;
import de.mpg.imeji.j2j.annotations.j2jLazyList;
import de.mpg.imeji.j2j.annotations.j2jList;
import de.mpg.imeji.j2j.annotations.j2jLiteral;
import de.mpg.imeji.j2j.annotations.j2jModel;
import de.mpg.imeji.j2j.annotations.j2jResource;

/**
 * Helper related to j2j
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public class J2JHelper {

  /**
   * private constructor
   */
  private J2JHelper() {

  }

  /**
   * Get the Id of an {@link Object} (a {@link j2jId} must be defined)
   *
   * @param o
   * @return
   */
  public static URI getId(Object o) {
    if (o == null) {
      return null;
    }
    j2jId ano = o.getClass().getAnnotation(j2jId.class);
    if (ano != null) {
      try {
        Object id = o.getClass().getMethod(ano.getMethod(), null).invoke(o, null);
        if (id != null) {
          return new URI(id.toString());
        }
      } catch (Exception e) {
        throw new RuntimeException("Error reading ID of " + o, e);
      }
    }
    return null;
  }

  /**
   * Set the Id of an {@link Object} (a {@link j2jId} must be defined)
   *
   * @param o
   * @param id
   */
  public static Object setId(Object o, URI id) {
    j2jId ano = o.getClass().getAnnotation(j2jId.class);
    Object[] args = {id};
    if (ano != null) {
      try {
        o.getClass().getMethod(ano.setMethod(), URI.class).invoke(o, args);
      } catch (Exception e) {
        throw new RuntimeException("Error setting ID of " + o, e);
      }
    }
    return o;
  }

  /**
   * Cast a {@link List} of any type to a {@link List} of {@link Object}
   *
   * @param l
   * @return
   */
  public static List<Object> cast2ObjectList(List<?> l) {
    return Arrays.asList(l.toArray());
  }

  /**
   * Read the {@link j2jDataType} value
   *
   * @param o
   * @return
   */
  public static String getType(Object o) {
    if (hasDataType(o)) {
      return o.getClass().getAnnotation(j2jDataType.class).value();
    }
    return null;
  }

  /**
   * Read the {@link j2jModel} value
   *
   * @param o
   * @return
   */
  public static String getModel(Object o) {
    return o.getClass().getAnnotation(j2jModel.class).value();
  }

  /**
   * Read the namespace of an imeji {@link Object}<br/>
   * - if a {@link Field} is passed, look for the namespace of this {@link Field}
   *
   * @param o
   * @param f
   * @return
   */
  public static String getNamespace(Object o, Field f) {
    if (isResource(o)) {
      return getResourceNamespace(o);
    } else if (isLiteral(f)) {
      return getLiteralNamespace(f);
    } else if (isURIResource(o, f)) {
      return getURIResourceNamespace(o, f);
    } else if (isList(f)) {
      return getListNamespace(f);
    } else if (isLazyList(f)) {
      return getLazyListNamespace(f);
    }
    return null;
  }

  /**
   * To use only if getNamespace(Object o, Field f) can not be use
   *
   * @param f
   * @return
   */
  public static String getNamespace(Field f) {
    if (isResource(f)) {
      String resourceNamespace = getResourceNamespace(f);
      if (resourceNamespace != null) {
        return resourceNamespace;
      }
      return f.getAnnotation(j2jResource.class).value();
    } else if (isLiteral(f)) {
      return getLiteralNamespace(f);
    } else if (isList(f)) {
      return getListNamespace(f);
    }
    return null;
  }

  /**
   * Return {@link j2jResource} value
   *
   * @param o
   * @return
   */
  public static String getResourceNamespace(Object o) {
    if (isResource(o)) {
      return o.getClass().getAnnotation(j2jResource.class).value();
    } else {
      return null;
    }
  }

  /**
   * Return the namespace of an URI object defined by a {@link j2jResource}
   *
   * @param o
   * @param f
   * @return
   */
  public static String getURIResourceNamespace(Object o, Field f) {
    if (isURIResource(o, f)) {
      if (f.getAnnotation(j2jResource.class) != null) {
        return f.getAnnotation(j2jResource.class).value();
      } else if (f.getAnnotation(j2jList.class) != null) {
        return f.getAnnotation(j2jList.class).value();
      } else if (isLazyList(f)) {
        return f.getAnnotation(j2jLazyList.class).value();
      }
    }
    return null;
  }

  /**
   * Return the namespace of an object defined by a {@link j2jLiteral}
   *
   * @param f
   * @return
   */
  public static String getLiteralNamespace(Field f) {
    if (isLiteral(f)) {
      return f.getAnnotation(j2jLiteral.class).value();
    } else if (isList(f)) {
      return getListNamespace(f);
    } else {
      return null;
    }
  }

  /**
   * Read {@link j2jList} value
   *
   * @param f
   * @return
   */
  public static String getListNamespace(Field f) {
    if (isList(f)) {
      return f.getAnnotation(j2jList.class).value();
    } else {
      return null;
    }
  }

  /**
   * Read the {@link j2jLazyList} value
   *
   * @param f
   * @return
   */
  public static String getLazyListNamespace(Field f) {
    if (isLazyList(f)) {
      return f.getAnnotation(j2jLazyList.class).value();
    } else {
      return null;
    }
  }

  /**
   * True if object is a {@link j2jResource}
   *
   * @param o
   * @return
   */
  public static boolean isResource(Object o) {
    return o != null && o.getClass().getAnnotation(j2jResource.class) != null;
  }

  /**
   * True id the {@link Field} is a {@link j2jResource}
   *
   * @param f
   * @return
   */
  public static boolean isResource(Field f) {
    return f != null && f.getAnnotation(j2jResource.class) != null;
  }

  /**
   * True if the {@link Field} is a {@link j2jResource} and and {@link URI}
   *
   * @param o
   * @param f
   * @return
   */
  public static boolean isURIResource(Object o, Field f) {
    return f != null && (f.getType().equals(URI.class) && f.getAnnotation(j2jResource.class) != null
        || ((isList(f) || isLazyList(f)) && o instanceof URI));
  }

  /**
   * True if the {@link Field} is a {@link j2jLiteral}
   *
   * @param f
   * @return
   */
  public static boolean isLiteral(Field f) {
    return f != null && f.getAnnotation(j2jLiteral.class) != null;
  }

  /**
   * True if the {@link Field} is a {@link j2jList}
   *
   * @param f
   * @return
   */
  public static boolean isList(Field f) {
    return f != null && f.getAnnotation(j2jList.class) != null;
  }

  /**
   * True if the {@link Field} is a {@link j2jLazyList}
   *
   * @param f
   * @return
   */
  public static boolean isLazyList(Field f) {
    return f != null && f.getAnnotation(j2jLazyList.class) != null;
  }

  /**
   * true if the {@link Object} has a {@link j2jDataType} value
   *
   * @param o
   * @return
   */
  public static boolean hasDataType(Object o) {
    return o.getClass().getAnnotation(j2jDataType.class) != null;
  }

  /**
   * Return a {@link Field} as a {@link Object} of the same {@link Class} as the {@link Object}
   * passed as parameter
   *
   * @param f
   * @param o
   * @return
   */
  public static Object getFieldAsJavaObject(Field f, Object o) {
    f.setAccessible(true);
    try {
      Object object = f.get(o);
      if (object == null) {
        object = f.getType().newInstance();
      }
      return object;
    } catch (Exception e) {
      return null;
    }
  }

  /**
   * Return all the Fields that should be written as rdf property If class is not inheriting from a
   * {@link RDFObject} then return empty list.
   *
   * @param clazz
   * @return
   */
  public static List<Field> getAllObjectFields(Class<?> clazz) {
    if (clazz.getAnnotation(j2jResource.class) != null) {
      List<Field> l = new ArrayList<Field>(Arrays.asList(clazz.getDeclaredFields()));
      if (clazz.getSuperclass() != null
          && clazz.getSuperclass().getAnnotation(j2jResource.class) != null) {
        l.addAll(getAllObjectFields(clazz.getSuperclass()));
      }
      return getOnlyAnnotatedFields(l);
    }
    return new ArrayList<Field>();
  }

  /**
   * Return only {@link Field} which have a j2j Annotation
   *
   * @param l
   * @return
   */
  public static List<Field> getOnlyAnnotatedFields(List<Field> l) {
    List<Field> afl = new ArrayList<Field>();
    for (Field f : l) {
      if (isAnnotated(f)) {
        afl.add(f);
      }
    }
    return afl;
  }

  /**
   * True if the {@link Field} is a j2j annotation
   *
   * @param f
   * @return
   */
  public static boolean isAnnotated(Field f) {
    return isLazyList(f) || isList(f) || isLiteral(f) || isResource(f);
  }
}
