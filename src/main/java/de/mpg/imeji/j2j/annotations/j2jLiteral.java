package de.mpg.imeji.j2j.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import com.hp.hpl.jena.rdf.model.Literal;

/**
 * j2j {@link Literal}
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface j2jLiteral {
  public String value();
}
