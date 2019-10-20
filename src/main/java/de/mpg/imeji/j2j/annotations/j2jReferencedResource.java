package de.mpg.imeji.j2j.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * This annotation can be used for reading (literal) values of a given resource from Jena.
 * 
 * If you have a field in a {@link de.mpg.imeji.logic.model} class and you would like to read the
 * value of a certain resource literal to this field, you can specify (1) a field that holds the URI
 * of the targeted resource (this field must be declared before the field on which you use this
 * annotation) (2) a field that will be used to store the read (literal) value of the targeted
 * resource
 * 
 * Example: In the class "LinkedCollection" with fields - String linkedCollectionUriString and -
 * String linkedCollectionName I would like to to read the name the collection whose URI stored in
 * linkedCollectionUriString into nameOfLinkedCollection.
 * 
 * I can write:
 * 
 * @j2jLiteral("http://imeji.org/terms/uri") private String linkedCollectionUriString;
 * 
 * @j2jReferencedResource(referencedClass = "de.mpg.imeji.logic.model.CollectionImeji",
 *                                        referencedResourceUri = "linkedCollectionUriString",
 *                                        referencedField = "title") private String
 *                                        linkedCollectionName;
 * 
 * 
 *                                        Full example {@see LinkedCollection}
 * 
 * @author breddin
 *
 */

@Retention(RetentionPolicy.RUNTIME)
public @interface j2jReferencedResource {


  /**
   * Name of the class of the referenced resource
   * 
   * @return
   */
  String referencedClass();

  /**
   * Name of the field in my class that stores the URI of the resource whose field I want to read.
   * 
   * @return
   */
  String referencedResourceUri();



  /**
   * Name of the field that I want to read.
   * 
   * @return
   */
  String referencedField();
}
