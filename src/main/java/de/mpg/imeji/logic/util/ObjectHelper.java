package de.mpg.imeji.logic.util;


import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import de.mpg.imeji.j2j.annotations.j2jModel;
import de.mpg.imeji.j2j.annotations.j2jResource;
import de.mpg.imeji.logic.config.Imeji;

/**
 * Helper for imeji {@link Object}
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public class ObjectHelper {

  private static final Logger LOGGER = Logger.getLogger(ObjectHelper.class);
  public static String baseUri;

  public enum ObjectType {
    COLLECTION, ITEM, ALBUM, PROFILE, SYSTEM;
  }

  /**
   * Private constructor
   */
  private ObjectHelper() {
    // avoid to create it
  }

  /**
   * Ensure that the {@link URI} uses the correct base uri (see property
   * imeji.jena.resource.base_uri)
   *
   * @param c
   * @param uri
   * @return
   */
  public static URI normalizeURI(Class<?> c, URI uri) {
    return getURI(c, getId(uri));
  }

  /**
   * Get the {@link URI} of {@link Object} according to its {@link Class} and the id (not uri)
   *
   * @param o
   * @return
   */
  public static URI getURI(Class<?> c, String id) {
    String baseURI = baseUri;
    final j2jModel modelName = c.getAnnotation(j2jModel.class);
    if (modelName != null) {
      baseURI = StringHelper.normalizeURI(baseURI + modelName.value());
    } else {
      baseURI = StringHelper.normalizeURI(c.getAnnotation(j2jResource.class).value());
    }
    String encodedId = id;
    try {
      encodedId = URLEncoder.encode(id, "UTF-8");
    } catch (final UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    }
    return URI.create(baseURI + encodedId);
  }

  /**
   * Extract the id number from the object uri
   *
   * @param uri
   * @return
   */
  public static String getId(URI uri) {
    final Pattern p = Pattern.compile("(.*)/(\\d+)");
    final Matcher m = p.matcher(uri.getPath());
    if (m.matches()) {
      return m.group(2);
    }
    return uri.toString().substring(uri.toString().lastIndexOf("/"), uri.toString().length())
        .replace("/", "");
  }

  /**
   * Parse an object URI to get its {@link ObjectType}
   *
   * @param uri
   * @return
   */
  public static ObjectType getObjectType(URI uri) {
    final String path = uri.getPath();
    if (uri.toString().equals(Imeji.PROPERTIES.getBaseURI())) {
      return ObjectType.SYSTEM;
    }
    for (final ObjectType type : ObjectType.values()) {
      if (path.contains("/" + type.name().toLowerCase())) {
        return type;
      }
    }
    if (StringUtils.containsIgnoreCase(path, ObjectType.PROFILE.name())) {
      return ObjectType.PROFILE;
    }
    return null;
  }

  /**
   * Return Fields of this class (excluding superclass fields)
   *
   * @param cl
   * @return
   */
  public static List<Field> getObjectFields(Class<?> cl) {
    final List<Field> fields = new ArrayList<Field>();
    for (final Field f : cl.getDeclaredFields()) {
      fields.add(f);
    }
    return fields;
  }

  /**
   * Returns all Field of a class, including those of superclass.
   *
   * @param cl
   * @return
   */
  public static List<Field> getAllObjectFields(Class<?> cl) {
    final List<Field> fields = getObjectFields(cl);
    if (cl.getSuperclass() != null) {
      fields.addAll(getAllObjectFields(cl.getSuperclass()));
    }
    return fields;
  }

  /**
   * Copy {@link Field} from obj1 to obj2. Only Fields with same name and same type are copied.
   * Fields from superclass are not copied.
   *
   * @param obj1
   * @param obj2
   */
  public static void copyFields(Object obj1, Object obj2) {
    for (final Field f2 : getObjectFields(obj2.getClass())) {
      try {
        f2.setAccessible(true);
        for (final Field f1 : getObjectFields(obj1.getClass())) {
          f1.setAccessible(true);
          if (f1.getName().equals(f2.getName()) && f1.getType().equals(f2.getType())) {
            f2.set(obj2, f1.get(obj1));
          }
        }
      } catch (final Exception e) {
        LOGGER.error("CopyFields issue", e);
      }
    }
  }

  /**
   * Copy {@link Field} from obj1 to obj2. Only Fields with same name and same type are copied.
   * Fields from superclass are copied.
   *
   * @param obj1
   * @param obj2
   */
  public static void copyAllFields(Object obj1, Object obj2) {
    for (final Field f2 : getAllObjectFields(obj2.getClass())) {
      try {
        f2.setAccessible(true);
        for (final Field f1 : getAllObjectFields(obj1.getClass())) {
          f1.setAccessible(true);
          if (f1.getName().equals(f2.getName()) && f1.getType().equals(f2.getType())
              && !f2.toGenericString().contains("final")) {
            f2.set(obj2, f1.get(obj1));
          }
        }
      } catch (final Exception e) {
        LOGGER.error("copyAllFields issue", e);
      }
    }
  }

  /**
   * Transfer a value from {@code obj1} to {@code obj2}. Value wil be get with the {@code getter}
   * method and set with {@code setter} method. Type of the transferred value should be same for
   * {@code obj1} and {@code obj2}
   *
   * @param obj1
   * @param obj2
   *
   */
  public static void transferField(String getter, Object obj1, String setter, Object obj2) {
    if (obj1 == null || obj2 == null || StringHelper.isNullOrEmptyTrim(getter)
        || StringHelper.isNullOrEmptyTrim(setter)) {
      return;
    }
    final Class fromClass = obj1.getClass();
    final Class toClass = obj2.getClass();
    try {

      Method getterMethod = null;
      Method setterMethod = null;
      for (final Method m : fromClass.getMethods()) {
        if (m.getName().equalsIgnoreCase(getter)) {
          getterMethod = m;
          break;
        }
      }
      for (final Method m : toClass.getMethods()) {
        if (m.getName().equalsIgnoreCase(setter)) {
          setterMethod = m;
          break;
        }
      }
      if (getterMethod != null && setterMethod != null) {
        Object val = null;
        val = getterMethod.invoke(obj1);
        if (val != null) {
          setterMethod.invoke(obj2, val);
        }
      }
    } catch (final InvocationTargetException e) {
      LOGGER.error("Invocation Target in transfer Fields", e);
    } catch (final IllegalAccessException e) {
      LOGGER.error("Illegal Access in transfer fields", e);
    }

  }
}
