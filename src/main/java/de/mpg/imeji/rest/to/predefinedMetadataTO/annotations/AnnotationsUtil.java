package de.mpg.imeji.rest.to.predefinedMetadataTO.annotations;

/**
 * Utility class for Annotations
 * 
 * @author saquet
 *
 */
public class AnnotationsUtil {

  private AnnotationsUtil() {
    // Avoid Constructor
  }

  /**
   * Read the {@link MetadataTOType} value
   *
   * @param o
   * @return
   */
  public static String getType(Object o) {
    if (hasMetadataTOType(o)) {
      return o.getClass().getAnnotation(MetadataTOType.class).value();
    }
    return null;
  }

  /**
   * true if the {@link Object} has a {@link MetadataTOType} value
   *
   * @param o
   * @return
   */
  public static boolean hasMetadataTOType(Object o) {
    return o.getClass().getAnnotation(MetadataTOType.class) != null;
  }

}
