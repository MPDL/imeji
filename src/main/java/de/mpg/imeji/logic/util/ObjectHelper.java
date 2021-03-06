package de.mpg.imeji.logic.util;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.net.URI;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import de.mpg.imeji.j2j.annotations.j2jModel;
import de.mpg.imeji.j2j.annotations.j2jResource;
import de.mpg.imeji.logic.model.CollectionImeji;
import de.mpg.imeji.logic.model.Item;
import de.mpg.imeji.logic.model.Statement;
import de.mpg.imeji.logic.model.User;
import de.mpg.imeji.logic.model.UserGroup;
import de.mpg.imeji.logic.search.facet.model.Facet;

/**
 * Helper for imeji {@link Object}
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public class ObjectHelper {

  private static final Logger LOGGER = LogManager.getLogger(ObjectHelper.class);
  public static String baseUri;

  public enum ObjectType {
    COLLECTION,
    ITEM,
    SYSTEM;
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
    try {
      return uri.toString().substring(uri.toString().lastIndexOf("/"), uri.toString().length()).replace("/", "");
    } catch (Exception e) {
      return uri.toString();
    }

  }

  /**
   * Parse an object URI to get its {@link ObjectType}
   *
   * @param uri
   * @return
   */
  public static ObjectType getObjectType(URI uri) {
    final String path = uri.getPath();
    if (uri.toString().equals(baseUri)) {
      return ObjectType.SYSTEM;
    }
    for (final ObjectType type : ObjectType.values()) {
      if (path.contains("/" + type.name().toLowerCase())) {
        return type;
      }
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
          if (f1.getName().equals(f2.getName()) && f1.getType().equals(f2.getType()) && !f2.toGenericString().contains("final")) {
            f2.set(obj2, f1.get(obj1));
          }
        }
      } catch (final Exception e) {
        LOGGER.error("copyAllFields issue", e);
      }
    }
  }

  /**
   * Function returns the GUI label of the given object, i.e. "Collection" for a CollectionImeji
   * object. See src/main/resources/labels_*.properties for available GUI labels.
   * 
   * @param object
   * @return label for object, base line: "Object"
   */
  public static String getGUILabel(Object object) {

    if (object instanceof CollectionImeji) {
      return "COLLECTION";
    } else if (object instanceof User) {
      return "user";
    } else if (object instanceof Item) {
      return "image";
    } else if (object instanceof UserGroup) {
      return "admin_userGroup";
    } else if (object instanceof Statement) {
      return "statement";
    } else if (object instanceof Facet) {
      return "facet";
    } else {
      return "object";
    }
  }

}
