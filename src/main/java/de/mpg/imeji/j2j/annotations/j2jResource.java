package de.mpg.imeji.j2j.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import com.hp.hpl.jena.rdf.model.Resource;

/**
 * j2j {@link Resource}
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface j2jResource {
  /**
   * The namespace of the resource
   *
   * @return
   */
  public String value();
}
