package de.mpg.imeji.j2j.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;

import com.hp.hpl.jena.Jena;

/**
 * For persistence of {@link List} in {@link Jena}
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface j2jList {
  String value();
}
